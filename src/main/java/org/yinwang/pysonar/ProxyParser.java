package org.yinwang.pysonar;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.pysonar.ast.*;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class ProxyParser {
    @Nullable
    Process python2Process;
    @Nullable
    Process python3Process;
    private static Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final String PYTHON2_EXE = "python";
    private static final String PYTHON3_EXE = "python3";
    private String exchangeFile;
    private String endMark;
    private String pyStub;

    public ProxyParser() {
        String tmpDir = Util.getSystemTempDir();
        String sid = Util.newSessionId();

        exchangeFile = Util.makePathString(tmpDir, "pysonar2", "json." + sid);
        endMark = Util.makePathString(tmpDir, "pysonar2", "end." + sid);
        pyStub = Util.makePathString(tmpDir, "pysonar2", "ast2json." + sid);

        python2Process = startPython(PYTHON2_EXE);
        python3Process = startPython(PYTHON3_EXE);

        if (python2Process == null && python3Process == null) {
            Util.die("You don't seem to have either of Python or Python3 on PATH");
        }
    }


    public void close() {
        new File(pyStub).delete();
        new File(exchangeFile).delete();
        new File(endMark).delete();
    }


    public Map<String, Object> deserialize(String text) {
        return gson.fromJson(text, Map.class);
    }


    @Nullable
    private Block convertBlock(@Nullable Object o) {
        if (o == null) {
            return null;
        } else {
            return new Block(convertList(o), 0, 0);
        }
    }


    @Nullable
    private List<Node> convertList(@Nullable Object o) {
        if (o == null) {
            return null;
        } else {
            List<Map<String, Object>> in = (List<Map<String, Object>>) o;
            List<Node> out = new ArrayList<Node>();

            for (Map<String, Object> m : in) {
                Node n = deJson(m);
                if (n != null) out.add(n);
            }

            return out;
        }
    }


    @Nullable
    private List<Keyword> convertListKeyword(@Nullable Object o) {
        if (o == null) {
            return null;
        } else {
            List<Map<String, Object>> in = (List<Map<String, Object>>) o;
            List<Keyword> out = new ArrayList<Keyword>();

            for (Map<String, Object> m : in) {
                Node n = deJson(m);
                if (n != null) out.add((Keyword) n);
            }

            return out;
        }
    }


    @Nullable
    private List<ExceptHandler> convertListExceptHandler(@Nullable Object o) {
        if (o == null) {
            return null;
        } else {
            List<Map<String, Object>> in = (List<Map<String, Object>>) o;
            List<ExceptHandler> out = new ArrayList<ExceptHandler>();

            for (Map<String, Object> m : in) {
                Node n = deJson(m);
                if (n != null) out.add((ExceptHandler) n);
            }

            return out;
        }
    }


    @Nullable
    private List<Alias> convertListAlias(@Nullable Object o) {
        if (o == null) {
            return null;
        } else {
            List<Map<String, Object>> in = (List<Map<String, Object>>) o;
            List<Alias> out = new ArrayList<Alias>();

            for (Map<String, Object> m : in) {
                Node n = deJson(m);
                if (n != null) out.add((Alias) n);
            }

            return out;
        }
    }


    @Nullable
    private List<Comprehension> convertListComprehension(@Nullable Object o) {
        if (o == null) {
            return null;
        } else {
            List<Map<String, Object>> in = (List<Map<String, Object>>) o;
            List<Comprehension> out = new ArrayList<Comprehension>();

            for (Map<String, Object> m : in) {
                Node n = deJson(m);
                if (n != null) out.add((Comprehension) n);
            }

            return out;
        }
    }


    @NotNull
    List<Name> segmentQname(@NotNull String qname, int start) {
        List<Name> result = new ArrayList<>();

        for (int i = 0; i < qname.length(); i++) {
            String name = "";
            while (Character.isSpaceChar(qname.charAt(i))) i++;
            int nameStart = i;

            while (i < qname.length() &&
                    (Character.isJavaIdentifierPart(qname.charAt(i)) ||
                            qname.charAt(i) == '*') &&
                    qname.charAt(i) != '.') {
                name += qname.charAt(i);
                i++;
            }

            int nameStop = i;
            result.add(new Name(name, start + nameStart, start + nameStop));
        }

        return result;
    }


    @Nullable
    public Node deJson(Object o) {
        if (!(o instanceof Map)) {
            return null;
        }

        Map<String, Object> map = (Map<String, Object>) o;

        String type = (String) map.get("ast_type");
        Double startDouble = (Double) map.get("node_start");
        Double endDouble = (Double) map.get("node_end");

        int start = startDouble == null ? 0 : startDouble.intValue();
        int end = endDouble == null ? 1 : endDouble.intValue();


        if (type.equals("Module")) {
            Block b = convertBlock(map.get("body"));
            Module m = new Module(b, start, end);
            try {
                m.setFile(Util.unifyPath((String) map.get("filename")));
            } catch (Exception e) {

            }
            return m;
        }

        if (type.equals("alias")) {         // lower case alias
            String qname = (String) map.get("name");
            List<Name> names = segmentQname(qname, start + "import ".length());
            Name asname = map.get("asname") == null ? null : new Name((String) map.get("asname"));
            return new Alias(names, asname, start, end);
        }

        if (type.equals("Assert")) {
            Node test = deJson(map.get("test"));
            Node msg = deJson(map.get("msg"));
            return new Assert(test, msg, start, end);
        }

        if (type.equals("Assign")) {
            List<Node> targets = convertList(map.get("targets"));
            Node value = deJson(map.get("value"));
            return new Assign(targets, value, start, end);
        }

        if (type.equals("Attribute")) {
            Node value = deJson(map.get("value"));
            Name attr = (Name) deJson(map.get("attr_name"));
            if (attr == null) {
                attr = new Name((String) map.get("attr"));
            }
            return new Attribute(value, attr, start, end);
        }

        if (type.equals("AugAssign")) {
            Node target = deJson(map.get("target"));
            Node value = deJson(map.get("value"));
            Name op = (Name) deJson(map.get("op_node"));              // hack
            return new AugAssign(target, value, op, start, end);
        }

        if (type.equals("BinOp")) {
            Node left = deJson(map.get("left"));
            Node right = deJson(map.get("right"));
            Node op = deJson(map.get("op"));
            return new BinOp(left, right, op, start, end);
        }

        if (type.equals("BoolOp")) {
            List<Node> values = convertList(map.get("values"));
            Name op = (Name) deJson(map.get("op_node"));
            return new BoolOp(op, values, start, end);
        }

        if (type.equals("Break")) {
            return new Break(start, end);
        }

        if (type.equals("Call")) {
            Node func = deJson(map.get("func"));
            List<Node> args = convertList(map.get("args"));
            List<Keyword> keywords = convertListKeyword(map.get("keywords"));
            Node kwargs = deJson(map.get("kwarg"));
            Node starargs = deJson(map.get("starargs"));
            return new Call(func, args, keywords, kwargs, starargs, start, end);
        }

        if (type.equals("ClassDef")) {
            Name name = (Name) deJson(map.get("name_node"));      // hack
            List<Node> bases = convertList(map.get("bases"));
            Block body = convertBlock(map.get("body"));
            return new ClassDef(name, bases, body, start, end);
        }

        if (type.equals("Compare")) {
            Node name = deJson(map.get("left"));
            List<Node> ops = convertList(map.get("ops"));
            List<Node> comparators = convertList(map.get("comparators"));
            return new Compare(name, ops, comparators, start, end);
        }

        if (type.equals("comprehension")) {
            Node target = deJson(map.get("target"));
            Node iter = deJson(map.get("iter"));
            List<Node> ifs = convertList(map.get("ifs"));
            return new Comprehension(target, iter, ifs, start, end);
        }

        if (type.equals("Continue")) {
            return new Continue(start, end);
        }

        if (type.equals("Delete")) {
            List<Node> targets = convertList(map.get("targets"));
            return new Delete(targets, start, end);
        }

        if (type.equals("Dict")) {
            List<Node> keys = convertList(map.get("keys"));
            List<Node> values = convertList(map.get("values"));
            return new Dict(keys, values, start, end);
        }

        if (type.equals("DictComp")) {
            Node key = deJson(map.get("key"));
            Node value = deJson(map.get("value"));
            List<Comprehension> generators = convertListComprehension(map.get("generators"));
            return new DictComp(key, value, generators, start, end);
        }

        if (type.equals("Ellipsis")) {
            return new Ellipsis(start, end);
        }

        if (type.equals("ExceptHandler")) {
            Node name = deJson(map.get("name"));
            Node exceptionType = deJson(map.get("type"));
            Block body = convertBlock(map.get("body"));
            return new ExceptHandler(name, exceptionType, body, start, end);
        }

        if (type.equals("Exec")) {
            Node body = deJson(map.get("body"));
            Node globals = deJson(map.get("globals"));
            Node locals = deJson(map.get("locals"));
            return new Exec(body, globals, locals, start, end);
        }

        if (type.equals("Expr")) {
            Node value = deJson(map.get("value"));
            return new Expr(value, start, end);
        }

        if (type.equals("For")) {
            Node target = deJson(map.get("target"));
            Node iter = deJson(map.get("iter"));
            Block body = convertBlock(map.get("body"));
            Block orelse = convertBlock(map.get("orelse"));
            return new For(target, iter, body, orelse, start, end);
        }

        if (type.equals("FunctionDef")) {
            Name name = (Name) deJson(map.get("name_node"));
            Map<String, Object> argsMap = (Map<String, Object>) map.get("args");
            List<Node> args = convertList(argsMap.get("args"));
            List<Node> defaults = convertList(argsMap.get("defaults"));
            Block body = convertBlock(map.get("body"));
            Name vararg = argsMap.get("vararg") == null ? null : new Name((String) argsMap.get("vararg"));
            Name kwarg = argsMap.get("kwarg") == null ? null : new Name((String) argsMap.get("kwarg"));
            return new FunctionDef(name, args, body, defaults, vararg, kwarg, start, end);
        }

        if (type.equals("GeneratorExp")) {
            Node elt = deJson(map.get("elt"));
            List<Comprehension> generators = convertListComprehension(map.get("generators"));
            return new GeneratorExp(elt, generators, start, end);
        }

        if (type.equals("Global")) {
            List<String> names = (List<String>) map.get("names");
            List<Name> nameNodes = new ArrayList<>();
            for (String name : names) {
                nameNodes.add(new Name(name));
            }
            return new Global(nameNodes, start, end);
        }

        if (type.equals("If")) {
            Node test = deJson(map.get("test"));
            Block body = convertBlock(map.get("body"));
            Block orelse = convertBlock(map.get("orelse"));
            return new If(test, body, orelse, start, end);
        }

        if (type.equals("IfExp")) {
            Node test = deJson(map.get("test"));
            Node body = deJson(map.get("body"));
            Node orelse = deJson(map.get("orelse"));
            return new IfExp(test, body, orelse, start, end);
        }


        if (type.equals("Import")) {
            List<Alias> aliases = convertListAlias(map.get("names"));
            return new Import(aliases, start, end);
        }

        if (type.equals("ImportFrom")) {
            String module = (String) map.get("module");
            List<Name> moduleSeg = module == null ? null : segmentQname(module, start + "from ".length());
            List<Alias> names = convertListAlias(map.get("names"));
            int level = ((Double) map.get("level")).intValue();
            return new ImportFrom(moduleSeg, names, level, start, end);
        }

        if (type.equals("Index")) {
            Node value = deJson(map.get("value"));
            return new Index(value, start, end);
        }

        if (type.equals("Keyword")) {
            String arg = (String) map.get("arg");
            Node value = deJson(map.get("op_node"));
            return new Keyword(arg, value, start, end);
        }

        if (type.equals("Lambda")) {
            Map<String, Object> argsMap = (Map<String, Object>) map.get("args");
            List<Node> args = convertList(argsMap.get("args"));
            List<Node> defaults = convertList(argsMap.get("defaults"));
            Node body = deJson(map.get("body"));
            Name vararg = argsMap.get("vararg") == null ? null : new Name((String) argsMap.get("vararg"));
            Name kwarg = argsMap.get("kwarg") == null ? null : new Name((String) argsMap.get("kwarg"));
            return new Lambda(args, body, defaults, vararg, kwarg, start, end);
        }

        if (type.equals("List")) {
            List<Node> elts = convertList(map.get("elts"));
            return new NList(elts, start, end);
        }

        if (type.equals("ListComp")) {
            Node elt = deJson(map.get("elt"));
            List<Comprehension> generators = convertListComprehension(map.get("generators"));
            return new ListComp(elt, generators, start, end);
        }

        if (type.equals("Name")) {
            String id = (String) map.get("id");
            return new Name(id, start, end);
        }

        if (type.equals("Num")) {
            Object n = map.get("n");
            return new Num(n, start, end);
        }

        if (type.equals("SetComp")) {
            Node elt = deJson(map.get("elt"));
            List<Comprehension> generators = convertListComprehension(map.get("generators"));
            return new SetComp(elt, generators, start, end);
        }

        if (type.equals("Pass")) {
            return new Pass(start, end);
        }

        if (type.equals("Print")) {
            List<Node> values = convertList(map.get("values"));
            Node destination = deJson(map.get("destination"));
            return new Print(destination, values, start, end);
        }

        if (type.equals("Raise")) {
            Node exceptionType = deJson(map.get("type"));
            Node inst = deJson(map.get("inst"));
            Node tback = deJson(map.get("tback"));
            return new Raise(exceptionType, inst, tback, start, end);
        }

        if (type.equals("Repr")) {
            Node value = deJson(map.get("value"));
            return new Repr(value, start, end);
        }

        if (type.equals("Return")) {
            Node value = deJson(map.get("value"));
            return new Return(value, start, end);
        }

        if (type.equals("Set")) {
            List<Node> elts = convertList(map.get("elts"));
            return new Set(elts, start, end);
        }

        if (type.equals("SetComp")) {
            Node elt = deJson(map.get("elt"));
            List<Comprehension> generators = convertListComprehension(map.get("generators"));
            return new SetComp(elt, generators, start, end);
        }

        if (type.equals("Slice")) {
            Node lower = deJson(map.get("lower"));
            Node step = deJson(map.get("step"));
            Node upper = deJson(map.get("upper"));
            return new Slice(lower, step, upper, start, end);
        }

        if (type.equals("ExtSlice")) {
            List<Node> dims = convertList(map.get("dims"));
            return new ExtSlice(dims, start, end);
        }

        if (type.equals("Str")) {
            String s = (String) map.get("s");
            return new Str(s, start, end);
        }

        if (type.equals("Subscript")) {
            Node value = deJson(map.get("value"));
            Node slice = deJson(map.get("slice"));
            return new Subscript(value, slice, start, end);
        }

        if (type.equals("TryExcept")) {
            Block body = convertBlock(map.get("body"));
            Block orelse = convertBlock(map.get("orelse"));
            List<ExceptHandler> handlers = convertListExceptHandler(map.get("handlers"));
            return new TryExcept(handlers, body, orelse, start, end);
        }

        if (type.equals("TryFinally")) {
            Block body = convertBlock(map.get("body"));
            Block finalbody = convertBlock(map.get("finalbody"));
            return new TryFinally(body, finalbody, start, end);
        }

        if (type.equals("Tuple")) {
            List<Node> elts = convertList(map.get("elts"));
            return new Tuple(elts, start, end);
        }

        if (type.equals("UnaryOp")) {
            Node op = deJson(map.get("op"));
            Node operand = deJson(map.get("operand"));
            return new UnaryOp(op, operand, start, end);
        }

        if (type.equals("While")) {
            Node test = deJson(map.get("test"));
            Block body = convertBlock(map.get("body"));
            Block orelse = convertBlock(map.get("orelse"));
            return new While(test, body, orelse, start, end);
        }

        if (type.equals("With")) {
            List<Withitem> items = new ArrayList<>();

            Node context_expr = deJson(map.get("context_expr"));
            Node optional_vars = deJson(map.get("optional_vars"));
            Block body = convertBlock(map.get("body"));

            // Python 3 puts context_expr and optional_vars inside "items"
            if (context_expr != null) {
                Withitem item = new Withitem(context_expr, optional_vars, -1, -1);
                items.add(item);
            } else {
                List<Map<String, Object>> itemsMap = (List<Map<String, Object>>) map.get("items");

                for (Map<String, Object> m : itemsMap) {
                    context_expr = deJson(m.get("context_expr"));
                    optional_vars = deJson(m.get("optional_vars"));
                    Withitem item = new Withitem(context_expr, optional_vars, -1, -1);
                    items.add(item);
                }
            }

            return new With(items, body, start, end);
        }

        if (type.equals("Yield")) {
            Node value = deJson(map.get("value"));
            return new Yield(value, start, end);
        }

        // default
        return null;
    }


    public String prettyJson(String json) {
        Map<String, Object> obj = gson.fromJson(json, Map.class);
        return gson.toJson(obj);
    }


    @Nullable
    public Process startPython(String pythonExe) {
        try {
            InputStream jsonize = Thread.currentThread().getContextClassLoader().getResourceAsStream("org/yinwang/pysonar/ast2json.py");
            String jsonizeStr = Util.readWholeStream(jsonize);
            FileWriter fw = new FileWriter(pyStub);
            fw.write(jsonizeStr);
            fw.close();
            ProcessBuilder builder = new ProcessBuilder(pythonExe, "-i", pyStub);
            builder.redirectErrorStream(true);
            builder.environment().remove("PYTHONPATH");
            Process p =  builder.start();
            Util.msg("Started process: " + pythonExe);
            return p;
        } catch (Exception e) {
            Util.msg("Not found: " + pythonExe);
            return null;
        }
    }


    @Nullable
    public Node parseFile(String filename) {
        Node n2 = parseFileInner(filename, python2Process);
        if (n2 != null) {
            return n2;
        } else if (python3Process != null) {
            Node n3 = parseFileInner(filename, python3Process);
            if (n3 == null) {
                return null;
            } else {
                return n3;
            }
        } else {
            Indexer.idx.failedToParse.add(filename);
            return null;
        }
    }


    @Nullable
    public Node parseFileInner(String filename, @NotNull Process pythonProcess) {
//        Util.msg("parsing: " + filename);

        File exchange = new File(exchangeFile);
        File marker = new File(endMark);
        exchange.delete();
        marker.delete();

        try {
            OutputStreamWriter writer = new OutputStreamWriter(pythonProcess.getOutputStream());
            writer.write("parse_file('" + filename + "', '" + exchangeFile + "', '" + endMark + "')\n");
            writer.flush();
        } catch (Exception e) {
            Util.msg("\nFailed to send file to Python: " + filename);
            exchange.delete();
            marker.delete();
            return null;
        }


        while (!marker.exists()) {
            try {
                Thread.sleep(1);
            } catch (Exception e) {
                break;
            }
        }

        String json;
        try {
            json = Util.readFile(exchangeFile);
        } catch (Exception e) {
            exchange.delete();
            marker.delete();
            return null;
        }

        exchange.delete();
        marker.delete();


//            Util.msg("json: " + json);
        if (json != null) {
            Map<String, Object> map = deserialize(json);
            return deJson(map);
        } else {
            return null;
        }
    }

}
