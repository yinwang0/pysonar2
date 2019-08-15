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
    private static final Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private String testFile;
    private String expecteRefsFile;
    private String missingRefsFile;
    private String wrongTypeFile;

    public TestInference(String testFile)
    {
        this.testFile = testFile;
        if (new File(testFile).isDirectory())
        {
            expecteRefsFile = $.makePathString(testFile, "refs.json");
            missingRefsFile = $.makePathString(testFile, "missing_refs");
            wrongTypeFile = $.makePathString(testFile, "wrong_types");
        }
        else
        {
            expecteRefsFile = $.makePathString(testFile + ".refs.json");
            missingRefsFile = $.makePathString(testFile + ".missing_refs");
            wrongTypeFile = $.makePathString(testFile + ".wrong_types");
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
        for (Node node: analyzer.references.keys())
        {
            String filename = node.file;
            List<Binding> bindings = analyzer.references.get(node);

            // only record those in the testFile
            if (filename != null && filename.startsWith(Analyzer.self.projectDir))
            {
                filename = $.projRelPath(filename).replaceAll("\\\\", "/");
                Map<String, Object> writeout = new LinkedHashMap<>();

                Map<String, Object> ref = new LinkedHashMap<>();
                ref.put("name", node.name);
                ref.put("file", filename);
                ref.put("start", node.start);
                ref.put("end", node.end);
                ref.put("line", node.line);
                ref.put("col", node.col);

                List<Map<String, Object>> dests = new ArrayList<>();
                Collections.sort(bindings, (a, b) -> a.start == b.start ? a.end - b.end : a.start - b.start);
                for (Binding b : bindings)
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
                        dest.put("line", b.line);
                        dest.put("col", b.col);
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
        List<String> missing = new ArrayList<>();
        List<String> wrongType = new ArrayList<>();
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
            List<Binding> actual = analyzer.references.get(dummy);

            for (Map<String, Object> d : dests)
            {
                String name1 = (String) refMap.get("name");
                String file1 = (String) refMap.get("file");
                int line1 = (int) Math.floor((double) refMap.get("line"));
                int col1 = (int) Math.floor((double) refMap.get("col"));
                String[] type1 = new String[1];

                String fileShort2 = (String) d.get("file");
                String file2 = $.projAbsPath(fileShort2);
                int start2 = (int) Math.floor((double) d.get("start"));
                int end2 = (int) Math.floor((double) d.get("end"));
                int line2 = (int) Math.floor((double) d.get("line"));
                int col2 = (int) Math.floor((double) d.get("col"));
                String type2 = (String) d.get("type");

                if (!checkExist(actual, file2, start2, end2))
                {
                    String variable = name1 + ":" + line1 + ":" + col1;
                    String loc = name1 + ":" + line2 + ":" + col2;
                    if (!file1.equals(fileShort2))
                    {
                        loc = fileShort2 + ":" + loc;
                    }
                    String msg = "Missing reference from " + variable + " to " + loc;
                    missing.add(msg);
                }
                else if (!checkType(actual, file2, start2, end2, type2, type1))
                {
                    String variable = name1 + ":" + line1 + ":" + col1;
                    String loc = name1 + ":" + line2 + ":" + col2;
                    if (!file1.equals(fileShort2))
                    {
                        loc = fileShort2 + ":" + loc;
                    }
                    String msg = "Inferred wrong type for " + variable + ". ";
                    msg += "Localtion: " + loc + ", ";
                    msg += "Expected: " + type1[0] + ", ";
                    msg += "Actual: " + type2 + ".";
                    wrongType.add(msg);
                }
            }
        }

        boolean success = true;

        // record the ref & failed dests if any
        if (missing.isEmpty() && wrongType.isEmpty())
        {
            $.testmsg("   " + testFile);
        }
        else
        {
            $.testmsg(" - " + testFile);
        }

        if (!missing.isEmpty())
        {
            String report = String.join("\n     * ", missing);
            report = "     * " + report;
            $.testmsg(report);
            $.writeFile(missingRefsFile, report);
            success = false;
        }
        else
        {
            $.deleteFile(missingRefsFile);
        }

        if (!wrongType.isEmpty())
        {
            String report = String.join("\n     * ", wrongType);
            report = "     * " + report;
            $.testmsg(report);
            $.writeFile(wrongTypeFile, report);
            success = false;
        } else {
            $.deleteFile(wrongTypeFile);
        }

        return success;
    }

    private boolean checkExist(List<Binding> bindings, String file, int start, int end)
    {
        if (bindings == null)
        {
            return false;
        }

        for (Binding b : bindings)
        {
            if (((b.getFile() == null && file == null) ||
                 (b.getFile() != null && file != null && b.getFile().equals(file))) &&
                b.start == start && b.end == end)
            {
                return true;
            }
        }

        return false;
    }

    private boolean checkType(List<Binding> bindings, String file, int start, int end, String type, String[] actualType)
    {
        if (bindings == null)
        {
            return false;
        }

        for (Binding b : bindings)
        {
            if (((b.getFile() == null && file == null) ||
                 (b.getFile() != null && file != null && b.getFile().equals(file))) &&
                b.start == start && b.end == end && b.type.toString().equals(type))
            {
                return true;
            } else {
                actualType[0] = b.type.toString();
            }
        }

        return false;
    }

    public static Dummy makeDummy(Map<String, Object> m)
    {
        String file = $.projAbsPath((String) m.get("file"));
        int start = (int) Math.floor((double) m.get("start"));
        int end = (int) Math.floor((double) m.get("end"));
        return new Dummy(file, start, end, -1, -1);
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
        boolean result = checkRefs(analyzer);
        return result;
    }

    // ------------------------- static part -----------------------

    @Nullable
    public static List<String> testAll(String path, boolean generate)
    {
        List<String> failed = new ArrayList<>();
        if (generate)
        {
            $.testmsg("Generating tests:");
        }
        else
        {
            $.testmsg("Verifying tests:");
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
