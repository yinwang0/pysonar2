package org.yinwang.pysonar;

import org.yinwang.pysonar.ast.*;
import org.yinwang.pysonar.types.FunType;
import org.yinwang.pysonar.types.ModuleType;
import org.yinwang.pysonar.types.Type;
import org.yinwang.pysonar.types.UnionType;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Indexes a set of Python files and builds a code graph. <p>
 * This class is not thread-safe.
 */
public class Indexer {

    /**
     * The global indexer instance.  Provides convenient access to global
     * resources, as well as easy cleanup of resources after the index is built.
     */
    public static Indexer idx;

    /**
     * A scope containing bindings for all modules currently loaded by the indexer.
     */
    public Scope moduleTable = new Scope(null, Scope.ScopeType.GLOBAL);

    /**
     * The top-level (builtin) scope.
     */
    public Scope globaltable = new Scope(null, Scope.ScopeType.GLOBAL);

    /**
     * A map of all bindings created, keyed on their qnames.
     */
    public Map<String, List<Binding>> allBindings = new HashMap<String, List<Binding>>();

    /**
     * A map of references to their referenced bindings.  Most nodes will refer
     * to a single binding, and few ever refer to more than two.  One situation
     * in which a multiple reference can occur is the an attribute of a union
     * type.  For instance:
     *
     * <pre>
     *   class A:
     *     def foo(self): pass
     *   class B:
     *     def foo(self): pass
     *   if some_condition:
     *     var = A()
     *   else
     *     var = B()
     *   var.foo()  # foo here refers to both A.foo and B.foo
     * <pre>
     */
    private Map<Ref, List<Binding>> references = new HashMap<Ref, List<Binding>>();

    /**
     * Diagnostics: map from file names to Diagnostics
     */
    public Map<String, List<Diagnostic>> semanticErrors = new HashMap<String, List<Diagnostic>>();
    public Map<String, List<Diagnostic>> parseErrors = new HashMap<String, List<Diagnostic>>();

    public String cwd = null;
    public int nCalled = 0;

    public List<String> path = new ArrayList<String>();
    private Set<FunType> uncalled = new HashSet<FunType>();
    private Set<Object> callStack = new HashSet<Object>();

    private int threadCounter = 0;
    public int newThread() {
        threadCounter++;
        return threadCounter;
    }

    /**
     * Manages a store of serialized ASTs. Parsing is one of the slower and
     * more expensive phases of indexing; reusing parse trees can help with resource
     * utilization when indexing several projects (or re-indexing one project).
     */
    private AstCache astCache;

    /**
     * When resolving imports we look in various possible locations.
     * This set keeps track of modules we attempted but didn't find.
     */
    public Set<String> failedModules = new HashSet<String>();
    public Set<String> failedToParse = new HashSet<String>();

    /**
     * Manages the built-in modules -- that is, modules from the standard Python
     * library that are implemented in C and consequently have no Python source.
     */
    public Builtins builtins;

    public int nLoadedFiles = 0;

    private Logger logger;
    private Progress progress;

    public Indexer() {
    	progress = new Progress(10, 100);
    	logger = Logger.getLogger(Indexer.class.getCanonicalName());
        idx = this;
        builtins = new Builtins();
        builtins.init();
    }


    public void setCWD(String cd) throws IOException {
        cwd = Util.canonicalize(cd);
    }


    public void addPaths(List<String> p) throws IOException {
        for (String s : p) {
            addPath(s);
        }
    }


    public void addPath(String p) throws IOException {
        path.add(Util.canonicalize(p));
    }


    public void setPath(List<String> path) throws IOException {
        this.path = new ArrayList<String>(path.size());
        addPaths(path);
    }


    /**
     * Returns the module search path -- the project directory followed by any
     * paths that were added by {@link addPath}.
     */
    public List<String> getLoadPath() {
        List<String> loadPath = new ArrayList<String>();
        if (cwd != null) {
            loadPath.add(cwd);
        }
        loadPath.addAll(path);
        return loadPath;
    }

    public boolean inStack(Object f) {
    	return callStack.contains(f);
    }

    public void pushStack(Object f) {
    	callStack.add(f);
    }

    public void popStack(Object f) {
    	callStack.remove(f);
    }

    /**
     * Returns the mutable set of all bindings collected, keyed on their qnames.
     */
    public Map<String, List<Binding>> getBindings() {
        return allBindings;
    }

    public void addBinding(String qname, Binding b) {
        List<Binding> lb = allBindings.get(qname);
        if (lb == null) {
            lb = new ArrayList<Binding>();
            lb.add(b);
            allBindings.put(qname, lb);
        } else {
            lb.add(b);
        }
    }


    ModuleType getCachedModule(String file) {
        Type t = moduleTable.lookupType(file);
        if (t == null) {
            return null;
        } else if (t.isUnionType()) {
            for (Type tt : t.asUnionType().getTypes()) {
                if (tt.isModuleType()) {
                    return (ModuleType)tt;
                }
            }
            return null;
        } else if (t.isModuleType()){
            return (ModuleType)t;
        } else {
            return null;
        }
    }

    /**
     * Returns (loading/resolving if necessary) the module for a given source path.
     * @param file absolute file path
     */
    public ModuleType getModuleForFile(String file) throws Exception {
        if (failedModules.contains(file)) {
            return null;
        }
        ModuleType m = getCachedModule(file);
        if (m != null) {
            return m;
        }
        return loadFile(file);
    }

    /**
     * Returns the list, possibly empty but never {@code null}, of
     * errors and warnings generated in the file.
     */
    public List<Diagnostic> getDiagnosticsForFile(String file) {
        List<Diagnostic> errs = semanticErrors.get(file);
        if (errs != null) {
            return errs;
        }
        return new ArrayList<Diagnostic>();
    }

    /**
     * Create an outline for a file in the index.
     * @param path the file for which to build the outline
     * @return a list of entries constituting the file outline.
     * Returns an empty list if the indexer hasn't indexed that path.
     */
    public List<Outliner.Entry> generateOutline(String file) throws Exception {
        return new Outliner().generate(this, file);
    }

    /**
     * Add a reference to binding {@code b} at AST node {@code node}.
     * @param node a node referring to a name binding.  Typically a
     * {@link org.yinwang.pysonar.ast.Name}, {@link org.yinwang.pysonar.ast.Str} or {@link org.yinwang.pysonar.ast.Url}.
     */
    public void putLocation(Node node, Binding b) {
        if (node == null || node instanceof Url || b == null) {
            return;
        }
        Ref ref = new Ref(node);
        List<Binding> bindings = references.get(ref);
        if (bindings == null) {
            // The indexer is heavily memory-constrained, so we need small overhead.
            // Empirically using a capacity-1 ArrayList for the binding set
            // uses about 1/2 the memory of a LinkedList, and 1/4 the memory
            // of a default HashSet.
            bindings = new ArrayList<Binding>(1);
            references.put(ref, bindings);
        }
        if (!bindings.contains(b)) {
            bindings.add(b);
        }
        b.addRef(ref);
    }

    public Map<Ref, List<Binding>> getReferences() {
        return references;
    }

    public List<Binding> lookupReference(Ref r) {
        return references.get(r);
    }

    public void removeBinding(Binding b) {
        for (List<Binding> bs: allBindings.values()) {
            bs.remove(b);
        }
    }

    /*
     * Multiple bindings can exist for the same qname, but they should appear at
     * different locations. Otherwise they are considered the same by the equals
     * method of NBinding and their types will be the union of all types that
     * appear at the same location.
     */
    public Binding putBinding(Binding b) {
        if (b == null) {
            throw new IllegalArgumentException("null binding arg");
        }
        String qname = b.getQname();
        if (qname == null || qname.isEmpty()) {
            throw new IllegalArgumentException("Null/empty qname: " + b);
        }
        Binding existing = findBinding(b);
        if (existing == null) {
            addBinding(qname, b);
            return b;
        } else {
            existing.setType(UnionType.union(existing.getType(), b.getType()));
            return existing;
        }
    }


    private Binding findBinding(Binding b) {
        List<Binding> existing = allBindings.get(b.getQname());
        if (existing != null) {
            for (Binding eb : existing) {
                if (eb.equals(b)) {
                    return eb;
                }
            }
        }
        return null;
    }

    public void putProblem(Node loc, String msg) {
        String file;
        if (loc != null && ((file = loc.getFile()) != null)) {
            addFileErr(file, loc.start, loc.end, msg);
        }
    }

    public void putProblem(String file, int beg, int end, String msg) {
        if (file != null) {
            addFileErr(file, beg, end, msg);
        }
    }

    void addFileErr(String file, int beg, int end, String msg) {
        List<Diagnostic> msgs = getFileErrs(file, semanticErrors);
        msgs.add(new Diagnostic(file, Diagnostic.Type.ERROR, beg, end, msg));
    }

    List<Diagnostic> getParseErrs(String file) {
        return getFileErrs(file, parseErrors);
    }

    List<Diagnostic> getFileErrs(String file, Map<String, List<Diagnostic>> map) {
        List<Diagnostic> msgs = map.get(file);
        if (msgs == null) {
            msgs = new ArrayList<Diagnostic>();
            map.put(file, msgs);
        }
        return msgs;
    }

    /**
     * Loads a module from a string containing the module contents.
     * Idempotent:  looks in the module cache first. Used for simple unit tests.
     * @param path a path for reporting/caching purposes.  The filename
     *        component is used to construct the module qualified name.
     */
    public ModuleType loadString(String path, String contents) throws Exception {
        ModuleType module = getCachedModule(path);
        if (module != null) {
            finer("\nusing cached module " + path + " [succeeded]");
            return module;
        }
        return parseAndResolve(path, contents);
    }

    /**
     * Load, parse and analyze a source file given its absolute path.
     * By default, loads the entire ancestor package chain.
     *
     * @param path the absolute path to the file or directory.
     *        If it is a directory, it is suffixed with "__init__.py", and
     *        only that file is loaded from the directory.
     *
     * @return {@code null} if {@code path} could not be loaded
     */
    public ModuleType loadFile(String path) throws Exception {
        File f = new File(path);
        if (f.isDirectory()) {
            finer("\n    loading init file from directory: " + path);
            f = Util.joinPath(path, "__init__.py");
            path = f.getAbsolutePath();
        }

        if (!f.canRead()) {
            finer("\nfile not not found or cannot be read: " + path);
            return null;
        }

        ModuleType module = getCachedModule(path);
        if (module != null) {
            finer("\nusing cached module " + path + " [succeeded]");
            return module;
        }

        loadParentPackage(path);
        return parseAndResolve(path);
    }

    /**
     * When we load a module, load all its parent packages, top-down.
     * This is in part because Python does it anyway, and in part so that you
     * can click on all parent package components in import statements.
     * We load whole ancestor chain top-down, as does Python.
     */
    private void loadParentPackage(String file) throws Exception {
        File f = new File(file);
        File parent = f.getParentFile();
        if (parent == null || isInLoadPath(parent)) {
            return;
        }
        // the parent package of an __init__.py file is the grandparent dir
        if (f.isFile() && "__init__.py".equals(f.getName())) {
            parent = parent.getParentFile();
        }
        if (parent == null || isInLoadPath(parent)) {
            return;
        }
        File initpy = Util.joinPath(parent, "__init__.py");
        if (!(initpy.isFile() && initpy.canRead())) {
            return;
        }
        loadFile(initpy.getPath());
    }

    private boolean isInLoadPath(File dir) {
        for (String s : getLoadPath()) {
            if (new File(s).equals(dir)) {
                return true;
            }
        }
        return false;
    }

    private ModuleType parseAndResolve(String file) throws Exception {
    	finer("Indexing: " + file);
    	progress.tick();
        return parseAndResolve(file, null);
    }

    /**
     * Parse a file or string and return its module parse tree.
     * @param file the filename
     * @param contents optional file contents.  If {@code null}, loads the
     *        file contents from disk.
     */
    @SuppressWarnings("unchecked")
    private ModuleType parseAndResolve(String file, String contents) throws Exception {
        // Avoid infinite recursion if any caller forgets this check.  (Has happened.)
        ModuleType cached = (ModuleType)moduleTable.lookupType(file);
        if (cached != null) {
            return cached;
        }

        try {
            Module ast;
            if (contents != null) {
                ast = getAstForFile(file, contents);
            } else {
                ast = getAstForFile(file);
            }
            if (ast == null) {
                failedModules.add(file);
                return null;
            } else {
                finer("resolving: " + file);
                ModuleType mod = (ModuleType)ast.resolve(moduleTable, 0);
                finer("[success]");
                nLoadedFiles++;
                return mod;
            }
        } catch (OutOfMemoryError e) {
            if (astCache != null) {
                astCache.clear();
            }
            System.gc();
            return null;
        }
    }

    private AstCache getAstCache() throws Exception {
        if (astCache == null) {
            astCache = AstCache.get();
            astCache.clearDiskCache();
        }
        return astCache;
    }

    /**
     * Returns the syntax tree for {@code file}. <p>
     */
    public Module getAstForFile(String file) throws Exception {
        return getAstCache().getAST(file);
    }

    /**
     * Returns the syntax tree for {@code file}. <p>
     */
    public Module getAstForFile(String file, String contents) throws Exception {
        return getAstCache().getAST(file, contents);
    }

    public ModuleType getBuiltinModule(String qname) throws Exception {
        return builtins.get(qname);
    }

    /**
     * This method searches the module path for the module {@code modname}.
     * If found, it is passed to {@link #loadFile}.
     *
     * <p>The mechanisms for importing modules are in general statically
     * undecidable.  We make a reasonable effort to follow the most common
     * lookup rules.
     *
     * @param modname a module name.   Can be a relative path to a directory
     *        or a file (without the extension) or a possibly-qualified
     *        module name.  If it is a module name, cannot contain leading dots.
     *
     * @see http://docs.python.org/reference/simple_stmts.html#the-import-statement
     */
    public ModuleType loadModule(String modname) throws Exception {
        if (failedModules.contains(modname)) {
            return null;
        }

        ModuleType cached = getCachedModule(modname); // builtin file-less modules
        if (cached != null) {
            finer("using cached module " + modname);
            return cached;
        }

        ModuleType mt = getBuiltinModule(modname);
        if (mt != null) {
            return mt;
        }

        finer("looking for module " + modname);

        if (modname.endsWith(".py")) {
            modname = modname.substring(0, modname.length() - 3);
        }
        String modpath = modname.replace('.', '/');

        // A nasty hack to avoid e.g. python2.5 becoming python2/5.
        // Should generalize this for directory components containing '.'.
        modpath = modpath.replaceFirst("(/python[23])/([0-9]/)", "$1.$2");

        List<String> loadPath = getLoadPath();

        for (String p : loadPath) {
            String dirname = p + modpath;
            String pyname = dirname + ".py";
            String initname = Util.joinPath(dirname, "__init__.py").getAbsolutePath();
            String name;

            // foo/bar has priority over foo/bar.py
            // http://www.python.org/doc/essays/packages.html
            if (Util.isReadableFile(initname)) {
                name = initname;
            } else if (Util.isReadableFile(pyname)) {
                name = pyname;
            } else {
                continue;
            }

            name = Util.canonicalize(name);
            ModuleType m = loadFile(name);
            if (m != null) {
                finer("load of module " + modname + "[succeeded]");
                return m;
            }
        }
        finer("failed to find module " + modname + " in load path");
        failedModules.add(modname);
        return null;
    }

    /**
     * Load all Python source files recursively if the given fullname is a
     * directory; otherwise just load a file.  Looks at file extension to
     * determine whether to load a given file.
     */
    public void loadFileRecursive(String fullname) throws Exception {
        File file_or_dir = new File(fullname);
        if (file_or_dir.isDirectory()) {
            setCWD(fullname);
            for (File file : file_or_dir.listFiles()) {
                loadFileRecursive(file.getAbsolutePath());
            }
        } else {
            if (file_or_dir.getAbsolutePath().endsWith(".py")) {
                setCWD(file_or_dir.getParent());
                loadFile(file_or_dir.getAbsolutePath());
            }
        }
    }

    /**
     * Performs final indexing-building passes, including marking references to
     * undeclared variables. Caller should invoke this method after loading all
     * files.
     * @throws Exception
     */
    public void finish() throws Exception {
        progress.end();
        Util.msg("Finished loading files. " + nCalled + " functions were called.");
        Util.msg("Analyzing uncalled functions, count: " + uncalled.size());
        applyUncalled();

        for (List<Binding> lb : allBindings.values()) {
            for (Binding b: lb) {
                if (!b.getType().isClassType() &&
                        !b.getType().isFuncType() &&
                        !b.getType().isModuleType()
                        && b.getRefs().isEmpty()) {
                    for (Def def : b.getDefs()) {
                        Indexer.idx.putProblem(def.getNode(), "Unused variable: " + def.getName());
                    }
                }
            }
        }

        for (Entry<Ref, List<Binding>> ent : references.entrySet()) {
            convertCallToNew(ent.getKey(), ent.getValue());
        }
    }

    private void convertCallToNew(Ref ref, List<Binding> bindings) {

        if (ref.isRef()) {
            return;
        }

        if (bindings.isEmpty()) {
            return;
        }

        Binding nb = bindings.get(0);
        Type t = nb.getType();
        if (t.isUnionType()) {
            t = t.asUnionType().firstUseful();
            if (t == null) {
                return;
            }
        }

        if (!t.isUnknownType() && !t.isFuncType()) {
            ref.markAsNew();
        }
    }

    public void addUncalled(FunType cl) {
        if (!cl.func.called) {
            uncalled.add(cl);
        }
    }


    public void removeUncalled(FunType f) {
        uncalled.remove(f);
    }


    public void applyUncalled() throws Exception {
        Progress progress = new Progress(100, 50);
        while (!uncalled.isEmpty()) {
            List<FunType> uncalledDup = new ArrayList<FunType>(uncalled);
            for (FunType cl : uncalledDup) {
                progress.tick();
                Call.apply(cl, null, null, null, null, null, newThread());
            }
        }
        progress.end();
    }


    public String getStatusReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("Summary: \n")
                .append("- modules loaded:\t").append(nLoadedFiles)
                .append("\n- unresolved modules:\t").append(failedModules.size())
                .append("\n- semantic problems:\t").append(semanticErrors.size())
                .append("\n- failed to parse:\t").append(failedToParse.size());

        return sb.toString();
    }


    public List<String> getLoadedFiles() {
        List<String> files = new ArrayList<String>();
        for (String file : moduleTable.keySet()) {
            if (file.startsWith("/")) {
                files.add(file);
            }
        }
        return files;
    }

    public void log(Level level, String msg) {
        if (logger.isLoggable(level)) {
            logger.log(level, msg);
        }
    }

    public void severe(String msg) {
        log(Level.SEVERE, msg);
    }

    public void warn(String msg) {
        log(Level.WARNING, msg);
    }

    public void info(String msg) {
        log(Level.INFO, msg);
    }

    public void fine(String msg) {
        log(Level.FINE, msg);
    }

    public void finer(String msg) {
        log(Level.FINER, msg);
    }

    @Override
    public String toString() {
        return "<Indexer:locs=" + references.size() + ":probs="
                + semanticErrors.size() + ":files=" + nLoadedFiles + ">";
    }
}
