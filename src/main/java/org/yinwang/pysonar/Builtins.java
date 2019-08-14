package org.yinwang.pysonar;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.pysonar.ast.PyModule;
import org.yinwang.pysonar.ast.Url;
import org.yinwang.pysonar.types.*;

import java.util.HashMap;
import java.util.Map;

import static org.yinwang.pysonar.Binding.Kind.*;

/**
 * This file is messy. Should clean up.
 */
public class Builtins {

    public static final String LIBRARY_URL = "http://docs.python.org/library/";
    public static final String TUTORIAL_URL = "http://docs.python.org/tutorial/";
    public static final String REFERENCE_URL = "http://docs.python.org/reference/";
    public static final String DATAMODEL_URL = "http://docs.python.org/reference/datamodel#";


    @NotNull
    public static Url newLibUrl(String module, String name) {
        return newLibUrl(module + ".html#" + name);
    }


    @NotNull
    public static Url newLibUrl(@NotNull String path) {
        if (!path.contains("#") && !path.endsWith(".html")) {
            path += ".html";
        }
        return new Url(LIBRARY_URL + path);
    }


    @NotNull
    public static Url newRefUrl(String path) {
        return new Url(REFERENCE_URL + path);
    }


    @NotNull
    public static Url newDataModelUrl(String path) {
        return new Url(DATAMODEL_URL + path);
    }


    @NotNull
    public static Url newTutUrl(String path) {
        return new Url(TUTORIAL_URL + path);
    }


    // XXX:  need to model "types" module and reconcile with these types
    public ModuleType Builtin;
    public ClassType objectType;
    public ClassType BaseType;
    public ClassType BaseList;
    public InstanceType BaseListInst;
    public ClassType BaseArray;
    public ClassType BaseTuple;
    public ClassType BaseModule;
    public ClassType BaseFile;
    public InstanceType BaseFileInst;
    public ClassType BaseException;
    public ClassType BaseStruct;
    public ClassType BaseFunction;  // models functions, lambas and methods
    public ClassType BaseClass;  // models classes and instances

    public ClassType Datetime_datetime;
    public ClassType Datetime_date;
    public ClassType Datetime_time;
    public ClassType Datetime_timedelta;
    public ClassType Datetime_tzinfo;
    public InstanceType Time_struct_time;


    @NotNull
    String[] builtin_exception_types = {
            "ArithmeticError", "AssertionError", "AttributeError",
            "BaseException", "Exception", "DeprecationWarning", "EOFError",
            "EnvironmentError", "FloatingPointError", "FutureWarning",
            "GeneratorExit", "IOError", "ImportError", "ImportWarning",
            "IndentationError", "IndexError", "KeyError", "KeyboardInterrupt",
            "LookupError", "MemoryError", "NameError", "NotImplemented",
            "NotImplementedError", "OSError", "OverflowError",
            "PendingDeprecationWarning", "ReferenceError", "RuntimeError",
            "RuntimeWarning", "StandardError", "StopIteration", "SyntaxError",
            "SyntaxWarning", "SystemError", "SystemExit", "TabError",
            "TypeError", "UnboundLocalError", "UnicodeDecodeError",
            "UnicodeEncodeError", "UnicodeError", "UnicodeTranslateError",
            "UnicodeWarning", "UserWarning", "ValueError", "Warning",
            "ZeroDivisionError"
    };


    @NotNull
    ClassType newClass(@NotNull String name, State table) {
        return newClass(name, table, null);
    }


    @NotNull
    ClassType newClass(@NotNull String name, State table,
                       ClassType superClass, @NotNull ClassType... moreSupers)
    {
        ClassType t = new ClassType(name, table, superClass);
        for (ClassType c : moreSupers) {
            t.addSuper(c);
        }
        return t;
    }


    @Nullable
    ModuleType newModule(String name) {
        return new ModuleType(name, null, Analyzer.self.globaltable);
    }


    @NotNull
    ClassType newException(@NotNull String name, State t) {
        return newClass(name, t, BaseException);
    }


    @NotNull
    FunType newFunc() {
        return new FunType();
    }


    @Nullable
    FunType newFunc(@Nullable Type type) {
        if (type == null) {
            type = Types.UNKNOWN;
        }
        return new FunType(Types.UNKNOWN, type);
    }


    @NotNull
    ListType newList() {
        return newList(Types.UNKNOWN);
    }


    @NotNull
    ListType newList(Type type) {
        return new ListType(type);
    }


    @NotNull
    DictType newDict(Type ktype, Type vtype) {
        return new DictType(ktype, vtype);
    }


    @NotNull
    TupleType newTuple(Type... types) {
        return new TupleType(types);
    }


    @NotNull
    UnionType newUnion(Type... types) {
        return new UnionType(types);
    }


    String[] list(String... names) {
        return names;
    }


    private abstract class NativeModule {

        protected String name;
        @Nullable
        protected ModuleType module;
        @Nullable
        protected State table;  // the module's symbol table


        NativeModule(String name) {
            this.name = name;
            modules.put(name, this);
        }


        /**
         * Lazily load the module.
         */
        @Nullable
        ModuleType getModule() {
            if (module == null) {
                createModuleType();
                initBindings();
            }
            return module;
        }


        protected abstract void initBindings();


        protected void createModuleType() {
            if (module == null) {
                module = newModule(name);
                table = module.table;
                Analyzer.self.moduleTable.insert(name, liburl(), module, MODULE);
            }
        }


        @Nullable
        protected void update(String name, Url url, Type type, Binding.Kind kind) {
            table.insert(name, url, type, kind);
        }


        @Nullable
        protected void addClass(String name, Url url, Type type) {
            table.insert(name, url, type, CLASS);
        }


        @Nullable
        protected void addClass(ClassType type) {
            table.insert(type.name, liburl(type.name), type, CLASS);
        }


        @Nullable
        protected void addMethod(ClassType cls, String name, Type type) {
            cls.table.insert(name, liburl(cls.name + "." + name), newFunc(type), METHOD);
        }

        @Nullable
        protected void addMethod(ClassType cls, String name) {
            cls.table.insert(name, liburl(cls.name + "." + name), newFunc(), METHOD);
        }


        protected void addFunction(ModuleType module, String name, Type type) {
            Url url = this.module == module ? liburl(module.qname + "." + name) :
                newLibUrl(module.table.path, module.table.path + "." + name);
            module.table.insert(name, url, newFunc(type), FUNCTION);
        }


        protected void addFunction(String name, Type type) {
            addFunction(module, name, type);
        }


        // don't use this unless you're sure it's OK to share the type object
        protected void addFunctions_beCareful(Type type, @NotNull String... names) {
            for (String name : names) {
                addFunction(name, type);
            }
        }


        protected void addNoneFuncs(String... names) {
            addFunctions_beCareful(Types.NoneInstance, names);
        }


        protected void addNumFuncs(String... names) {
            addFunctions_beCareful(Types.IntInstance, names);
        }


        protected void addStrFuncs(String... names) {
            addFunctions_beCareful(Types.StrInstance, names);
        }


        protected void addUnknownFuncs(@NotNull String... names) {
            for (String name : names) {
                addFunction(name, Types.UNKNOWN);
            }
        }


        protected void addAttr(String name, Url url, Type type) {
            table.insert(name, url, type, ATTRIBUTE);
        }


        protected void addAttr(String name, Type type) {
            addAttr(table, name, type);
        }


        protected void addAttr(State s, String name, Type type) {
            s.insert(name, liburl(s.path + "." + name), type, ATTRIBUTE);
        }


        protected void addAttr(ClassType cls, String name, Type type) {
            addAttr(cls.table, name, type);
        }

        // don't use this unless you're sure it's OK to share the type object
        protected void addAttributes_beCareful(Type type, @NotNull String... names) {
            for (String name : names) {
                addAttr(name, type);
            }
        }


        protected void addNumAttrs(String... names) {
            addAttributes_beCareful(Types.IntInstance, names);
        }


        protected void addStrAttrs(String... names) {
            addAttributes_beCareful(Types.StrInstance, names);
        }


        protected void addUnknownAttrs(@NotNull String... names) {
            for (String name : names) {
                addAttr(name, Types.UNKNOWN);
            }
        }


        @NotNull
        protected Url liburl() {
            return newLibUrl(name);
        }


        @NotNull
        protected Url liburl(String anchor) {
            return newLibUrl(name, anchor);
        }

        @NotNull
        @Override
        public String toString() {
            return module == null
                    ? "<Non-loaded builtin module '" + name + "'>"
                    : "<NativeModule:" + module + ">";
        }
    }


    /**
     * The set of top-level native modules.
     */
    @NotNull
    private Map<String, NativeModule> modules = new HashMap<>();


    public Builtins() {
        buildTypes();
    }


    private void buildTypes() {
        new BuiltinsModule();
        State bt = Builtin.table;

        objectType = newClass("object", bt);
        BaseType = newClass("type", bt, objectType);
        BaseTuple = newClass("tuple", bt, objectType);
        BaseList = newClass("list", bt, objectType);
        BaseListInst = new InstanceType(BaseList);
        BaseArray = newClass("array", bt);
        ClassType numClass = newClass("int", bt, objectType);
        BaseModule = newClass("module", bt);
        BaseFile = newClass("file", bt, objectType);
        BaseFileInst = new InstanceType(BaseFile);
        BaseFunction = newClass("function", bt, objectType);
        BaseClass = newClass("classobj", bt, objectType);
    }


    void init() {
        buildObjectType();
        buildTupleType();
        buildArrayType();
        buildListType();
        buildDictType();
        buildNumTypes();
        buildStrType();
        buildModuleType();
        buildFileType();
        buildFunctionType();
        buildClassType();

        modules.get("__builtin__").initBindings();  // eagerly load these bindings

        new ArrayModule();
        new AudioopModule();
        new BinasciiModule();
        new Bz2Module();
        new CPickleModule();
        new CStringIOModule();
        new CMathModule();
        new CollectionsModule();
        new CryptModule();
        new CTypesModule();
        new DatetimeModule();
        new DbmModule();
        new ErrnoModule();
        new ExceptionsModule();
        new FcntlModule();
        new FpectlModule();
        new GcModule();
        new GdbmModule();
        new GrpModule();
        new ImpModule();
        new ItertoolsModule();
        new MarshalModule();
        new MathModule();
        new Md5Module();
        new MmapModule();
        new NisModule();
        new OperatorModule();
        new OsModule();
        new ParserModule();
        new PosixModule();
        new PwdModule();
        new PyexpatModule();
        new ReadlineModule();
        new ResourceModule();
        new SelectModule();
        new SignalModule();
        new ShaModule();
        new SpwdModule();
        new StropModule();
        new StructModule();
        new SysModule();
        new SyslogModule();
        new TermiosModule();
        new ThreadModule();
        new TimeModule();
        new UnicodedataModule();
        new ZipimportModule();
        new ZlibModule();
        new UnittestModule();
    }


    /**
     * Loads (if necessary) and returns the specified built-in module.
     */
    @Nullable
    public ModuleType get(@NotNull String name) {
        if (!name.contains(".")) {  // unqualified
            return getModule(name);
        }

        String[] mods = name.split("\\.");
        Type type = getModule(mods[0]);
        if (type == null) {
            return null;
        }
        for (int i = 1; i < mods.length; i++) {
            type = type.table.lookupType(mods[i]);
            if (!(type instanceof ModuleType)) {
                return null;
            }
        }
        return (ModuleType) type;
    }


    @Nullable
    private ModuleType getModule(String name) {
        NativeModule wrap = modules.get(name);
        return wrap == null ? null : wrap.getModule();
    }


    void buildObjectType() {
        String[] obj_methods = {
                "__delattr__", "__format__", "__getattribute__", "__hash__",
                "__init__", "__new__", "__reduce__", "__reduce_ex__",
                "__repr__", "__setattr__", "__sizeof__", "__str__", "__subclasshook__"
        };
        for (String m : obj_methods) {
            objectType.table.insert(m, newLibUrl("stdtypes"), newFunc(), METHOD);
        }
        objectType.table.insert("__doc__", newLibUrl("stdtypes"), Types.StrInstance, CLASS);
        objectType.table.insert("__class__", newLibUrl("stdtypes"), Types.UNKNOWN, CLASS);
    }


    void buildTupleType() {
        State bt = BaseTuple.table;
        String[] tuple_methods = {
                "__add__", "__contains__", "__eq__", "__ge__", "__getnewargs__",
                "__gt__", "__iter__", "__le__", "__len__", "__lt__", "__mul__",
                "__ne__", "__new__", "__rmul__", "count", "index"
        };
        for (String m : tuple_methods) {
            bt.insert(m, newLibUrl("stdtypes"), newFunc(), METHOD);
        }
        bt.insert("__getslice__", newDataModelUrl("object.__getslice__"), newFunc(), METHOD);
        bt.insert("__getitem__", newDataModelUrl("object.__getitem__"), newFunc(), METHOD);
        bt.insert("__iter__", newDataModelUrl("object.__iter__"), newFunc(), METHOD);
    }


    void buildArrayType() {
        String[] array_methods_none = {
                "append", "buffer_info", "byteswap", "extend", "fromfile",
                "fromlist", "fromstring", "fromunicode", "index", "insert", "pop",
                "read", "remove", "reverse", "tofile", "tolist", "typecode", "write"
        };
        for (String m : array_methods_none) {
            BaseArray.table.insert(m, newLibUrl("array"), newFunc(Types.NoneInstance), METHOD);
        }
        String[] array_methods_num = {"count", "itemsize",};
        for (String m : array_methods_num) {
            BaseArray.table.insert(m, newLibUrl("array"), newFunc(Types.IntInstance), METHOD);
        }
        String[] array_methods_str = {"tostring", "tounicode",};
        for (String m : array_methods_str) {
            BaseArray.table.insert(m, newLibUrl("array"), newFunc(Types.StrInstance), METHOD);
        }
    }


    void buildListType() {
        BaseList.table.insert("__getslice__", newDataModelUrl("object.__getslice__"),
                newFunc(BaseListInst), METHOD);
        BaseList.table.insert("__getitem__", newDataModelUrl("object.__getitem__"),
                newFunc(BaseList), METHOD);
        BaseList.table.insert("__iter__", newDataModelUrl("object.__iter__"),
                newFunc(BaseList), METHOD);

        String[] list_methods_none = {
                "append", "extend", "index", "insert", "pop", "remove", "reverse", "sort"
        };
        for (String m : list_methods_none) {
            BaseList.table.insert(m, newLibUrl("stdtypes"), newFunc(Types.NoneInstance), METHOD);
        }
        String[] list_methods_num = {"count"};
        for (String m : list_methods_num) {
            BaseList.table.insert(m, newLibUrl("stdtypes"), newFunc(Types.IntInstance), METHOD);
        }
    }


    @NotNull
    Url numUrl() {
        return newLibUrl("stdtypes", "typesnumeric");
    }


    void buildNumTypes() {
        State bft = Types.FloatInstance.table;
        String[] float_methods_num = {
                "__abs__", "__add__", "__coerce__", "__div__", "__divmod__",
                "__eq__", "__float__", "__floordiv__", "__format__",
                "__ge__", "__getformat__", "__gt__", "__int__",
                "__le__", "__long__", "__lt__", "__mod__", "__mul__", "__ne__",
                "__neg__", "__new__", "__nonzero__", "__pos__", "__pow__",
                "__radd__", "__rdiv__", "__rdivmod__", "__rfloordiv__", "__rmod__",
                "__rmul__", "__rpow__", "__rsub__", "__rtruediv__", "__setformat__",
                "__sub__", "__truediv__", "__trunc__", "as_integer_ratio",
                "fromhex", "is_integer"
        };
        for (String m : float_methods_num) {
            bft.insert(m, numUrl(), newFunc(Types.FloatInstance), METHOD);
        }
        State bnt = Types.IntInstance.table;
        String[] num_methods_num = {
                "__abs__", "__add__", "__and__",
                "__class__", "__cmp__", "__coerce__", "__delattr__", "__div__",
                "__divmod__", "__doc__", "__float__", "__floordiv__",
                "__getattribute__", "__getnewargs__", "__hash__", "__hex__",
                "__index__", "__init__", "__int__", "__invert__", "__long__",
                "__lshift__", "__mod__", "__mul__", "__neg__", "__new__",
                "__nonzero__", "__oct__", "__or__", "__pos__", "__pow__",
                "__radd__", "__rand__", "__rdiv__", "__rdivmod__",
                "__reduce__", "__reduce_ex__", "__repr__", "__rfloordiv__",
                "__rlshift__", "__rmod__", "__rmul__", "__ror__", "__rpow__",
                "__rrshift__", "__rshift__", "__rsub__", "__rtruediv__",
                "__rxor__", "__setattr__", "__str__", "__sub__", "__truediv__",
                "__xor__"
        };
        for (String m : num_methods_num) {
            bnt.insert(m, numUrl(), newFunc(Types.IntInstance), METHOD);
        }
        bnt.insert("__getnewargs__", numUrl(), newFunc(newTuple(Types.IntInstance)), METHOD);
        bnt.insert("hex", numUrl(), newFunc(Types.StrInstance), METHOD);
        bnt.insert("conjugate", numUrl(), newFunc(Types.ComplexInstance), METHOD);

        State bct = Types.ComplexInstance.table;
        String[] complex_methods = {
                "__abs__", "__add__", "__div__", "__divmod__",
                "__float__", "__floordiv__", "__format__", "__getformat__", "__int__",
                "__long__", "__mod__", "__mul__", "__neg__", "__new__",
                "__pos__", "__pow__", "__radd__", "__rdiv__", "__rdivmod__",
                "__rfloordiv__", "__rmod__", "__rmul__", "__rpow__", "__rsub__",
                "__rtruediv__", "__sub__", "__truediv__", "conjugate"
        };
        for (String c : complex_methods) {
            bct.insert(c, numUrl(), newFunc(Types.ComplexInstance), METHOD);
        }
        String[] complex_methods_num = {
                "__eq__", "__ge__", "__gt__", "__le__", "__lt__", "__ne__",
                "__nonzero__", "__coerce__"
        };
        for (String cn : complex_methods_num) {
            bct.insert(cn, numUrl(), newFunc(Types.IntInstance), METHOD);
        }
        bct.insert("__getnewargs__", numUrl(), newFunc(newTuple(Types.ComplexInstance)), METHOD);
        bct.insert("imag", numUrl(), Types.IntInstance, ATTRIBUTE);
        bct.insert("real", numUrl(), Types.IntInstance, ATTRIBUTE);
    }


    void buildStrType() {
        Types.StrInstance.table.insert("__getslice__", newDataModelUrl("object.__getslice__"),
                                       newFunc(Types.StrInstance), METHOD);
        Types.StrInstance.table.insert("__getitem__", newDataModelUrl("object.__getitem__"),
                                       newFunc(Types.StrInstance), METHOD);
        Types.StrInstance.table.insert("__iter__", newDataModelUrl("object.__iter__"),
                                       newFunc(Types.StrInstance), METHOD);

        String[] str_methods_str = {
                "capitalize", "center", "decode", "encode", "expandtabs", "format",
                "index", "join", "ljust", "lower", "lstrip", "partition", "replace",
                "rfind", "rindex", "rjust", "rpartition", "rsplit", "rstrip",
                "strip", "swapcase", "title", "translate", "upper", "zfill"
        };
        for (String m : str_methods_str) {
            Types.StrInstance.table.insert(m, newLibUrl("stdtypes", "str." + m),
                                           newFunc(Types.StrInstance), METHOD);
        }

        String[] str_methods_num = {
                "count", "isalnum", "isalpha", "isdigit", "islower", "isspace",
                "istitle", "isupper", "find", "startswith", "endswith"
        };
        for (String m : str_methods_num) {
            Types.StrInstance.table.insert(m, newLibUrl("stdtypes", "str." + m),
                                           newFunc(Types.IntInstance), METHOD);
        }

        String[] str_methods_list = {"split", "splitlines"};
        for (String m : str_methods_list) {
            Types.StrInstance.table.insert(m, newLibUrl("stdtypes", "str." + m),
                                           newFunc(newList(Types.StrInstance)), METHOD);
        }
        Types.StrInstance.table.insert("partition", newLibUrl("stdtypes", "str.partition"),
                                       newFunc(newTuple(Types.StrInstance)), METHOD);
    }


    void buildModuleType() {
        String[] attrs = {"__doc__", "__file__", "__name__", "__package__"};
        for (String m : attrs) {
            BaseModule.table.insert(m, newTutUrl("modules.html"), Types.StrInstance, ATTRIBUTE);
        }
        BaseModule.table.insert("__dict__", newLibUrl("stdtypes", "modules"),
                                newDict(Types.StrInstance, Types.UNKNOWN), ATTRIBUTE);
    }


    void buildDictType() {
        String url = "datastructures.html#dictionaries";
        State bt = Types.BaseDict.table;

        bt.insert("__getitem__", newTutUrl(url), newFunc(), METHOD);
        bt.insert("__iter__", newTutUrl(url), newFunc(), METHOD);
        bt.insert("get", newTutUrl(url), newFunc(), METHOD);

        bt.insert("items", newTutUrl(url),
                  newFunc(newList(newTuple(Types.UNKNOWN, Types.UNKNOWN))), METHOD);

        bt.insert("keys", newTutUrl(url), newFunc(BaseList), METHOD);
        bt.insert("values", newTutUrl(url), newFunc(BaseList), METHOD);

        String[] dict_method_unknown = {
                "clear", "copy", "fromkeys", "get", "iteritems", "iterkeys",
                "itervalues", "pop", "popitem", "setdefault", "update"
        };
        for (String m : dict_method_unknown) {
            bt.insert(m, newTutUrl(url), newFunc(), METHOD);
        }

        String[] dict_method_num = {"has_key"};
        for (String m : dict_method_num) {
            bt.insert(m, newTutUrl(url), newFunc(Types.IntInstance), METHOD);
        }
    }


    void buildFileType() {
        State table = BaseFile.table;

        table.insert("__enter__", newLibUrl("stdtypes", "contextmanager.__enter__"), newFunc(), METHOD);
        table.insert("__exit__", newLibUrl("stdtypes", "contextmanager.__exit__"), newFunc(), METHOD);
        table.insert("__iter__", newLibUrl("stdtypes", "iterator-types"), newFunc(), METHOD);

        String[] file_methods_unknown = {
            "__enter__", "__exit__", "__iter__", "flush", "readinto", "truncate"
        };
        for (String m : file_methods_unknown) {
            table.insert(m, newLibUrl("stdtypes", "file." + m), newFunc(), METHOD);
        }

        String[] methods_str = {"next", "read", "readline"};
        for (String m : methods_str) {
            table.insert(m, newLibUrl("stdtypes", "file." + m), newFunc(Types.StrInstance), METHOD);
        }

        String[] num = {"fileno", "isatty", "tell"};
        for (String m : num) {
            table.insert(m, newLibUrl("stdtypes", "file." + m), newFunc(Types.IntInstance), METHOD);
        }

        String[] methods_none = {"close", "seek", "write", "writelines"};
        for (String m : methods_none) {
            table.insert(m, newLibUrl("stdtypes", "file." + m), newFunc(Types.NoneInstance), METHOD);
        }

        table.insert("readlines", newLibUrl("stdtypes", "file.readlines"), newFunc(newList(Types.StrInstance)), METHOD);
        table.insert("xreadlines", newLibUrl("stdtypes", "file.xreadlines"), newFunc(Types.StrInstance), METHOD);
        table.insert("closed", newLibUrl("stdtypes", "file.closed"), Types.IntInstance, ATTRIBUTE);
        table.insert("encoding", newLibUrl("stdtypes", "file.encoding"), Types.StrInstance, ATTRIBUTE);
        table.insert("errors", newLibUrl("stdtypes", "file.errors"), Types.UNKNOWN, ATTRIBUTE);
        table.insert("mode", newLibUrl("stdtypes", "file.mode"), Types.IntInstance, ATTRIBUTE);
        table.insert("name", newLibUrl("stdtypes", "file.name"), Types.StrInstance, ATTRIBUTE);
        table.insert("softspace", newLibUrl("stdtypes", "file.softspace"), Types.IntInstance, ATTRIBUTE);
        table.insert("newlines", newLibUrl("stdtypes", "file.newlines"), newUnion(Types.StrInstance, newTuple(Types.StrInstance)), ATTRIBUTE);
    }


    void buildFunctionType() {
        State t = BaseFunction.table;

        for (String s : list("func_doc", "__doc__", "func_name", "__name__", "__module__")) {
            t.insert(s, new Url(DATAMODEL_URL), Types.StrInstance, ATTRIBUTE);
        }

        t.insert("func_closure", new Url(DATAMODEL_URL), newTuple(), ATTRIBUTE);
        t.insert("func_code", new Url(DATAMODEL_URL), Types.UNKNOWN, ATTRIBUTE);
        t.insert("func_defaults", new Url(DATAMODEL_URL), newTuple(), ATTRIBUTE);
        t.insert("func_globals", new Url(DATAMODEL_URL), new DictType(Types.StrInstance, Types.UNKNOWN),
                ATTRIBUTE);
        t.insert("func_dict", new Url(DATAMODEL_URL), new DictType(Types.StrInstance, Types.UNKNOWN), ATTRIBUTE);

        // Assume any function can become a method, for simplicity.
        for (String s : list("__func__", "im_func")) {
            t.insert(s, new Url(DATAMODEL_URL), new FunType(), METHOD);
        }
    }


    // XXX:  finish wiring this up.  ClassType needs to inherit from it somehow,
    // so we can remove the per-instance attributes from NClassDef.
    void buildClassType() {
        State t = BaseClass.table;

        for (String s : list("__name__", "__doc__", "__module__")) {
            t.insert(s, new Url(DATAMODEL_URL), Types.StrInstance, ATTRIBUTE);
        }

        t.insert("__dict__", new Url(DATAMODEL_URL), new DictType(Types.StrInstance, Types.UNKNOWN), ATTRIBUTE);
    }


    class BuiltinsModule extends NativeModule {
        public BuiltinsModule() {
            super("__builtin__");
            Builtin = module = newModule(name);
            table = module.table;
        }


        @Nullable
        protected void addFunction(String name, Url url, Type type) {
            table.insert(name, url, newFunc(type), FUNCTION);
        }


        @Override
        public void initBindings() {
            Analyzer.self.moduleTable.insert(name, liburl(), module, MODULE);
            table.addSuper(BaseModule.table);

            addClass("object", newLibUrl("functions", "object"), Types.ObjectClass);
            addFunction("type", newLibUrl("functions", "type"), Types.TypeClass);

            addFunction("bool", newLibUrl("functions", "bool"), Types.BoolInstance);
            addClass("int", newLibUrl("functions", "int"), Types.IntClass);
            addClass("str", newLibUrl("functions", "func-str"), Types.StrClass);
            addClass("long", newLibUrl("functions", "long"), Types.LongClass);
            addClass("float", newLibUrl("functions", "float"), Types.FloatClass);
                addClass("complex", newLibUrl("functions", "complex"), Types.ComplexClass);

            addClass("None", newLibUrl("constants", "None"), Types.NoneInstance);

            addClass("dict", newLibUrl("stdtypes", "typesmapping"), Types.BaseDict);
            addFunction("file", newLibUrl("functions", "file"), BaseFileInst);
            addFunction("list", newLibUrl("functions", "list"), new InstanceType(BaseList));
            addFunction("tuple", newLibUrl("functions", "tuple"), new InstanceType(BaseTuple));

            // XXX:  need to model the following as built-in class types:
            //   basestring, bool, buffer, frozenset, property, set, slice,
            //   staticmethod, super and unicode
            String[] builtin_func_unknown = {
                    "apply", "basestring", "callable", "classmethod",
                    "coerce", "compile", "copyright", "credits", "delattr", "enumerate",
                    "eval", "execfile", "exit", "filter", "frozenset", "getattr",
                    "help", "input", "intern", "iter", "license", "long",
                    "property", "quit", "raw_input", "reduce", "reload", "reversed",
                    "set", "setattr", "slice", "sorted", "staticmethod", "super",
                    "type", "unichr", "unicode",
            };
            for (String f : builtin_func_unknown) {
                addFunction(f, newLibUrl("functions", f), Types.UNKNOWN);
            }

            String[] builtin_func_num = {
                    "abs", "all", "any", "cmp", "coerce", "divmod",
                    "hasattr", "hash", "id", "isinstance", "issubclass", "len", "max",
                    "min", "ord", "pow", "round", "sum"
            };
            for (String f : builtin_func_num) {
                addFunction(f, newLibUrl("functions", f), Types.IntInstance);
            }

            for (String f : list("hex", "oct", "repr", "chr")) {
                addFunction(f, newLibUrl("functions", f), Types.StrInstance);
            }

            addFunction("dir", newLibUrl("functions", "dir"), newList(Types.StrInstance));
            addFunction("map", newLibUrl("functions", "map"), newList(Types.UNKNOWN));
            addFunction("range", newLibUrl("functions", "range"), newList(Types.IntInstance));
            addFunction("xrange", newLibUrl("functions", "range"), newList(Types.IntInstance));
            addFunction("buffer", newLibUrl("functions", "buffer"), newList(Types.UNKNOWN));
            addFunction("zip", newLibUrl("functions", "zip"), newList(newTuple(Types.UNKNOWN)));


            for (String f : list("globals", "vars", "locals")) {
                addFunction(f, newLibUrl("functions.html#" + f), newDict(Types.StrInstance, Types.UNKNOWN));
            }

            for (String f : builtin_exception_types) {
                addClass(f, newLibUrl("exceptions", f),
                        newClass(f, Analyzer.self.globaltable, objectType));
            }
            BaseException = (ClassType) table.lookupType("BaseException");

            addAttr("True", newLibUrl("constants", "True"), Types.BoolInstance);
            addAttr("False", newLibUrl("constants", "False"), Types.BoolInstance);
            addAttr("None", newLibUrl("constants", "None"), Types.NoneInstance);
            addFunction("open", newTutUrl("inputoutput.html#reading-and-writing-files"), BaseFileInst);
            addFunction("__import__", newLibUrl("functions", "__import__"), newModule("<?>"));

            Analyzer.self.globaltable.insert("__builtins__", liburl(), module, ATTRIBUTE);
            Analyzer.self.globaltable.putAll(table);
        }
    }


    class ArrayModule extends NativeModule {
        public ArrayModule() {
            super("array");
        }


        @Override
        public void initBindings() {
            addClass("array", liburl("array.array"), BaseArray);
            addClass("ArrayType", liburl("array.ArrayType"), BaseArray);
        }
    }


    class AudioopModule extends NativeModule {
        public AudioopModule() {
            super("audioop");
        }


        @Override
        public void initBindings() {
            addClass(newException("error", table));

            addStrFuncs("add", "adpcm2lin", "alaw2lin", "bias", "lin2alaw", "lin2lin",
                    "lin2ulaw", "mul", "reverse", "tomono", "ulaw2lin");

            addNumFuncs("avg", "avgpp", "cross", "findfactor", "findmax",
                    "getsample", "max", "maxpp", "rms");

            for (String s : list("adpcm2lin", "findfit", "lin2adpcm", "minmax", "ratecv")) {
                addFunction(s, newTuple());
            }
        }
    }


    class BinasciiModule extends NativeModule {
        public BinasciiModule() {
            super("binascii");
        }


        @Override
        public void initBindings() {
            addStrFuncs(
                    "a2b_uu", "b2a_uu", "a2b_base64", "b2a_base64", "a2b_qp",
                    "b2a_qp", "a2b_hqx", "rledecode_hqx", "rlecode_hqx", "b2a_hqx",
                    "b2a_hex", "hexlify", "a2b_hex", "unhexlify");

            addNumFuncs("crc_hqx", "crc32");

            addClass(newException("Error", table));
            addClass(newException("Incomplete", table));
        }
    }


    class Bz2Module extends NativeModule {
        public Bz2Module() {
            super("bz2");
        }


        @Override
        public void initBindings() {
            ClassType bz2 = newClass("BZ2File", table, BaseFile);  // close enough.
            addClass(bz2);

            ClassType bz2c = newClass("BZ2Compressor", table, objectType);
            addMethod(bz2c, "compress", Types.StrInstance);
            addMethod(bz2c, "flush", Types.NoneInstance);
            addClass(bz2c);

            ClassType bz2d = newClass("BZ2Decompressor", table, objectType);
            addMethod(bz2d, "decompress", Types.StrInstance);
            addClass(bz2d);

            addFunction("compress", Types.StrInstance);
            addFunction("decompress", Types.StrInstance);
        }
    }


    class CPickleModule extends NativeModule {
        public CPickleModule() {
            super("cPickle");
        }


        @NotNull
        @Override
        protected Url liburl() {
            return newLibUrl("pickle", "module-cPickle");
        }


        @Override
        public void initBindings() {
            addUnknownFuncs("dump", "load", "dumps", "loads");

            addClass(newException("PickleError", table));

            ClassType picklingError = newException("PicklingError", table);
            addClass(picklingError);
            update("UnpickleableError", liburl(table.path + "." + "UnpickleableError"),
                    newClass("UnpickleableError", table, picklingError), CLASS);
            ClassType unpicklingError = newException("UnpicklingError", table);
            addClass(unpicklingError);
            update("BadPickleGet", liburl(table.path + "." + "BadPickleGet"),
                    newClass("BadPickleGet", table, unpicklingError), CLASS);

            ClassType pickler = newClass("Pickler", table, objectType);
            addMethod(pickler, "dump");
            addMethod(pickler, "clear_memo");
            addClass(pickler);

            ClassType unpickler = newClass("Unpickler", table, objectType);
            addMethod(unpickler, "load");
            addMethod(unpickler, "noload");
            addClass(unpickler);
        }
    }


    class CStringIOModule extends NativeModule {
        public CStringIOModule() {
            super("cStringIO");
        }


        @NotNull
        @Override
        protected Url liburl() {
            return newLibUrl("stringio");
        }


        @NotNull
        @Override
        protected Url liburl(String anchor) {
            return newLibUrl("stringio", anchor);
        }

        @Override
        public void initBindings() {
            ClassType StringIO = newClass("StringIO", table, BaseFile);
            addFunction("StringIO", new InstanceType(StringIO));
            addAttr("InputType", BaseType);
            addAttr("OutputType", BaseType);
            addAttr("cStringIO_CAPI", Types.UNKNOWN);
        }
    }


    class CMathModule extends NativeModule {
        public CMathModule() {
            super("cmath");
        }


        @Override
        public void initBindings() {
            addFunction("phase", Types.IntInstance);
            addFunction("polar", newTuple(Types.IntInstance, Types.IntInstance));
            addFunction("rect", Types.ComplexInstance);

            for (String plf : list("exp", "log", "log10", "sqrt")) {
                addFunction(plf, Types.IntInstance);
            }

            for (String tf : list("acos", "asin", "atan", "cos", "sin", "tan")) {
                addFunction(tf, Types.IntInstance);
            }

            for (String hf : list("acosh", "asinh", "atanh", "cosh", "sinh", "tanh")) {
                addFunction(hf, Types.ComplexInstance);
            }

            for (String cf : list("isinf", "isnan")) {
                addFunction(cf, Types.BoolInstance);
            }

            for (String c : list("pi", "e")) {
                addAttr(c, Types.IntInstance);
            }
        }
    }


    class CollectionsModule extends NativeModule {
        public CollectionsModule() {
            super("collections");
        }


        @NotNull
        private Url abcUrl() {
            return liburl("abcs-abstract-base-classes");
        }


        @NotNull
        private Url dequeUrl() {
            return liburl("deque-objects");
        }


        @Override
        public void initBindings() {
            ClassType callable = newClass("Callable", table, objectType);
            callable.table.insert("__call__", abcUrl(), newFunc(), METHOD);
            addClass(callable);

            ClassType iterableType = newClass("Iterable", table, objectType);
            // TODO should this jump to url like https://docs.python.org/2.7/library/stdtypes.html#iterator.__iter__ ?
            iterableType.table.insert("__next__", abcUrl(), newFunc(), METHOD);
            iterableType.table.insert("__iter__", abcUrl(), newFunc(), METHOD);
            addClass(iterableType);

            ClassType Hashable = newClass("Hashable", table, objectType);
            Hashable.table.insert("__hash__", abcUrl(), newFunc(Types.IntInstance), METHOD);
            addClass(Hashable);

            ClassType Sized = newClass("Sized", table, objectType);
            Sized.table.insert("__len__", abcUrl(), newFunc(Types.IntInstance), METHOD);
            addClass(Sized);

            ClassType containerType = newClass("Container", table, objectType);
            containerType.table.insert("__contains__", abcUrl(), newFunc(Types.IntInstance), METHOD);
            addClass(containerType);

            ClassType iteratorType = newClass("Iterator", table, iterableType);
            addClass(iteratorType);

            ClassType sequenceType = newClass("Sequence", table, Sized, iterableType, containerType);
            sequenceType.table.insert("__getitem__", abcUrl(), newFunc(), METHOD);
            sequenceType.table.insert("reversed", abcUrl(), newFunc(sequenceType), METHOD);
            sequenceType.table.insert("index", abcUrl(), newFunc(Types.IntInstance), METHOD);
            sequenceType.table.insert("count", abcUrl(), newFunc(Types.IntInstance), METHOD);
            addClass(sequenceType);

            ClassType mutableSequence = newClass("MutableSequence", table, sequenceType);
            mutableSequence.table.insert("__setitem__", abcUrl(), newFunc(), METHOD);
            mutableSequence.table.insert("__delitem__", abcUrl(), newFunc(), METHOD);
            addClass(mutableSequence);

            ClassType setType = newClass("Set", table, Sized, iterableType, containerType);
            setType.table.insert("__getitem__", abcUrl(), newFunc(), METHOD);
            addClass(setType);

            ClassType mutableSet = newClass("MutableSet", table, setType);
            mutableSet.table.insert("add", abcUrl(), newFunc(), METHOD);
            mutableSet.table.insert("discard", abcUrl(), newFunc(), METHOD);
            addClass(mutableSet);

            ClassType mapping = newClass("Mapping", table, Sized, iterableType, containerType);
            mapping.table.insert("__getitem__", abcUrl(), newFunc(), METHOD);
            addClass(mapping);

            ClassType mutableMapping = newClass("MutableMapping", table, mapping);
            mutableMapping.table.insert("__setitem__", abcUrl(), newFunc(), METHOD);
            mutableMapping.table.insert("__delitem__", abcUrl(), newFunc(), METHOD);
            addClass(mutableMapping);

            ClassType MappingView = newClass("MappingView", table, Sized);
            addClass(MappingView);

            ClassType KeysView = newClass("KeysView", table, Sized);
            addClass(KeysView);

            ClassType ItemsView = newClass("ItemsView", table, Sized);
            addClass(ItemsView);

            ClassType ValuesView = newClass("ValuesView", table, Sized);
            addClass(ValuesView);

            ClassType deque = newClass("deque", table, objectType);
            for (String n : list("append", "appendLeft", "clear",
                    "extend", "extendLeft", "rotate"))
            {
                deque.table.insert(n, dequeUrl(), newFunc(Types.NoneInstance), METHOD);
            }
            for (String u : list("__getitem__", "__iter__",
                    "pop", "popleft", "remove"))
            {
                deque.table.insert(u, dequeUrl(), newFunc(), METHOD);
            }
            addClass(deque);

            ClassType defaultdict = newClass("defaultdict", table, objectType);
            defaultdict.table.insert("__missing__", liburl("defaultdict-objects"),
                    newFunc(), METHOD);
            defaultdict.table.insert("default_factory", liburl("defaultdict-objects"),
                    newFunc(), METHOD);
            addClass(defaultdict);

            String argh = "namedtuple-factory-function-for-tuples-with-named-fields";
            ClassType namedtuple = newClass("(namedtuple)", table, BaseTuple);
            namedtuple.table.insert("_fields", liburl(argh),
                                    new ListType(Types.StrInstance), ATTRIBUTE);
            addFunction("namedtuple", namedtuple);
        }
    }


    class CTypesModule extends NativeModule {
        public CTypesModule() {
            super("ctypes");
        }


        @Override
        public void initBindings() {
            String[] ctypes_attrs = {
                    "ARRAY", "ArgumentError", "Array", "BigEndianStructure", "CDLL",
                    "CFUNCTYPE", "DEFAULT_MODE", "DllCanUnloadNow", "DllGetClassObject",
                    "FormatError", "GetLastError", "HRESULT", "LibraryLoader",
                    "LittleEndianStructure", "OleDLL", "POINTER", "PYFUNCTYPE", "PyDLL",
                    "RTLD_GLOBAL", "RTLD_LOCAL", "SetPointerType", "Structure", "Union",
                    "WINFUNCTYPE", "WinDLL", "WinError", "_CFuncPtr", "_FUNCFLAG_CDECL",
                    "_FUNCFLAG_PYTHONAPI", "_FUNCFLAG_STDCALL", "_FUNCFLAG_USE_ERRNO",
                    "_FUNCFLAG_USE_LASTERROR", "_Pointer", "_SimpleCData",
                    "_c_functype_cache", "_calcsize", "_cast", "_cast_addr",
                    "_check_HRESULT", "_check_size", "_ctypes_version", "_dlopen",
                    "_endian", "_memmove_addr", "_memset_addr", "_os",
                    "_pointer_type_cache", "_string_at", "_string_at_addr", "_sys",
                    "_win_functype_cache", "_wstring_at", "_wstring_at_addr",
                    "addressof", "alignment", "byref", "c_bool", "c_buffer", "c_byte",
                    "c_char", "c_char_p", "c_double", "c_float", "c_int", "c_int16",
                    "c_int32", "c_int64", "c_int8", "c_long", "c_longdouble",
                    "c_longlong", "c_short", "c_size_t", "c_ubyte", "c_uint",
                    "c_uint16", "c_uint32", "c_uint64", "c_uint8", "c_ulong",
                    "c_ulonglong", "c_ushort", "c_void_p", "c_voidp", "c_wchar",
                    "c_wchar_p", "cast", "cdll", "create_string_buffer",
                    "create_unicode_buffer", "get_errno", "get_last_error", "memmove",
                    "memset", "oledll", "pointer", "py_object", "pydll", "pythonapi",
                    "resize", "set_conversion_mode", "set_errno", "set_last_error",
                    "sizeof", "string_at", "windll", "wstring_at"
            };
            for (String attr : ctypes_attrs) {
                addAttr(attr, Types.UNKNOWN);
            }
        }
    }


    class CryptModule extends NativeModule {
        public CryptModule() {
            super("crypt");
        }


        @Override
        public void initBindings() {
            addStrFuncs("crypt");
        }
    }


    class DatetimeModule extends NativeModule {
        public DatetimeModule() {
            super("datetime");
        }


        @NotNull
        private Url dtUrl(String anchor) {
            return liburl("datetime." + anchor);
        }


        @Override
        public void initBindings() {
            // XXX:  make datetime, time, date, timedelta and tzinfo Base* objects,
            // so built-in functions can return them.

            addNumAttrs("MINYEAR", "MAXYEAR");

            ClassType timedelta = Datetime_timedelta = newClass("timedelta", table, objectType);
            addClass(timedelta);
            addAttr(timedelta, "min", timedelta);
            addAttr(timedelta, "max", timedelta);
            addAttr(timedelta, "resolution", timedelta);
            addAttr(timedelta, "days", Types.IntInstance);
            addAttr(timedelta, "seconds", Types.IntInstance);
            addAttr(timedelta, "microseconds", Types.IntInstance);

            ClassType tzinfo = Datetime_tzinfo = newClass("tzinfo", table, objectType);
            addClass(tzinfo);
            addMethod(tzinfo, "utcoffset", timedelta);
            addMethod(tzinfo, "dst", timedelta);
            addMethod(tzinfo, "tzname", Types.StrInstance);
            addMethod(tzinfo, "fromutc", tzinfo);

            ClassType date = Datetime_date = newClass("date", table, objectType);
            addClass(date);
            addAttr(date, "min", date);
            addAttr(date, "max", date);
            addAttr(date, "resolution", timedelta);

            addMethod(date, "today", date);
            addMethod(date, "fromtimestamp", date);
            addMethod(date, "fromordinal", date);

            addAttr(date, "year", Types.IntInstance);
            addAttr(date, "month", Types.IntInstance);
            addAttr(date, "day", Types.IntInstance);

            addMethod(date, "replace", date);
            addMethod(date, "timetuple", Time_struct_time);

            for (String n : list("toordinal", "weekday", "isoweekday")) {
                addMethod(date, n, Types.IntInstance);
            }
            for (String r : list("ctime", "strftime", "isoformat")) {
                addMethod(date, r, Types.StrInstance);
            }
            addMethod(date, "isocalendar", newTuple(Types.IntInstance, Types.IntInstance, Types.IntInstance));

            ClassType time = Datetime_time = newClass("time", table, objectType);
            addClass(time);

            addAttr(time, "min", time);
            addAttr(time, "max", time);
            addAttr(time, "resolution", timedelta);

            addAttr(time, "hour", Types.IntInstance);
            addAttr(time, "minute", Types.IntInstance);
            addAttr(time, "second", Types.IntInstance);
            addAttr(time, "microsecond", Types.IntInstance);
            addAttr(time, "tzinfo", tzinfo);

            addMethod(time, "replace", time);

            for (String l : list("isoformat", "strftime", "tzname")) {
                addMethod(time, l, Types.StrInstance);
            }
            for (String f : list("utcoffset", "dst")) {
                addMethod(time, f, timedelta);
            }

            ClassType datetime = Datetime_datetime = newClass("datetime", table, date, time);
            addClass(datetime);

            for (String c : list("combine", "fromordinal", "fromtimestamp", "now",
                    "strptime", "today", "utcfromtimestamp", "utcnow")) {
                addMethod(datetime, c, datetime);
            }

            addAttr(datetime, "min", datetime);
            addAttr(datetime, "max", datetime);
            addAttr(datetime, "resolution", timedelta);

            addMethod(datetime, "date", date);

            for (String x : list("time", "timetz")) {
                addMethod(datetime, x, time);
            }

            for (String y : list("replace", "astimezone")) {
                addMethod(datetime, y, datetime);
            }

            addMethod(datetime, "utctimetuple", Time_struct_time);
        }
    }


    class DbmModule extends NativeModule {
        public DbmModule() {
            super("dbm");
        }


        @Override
        public void initBindings() {
            ClassType dbm = new ClassType("dbm", table, Types.BaseDict);
            addClass(dbm);
            addClass(newException("error", table));
            addStrAttrs("library");
            addFunction("open", dbm);
        }
    }


    class ErrnoModule extends NativeModule {
        public ErrnoModule() {
            super("errno");
        }


        @Override
        public void initBindings() {
            addNumAttrs(
                    "E2BIG", "EACCES", "EADDRINUSE", "EADDRNOTAVAIL", "EAFNOSUPPORT",
                    "EAGAIN", "EALREADY", "EBADF", "EBUSY", "ECHILD", "ECONNABORTED",
                    "ECONNREFUSED", "ECONNRESET", "EDEADLK", "EDEADLOCK",
                    "EDESTADDRREQ", "EDOM", "EDQUOT", "EEXIST", "EFAULT", "EFBIG",
                    "EHOSTDOWN", "EHOSTUNREACH", "EILSEQ", "EINPROGRESS", "EINTR",
                    "EINVAL", "EIO", "EISCONN", "EISDIR", "ELOOP", "EMFILE", "EMLINK",
                    "EMSGSIZE", "ENAMETOOLONG", "ENETDOWN", "ENETRESET", "ENETUNREACH",
                    "ENFILE", "ENOBUFS", "ENODEV", "ENOENT", "ENOEXEC", "ENOLCK",
                    "ENOMEM", "ENOPROTOOPT", "ENOSPC", "ENOSYS", "ENOTCONN", "ENOTDIR",
                    "ENOTEMPTY", "ENOTSOCK", "ENOTTY", "ENXIO", "EOPNOTSUPP", "EPERM",
                    "EPFNOSUPPORT", "EPIPE", "EPROTONOSUPPORT", "EPROTOTYPE", "ERANGE",
                    "EREMOTE", "EROFS", "ESHUTDOWN", "ESOCKTNOSUPPORT", "ESPIPE",
                    "ESRCH", "ESTALE", "ETIMEDOUT", "ETOOMANYREFS", "EUSERS",
                    "EWOULDBLOCK", "EXDEV", "WSABASEERR", "WSAEACCES", "WSAEADDRINUSE",
                    "WSAEADDRNOTAVAIL", "WSAEAFNOSUPPORT", "WSAEALREADY", "WSAEBADF",
                    "WSAECONNABORTED", "WSAECONNREFUSED", "WSAECONNRESET",
                    "WSAEDESTADDRREQ", "WSAEDISCON", "WSAEDQUOT", "WSAEFAULT",
                    "WSAEHOSTDOWN", "WSAEHOSTUNREACH", "WSAEINPROGRESS", "WSAEINTR",
                    "WSAEINVAL", "WSAEISCONN", "WSAELOOP", "WSAEMFILE", "WSAEMSGSIZE",
                    "WSAENAMETOOLONG", "WSAENETDOWN", "WSAENETRESET", "WSAENETUNREACH",
                    "WSAENOBUFS", "WSAENOPROTOOPT", "WSAENOTCONN", "WSAENOTEMPTY",
                    "WSAENOTSOCK", "WSAEOPNOTSUPP", "WSAEPFNOSUPPORT", "WSAEPROCLIM",
                    "WSAEPROTONOSUPPORT", "WSAEPROTOTYPE", "WSAEREMOTE", "WSAESHUTDOWN",
                    "WSAESOCKTNOSUPPORT", "WSAESTALE", "WSAETIMEDOUT",
                    "WSAETOOMANYREFS", "WSAEUSERS", "WSAEWOULDBLOCK",
                    "WSANOTINITIALISED", "WSASYSNOTREADY", "WSAVERNOTSUPPORTED");

            addAttr("errorcode", newDict(Types.IntInstance, Types.StrInstance));
        }
    }


    class ExceptionsModule extends NativeModule {
        public ExceptionsModule() {
            super("exceptions");
        }


        @Override
        public void initBindings() {
            ModuleType builtins = get("__builtin__");
            for (String s : builtin_exception_types) {
//                Binding b = builtins.getTable().lookup(s);
//                table.update(b.getName(), b.getFirstNode(), b.getType(), b.getKind());
            }
        }
    }


    class FcntlModule extends NativeModule {
        public FcntlModule() {
            super("fcntl");
        }


        @Override
        public void initBindings() {
            for (String s : list("fcntl", "ioctl")) {
                addFunction(s, newUnion(Types.IntInstance, Types.StrInstance));
            }
            addNumFuncs("flock");
            addUnknownFuncs("lockf");

            addNumAttrs(
                    "DN_ACCESS", "DN_ATTRIB", "DN_CREATE", "DN_DELETE", "DN_MODIFY",
                    "DN_MULTISHOT", "DN_RENAME", "FASYNC", "FD_CLOEXEC", "F_DUPFD",
                    "F_EXLCK", "F_GETFD", "F_GETFL", "F_GETLEASE", "F_GETLK", "F_GETLK64",
                    "F_GETOWN", "F_GETSIG", "F_NOTIFY", "F_RDLCK", "F_SETFD", "F_SETFL",
                    "F_SETLEASE", "F_SETLK", "F_SETLK64", "F_SETLKW", "F_SETLKW64",
                    "F_SETOWN", "F_SETSIG", "F_SHLCK", "F_UNLCK", "F_WRLCK", "I_ATMARK",
                    "I_CANPUT", "I_CKBAND", "I_FDINSERT", "I_FIND", "I_FLUSH",
                    "I_FLUSHBAND", "I_GETBAND", "I_GETCLTIME", "I_GETSIG", "I_GRDOPT",
                    "I_GWROPT", "I_LINK", "I_LIST", "I_LOOK", "I_NREAD", "I_PEEK",
                    "I_PLINK", "I_POP", "I_PUNLINK", "I_PUSH", "I_RECVFD", "I_SENDFD",
                    "I_SETCLTIME", "I_SETSIG", "I_SRDOPT", "I_STR", "I_SWROPT",
                    "I_UNLINK", "LOCK_EX", "LOCK_MAND", "LOCK_NB", "LOCK_READ", "LOCK_RW",
                    "LOCK_SH", "LOCK_UN", "LOCK_WRITE");
        }
    }


    class FpectlModule extends NativeModule {
        public FpectlModule() {
            super("fpectl");
        }


        @Override
        public void initBindings() {
            addNoneFuncs("turnon_sigfpe", "turnoff_sigfpe");
            addClass(newException("FloatingPointError", table));
        }
    }


    class GcModule extends NativeModule {
        public GcModule() {
            super("gc");
        }


        @Override
        public void initBindings() {
            addNoneFuncs("enable", "disable", "set_debug", "set_threshold");
            addNumFuncs("isenabled", "collect", "get_debug", "get_count", "get_threshold");
            for (String s : list("get_objects", "get_referrers", "get_referents")) {
                addFunction(s, newList());
            }
            addAttr("garbage", newList());
            addNumAttrs("DEBUG_STATS", "DEBUG_COLLECTABLE", "DEBUG_UNCOLLECTABLE",
                    "DEBUG_INSTANCES", "DEBUG_OBJECTS", "DEBUG_SAVEALL", "DEBUG_LEAK");
        }
    }


    class GdbmModule extends NativeModule {
        public GdbmModule() {
            super("gdbm");
        }


        @Override
        public void initBindings() {
            addClass(newException("error", table));

            ClassType gdbm = new ClassType("gdbm", table, Types.BaseDict);
            addMethod(gdbm, "firstkey", Types.StrInstance);
            addMethod(gdbm, "nextkey", Types.StrInstance);
            addMethod(gdbm, "reorganize", Types.NoneInstance);
            addMethod(gdbm, "sync", Types.NoneInstance);
            addFunction("open", gdbm);
        }

    }


    class GrpModule extends NativeModule {
        public GrpModule() {
            super("grp");
        }


        @Override
        public void initBindings() {
            Builtins.this.get("struct");
            ClassType struct_group = newClass("struct_group", table, BaseStruct);
            addAttr(struct_group, "gr_name", Types.StrInstance);
            addAttr(struct_group, "gr_passwd", Types.StrInstance);
            addAttr(struct_group, "gr_gid", Types.IntInstance);
            addAttr(struct_group, "gr_mem", Types.StrInstance);
            addClass(struct_group);

            for (String s : list("getgrgid", "getgrnam")) {
                addFunction(s, struct_group);
            }
            addFunction("getgrall", new ListType(struct_group));
        }
    }


    class ImpModule extends NativeModule {
        public ImpModule() {
            super("imp");
        }


        @Override
        public void initBindings() {
            addStrFuncs("get_magic");
            addFunction("get_suffixes", newList(newTuple(Types.StrInstance, Types.StrInstance, Types.IntInstance)));
            addFunction("find_module", newTuple(Types.StrInstance, Types.StrInstance, Types.IntInstance));

            String[] module_methods = {
                    "load_module", "new_module", "init_builtin", "init_frozen",
                    "load_compiled", "load_dynamic", "load_source"
            };
            for (String mm : module_methods) {
                addFunction(mm, newModule("<?>"));
            }

            addUnknownFuncs("acquire_lock", "release_lock");

            addNumAttrs("PY_SOURCE", "PY_COMPILED", "C_EXTENSION",
                    "PKG_DIRECTORY", "C_BUILTIN", "PY_FROZEN", "SEARCH_ERROR");

            addNumFuncs("lock_held", "is_builtin", "is_frozen");

            ClassType impNullImporter = newClass("NullImporter", table, objectType);
            addMethod(impNullImporter, "find_module",  Types.NoneInstance);
            addClass(impNullImporter);
        }
    }


    class ItertoolsModule extends NativeModule {
        public ItertoolsModule() {
            super("itertools");
        }


        @Override
        public void initBindings() {
            ClassType iterator = newClass("iterator", table, objectType);
            addMethod(iterator, "from_iterable", iterator);
            addMethod(iterator, "next");

            for (String s : list("chain", "combinations", "count", "cycle",
                    "dropwhile", "groupby", "ifilter",
                    "ifilterfalse", "imap", "islice", "izip",
                    "izip_longest", "permutations", "product",
                    "repeat", "starmap", "takewhile", "tee"))
            {
                addClass(iterator);
            }
        }
    }


    class MarshalModule extends NativeModule {
        public MarshalModule() {
            super("marshal");
        }


        @Override
        public void initBindings() {
            addNumAttrs("version");
            addStrFuncs("dumps");
            addUnknownFuncs("dump", "load", "loads");
        }
    }


    class MathModule extends NativeModule {
        public MathModule() {
            super("math");
        }


        @Override
        public void initBindings() {
            addNumFuncs(
                    "acos", "acosh", "asin", "asinh", "atan", "atan2", "atanh", "ceil",
                    "copysign", "cos", "cosh", "degrees", "exp", "fabs", "factorial",
                    "floor", "fmod", "frexp", "fsum", "hypot", "isinf", "isnan",
                    "ldexp", "log", "log10", "log1p", "modf", "pow", "radians", "sin",
                    "sinh", "sqrt", "tan", "tanh", "trunc");
            addNumAttrs("pi", "e");
        }
    }


    class Md5Module extends NativeModule {
        public Md5Module() {
            super("md5");
        }


        @Override
        public void initBindings() {
            addNumAttrs("blocksize", "digest_size");

            ClassType md5 = newClass("md5", table, objectType);
            addMethod(md5, "update");
            addMethod(md5, "digest", Types.StrInstance);
            addMethod(md5, "hexdigest", Types.StrInstance);
            addMethod(md5, "copy", md5);

            update("new", liburl(), newFunc(md5), CONSTRUCTOR);
            update("md5", liburl(), newFunc(md5), CONSTRUCTOR);
        }
    }


    class MmapModule extends NativeModule {
        public MmapModule() {
            super("mmap");
        }


        @Override
        public void initBindings() {
            ClassType mmap = newClass("mmap", table, objectType);

            for (String s : list("ACCESS_COPY", "ACCESS_READ", "ACCESS_WRITE",
                    "ALLOCATIONGRANULARITY", "MAP_ANON", "MAP_ANONYMOUS",
                    "MAP_DENYWRITE", "MAP_EXECUTABLE", "MAP_PRIVATE",
                    "MAP_SHARED", "PAGESIZE", "PROT_EXEC", "PROT_READ",
                    "PROT_WRITE"))
            {
                addAttr(mmap, s, Types.IntInstance);
            }

            for (String fstr : list("read", "read_byte", "readline")) {
                addMethod(mmap, fstr, Types.StrInstance);
            }

            for (String fnum : list("find", "rfind", "tell")) {
                addMethod(mmap, fnum, Types.IntInstance);
            }

            for (String fnone : list("close", "flush", "move", "resize", "seek",
                    "write", "write_byte"))
            {
                addMethod(mmap, fnone, Types.NoneInstance);
            }

            addClass(mmap);
        }
    }


    class NisModule extends NativeModule {
        public NisModule() {
            super("nis");
        }


        @Override
        public void initBindings() {
            addStrFuncs("match", "cat", "get_default_domain");
            addFunction("maps", newList(Types.StrInstance));
            addClass(newException("error", table));
        }
    }


    class OsModule extends NativeModule {
        public OsModule() {
            super("os");
        }


        @Override
        public void initBindings() {
            addAttr("name", Types.StrInstance);
            addClass(newException("error", table));  // XXX: OSError

            initProcBindings();
            initProcMgmtBindings();
            initFileBindings();
            initFileAndDirBindings();
            initMiscSystemInfo();
            initOsPathModule();

            String[] str_attrs = {
                "altsep", "curdir", "devnull", "defpath", "pardir", "pathsep", "sep",
            };
            for (String s : str_attrs) {
                addAttr(s, Types.StrInstance);
            }

            // TODO this is not needed?
            addAttr("errno", liburl(), newModule("errno"));

            addFunction("urandom", Types.StrInstance);
            addAttr("NGROUPS_MAX", Types.IntInstance);

            for (String s : list("_Environ", "_copy_reg", "_execvpe", "_exists",
                    "_get_exports_list", "_make_stat_result",
                    "_make_statvfs_result", "_pickle_stat_result",
                    "_pickle_statvfs_result", "_spawnvef"))
            {
                addFunction(s, Types.UNKNOWN);
            }
        }


        private void initProcBindings() {
            addAttr("environ", newDict(Types.StrInstance, Types.StrInstance));

            for (String s : list("chdir", "fchdir", "putenv", "setegid", "seteuid",
                    "setgid", "setgroups", "setpgrp", "setpgid",
                    "setreuid", "setregid", "setuid", "unsetenv"))
            {
                addFunction(s, Types.NoneInstance);
            }

            for (String s : list("getegid", "getgid", "getpgid", "getpgrp",
                    "getppid", "getuid", "getsid", "umask"))
            {
                addFunction(s, Types.IntInstance);
            }

            for (String s : list("getcwd", "ctermid", "getlogin", "getenv", "strerror")) {
                addFunction(s, Types.StrInstance);
            }

            addFunction("getgroups", newList(Types.StrInstance));
            addFunction("uname", newTuple(Types.StrInstance, Types.StrInstance, Types.StrInstance,
                                                     Types.StrInstance, Types.StrInstance));
        }


        private void initProcMgmtBindings() {
            for (String s : list("EX_CANTCREAT", "EX_CONFIG", "EX_DATAERR",
                    "EX_IOERR", "EX_NOHOST", "EX_NOINPUT",
                    "EX_NOPERM", "EX_NOUSER", "EX_OK", "EX_OSERR",
                    "EX_OSFILE", "EX_PROTOCOL", "EX_SOFTWARE",
                    "EX_TEMPFAIL", "EX_UNAVAILABLE", "EX_USAGE",
                    "P_NOWAIT", "P_NOWAITO", "P_WAIT", "P_DETACH",
                    "P_OVERLAY", "WCONTINUED", "WCOREDUMP",
                    "WEXITSTATUS", "WIFCONTINUED", "WIFEXITED",
                    "WIFSIGNALED", "WIFSTOPPED", "WNOHANG", "WSTOPSIG",
                    "WTERMSIG", "WUNTRACED"))
            {
                addAttr(s, Types.IntInstance);
            }

            for (String s : list("abort", "execl", "execle", "execlp", "execlpe",
                    "execv", "execve", "execvp", "execvpe", "_exit",
                    "kill", "killpg", "plock", "startfile"))
            {
                addFunction(s, Types.NoneInstance);
            }

            for (String s : list("nice", "spawnl", "spawnle", "spawnlp", "spawnlpe",
                    "spawnv", "spawnve", "spawnvp", "spawnvpe", "system"))
            {
                addFunction(s, Types.IntInstance);
            }

            addFunction("fork", newUnion(BaseFileInst, Types.IntInstance));
            addFunction("times", newTuple(Types.IntInstance, Types.IntInstance, Types.IntInstance, Types.IntInstance, Types.IntInstance));

            for (String s : list("forkpty", "wait", "waitpid")) {
                addFunction(s, newTuple(Types.IntInstance, Types.IntInstance));
            }

            for (String s : list("wait3", "wait4")) {
                addFunction(s, newTuple(Types.IntInstance, Types.IntInstance, Types.IntInstance));
            }
        }


        private void initFileBindings() {
            for (String s : list("fdopen", "popen", "tmpfile")) {
                addFunction(s, BaseFileInst);
            }

            addFunction("popen2", newTuple(BaseFileInst, BaseFileInst));
            addFunction("popen3", newTuple(BaseFileInst, BaseFileInst, BaseFileInst));
            addFunction("popen4", newTuple(BaseFileInst, BaseFileInst));

            addFunction("open", BaseFileInst);

            for (String s : list("close", "closerange", "dup2", "fchmod",
                    "fchown", "fdatasync", "fsync", "ftruncate",
                    "lseek", "tcsetpgrp", "write"))
            {
                addFunction(s, Types.NoneInstance);
            }

            for (String s : list("dup2", "fpathconf", "fstat", "fstatvfs",
                    "isatty", "tcgetpgrp"))
            {
                addFunction(s, Types.IntInstance);
            }

            for (String s : list("read", "ttyname")) {
                addFunction(s, Types.StrInstance);
            }

            for (String s : list("openpty", "pipe", "fstat", "fstatvfs",
                    "isatty"))
            {
                addFunction(s, newTuple(Types.IntInstance, Types.IntInstance));
            }

            for (String s : list("O_APPEND", "O_CREAT", "O_DIRECT", "O_DIRECTORY",
                    "O_DSYNC", "O_EXCL", "O_LARGEFILE", "O_NDELAY",
                    "O_NOCTTY", "O_NOFOLLOW", "O_NONBLOCK", "O_RDONLY",
                    "O_RDWR", "O_RSYNC", "O_SYNC", "O_TRUNC", "O_WRONLY",
                    "SEEK_CUR", "SEEK_END", "SEEK_SET"))
            {
                addAttr(s, Types.IntInstance);
            }
        }


        private void initFileAndDirBindings() {
            for (String s : list("F_OK", "R_OK", "W_OK", "X_OK")) {
                addAttr(s, Types.IntInstance);
            }

            for (String s : list("chflags", "chroot", "chmod", "chown", "lchflags",
                    "lchmod", "lchown", "link", "mknod", "mkdir",
                    "mkdirs", "remove", "removedirs", "rename", "renames",
                    "rmdir", "symlink", "unlink", "utime"))
            {
                addAttr(s, Types.NoneInstance);
            }

            for (String s : list("access", "lstat", "major", "minor",
                    "makedev", "pathconf", "stat_float_times"))
            {
                addFunction(s, Types.IntInstance);
            }

            for (String s : list("getcwdu", "readlink", "tempnam", "tmpnam")) {
                addFunction(s, Types.StrInstance);
            }

            for (String s : list("listdir")) {
                addFunction(s, newList(Types.StrInstance));
            }

            addFunction("mkfifo", BaseFileInst);

            addFunction("stat", newList(Types.IntInstance));  // XXX: posix.stat_result
            addFunction("statvfs", newList(Types.IntInstance));  // XXX: pos.statvfs_result

            addAttr("pathconf_names", newDict(Types.StrInstance, Types.IntInstance));
            addAttr("TMP_MAX", Types.IntInstance);

            addFunction("walk", newList(newTuple(Types.StrInstance, Types.StrInstance, Types.StrInstance)));
        }


        private void initMiscSystemInfo() {
            addAttr("confstr_names", newDict(Types.StrInstance, Types.IntInstance));
            addAttr("sysconf_names", newDict(Types.StrInstance, Types.IntInstance));

            for (String s : list("curdir", "pardir", "sep", "altsep", "extsep",
                    "pathsep", "defpath", "linesep", "devnull"))
            {
                addAttr(s, Types.StrInstance);
            }

            for (String s : list("getloadavg", "sysconf")) {
                addFunction(s, Types.IntInstance);
            }

            addFunction("confstr", Types.StrInstance);
        }


        private class OSPathModule extends NativeModule {
            OSPathModule(String name) {
                super("os.path");
            }

            @Override
            protected void initBindings() {

            }
        }
        private void initOsPathModule() {
            ModuleType m = newModule("path");
            State ospath = m.table;
            ospath.setPath("os.path");  // make sure global qnames are correct

            update("path", newLibUrl("os.path.html#module-os.path"), m, MODULE);

            String[] str_funcs = {
                    "_resolve_link", "abspath", "basename", "commonprefix",
                    "dirname", "expanduser", "expandvars", "join",
                    "normcase", "normpath", "realpath", "relpath",
            };
            for (String s : str_funcs) {
                addFunction(m, s, Types.StrInstance);
            }

            String[] num_funcs = {
                    "exists", "lexists", "getatime", "getctime", "getmtime", "getsize",
                    "isabs", "isdir", "isfile", "islink", "ismount", "samefile",
                    "sameopenfile", "samestat", "supports_unicode_filenames",
            };
            for (String s : num_funcs) {
                addFunction(m, s, Types.IntInstance);
            }

            for (String s : list("split", "splitdrive", "splitext", "splitunc")) {
                addFunction(m, s, newTuple(Types.StrInstance, Types.StrInstance));
            }

            addFunction(m, "walk", newFunc(Types.NoneInstance));

            addAttr(ospath, "os", this.module);
            ospath.insert("stat", newLibUrl("stat"),
                    // moduleTable.lookupLocal("stat").getType(),
                    newModule("<stat-fixme>"), ATTRIBUTE);

            // XXX:  this is an re object, I think
            addAttr(ospath, "_varprog", Types.UNKNOWN);
        }
    }


    class OperatorModule extends NativeModule {
        public OperatorModule() {
            super("operator");
        }


        @Override
        public void initBindings() {
            // XXX:  mark __getslice__, __setslice__ and __delslice__ as deprecated.
            addNumFuncs(
                    "__abs__", "__add__", "__and__", "__concat__", "__contains__",
                    "__div__", "__doc__", "__eq__", "__floordiv__", "__ge__",
                    "__getitem__", "__getslice__", "__gt__", "__iadd__", "__iand__",
                    "__iconcat__", "__idiv__", "__ifloordiv__", "__ilshift__",
                    "__imod__", "__imul__", "__index__", "__inv__", "__invert__",
                    "__ior__", "__ipow__", "__irepeat__", "__irshift__", "__isub__",
                    "__itruediv__", "__ixor__", "__le__", "__lshift__", "__lt__",
                    "__mod__", "__mul__", "__name__", "__ne__", "__neg__", "__not__",
                    "__or__", "__package__", "__pos__", "__pow__", "__repeat__",
                    "__rshift__", "__setitem__", "__setslice__", "__sub__",
                    "__truediv__", "__xor__", "abs", "add", "and_", "concat",
                    "contains", "countOf", "div", "eq", "floordiv", "ge", "getitem",
                    "getslice", "gt", "iadd", "iand", "iconcat", "idiv", "ifloordiv",
                    "ilshift", "imod", "imul", "index", "indexOf", "inv", "invert",
                    "ior", "ipow", "irepeat", "irshift", "isCallable",
                    "isMappingType", "isNumberType", "isSequenceType", "is_",
                    "is_not", "isub", "itruediv", "ixor", "le", "lshift", "lt", "mod",
                    "mul", "ne", "neg", "not_", "or_", "pos", "pow", "repeat",
                    "rshift", "sequenceIncludes", "setitem", "setslice", "sub",
                    "truediv", "truth", "xor");

            addUnknownFuncs("attrgetter", "itemgetter", "methodcaller");
            addNoneFuncs("__delitem__", "__delslice__", "delitem", "delclice");
        }
    }


    class ParserModule extends NativeModule {
        public ParserModule() {
            super("parser");
        }


        @Override
        public void initBindings() {
            ClassType st = newClass("ST", table, objectType);
            addMethod(st, "compile", Types.NoneInstance);
            addMethod(st, "isexpr", Types.IntInstance);
            addMethod(st, "issuite", Types.IntInstance);
            addMethod(st, "tolist", newList());
            addMethod(st, "totuple", newTuple());

            addAttr("STType", BaseType);

            for (String s : list("expr", "suite", "sequence2st", "tuple2st")) {
                addFunction(s, st);
            }

            addFunction("st2list", newList());
            addFunction("st2tuple", newTuple());
            addFunction("compilest", Types.UNKNOWN);
            addFunction("isexpr", Types.BoolInstance);
            addFunction("issuite", Types.BoolInstance);

            addClass(newException("ParserError", table));
        }
    }


    class PosixModule extends NativeModule {
        public PosixModule() {
            super("posix");
        }


        @Override
        public void initBindings() {
            addAttr("environ", newDict(Types.StrInstance, Types.StrInstance));
        }
    }


    class PwdModule extends NativeModule {
        public PwdModule() {
            super("pwd");
        }


        @Override
        public void initBindings() {
            ClassType struct_pwd = newClass("struct_pwd", table, objectType);
            for (String s : list("pw_nam", "pw_passwd", "pw_uid", "pw_gid",
                    "pw_gecos", "pw_dir", "pw_shell"))
            {
                struct_pwd.table.insert(s, liburl(), Types.IntInstance, ATTRIBUTE);
            }
            addAttr("struct_pwd", liburl(), struct_pwd);

            addFunction("getpwuid", struct_pwd);
            addFunction("getpwnam", struct_pwd);
            addFunction("getpwall", newList(struct_pwd));
        }
    }


    class PyexpatModule extends NativeModule {
        public PyexpatModule() {
            super("pyexpat");
        }


        @Override
        public void initBindings() {
            // XXX
        }
    }


    class ReadlineModule extends NativeModule {
        public ReadlineModule() {
            super("readline");
        }


        @Override
        public void initBindings() {
            addNoneFuncs("parse_and_bind", "insert_text", "read_init_file",
                    "read_history_file", "write_history_file",
                    "clear_history", "set_history_length",
                    "remove_history_item", "replace_history_item",
                    "redisplay", "set_startup_hook", "set_pre_input_hook",
                    "set_completer", "set_completer_delims",
                    "set_completion_display_matches_hook", "add_history");

            addNumFuncs("get_history_length", "get_current_history_length",
                    "get_begidx", "get_endidx");

            addStrFuncs("get_line_buffer", "get_history_item");

            addUnknownFuncs("get_completion_type");

            addFunction("get_completer", newFunc());
            addFunction("get_completer_delims", newList(Types.StrInstance));
        }
    }


    class ResourceModule extends NativeModule {
        public ResourceModule() {
            super("resource");
        }


        @Override
        public void initBindings() {
            addFunction("getrlimit", newTuple(Types.IntInstance, Types.IntInstance));
            addFunction("getrlimit", Types.UNKNOWN);

            String[] constants = {
                    "RLIMIT_CORE", "RLIMIT_CPU", "RLIMIT_FSIZE", "RLIMIT_DATA",
                    "RLIMIT_STACK", "RLIMIT_RSS", "RLIMIT_NPROC", "RLIMIT_NOFILE",
                    "RLIMIT_OFILE", "RLIMIT_MEMLOCK", "RLIMIT_VMEM", "RLIMIT_AS"
            };
            for (String c : constants) {
                addAttr(c, Types.IntInstance);
            }

            ClassType ru = newClass("struct_rusage", table, objectType);
            String[] ru_fields = {
                    "ru_utime", "ru_stime", "ru_maxrss", "ru_ixrss", "ru_idrss",
                    "ru_isrss", "ru_minflt", "ru_majflt", "ru_nswap", "ru_inblock",
                    "ru_oublock", "ru_msgsnd", "ru_msgrcv", "ru_nsignals",
                    "ru_nvcsw", "ru_nivcsw"
            };
            for (String ruf : ru_fields) {
                addAttr(ru, ruf, Types.IntInstance);
            }
            addClass(ru);

            addFunction("getrusage", ru);
            addFunction("getpagesize", Types.IntInstance);

            for (String s : list("RUSAGE_SELF", "RUSAGE_CHILDREN", "RUSAGE_BOTH")) {
                addAttr(s, Types.IntInstance);
            }
        }
    }


    class SelectModule extends NativeModule {
        public SelectModule() {
            super("select");
        }


        @Override
        public void initBindings() {
            addClass(newException("error", table));

            addFunction("select",  newTuple(newList(), newList(), newList()));
            
            ClassType epoll = newClass("epoll", table, objectType);
            addMethod(epoll, "close", Types.NoneInstance);
            addMethod(epoll, "fileno", Types.IntInstance);
            addMethod(epoll, "fromfd", epoll);
            for (String s : list("register", "modify", "unregister", "poll")) {
                addMethod(epoll, s);
            }
            addClass(epoll);

            for (String s : list("EPOLLERR", "EPOLLET", "EPOLLHUP", "EPOLLIN", "EPOLLMSG",
                    "EPOLLONESHOT", "EPOLLOUT", "EPOLLPRI", "EPOLLRDBAND",
                    "EPOLLRDNORM", "EPOLLWRBAND", "EPOLLWRNORM"))
            {
                addAttr(s, Types.IntInstance);
            }


            ClassType poll = newClass("poll", table, objectType);
            addMethod(poll, "register");
            addMethod(poll, "modify");
            addMethod(poll, "unregister");
            addMethod(poll, "poll", newList(newTuple(Types.IntInstance, Types.IntInstance)));
            addClass(poll);

            for (String s : list("POLLERR", "POLLHUP", "POLLIN", "POLLMSG",
                    "POLLNVAL", "POLLOUT", "POLLPRI", "POLLRDBAND",
                    "POLLRDNORM", "POLLWRBAND", "POLLWRNORM"))
            {
                addAttr(s, Types.IntInstance);
            }

            ClassType kqueue = newClass("kqueue", table, objectType);
            addMethod(kqueue, "close", Types.NoneInstance);
            addMethod(kqueue, "fileno", Types.IntInstance);
            addMethod(kqueue, "fromfd", kqueue);
            addMethod(kqueue, "control", newList(newTuple(Types.IntInstance, Types.IntInstance)));
            addClass(kqueue);

            ClassType kevent = newClass("kevent", table, objectType);
            for (String s : list("ident", "filter", "flags", "fflags", "data", "udata")) {
                addAttr(kevent, s, Types.UNKNOWN);
            }
            addClass(kevent);
        }
    }


    class SignalModule extends NativeModule {
        public SignalModule() {
            super("signal");
        }


        @Override
        public void initBindings() {
            addNumAttrs(
                    "NSIG", "SIGABRT", "SIGALRM", "SIGBUS", "SIGCHLD", "SIGCLD",
                    "SIGCONT", "SIGFPE", "SIGHUP", "SIGILL", "SIGINT", "SIGIO",
                    "SIGIOT", "SIGKILL", "SIGPIPE", "SIGPOLL", "SIGPROF", "SIGPWR",
                    "SIGQUIT", "SIGRTMAX", "SIGRTMIN", "SIGSEGV", "SIGSTOP", "SIGSYS",
                    "SIGTERM", "SIGTRAP", "SIGTSTP", "SIGTTIN", "SIGTTOU", "SIGURG",
                    "SIGUSR1", "SIGUSR2", "SIGVTALRM", "SIGWINCH", "SIGXCPU", "SIGXFSZ",
                    "SIG_DFL", "SIG_IGN");

            addUnknownFuncs("default_int_handler", "getsignal", "set_wakeup_fd", "signal");
        }
    }


    class ShaModule extends NativeModule {
        public ShaModule() {
            super("sha");
        }


        @Override
        public void initBindings() {
            addNumAttrs("blocksize", "digest_size");

            ClassType sha = newClass("sha", table, objectType);
            addMethod(sha, "update");
            addMethod(sha, "digest", Types.StrInstance);
            addMethod(sha, "hexdigest", Types.StrInstance);
            addMethod(sha, "copy", sha);
            addClass(sha);

            update("new", liburl(), newFunc(sha), CONSTRUCTOR);
        }
    }


    class SpwdModule extends NativeModule {
        public SpwdModule() {
            super("spwd");
        }


        @Override
        public void initBindings() {
            ClassType struct_spwd = newClass("struct_spwd", table, objectType);
            for (String s : list("sp_nam", "sp_pwd", "sp_lstchg", "sp_min",
                    "sp_max", "sp_warn", "sp_inact", "sp_expire",
                    "sp_flag"))
            {
                addAttr(struct_spwd, s, Types.IntInstance);
            }
            addAttr("struct_spwd", struct_spwd);

            addFunction("getspnam", struct_spwd);
            addFunction("getspall", newList(struct_spwd));
        }
    }


    class StropModule extends NativeModule {
        public StropModule() {
            super("strop");
        }


        @Override
        public void initBindings() {
            table.putAll(Types.StrInstance.table);
        }
    }


    class StructModule extends NativeModule {
        public StructModule() {
            super("struct");
        }


        @Override
        public void initBindings() {
            addClass(newException("error", table));
            addStrFuncs("pack");
            addUnknownFuncs("pack_into");
            addNumFuncs("calcsize");
            addFunction("unpack", newTuple());
            addFunction("unpack_from", newTuple());

            BaseStruct = newClass("Struct", table, objectType);
            addClass(BaseStruct);
            State t = BaseStruct.table;

            addMethod(BaseStruct, "pack", Types.StrInstance);
            addMethod(BaseStruct, "pack_into");
            addMethod(BaseStruct, "unpack", newTuple());
            addMethod(BaseStruct, "unpack_from", newTuple());
            addMethod(BaseStruct, "format", Types.StrInstance);
            addMethod(BaseStruct, "size", Types.IntInstance);
        }
    }


    class SysModule extends NativeModule {
        public SysModule() {
            super("sys");
        }


        @Override
        public void initBindings() {
            addUnknownFuncs(
                    "_clear_type_cache", "call_tracing", "callstats", "_current_frames",
                    "_getframe", "displayhook", "dont_write_bytecode", "exitfunc",
                    "exc_clear", "exc_info", "excepthook", "exit",
                    "last_traceback", "last_type", "last_value", "modules",
                    "path_hooks", "path_importer_cache", "getprofile", "gettrace",
                    "setcheckinterval", "setprofile", "setrecursionlimit", "settrace");

            addAttr("exc_type", Types.NoneInstance);

            addUnknownAttrs("__stderr__", "__stdin__", "__stdout__",
                    "stderr", "stdin", "stdout", "version_info");

            addNumAttrs("api_version", "hexversion", "winver", "maxint", "maxsize",
                    "maxunicode", "py3kwarning", "dllhandle");

            addStrAttrs("platform", "byteorder", "copyright", "prefix", "version",
                    "exec_prefix", "executable");

            addNumFuncs("getrecursionlimit", "getwindowsversion", "getrefcount",
                    "getsizeof", "getcheckinterval");

            addStrFuncs("getdefaultencoding", "getfilesystemencoding");

            for (String s : list("argv", "builtin_module_names", "path",
                    "meta_path", "subversion"))
            {
                addAttr(s, newList(Types.StrInstance));
            }

            for (String s : list("flags", "warnoptions", "float_info")) {
                addAttr(s, newDict(Types.StrInstance, Types.IntInstance));
            }
        }
    }


    class SyslogModule extends NativeModule {
        public SyslogModule() {
            super("syslog");
        }


        @Override
        public void initBindings() {
            addNoneFuncs("syslog", "openlog", "closelog", "setlogmask");
            addNumAttrs("LOG_ALERT", "LOG_AUTH", "LOG_CONS", "LOG_CRIT", "LOG_CRON",
                    "LOG_DAEMON", "LOG_DEBUG", "LOG_EMERG", "LOG_ERR", "LOG_INFO",
                    "LOG_KERN", "LOG_LOCAL0", "LOG_LOCAL1", "LOG_LOCAL2", "LOG_LOCAL3",
                    "LOG_LOCAL4", "LOG_LOCAL5", "LOG_LOCAL6", "LOG_LOCAL7", "LOG_LPR",
                    "LOG_MAIL", "LOG_MASK", "LOG_NDELAY", "LOG_NEWS", "LOG_NOTICE",
                    "LOG_NOWAIT", "LOG_PERROR", "LOG_PID", "LOG_SYSLOG", "LOG_UPTO",
                    "LOG_USER", "LOG_UUCP", "LOG_WARNING");
        }
    }


    class TermiosModule extends NativeModule {
        public TermiosModule() {
            super("termios");
        }


        @Override
        public void initBindings() {
            addFunction("tcgetattr", newList());
            addUnknownFuncs("tcsetattr", "tcsendbreak", "tcdrain", "tcflush", "tcflow");
        }
    }


    class ThreadModule extends NativeModule {
        public ThreadModule() {
            super("thread");
        }


        @Override
        public void initBindings() {
            addClass(newException("error", table));

            ClassType lock = newClass("lock", table, objectType);
            addMethod(lock, "acquire", Types.IntInstance);
            addMethod(lock, "locked", Types.IntInstance);
            addMethod(lock, "release", Types.NoneInstance);
            addAttr("LockType", BaseType);

            addNoneFuncs("interrupt_main", "exit", "exit_thread");
            addNumFuncs("start_new", "start_new_thread", "get_ident", "stack_size");

            addFunction("allocate", lock);
            addFunction("allocate_lock", lock);  // synonym

            addAttr("_local", BaseType);
        }
    }


    class TimeModule extends NativeModule {
        public TimeModule() {
            super("time");
        }


        @Override
        public void initBindings() {
            InstanceType struct_time = Time_struct_time = new InstanceType(newClass("datetime", table, objectType));
            addAttr("struct_time", struct_time);

            String[] struct_time_attrs = {
                    "n_fields", "n_sequence_fields", "n_unnamed_fields",
                    "tm_hour", "tm_isdst", "tm_mday", "tm_min",
                    "tm_mon", "tm_wday", "tm_yday", "tm_year",
            };
            for (String s : struct_time_attrs) {
                addAttr(struct_time.table, s, Types.IntInstance);
            }

            addNumAttrs("accept2dyear", "altzone", "daylight", "timezone");

            addAttr("tzname", newTuple(Types.StrInstance, Types.StrInstance));
            addNoneFuncs("sleep", "tzset");

            addNumFuncs("clock", "mktime", "time", "tzname");
            addStrFuncs("asctime", "ctime", "strftime");

            addFunctions_beCareful(struct_time, "gmtime", "localtime", "strptime");
        }
    }


    class UnicodedataModule extends NativeModule {
        public UnicodedataModule() {
            super("unicodedata");
        }


        @Override
        public void initBindings() {
            addNumFuncs("decimal", "digit", "numeric", "combining",
                    "east_asian_width", "mirrored");
            addStrFuncs("lookup", "name", "category", "bidirectional",
                    "decomposition", "normalize");
            addNumAttrs("unidata_version");
            addUnknownAttrs("ucd_3_2_0");
        }
    }


    class ZipimportModule extends NativeModule {
        public ZipimportModule() {
            super("zipimport");
        }


        @Override
        public void initBindings() {
            addClass(newException("ZipImportError", table));

            ClassType zipimporter = newClass("zipimporter", table, objectType);
            addMethod(zipimporter, "find_module", zipimporter);
            addMethod(zipimporter, "get_code", Types.UNKNOWN);  // XXX:  code object
            addMethod(zipimporter, "get_data", Types.UNKNOWN);
            addMethod(zipimporter, "get_source", Types.StrInstance);
            addMethod(zipimporter, "is_package", Types.IntInstance);
            addMethod(zipimporter, "load_module", newModule("<?>"));
            addMethod(zipimporter, "archive", Types.StrInstance);
            addMethod(zipimporter, "prefix", Types.StrInstance);

            addClass(zipimporter);
            addAttr("_zip_directory_cache", newDict(Types.StrInstance, Types.UNKNOWN));
        }
    }


    class ZlibModule extends NativeModule {
        public ZlibModule() {
            super("zlib");
        }


        @Override
        public void initBindings() {
            ClassType compress = newClass("Compress", table, objectType);
            for (String s : list("compress", "flush")) {
                addMethod(compress, s, Types.StrInstance);
            }
            addMethod(compress, "copy", compress);
            addClass(compress);

            ClassType decompress = newClass("Decompress", table, objectType);
            for (String s : list("unused_data", "unconsumed_tail")) {
                addAttr(decompress, s, Types.StrInstance);
            }
            for (String s : list("decompress", "flush")) {
                addMethod(decompress, s, Types.StrInstance);
            }
            addMethod(decompress, "copy", decompress);
            addClass(decompress);

            addFunction("adler32", Types.IntInstance);
            addFunction("compress", Types.StrInstance);
            addFunction("compressobj", compress);
            addFunction("crc32", Types.IntInstance);
            addFunction("decompress", Types.StrInstance);
            addFunction("decompressobj", decompress);
        }
    }

    class UnittestModule extends NativeModule {
        public UnittestModule() {
            super("unittest");
        }

        @Override
        protected void initBindings() {
            ClassType testResult = newClass("TestResult", table, objectType);
            addAttr(testResult, "shouldStop", Types.BoolInstance);
            addAttr(testResult, "testsRun", Types.IntInstance);
            addAttr(testResult, "buffer", Types.BoolInstance);
            addAttr(testResult, "failfast", Types.BoolInstance);

            addMethod(testResult, "wasSuccessful", Types.BoolInstance);
            addMethod(testResult, "stop", Types.NoneInstance);
            addMethod(testResult, "startTest", Types.NoneInstance);
            addMethod(testResult, "stopTest", Types.NoneInstance);
            addMethod(testResult, "startTestRun", Types.NoneInstance);
            addMethod(testResult, "stopTestRun", Types.NoneInstance);
            addMethod(testResult, "addError", Types.NoneInstance);
            addMethod(testResult, "addFailure", Types.NoneInstance);
            addMethod(testResult, "addSuccess", Types.NoneInstance);
            addMethod(testResult, "addSkip", Types.NoneInstance);
            addMethod(testResult, "addExpectedFailure", Types.NoneInstance);
            addMethod(testResult, "addUnexpectedSuccess", Types.NoneInstance);
            addClass(testResult);

            ClassType textTestResult = newClass("TextTestResult", table, testResult);
            addClass(textTestResult);

            ClassType testCase = newClass("TestCase", table, objectType);
            for (String s : list("setUp", "tearDown", "setUpClass", "tearDownClass", "skipTest", "debug",
                "assertEqual", "assertNotEqual", "assertTrue", "assertFalse", "assertIs", "assertIsNot", "assertIsNone",
                "assertIsNotNone", "assertIn", "assertNotIn", "assertIsInstance", "assertNotIsInstance", "assertRaises",
                "assertRaisesRegexp", "assertAlmostEqual", "assertNotAlmostEqual", "assertGreater",
                "assertGreaterEqual", "assertLess", "assertLessEqual", "assertRegexpMatches", "assertNotRegexpMatches",
                "assertItemsEqual", "assertDictContainsSubset", "addTypeEqualityFunc", "assertMultiLineEqual",
                "assertSequenceEqual", "assertListEqual", "assertTupleEqual", "assertSetEqual", "assertDictEqual",
                "fail", "failureException", "addCleanup", "doCleanups")) {
                addMethod(testCase, s, Types.NoneInstance);
            }
            addMethod(testCase, "countTestCases", Types.IntInstance);
            addMethod(testCase, "id", Types.StrInstance);
            addMethod(testCase, "shortDescription", Types.StrInstance);
            addMethod(testCase, "defaultTestResult", testResult);
            addMethod(testCase, "run", testResult);
            addAttr(testCase, "longMessage", Types.BoolInstance);
            addAttr(testCase, "maxDiff", Types.IntInstance);
            addClass(testCase);


            ClassType testSuite = newClass("TestSuite", table, objectType);
            addMethod(testSuite, "addTest", Types.NoneInstance);
            addMethod(testSuite, "addTests", Types.NoneInstance);
            addMethod(testSuite, "run", testResult);
            addMethod(testSuite, "debug", Types.NoneInstance);
            addMethod(testSuite, "countTestCases", Types.IntInstance);
            addMethod(testSuite, "__iter__", newFunc(testCase));
            addClass(testSuite);


            ClassType testLoader = newClass("TestLoader", table, objectType);
            addMethod(testLoader, "loadTestsFromTestCase", testSuite);
            addMethod(testLoader, "loadTestsFromModule", testSuite);
            addMethod(testLoader, "loadTestsFromName", testSuite);
            addMethod(testLoader, "loadTestsFromNames", testSuite);
            addMethod(testLoader, "getTestCaseNames", testCase);
            addMethod(testLoader, "discover", testSuite);
            addAttr(testLoader, "testMethodPrefix", Types.StrInstance);
            addAttr(testLoader, "sortTestMethodsUsing", Types.StrInstance);
            addAttr(testLoader, "suiteClass", newFunc(testSuite));
            addClass(testLoader);

            addAttr("defaultTestLoader", testLoader);

            ClassType textTestRunner = newClass("TextTestRunner", table, objectType);
            addClass(textTestRunner);

            addNoneFuncs("main", "installHandler", "registerResult", "removeResult", "removeHandler");

            addAttr(testResult, "errors", newList(newTuple(testCase, Types.StrInstance)));
            addAttr(testResult, "failures", newList(newTuple(testCase, Types.StrInstance)));
            addAttr(testResult, "skipped", newList(newTuple(testCase, Types.StrInstance)));
            addAttr(testResult, "expectedFailures", newList(newTuple(testCase, Types.StrInstance)));
            addAttr(testResult, "unexpectedSuccesses",newList(newTuple(testCase, Types.StrInstance)));

        }
    }
}
