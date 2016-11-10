package org.yinwang.pysonar;

import org.jetbrains.annotations.Nullable;
import org.yinwang.pysonar.ast.Dummy;
import org.yinwang.pysonar.ast.Node;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class TestInference
{
    private static Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private String testFile;
    private String expecteRefsFile;
    private String failedRefsFile;

    public TestInference(String testFile)
    {
        this.testFile = testFile;
        if (new File(testFile).isDirectory())
        {
            expecteRefsFile = $.makePathString(testFile, "refs.json");
            failedRefsFile = $.makePathString(testFile, "failed_refs.json");
        }
        else
        {
            expecteRefsFile = $.makePathString(testFile + ".refs.json");
            failedRefsFile = $.makePathString(testFile + ".failed_refs.json");
        }
    }

    public Analyzer runAnalysis(String dir)
    {
        Map<String, Object> options = new HashMap<>();
        options.put("quiet", true);
        Analyzer analyzer = new Analyzer(options);
        analyzer.analyze(dir);
        analyzer.finish();
        return analyzer;
    }

    public void generateRefs(Analyzer analyzer)
    {
        List<Map<String, Object>> refs = new ArrayList<>();
        for (Map.Entry<Node, List<Binding>> e : analyzer.getReferences().entrySet())
        {

            String file = e.getKey().file;

            // only record those in the testFile
            if (file != null && file.startsWith(Analyzer.self.projectDir))
            {
                file = $.projRelPath(file).replaceAll("\\\\", "/");
                Map<String, Object> writeout = new LinkedHashMap<>();

                Map<String, Object> ref = new LinkedHashMap<>();
                ref.put("name", e.getKey().name);
                ref.put("file", file);
                ref.put("start", e.getKey().start);
                ref.put("end", e.getKey().end);

                List<Map<String, Object>> dests = new ArrayList<>();
                List<Binding> sorted = e.getValue();
                Collections.sort(sorted, (a, b) -> a.start == b.start ? a.end - b.end : a.start - b.start);
                for (Binding b : sorted)
                {
                    String destFile = b.getFile();
                    if (destFile != null && destFile.startsWith(Analyzer.self.projectDir))
                    {
                        destFile = $.projRelPath(destFile).replaceAll("\\\\", "/");
                        Map<String, Object> dest = new LinkedHashMap<>();
                        dest.put("name", b.name);
                        dest.put("file", destFile);
                        dest.put("start", b.start);
                        dest.put("end", b.end);
                        dest.put("type", b.type.toString());
                        dests.add(dest);
                    }
                }
                if (!dests.isEmpty())
                {
                    writeout.put("ref", ref);
                    writeout.put("dests", dests);
                    refs.add(writeout);
                }
            }
        }

        String json = gson.toJson(refs);
        $.writeFile(expecteRefsFile, json);
    }

    public boolean checkRefs(Analyzer analyzer)
    {
        List<Map<String, Object>> failedRefs = new ArrayList<>();
        String json = $.readFile(expecteRefsFile);
        if (json == null)
        {
            $.msg("Expected refs not found in: " + expecteRefsFile +
                  "Please run Test with -generate to generate");
            return false;
        }
        List<Map<String, Object>> expectedRefs = gson.fromJson(json, List.class);
        for (Map<String, Object> r : expectedRefs)
        {
            Map<String, Object> refMap = (Map) r.get("ref");
            Dummy dummy = makeDummy(refMap);

            List<Map<String, Object>> dests = (List) r.get("dests");
            List<Binding> actualDests = analyzer.getReferences().get(dummy);
            List<Map<String, Object>> failedDests = new ArrayList<>();

            for (Map<String, Object> d : dests)
            {
                // names are ignored, they are only for human readers
                String file = $.projAbsPath((String) d.get("file"));
                int start = (int) Math.floor((double) d.get("start"));
                int end = (int) Math.floor((double) d.get("end"));
                String type = (String) d.get("type");

                if (!checkBindingExist(actualDests, file, start, end, type))
                {
                    failedDests.add(d);
                }
            }

            // record the ref & failed dests if any
            if (!failedDests.isEmpty())
            {
                Map<String, Object> failedRef = new LinkedHashMap<>();
                failedRef.put("ref", refMap);
                failedRef.put("dests", failedDests);
                failedRefs.add(failedRef);
            }
        }

        if (failedRefs.isEmpty())
        {
            $.deleteFile(failedRefsFile);
            $.testmsg("   " + testFile);
            return true;
        }
        else
        {
            String failedJson = gson.toJson(failedRefs);
            $.writeFile(failedRefsFile, failedJson);
            $.testmsg(" - " + testFile);
            return false;
        }
    }

    boolean checkBindingExist(List<Binding> bs, String file, int start, int end, String type)
    {
        if (bs == null)
        {
            return false;
        }

        for (Binding b : bs)
        {
            if (((b.getFile() == null && file == null) ||
                 (b.getFile() != null && file != null && b.getFile().equals(file))) &&
                b.start == start && b.end == end && b.type.toString().equals(type))
            {
                return true;
            }
        }

        return false;
    }

    public static Dummy makeDummy(Map<String, Object> m)
    {
        String file = $.projAbsPath((String) m.get("file"));
        int start = (int) Math.floor((double) m.get("start"));
        int end = (int) Math.floor((double) m.get("end"));
        return new Dummy(file, start, end);
    }

    public void generateTest()
    {
        Analyzer analyzer = runAnalysis(testFile);
        generateRefs(analyzer);
        $.testmsg("  * " + testFile);
    }

    public boolean runTest()
    {
        Analyzer analyzer = runAnalysis(testFile);
        return checkRefs(analyzer);
    }

    // ------------------------- static part -----------------------

    @Nullable
    public static List<String> testAll(String path, boolean generate)
    {
        List<String> failed = new ArrayList<>();
        if (generate)
        {
            $.testmsg("Generating tests");
        }
        else
        {
            $.testmsg("Verifying tests");
        }

        testRecursive(path, generate, failed);

        if (generate)
        {
            $.testmsg("All tests generated.");
            return null;
        }
        else if (failed.isEmpty())
        {
            $.testmsg("All tests passed.");
            return null;
        }
        else
        {
            return failed;
        }
    }

    public static void testRecursive(String path, boolean generate, List<String> failed)
    {
        File file_or_dir = new File(path);

        if (file_or_dir.isDirectory())
        {
            if (path.endsWith(".test"))
            {
                TestInference test = new TestInference(path);
                if (generate)
                {
                    test.generateTest();
                }
                else if (!test.runTest())
                {
                    failed.add(path);
                }
            }
            else
            {
                for (File file : file_or_dir.listFiles())
                {
                    testRecursive(file.getPath(), generate, failed);
                }
            }
        }
    }

    public static void main(String[] args) throws Exception
    {
        Options options = new Options(args);
        List<String> argsList = options.getArgs();
        String inputDir = $.unifyPath(argsList.get(0));

        // generate expected file?
        boolean generate = options.hasOption("generate");
        testAll(inputDir, generate);
    }
}
