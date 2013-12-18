package org.yinwang.pysonar;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.pysonar.ast.Call;
import org.yinwang.pysonar.ast.Name;
import org.yinwang.pysonar.ast.Node;
import org.yinwang.pysonar.ast.Url;
import org.yinwang.pysonar.types.FunType;
import org.yinwang.pysonar.types.ModuleType;
import org.yinwang.pysonar.types.Type;

import java.io.File;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Analyzer {

    // global static instance of the analyzer itself
    public static Analyzer self;
    public boolean debug = false;

    public State moduleTable = new State(null, State.StateType.GLOBAL);
    public List<String> loadedFiles = new ArrayList<>();
    public State globaltable = new State(null, State.StateType.GLOBAL);
    public List<Binding> allBindings = new ArrayList<>();
    private Map<Ref, List<Binding>> references = new LinkedHashMap<>();
    public Map<String, List<Diagnostic>> semanticErrors = new HashMap<>();
    public Map<String, List<Diagnostic>> parseErrors = new HashMap<>();
    public String cwd = null;
    public int nCalled = 0;
    public boolean multilineFunType = false;
    public List<String> path = new ArrayList<>();
    private Set<FunType> uncalled = new HashSet<>();
    private Set<Object> callStack = new HashSet<>();
    private Set<Object> importStack = new HashSet<>();

    private AstCache astCache;
    public String cacheDir;
    public Set<String> failedToParse = new HashSet<>();
    public Stats stats = new Stats();
    public Builtins builtins;
    private Logger logger;
    private Progress loadingProgress = null;

    public String projectDir;
    public String suffix;


    public Analyzer() {
        stats.putInt("startTime", System.currentTimeMillis());
        logger = Logger.getLogger(Analyzer.class.getCanonicalName());
        self = this;
        this.suffix = ".py";
        builtins = new Builtins();
        builtins.init();
        addPythonPath();
        createCacheDir();
        getAstCache();
    }


    public Analyzer(boolean debug) {
        this();
        this.debug = debug;
    }


    // main entry to the analyzer
    public void analyze(String path) {
        projectDir = _.unifyPath(path);
        loadFileRecursive(projectDir);
    }


    public void setCWD(String cd) {
        if (cd != null) {
            cwd = _.unifyPath(cd);
        }
    }


    public void addPaths(@NotNull List<String> p) {
        for (String s : p) {
            addPath(s);
        }
    }


    public void addPath(String p) {
        path.add(_.unifyPath(p));
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


    @NotNull
    public List<String> getLoadPath() {
        List<String> loadPath = new ArrayList<>();
        if (cwd != null) {
            loadPath.add(cwd);
        }
        if (projectDir != null && (new File(projectDir).isDirectory())) {
            loadPath.add(projectDir);
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
    public List<Binding> getAllBindings() {
        return allBindings;
    }


    @Nullable
    ModuleType getCachedModule(String file) {
        Type t = moduleTable.lookupType(_.moduleQname(file));
        if (t == null) {
            return null;
        } else if (t.isUnionType()) {
            for (Type tt : t.asUnionType().getTypes()) {
                if (tt.isModuleType()) {
                    return (ModuleType) tt;
                }
            }
            return null;
        } else if (t.isModuleType()) {
            return (ModuleType) t;
        } else {
            return null;
        }
    }


    public List<Diagnostic> getDiagnosticsForFile(String file) {
        List<Diagnostic> errs = semanticErrors.get(file);
        if (errs != null) {
            return errs;
        }
        return new ArrayList<>();
    }


    public void putRef(@NotNull Node node, @NotNull List<Binding> bs) {
        if (!(node instanceof Url)) {
            Ref ref = new Ref(node);
            List<Binding> bindings = references.get(ref);
            if (bindings == null) {
                bindings = new ArrayList<>(1);
                references.put(ref, bindings);
            }
            for (Binding b : bs) {
                if (!bindings.contains(b)) {
                    bindings.add(b);
                }
                b.addRef(ref);
            }
        }
    }


    public void putRef(@NotNull Node node, @NotNull Binding b) {
        List<Binding> bs = new ArrayList<>();
        bs.add(b);
        putRef(node, bs);
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
    public Type loadFile(String path) {
//        Util.msg("loading: " + path);

        path = _.unifyPath(path);
        File f = new File(path);

        if (!f.canRead()) {
            finer("\nfile not not found or cannot be read: " + path);
            return null;
        }

        Type module = getCachedModule(path);
        if (module != null) {
            finer("\nusing cached module " + path + " [succeeded]");
            return module;
        }

        // detect circular import
        if (Analyzer.self.inImportStack(path)) {
            return null;
        }

        // set new CWD and save the old one on stack
        String oldcwd = cwd;
        setCWD(f.getParent());

        Analyzer.self.pushImportStack(path);
        Type type = parseAndResolve(path);

        // restore old CWD
        setCWD(oldcwd);
        return type;
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
    private Type parseAndResolve(String file) {
        finer("Analyzing: " + file);
        loadingProgress.tick();

        try {
            Node ast = getAstForFile(file);

            if (ast == null) {
                failedToParse.add(file);
                return null;
            } else {
                finer("resolving: " + file);
                Type type = Node.transformExpr(ast, moduleTable);
                finer("[success]");
                loadedFiles.add(file);
                return type;
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
        cacheDir = _.makePathString(_.getSystemTempDir(), "pysonar2", "ast_cache");
        File f = new File(cacheDir);
        _.msg("AST cache is at: " + cacheDir);

        if (!f.exists()) {
            if (!f.mkdirs()) {
                _.die("Failed to create tmp directory: " + cacheDir +
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
    public Node getAstForFile(String file) {
        return getAstCache().getAST(file);
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
     *
     * @param headName first module name segment
     */
    public String locateModule(String headName) {
        List<String> loadPath = getLoadPath();

        for (String p : loadPath) {
            File startDir = new File(p, headName);
            File initFile = new File(_.joinPath(startDir, "__init__.py").getPath());

            if (initFile.exists()) {
                return p;
            }

            File startFile = new File(startDir + suffix);
            if (startFile.exists()) {
                return p;
            }
        }

        return null;
    }


    @Nullable
    public Type loadModule(@NotNull List<Name> name, @NotNull State state) {
        if (name.isEmpty()) {
            return null;
        }

        String qname = makeQname(name);

        Type mt = getBuiltinModule(qname);
        if (mt != null) {
            state.insert(name.get(0).id,
                    new Url(Builtins.LIBRARY_URL + mt.getTable().getPath() + ".html"),
                    mt, Binding.Kind.SCOPE);
            return mt;
        }

        // If there are more than one segment
        // load the packages first
        Type prev = null;
        String startPath = locateModule(name.get(0).id);

        if (startPath == null) {
            return null;
        }

        File path = new File(startPath);

        for (int i = 0; i < name.size(); i++) {
            path = new File(path, name.get(i).id);
            File initFile = new File(_.joinPath(path, "__init__.py").getPath());

            if (initFile.exists()) {
                Type mod = loadFile(initFile.getPath());
                if (mod == null) {
                    return null;
                }

                if (prev != null) {
                    prev.getTable().insert(name.get(i).id, name.get(i), mod, Binding.Kind.VARIABLE);
                } else {
                    state.insert(name.get(i).id, name.get(i), mod, Binding.Kind.VARIABLE);
                }

                prev = mod;

            } else if (i == name.size() - 1) {
                File startFile = new File(path + suffix);
                if (startFile.exists()) {
                    Type mod = loadFile(startFile.getPath());
                    if (mod == null) {
                        return null;
                    }
                    if (prev != null) {
                        prev.getTable().insert(name.get(i).id, name.get(i), mod, Binding.Kind.VARIABLE);
                    } else {
                        state.insert(name.get(i).id, name.get(i), mod, Binding.Kind.VARIABLE);
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
        int count = countFileRecursive(fullname);
        if (loadingProgress == null) {
            loadingProgress = new Progress(count, 50);
        }

        File file_or_dir = new File(fullname);

        if (file_or_dir.isDirectory()) {
            for (File file : file_or_dir.listFiles()) {
                loadFileRecursive(file.getPath());
            }
        } else {
            if (file_or_dir.getPath().endsWith(suffix)) {
                loadFile(file_or_dir.getPath());
            }
        }
    }


    // count number of .py files
    public int countFileRecursive(String fullname) {
        File file_or_dir = new File(fullname);
        int sum = 0;

        if (file_or_dir.isDirectory()) {
            for (File file : file_or_dir.listFiles()) {
                sum += countFileRecursive(file.getPath());
            }
        } else {
            if (file_or_dir.getPath().endsWith(suffix)) {
                sum += 1;
            }
        }
        return sum;
    }


    public void finish() {
//        progress.end();
        _.msg("\nFinished loading files. " + nCalled + " functions were called.");
        _.msg("Analyzing uncalled functions");
        applyUncalled();

        // mark unused variables
        for (Binding b : allBindings) {
            if (!b.getType().isClassType() &&
                    !b.getType().isFuncType() &&
                    !b.getType().isModuleType()
                    && b.getRefs().isEmpty())
            {
                Analyzer.self.putProblem(b.getNode(), "Unused variable: " + b.getName());
            }
        }

        _.msg(getAnalysisSummary());
    }


    public void close() {
        astCache.close();
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
        Progress progress = new Progress(uncalled.size(), 50);

        while (!uncalled.isEmpty()) {
            List<FunType> uncalledDup = new ArrayList<>(uncalled);

            for (FunType cl : uncalledDup) {
                progress.tick();
                Call.apply(cl, null, null, null, null, null);
            }
        }
    }


    @NotNull
    public String getAnalysisSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n" + _.banner("analysis summary"));

        String duration = _.formatTime(System.currentTimeMillis() - stats.getInt("startTime"));
        sb.append("\n- total time: " + duration);
        sb.append("\n- modules loaded: " + loadedFiles.size());
        sb.append("\n- semantic problems: " + semanticErrors.size());
        sb.append("\n- failed to parse: " + failedToParse.size());

        // calculate number of defs, refs, xrefs
        int nDef = 0, nXRef = 0;
        for (Binding b : getAllBindings()) {
            nDef += 1;
            nXRef += b.getRefs().size();
        }

        sb.append("\n- number of definitions: " + nDef);
        sb.append("\n- number of cross references: " + nXRef);
        sb.append("\n- number of references: " + getReferences().size());

        long resolved = stats.getInt("resolved");
        long unresolved = stats.getInt("unresolved");
        sb.append("\n- resolved names: " + resolved);
        sb.append("\n- unresolved names: " + unresolved);
        sb.append("\n- name resolve rate: " + _.percent(resolved, resolved + unresolved));
        sb.append("\n" + _.getGCStats());

        return sb.toString();
    }


    @NotNull
    public List<String> getLoadedFiles() {
        List<String> files = new ArrayList<>();
        for (String file : loadedFiles) {
            if (file.endsWith(suffix)) {
                files.add(file);
            }
        }
        return files;
    }


    public void registerBinding(@NotNull Binding b) {
        allBindings.add(b);
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
        return "<Analyzer:locs=" + references.size() + ":probs="
                + semanticErrors.size() + ":files=" + loadedFiles.size() + ">";
    }
}
