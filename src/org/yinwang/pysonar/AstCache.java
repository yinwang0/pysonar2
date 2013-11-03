package org.yinwang.pysonar;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.pysonar.ast.Module;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides a factory for python source ASTs.  Maintains configurable on-disk and
 * in-memory caches to avoid re-parsing files during analysis.
 */
public class AstCache {

    public static final String CACHE_DIR = Util.getSystemTempDir() + "pysonar2/ast_cache/";

    private static final Logger LOG = Logger.getLogger(AstCache.class.getCanonicalName());

    @NotNull
    private Map<String, Module> cache = new HashMap<String, Module>();

    private static AstCache INSTANCE;

    @NotNull
    private static ProxyParser parser = new ProxyParser();

    private AstCache() throws Exception {
        File f = new File(CACHE_DIR);
        if (!f.exists()) {
            f.mkdirs();
        }
    }

    public static AstCache get() throws Exception {
        if (INSTANCE == null) {
            INSTANCE = new AstCache();
        }
        return INSTANCE;
    }

    /**
     * Clears the memory cache.
     */
    public void clear() {
        cache.clear();
    }

    /**
     * Removes all serialized ASTs from the on-disk cache.
     *
     * @return {@code true} if all cached AST files were removed
     */
    public boolean clearDiskCache() {
        try {
            File dir = new File(CACHE_DIR);
            for (File f : dir.listFiles()) {
                if (f.isFile()) {
                    f.delete();
                }
            }
            return true;
        } catch (Exception x) {
            severe("Failed to clear disk cache: " + x);
            return false;
        }
    }

    /**
     * Returns the syntax tree for {@code path}.  May find and/or create a
     * cached copy in the mem cache or the disk cache.
     *
     * @param path absolute path to a source file
     * @return the AST, or {@code null} if the parse failed for any reason
     * @throws Exception if anything unexpected occurs
     */
    @Nullable
    public Module getAST(@Nullable String path) throws Exception {
        if (path == null) throw new IllegalArgumentException("null path");
        return fetch(path);
    }

    /**
     * Returns the syntax tree for {@code path} with {@code contents}.
     * Uses the memory cache but not the disk cache.
     * This method exists primarily for unit testing.
     *
     * @param path     a name for the file.  Can be relative.
     * @param contents the source to parse
     */
    @Nullable
    public Module getAST(@Nullable String path, @Nullable String contents) throws Exception {
        if (path == null) throw new IllegalArgumentException("null path");
        if (contents == null) throw new IllegalArgumentException("null contents");

        // Cache stores null value if the parse failed.
        if (cache.containsKey(path)) {
            return cache.get(path);
        }

        Module mod = null;
        try {
            mod = parse(path, contents);
            if (mod != null) {
                mod.setFileAndMD5(path, Util.getMD5(contents.getBytes("UTF-8")));
            }
        } finally {
            cache.put(path, mod);  // may be null
        }
        return mod;
    }

    /**
     * Get or create an AST for {@code path}, checking and if necessary updating
     * the disk and memory caches.
     *
     * @param path absolute source path
     */
    @Nullable
    private Module fetch(String path) throws Exception {
        // Cache stores null value if the parse failed.
        if (cache.containsKey(path)) {
            return cache.get(path);
        }

        // Might be cached on disk but not in memory.
        Module mod = getSerializedModule(path);
        if (mod != null) {
            fine("reusing " + path);
            cache.put(path, mod);
            return mod;
        }

        mod = null;
        try {
            mod = parse(path);
        } finally {
            cache.put(path, mod);  // may be null
        }

        if (mod != null) {
            serialize(mod);
        }

        return mod;
    }

    /**
     * Parse a file.  Does not look in the cache or cache the result.
     */
    @NotNull
    private Module parse(String path) throws Exception {
        fine("parsing " + path);
//        mod ast = invokeANTLR(path);
//        return generateAST(ast, path);
        return (Module) parser.parseFile(path);
    }

    /**
     * Parse a string.  Does not look in the cache or cache the result.
     */
    @NotNull
    private Module parse(String path, String contents) throws Exception {
        fine("parsing " + path);
//        mod ast = invokeANTLR(path, contents);
//        return generateAST(ast, path);
        return (Module) parser.parseFile(path);
    }


    /**
     * Each source file's AST is saved in an object file named for the MD5
     * checksum of the source file.  All that is needed is the MD5, but the
     * file's base name is included for ease of debugging.
     */
    @NotNull
    public String getCachePath(@NotNull File sourcePath) throws Exception {
        return getCachePath(Util.getMD5(sourcePath), sourcePath.getName());
    }

    @NotNull
    public String getCachePath(String md5, String name) {
        return CACHE_DIR + name + md5 + ".ast";
    }

    // package-private for testing
    void serialize(@NotNull Module ast) throws Exception {
        String path = getCachePath(ast.getMD5(), new File(ast.getFile()).getName());
        ObjectOutputStream oos = null;
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(path);
            oos = new ObjectOutputStream(fos);
            oos.writeObject(ast);
        } finally {
            if (oos != null) {
                oos.close();
            } else if (fos != null) {
                fos.close();
            }
        }
    }

    // package-private for testing
    @Nullable
    Module getSerializedModule(String sourcePath) {
        try {
            File sourceFile = new File(sourcePath);
            if (sourceFile == null || !sourceFile.canRead()) {
                return null;
            }
            File cached = new File(getCachePath(sourceFile));
            if (!cached.canRead()) {
                return null;
            }
            return deserialize(sourceFile);
        } catch (Exception x) {
//            severe("Failed to deserialize " + sourcePath + ": " + x);
            return null;
        }
    }

    // package-private for testing
    @NotNull
    Module deserialize(@NotNull File sourcePath) throws Exception {
        String cachePath = getCachePath(sourcePath);
        FileInputStream fis = null;
        ObjectInputStream ois = null;
        try {
            fis = new FileInputStream(cachePath);
            ois = new ObjectInputStream(fis);
            Module mod = (Module) ois.readObject();
            // Files in different dirs may have the same base name and contents.
            mod.setFile(sourcePath);
            return mod;
        } finally {
            if (ois != null) {
                ois.close();
            } else if (fis != null) {
                fis.close();
            }
        }
    }

    private void log(Level level, String msg) {
        if (LOG.isLoggable(level)) {
            LOG.log(level, msg);
        }
    }

    private void severe(String msg) {
        log(Level.SEVERE, msg);
    }

    private void warn(String msg) {
        log(Level.WARNING, msg);
    }

    private void info(String msg) {
        log(Level.INFO, msg);
    }

    private void fine(String msg) {
        log(Level.FINE, msg);
    }

    private void finer(String msg) {
        log(Level.FINER, msg);
    }
}
