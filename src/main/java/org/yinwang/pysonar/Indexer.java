package org.yinwang.pysonar;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.pysonar.ast.*;
import org.yinwang.pysonar.types.FunType;
import org.yinwang.pysonar.types.ModuleType;
import org.yinwang.pysonar.types.Type;
import org.yinwang.pysonar.types.UnionType;

import java.io.File;
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

    public static Indexer idx;

    @NotNull
    public Scope moduleTable = new Scope(null, Scope.ScopeType.GLOBAL);
    @NotNull
    public Scope globaltable = new Scope(null, Scope.ScopeType.GLOBAL);
    @NotNull
    public Map<String, List<Binding>> allBindings = new HashMap<>();
    @NotNull
    private Map<Ref, List<Binding>> references = new HashMap<>();
    @NotNull
    public Map<String, List<Diagnostic>> semanticErrors = new HashMap<>();
    @NotNull
    public Map<String, List<Diagnostic>> parseErrors = new HashMap<>();
    @Nullable
    public String cwd = null;
    public int nCalled = 0;
    @NotNull
    public List<String> path = new ArrayList<>();
    @NotNull
    private Set<FunType> uncalled = new HashSet<>();
    @NotNull
    private Set<Object> callStack = new HashSet<>();
    @NotNull
    private Set<Object> importStack = new HashSet<>();


    private int threadCounter = 0;
    public int newThread() {
        threadCounter++;
        return threadCounter;
    }

    private AstCache astCache;
    public String cacheDir;

    @NotNull
    public Set<String> failedModules = new HashSet<>();
    @NotNull
    public Set<String> failedToParse = new HashSet<>();

    /**
     * Manages the built-in modules -- that is, modules from the standard Python
     * library that are implemented in C and consequently have no Python source.
     */
    public Builtins builtins;

    public int nLoadedFiles = 0;

    private Logger logger;
    private Progress progress;

    public Indexer() {
        progress = new Progress(10, 50);
        logger = Logger.getLogger(Indexer.class.getCanonicalName());
        idx = this;
        builtins = new Builtins();
        builtins.init();
        addPythonPath();
        createCacheDir();
    }


    public void setCWD(String cd) {
        if (cd != null) {
            cwd = Util.unifyPath(cd);
        }
    }


    public void addPaths(@NotNull List<String> p) {
        for (String s : p) {
            addPath(s);
        }
    }


    public void addPath(String p) {
        path.add(Util.unifyPath(p));
    }


    public void setPath(@NotNull List<String> path) {
        this.path = new ArrayList<>(path.size());
        addPaths(path);
    }


    private void addPythonPath() {
        String path = System.getenv("PYTHONPATH");
        if (path != null) {
            String[] segments = path.split(":");
            for (String p : segments) {
                addPath(p);
            }
        }
    }


    /**
     * Returns the module search path. Put cwd on top.
     */
    @NotNull
    public List<String> getLoadPath() {
        List<String> loadPath = new ArrayList<>();
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

    public boolean inImportStack(Object f) {
        return importStack.contains(f);
    }

    public void pushImportStack(Object f) {
        importStack.add(f);
    }

    public void popImportStack(Object f) {
        importStack.remove(f);
    }


    @NotNull
    public Map<String, List<Binding>> getAllBindings() {
        return allBindings;
    }


    @Nullable
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
    @Nullable
    public ModuleType getModuleForFile(String file) {
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
        return new ArrayList<>();
    }


    /**
     * Add a reference to binding {@code b} at AST node {@code node}.
     * @param node a node referring to a name binding.  Typically a
     * {@link org.yinwang.pysonar.ast.Name}, {@link org.yinwang.pysonar.ast.Str} or {@link org.yinwang.pysonar.ast.Url}.
     */
    public void putLocation(@Nullable Node node, @Nullable Binding b) {
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
            bindings = new ArrayList<>(1);
            references.put(ref, bindings);
        }
        if (!bindings.contains(b)) {
            bindings.add(b);
        }
        b.addRef(ref);
    }

    @NotNull
    public Map<Ref, List<Binding>> getReferences() {
        return references;
    }


    public void putProblem(@NotNull Node loc, String msg) {
        String file = loc.getFile();
        if (file != null) {
            addFileErr(file, loc.start, loc.end, msg);
        }
    }


    // for situations without a Node
    public void putProblem(@Nullable String file, int begin, int end, String msg) {
        if (file != null) {
            addFileErr(file, begin, end, msg);
        }
    }

    void addFileErr(String file, int begin, int end, String msg) {
        Diagnostic d = new Diagnostic(file, Diagnostic.Category.ERROR, begin, end, msg);
        getFileErrs(file, semanticErrors).add(d);
    }

    List<Diagnostic> getParseErrs(String file) {
        return getFileErrs(file, parseErrors);
    }

    List<Diagnostic> getFileErrs(String file, @NotNull Map<String, List<Diagnostic>> map) {
        List<Diagnostic> msgs = map.get(file);
        if (msgs == null) {
            msgs = new ArrayList<>();
            map.put(file, msgs);
        }
        return msgs;
    }


    @Nullable
    public ModuleType loadString(String path, String contents) {
        ModuleType module = getCachedModule(path);
        if (module != null) {
            finer("\nusing cached module " + path + " [succeeded]");
            return module;
        }
        return parseAndResolve(path, contents);
    }


    @Nullable
    public ModuleType loadFile(String path) {
//        Util.msg("loading: " + path);

        File f = new File(Util.unifyPath(path));

        if (!f.canRead()) {
            finer("\nfile not not found or cannot be read: " + path);
            return null;
        }

        ModuleType module = getCachedModule(path);
        if (module != null) {
            finer("\nusing cached module " + path + " [succeeded]");
            return module;
        }


        // detect circular import
        if (Indexer.idx.inImportStack(path)) {
            return null;
        }

        // set new CWD and save the old one on stack
        String oldcwd = cwd;
        setCWD(f.getParent());

        Indexer.idx.pushImportStack(path);
        ModuleType mod = parseAndResolve(path);

        // restore old CWD
        setCWD(oldcwd);
        return mod;
    }


    private boolean isInLoadPath(File dir) {
        for (String s : getLoadPath()) {
            if (new File(s).equals(dir)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private ModuleType parseAndResolve(String file) {
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
    @Nullable
    private ModuleType parseAndResolve(String file, @Nullable String contents) {
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


    private void createCacheDir() {
        cacheDir = Util.makePathString(Util.getSystemTempDir(),  "pysonar2", "ast_cache");
        File f = new File(cacheDir);

        if (!f.exists()) {
            if (!f.mkdirs()) {
                Util.die("Failed to create tmp directory: " + cacheDir +
                        ".Please check permissions");
            }
        }
    }


    private AstCache getAstCache() {
        if (astCache == null) {
            astCache = AstCache.get();
        }
        return astCache;
    }

    /**
     * Returns the syntax tree for {@code file}. <p>
     */
    @Nullable
    public Module getAstForFile(String file) {
        return getAstCache().getAST(file);
    }

    /**
     * Returns the syntax tree for {@code file}. <p>
     */
    @Nullable
    public Module getAstForFile(String file, String contents) {
        return getAstCache().getAST(file, contents);
    }

    @Nullable
    public ModuleType getBuiltinModule(@NotNull String qname) {
        return builtins.get(qname);
    }


    @Nullable
    public String makeQname(@NotNull List<Name> names) {
        if (names.isEmpty()) {
            return "";
        }

        String ret = "";

        for (int i = 0; i < names.size() - 1; i++) {
            ret += names.get(i).id + ".";
        }

        ret += names.get(names.size() - 1).id;
        return ret;
    }


    /**
     * Find the path that contains modname. Used to find the starting point of locating a qname.
     * @param headName first module name segment
     */
    public String locateModule(String headName) {
        List<String> loadPath = getLoadPath();

        for (String p : loadPath) {
            File startDir = new File(p, headName);
            File initFile = new File(Util.joinPath(startDir, "__init__.py").getPath());

            if (initFile.exists()) {
                return p;
            }

            File startFile = new File(startDir + ".py");
            if (startFile.exists()) {
                return p;
            }
        }

        return null;
    }


    @Nullable
    public ModuleType loadModule(@NotNull List<Name> name, @NotNull Scope scope, int tag) {
        if (name.isEmpty()) return null;

        String qname = makeQname(name);

        ModuleType mt = getBuiltinModule(qname);
        if (mt != null) {
            scope.update(name.get(0).id,
                    new Url(Builtins.LIBRARY_URL + mt.getTable().getPath() + ".html"),
                    mt, Binding.Kind.SCOPE);
            return mt;
        }

        // If there are more than one segment
        // load the packages first
        ModuleType prev = null;
        String startPath = locateModule(name.get(0).id);

        if (startPath == null) {
            return null;
        }

        File path = new File(startPath);

        for (int i = 0; i < name.size(); i++) {
            path = new File(path, name.get(i).id);
            File initFile = new File(Util.joinPath(path, "__init__.py").getPath());

            if (initFile.exists()) {
                ModuleType mod = loadFile(initFile.getPath());
                if (mod == null) return null;

                if (prev != null) {
                    Binding b = prev.getTable().put(name.get(i).id, name.get(i), mod, Binding.Kind.MODULE, tag);
                    Indexer.idx.putLocation(name.get(i), b);
                } else {
                    Binding b = scope.put(name.get(i).id, name.get(i), mod, Binding.Kind.MODULE, tag);
                    Indexer.idx.putLocation(name.get(i), b);
                }

                prev = mod;

            } else if (i == name.size() - 1) {
                File startFile = new File(path + ".py");
                if (startFile.exists()) {
                    ModuleType mod = loadFile(startFile.getPath());
                    if (mod == null) return null;
                    if (prev != null) {
                        Binding b = prev.getTable().put(name.get(i).id, name.get(i), mod, Binding.Kind.MODULE, tag);
                        Indexer.idx.putLocation(name.get(i), b);
                    } else {
                        Binding b = scope.put(name.get(i).id, name.get(i), mod, Binding.Kind.MODULE, tag);
                        Indexer.idx.putLocation(name.get(i), b);

                    }
                    prev = mod;
                } else {
                    return null;
                }
            }
        }
        return prev;
    }


    /**
     * Load all Python source files recursively if the given fullname is a
     * directory; otherwise just load a file.  Looks at file extension to
     * determine whether to load a given file.
     */
    public void loadFileRecursive(String fullname) {
        File file_or_dir = new File(fullname);

        if (file_or_dir.isDirectory()) {
            for (File file : file_or_dir.listFiles()) {
                loadFileRecursive(file.getPath());
            }
        } else {
            if (file_or_dir.getPath().endsWith(".py")) {
                loadFile(file_or_dir.getPath());
            }
        }
    }


    public void finish() {
        progress.end();
        Util.msg("Finished loading files. " + nCalled + " functions were called.");
        Util.msg("Analyzing uncalled functions, count: " + uncalled.size());
        applyUncalled();

        // mark unused variables
        for (List<Binding> bindings : allBindings.values()) {
            for (Binding b : bindings) {
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

        Util.msg("total defs added: " + Binding.totalDefs);
    }


    public void close() {
        astCache.close();
    }


    private void convertCallToNew(@NotNull Ref ref, @NotNull List<Binding> bindings) {

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

    public void addUncalled(@NotNull FunType cl) {
        if (!cl.func.called) {
            uncalled.add(cl);
        }
    }


    public void removeUncalled(FunType f) {
        uncalled.remove(f);
    }


    public void applyUncalled() {
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


    @NotNull
    public String getStatusReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("Summary: \n")
                .append("- modules loaded:\t").append(nLoadedFiles)
                .append("\n- unresolved modules:\t").append(failedModules.size())
                .append("\n- semantic problems:\t").append(semanticErrors.size())
                .append("\n- failed to parse:\t").append(failedToParse.size());

        return sb.toString();
    }


    public AstCache.DocstringInfo getModuleDocstringInfoForFile(String file) {
        return getAstCache().getModuleDocstringInfo(file);
    }


    @NotNull
    public List<String> getLoadedFiles() {
        List<String> files = new ArrayList<String>();
        for (String file : moduleTable.keySet()) {
            if (file.endsWith(".py")) {
                files.add(file);
            }
        }
        return files;
    }


    @Nullable
    private Binding findBinding(@NotNull Binding b) {
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


    @NotNull
    public Binding putBinding(@NotNull Binding b) {
        String qname = b.getQname();
        Binding existing = findBinding(b);

        if (existing == null) {
            addBinding(qname, b);
            return b;
        } else {
            existing.setType(UnionType.union(existing.getType(), b.getType()));
            return existing;
        }
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

    @NotNull
    @Override
    public String toString() {
        return "<Indexer:locs=" + references.size() + ":probs="
                + semanticErrors.size() + ":files=" + nLoadedFiles + ">";
    }
}
