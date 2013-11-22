package org.yinwang.pysonar;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.util.*;

public class Util {

    public static final Charset UTF_8 = Charset.forName("UTF-8");

    private static int gensymCount = -1;

    @NotNull
    public static String gensym(String base) {
        gensymCount++;
        return base + gensymCount;
    }

    public static String baseFileName(String filename) {
        return new File(filename).getName();
    }


    public static String getSystemTempDir() {
        String tmp = System.getProperty("java.io.tmpdir");
        String sep = System.getProperty("file.separator");
        if (tmp.endsWith(sep)) {
            return tmp;
        }
        return tmp + sep;
    }

    /**
     * Returns the parent qname of {@code qname} -- everything up to the
     * last dot (exclusive), or if there are no dots, the empty string.
     */
    public static String getQnameParent(@Nullable String qname) {
        if (qname == null || qname.isEmpty()) {
            return "";
        }
        int index = qname.lastIndexOf(".");
        if (index == -1) {
            return "";
        }
        return qname.substring(0, index);
    }

    /**
     * Determines the fully-qualified module name for the specified file.  A
     * module's qname is a function of the module's absolute path and the sys
     * path; it does not depend on how the module name may have been specified
     * in import statements. The module qname is the relative path of the module
     * under the load path, with slashes converted to dots.
     *
     * @param file absolute canonical path to a file (__init__.py for dirs)
     * @return null if {@code file} is not somewhere under the load path
     */
    @Nullable
    public static String moduleQname(@NotNull String file) {
        File f = new File(file);
        boolean initpy = f.getName().equals("__init__.py");

        if (initpy) {
            file = f.getAbsolutePath();
        } else if (file.endsWith(".py")) {
            file = file.substring(0, file.length() - ".py".length());
        }

        for (String root : Indexer.idx.getLoadPath()) {
            if (file.startsWith(root)) {
                if (file.contains("/")) {
                    return file.substring(root.length() + 1).replace('/', '.');
                } else if (file.contains("\\")) {
                    return file.substring(root.length() + 1).replace('\\', '.');
                }
            }
        }
        return null;
    }

    @NotNull
    public static String arrayToString(@NotNull Collection<String> strings) {
        StringBuffer sb = new StringBuffer();
        for (String s : strings) {
            sb.append(s).append("\n");
        }
        return sb.toString();
    }

    @NotNull
    public static String arrayToSortedStringSet(Collection<String> strings) {
        Set<String> sorter = new TreeSet<String>();
        sorter.addAll(strings);
        return arrayToString(sorter);
    }

    /**
     * Given an absolute {@code path} to a file (not a directory),
     * returns the module name for the file.  If the file is an __init__.py,
     * returns the last component of the file's parent directory, else
     * returns the filename without path or extension.
     */
    public static String moduleNameFor(String path) {
        File f = new File(path);
        if (f.isDirectory()) {
            throw new IllegalStateException("failed assertion: " + path);
        }
        String fname = f.getName();
        if (fname.equals("__init__.py")) {
            return f.getParentFile().getName();
        }
        return fname.substring(0, fname.lastIndexOf('.'));
    }

    @NotNull
    public static File joinPath(@NotNull File dir, String file) {
        return joinPath(dir.getAbsolutePath(), file);
    }

    @NotNull
    public static File joinPath(String dir, String file) {
        File file1 = new File(dir);
        File file2 = new File(file1, file);
        return file2;
    }

    public static void writeFile(String path, String contents) throws Exception {
        PrintWriter out = null;
        try {
            out = new PrintWriter(new BufferedWriter(new FileWriter(path)));
            out.print(contents);
            out.flush();
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    @NotNull
    public static String readFile(String filename) throws Exception {
        return readFile(new File(filename));
    }

    @NotNull
    public static String readFile(@NotNull File path) throws Exception {
        // Don't use line-oriented file read -- need to retain CRLF if present
        // so the style-run and link offsets are correct.
        return new String(getBytesFromFile(path), UTF_8);
    }


    @NotNull
    public static byte[] getBytesFromFile(@NotNull File file) {
        InputStream is = null;

        try {
            is = new FileInputStream(file);
            long length = file.length();
            if (length > Integer.MAX_VALUE) {
                throw new IOException("file too large: " + file);
            }

            byte[] bytes = new byte[(int)length];
            int offset = 0;
            int numRead = 0;
            while (offset < bytes.length
                    && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
                offset += numRead;
            }
            if (offset < bytes.length) {
                throw new IOException("Failed to read whole file " + file);
            }
            return bytes;
        } catch (Exception e) {
            return null;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                }
            }
        }
    }


    @NotNull
    public static String readWhole(@NotNull InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        byte[] bytes = new byte[8192];

        int nRead;
        while ((nRead = is.read(bytes, 0, 8192)) > 0) {
            sb.append(new String(bytes, 0, nRead));
        }
        return sb.toString();
    }


    @NotNull
    public static String getSHA1(@NotNull File path) {
        byte[] bytes = getBytesFromFile(path);
        if (bytes == null) {
            Util.msg("getSHA1: failed to read from file: " + path);
            System.exit(2);
            return "";
        } else {
            return getMD5(bytes);
        }
    }


    @NotNull
    public static String getMD5(byte[] fileContents) {
        MessageDigest algorithm;

        try {
            algorithm = MessageDigest.getInstance("SHA-1");
        } catch (Exception e) {
            Util.die("getSHA1: failed to get MD5, shouldn't happen");
            return "";
        }

        algorithm.reset();
        algorithm.update(fileContents);
        byte messageDigest[] = algorithm.digest();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < messageDigest.length; i++) {
            sb.append(String.format("%02x", 0xFF & messageDigest[i]));
        }
        return sb.toString();
    }


    static boolean isReadableFile(String path) {
        File f = new File(path);
        return f.canRead() && f.isFile();
    }

    static public String escapeQname_(@NotNull String s) {
        return s.replaceAll("[.&@%-]", "_");
    }

    @NotNull
    public static Collection<String> toStringCollection(@NotNull Collection<Integer> collection) {
        List<String> ret = new ArrayList<String>();
        for (Integer x : collection) {
            ret.add(x.toString());
        }
        return ret;
    }

    @NotNull
    static public String joinWithSep(@NotNull Collection<String> ls, String sep, @Nullable String start, @Nullable String end) {
        StringBuilder sb = new StringBuilder();
        if (start != null && ls.size() > 1) {
            sb.append(start);
        }
        int i = 0;
        for (String s: ls) {
            if (i > 0) {
                sb.append(sep);
            }
            sb.append(s);
            i++;
        }
        if (end != null && ls.size() > 1) {
            sb.append(end);
        }
        return sb.toString();
    }

    @NotNull
    public static String timeString(long millis) {
        long sec = millis / 1000;
        long min = sec / 60;
        sec = sec % 60;
        long hr = min / 60;
        min = min % 60;

        return hr + ":" + min + ":" + sec;
    }


    public static void msg(String m) {
        System.out.println(m);
    }

    @Nullable
    public static String readWholeFile(String filename) {
        try {
            return new Scanner(new File(filename)).useDelimiter("PYSONAR2END").next();
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    public static String readWholeStream(InputStream in) throws Exception {
        return new Scanner(in).useDelimiter("\\Z").next();
    }


    @NotNull
    public static String percent(long num, long total) {
        if (total == 0) {
            return "100%";
        } else {
            int pct = (int) (num * 100 / total);
            return String.format("%1$3d", pct) + "%";
        }
    }


    public static void die(String msg) {
        die(msg, null);
    }

    public static void die(String msg, Exception e) {
        System.err.println(msg);

        if (e != null) {
            System.err.println("Exception: " + e + "\n");
        }

        Thread.dumpStack();
        System.exit(2);
    }


    public static String format(Object n, int length) {
        if (length == 0) {
            length = 1;
        }

        if (n instanceof Integer) {
            return String.format("%1$" + length + "d", (int) n);
        } else if (n instanceof Long) {
            return String.format("%1$" + length + "d", (long) n);
        } else {
            return String.format("%1$" + length + "s", n.toString());
        }
    }


    public static boolean deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory()) {
                        deleteDirectory(f);
                    } else {
                        f.delete();
                    }
                }
            }
        }
        return directory.delete();
    }

    public static String newSessionId() {
        return UUID.randomUUID().toString();
    }

    public static File makePath(String... files) {
        File ret = new File(files[0]);

        for (int i = 1; i < files.length; i++) {
            ret = new File(ret, files[i]);
        }

        return ret;
    }

    public static String makePathString(String... files) {
        return unifyPath(makePath(files).getPath());
    }


    public static String unifyPath(String filename) {
        return unifyPath(new File(filename));
    }


    public static String unifyPath(File file) {
        return file.getAbsolutePath();
    }


    public static String relativize(String path1, String path2) {
        String a = unifyPath(path1);
        String b = unifyPath(path2);

        String[] as = a.split("[/\\\\]");
        String[] bs = b.split("[/\\\\]");

        int i;
        for (i = 0; i < Math.min(as.length, bs.length); i++) {
            if (!as[i].equals(bs[i])) {
                break;
            }
        }

        int ups = as.length - i - 1;

        File res = null;

        for (int x = 0; x < ups; x++) {
            res = new File(res, "..");
        }

        for (int y = i; y < bs.length; y++) {
            res = new File(res, bs[y]);
        }

        if (res == null) {
            return null;
        } else {
            return res.getPath();
        }
    }


    public static String printMem(long bytes) {
        double dbytes = (double) bytes;
        DecimalFormat df = new DecimalFormat("#.##");

        if (dbytes < 1024) {
            return df.format(bytes);
        } else if (dbytes < 1024 * 1024) {
            return df.format(dbytes / 1024);
        } else if (dbytes < 1024 * 1024 * 1024) {
            return df.format(dbytes / 1024 / 1024) + "M";
        } else if (dbytes < 1024 * 1024 * 1024 * 1024L) {
            return df.format(dbytes / 1024 / 1024 / 1024) + "G";
        } else {
            return "Too big to show you";
        }
    }


    public static String banner(String msg) {
        return "---------------- " + msg + " ----------------";
    }


    public static String printGCStats() {
        long totalGC = 0;
        long gcTime = 0;

        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            long count = gc.getCollectionCount();

            if (count >= 0) {
                totalGC += count;
            }

            long time = gc.getCollectionTime();

            if (time >= 0) {
                gcTime += time;
            }
        }

        StringBuilder sb = new StringBuilder();

        sb.append(banner("memory stats"));
        sb.append("\n- total collections: " + totalGC);
        sb.append("\n- total collection time: " + timeString(gcTime));

        Runtime runtime = Runtime.getRuntime();
        sb.append("\n- total memory: " + Util.printMem(runtime.totalMemory()));

        return sb.toString();
    }

}
