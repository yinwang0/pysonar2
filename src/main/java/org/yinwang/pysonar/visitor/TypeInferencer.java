package org.yinwang.pysonar.visitor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.pysonar.$;
import org.yinwang.pysonar.Analyzer;
import org.yinwang.pysonar.Binding;
import org.yinwang.pysonar.Builtins;
import org.yinwang.pysonar.CallStackEntry;
import org.yinwang.pysonar.State;
import org.yinwang.pysonar.ast.*;
import org.yinwang.pysonar.types.ClassType;
import org.yinwang.pysonar.types.DictType;
import org.yinwang.pysonar.types.FunType;
import org.yinwang.pysonar.types.InstanceType;
import org.yinwang.pysonar.types.ListType;
import org.yinwang.pysonar.types.ModuleType;
import org.yinwang.pysonar.types.TupleType;
import org.yinwang.pysonar.types.Type;
import org.yinwang.pysonar.types.Types;
import org.yinwang.pysonar.types.UnionType;

import static org.yinwang.pysonar.Binding.Kind.ATTRIBUTE;
import static org.yinwang.pysonar.Binding.Kind.CLASS;
import static org.yinwang.pysonar.Binding.Kind.CONSTRUCTOR;
import static org.yinwang.pysonar.Binding.Kind.FUNCTION;
import static org.yinwang.pysonar.Binding.Kind.METHOD;
import static org.yinwang.pysonar.Binding.Kind.MODULE;
import static org.yinwang.pysonar.Binding.Kind.PARAMETER;
import static org.yinwang.pysonar.Binding.Kind.SCOPE;
import static org.yinwang.pysonar.Binding.Kind.VARIABLE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TypeInferencer implements Visitor1<Type, State>
{

    @NotNull
    @Override
    public Type visit(PyModule node, State s)
    {
        ModuleType mt = new ModuleType(node.name, node.file, Analyzer.self.globaltable);
        s.insert($.moduleQname(node.file), node, mt, MODULE);
        if (node.body != null)
        {
            visit(node.body, mt.table);
        }
        return mt;
    }

    @NotNull
    @Override
    public Type visit(Alias node, State s)
    {
        return Types.UNKNOWN;
    }

    @NotNull
    @Override
    public Type visit(Assert node, State s)
    {
        if (node.test != null)
        {
            visit(node.test, s);
        }
        if (node.msg != null)
        {
            visit(node.msg, s);
        }
        return Types.CONT;
    }

    @NotNull
    @Override
    public Type visit(Assign node, State s)
    {
        Type valueType = visit(node.value, s);
        bind(s, node.target, valueType);
        return Types.CONT;
    }

    @NotNull
    @Override
    public Type visit(Attribute node, State s)
    {
        Type targetType = visit(node.target, s);
        if (targetType instanceof UnionType)
        {
            Set<Type> types = ((UnionType) targetType).types;
            Type retType = Types.UNKNOWN;
            for (Type tt : types)
            {
                retType = UnionType.union(retType, getAttrType(node, tt));
            }
            return retType;
        }
        else
        {
            return getAttrType(node, targetType);
        }
    }

    @NotNull
    @Override
    public Type visit(Await node, State s)
    {
        if (node.value == null)
        {
            return Types.NoneInstance;
        }
        else
        {
            return visit(node.value, s);
        }
    }

    @NotNull
    @Override
    public Type visit(BinOp node, State s)
    {
        Type ltype = visit(node.left, s);
        Type rtype = visit(node.right, s);
        if (operatorOverridden(ltype, node.op.getMethod()))
        {
            Type result = applyOp(node.op, ltype, rtype, node.op.getMethod(), node, node.left);
            if (result != null)
            {
                return result;
            }
        }
        else if (Op.isBoolean(node.op))
        {
            return Types.BoolInstance;
        }
        else if (ltype == Types.UNKNOWN)
        {
            return rtype;
        }
        else if (rtype == Types.UNKNOWN)
        {
            return ltype;
        }
        else if (ltype.typeEquals(rtype))
        {
            return ltype;
        }
        else if (node.op == Op.Or)
        {
            if (rtype == Types.NoneInstance)
            {
                return ltype;
            }
            else if (ltype == Types.NoneInstance)
            {
                return rtype;
            }
        }
        else if (node.op == Op.And)
        {
            if (rtype == Types.NoneInstance || ltype == Types.NoneInstance)
            {
                return Types.NoneInstance;
            }
        }

        addWarningToNode(node,
                         "Cannot apply binary operator " + node.op.getRep() + " to type " + ltype + " and " + rtype);
        return Types.UNKNOWN;
    }

    private boolean operatorOverridden(Type type, String method)
    {
        if (type instanceof InstanceType)
        {
            Type opType = type.table.lookupAttrType(method);
            if (opType != null)
            {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private Type applyOp(Op op, Type ltype, Type rtype, String method, Node node, Node left)
    {
        Type opType = ltype.table.lookupAttrType(method);
        if (opType instanceof FunType)
        {
            return apply((FunType) opType, ltype, Collections.singletonList(rtype), null, null, null, node);
        }
        else
        {
            addWarningToNode(left, "Operator method " + method + " is not a function");
            return null;
        }
    }

    @NotNull
    @Override
    public Type visit(Block node, State s)
    {
        // first pass: mark global names
        for (Node n : node.seq)
        {
            if (n instanceof Global)
            {
                for (Name name : ((Global) n).names)
                {
                    s.addGlobalName(name.id);
                    Set<Binding> nb = s.lookup(name.id);
                    if (nb != null)
                    {
                        Analyzer.self.putRef(name, nb);
                    }
                }
            }
        }

        boolean returned = false;
        Type retType = Types.UNKNOWN;

        for (Node n : node.seq)
        {
            Type t = visit(n, s);
            if (!returned)
            {
                retType = UnionType.union(retType, t);
                if (!UnionType.contains(t, Types.CONT))
                {
                    returned = true;
                    retType = UnionType.remove(retType, Types.CONT);
                }
            }
        }

        return retType;
    }

    @NotNull
    @Override
    public Type visit(Break node, State s)
    {
        return Types.NoneInstance;
    }

    @NotNull
    @Override
    public Type visit(Bytes node, State s)
    {
        return Types.StrInstance;
    }

    @NotNull
    @Override
    public Type visit(Call node, State s)
    {
        Type fun;
        Type selfType = null;

        if (node.func instanceof Attribute)
        {
            Node target = ((Attribute) node.func).target;
            Name attr = ((Attribute) node.func).attr;
            Type targetType = visit(target, s);
            if (!(targetType instanceof ModuleType))
            {
                selfType = targetType;
            }
            Set<Binding> b = targetType.table.lookupAttr(attr.id);
            if (b != null)
            {
                Analyzer.self.putRef(attr, b);
                fun = State.makeUnion(b);
            }
            else
            {
                Analyzer.self.putProblem(attr, "Attribute is not found in type: " + attr.id);
                fun = Types.UNKNOWN;
            }
        }
        else
        {
            fun = visit(node.func, s);
        }

        // Infer positional argument types
        List<Type> positional = visit(node.args, s);

        // Infer keyword argument types
        Map<String, Type> kwTypes = new HashMap<>();
        if (node.keywords != null)
        {
            for (Keyword k : node.keywords)
            {
                kwTypes.put(k.arg, visit(k.value, s));
            }
        }

        Type kwArg = node.kwargs == null ? null : visit(node.kwargs, s);
        Type starArg = node.starargs == null ? null : visit(node.starargs, s);

        if (fun instanceof UnionType)
        {
            Set<Type> types = ((UnionType) fun).types;
            Type resultType = Types.UNKNOWN;
            for (Type funType : types)
            {
                Type returnType = resolveCall(funType, selfType, positional, kwTypes, kwArg, starArg, node);
                resultType = UnionType.union(resultType, returnType);
            }
            return resultType;
        }
        else
        {
            return resolveCall(fun, selfType, positional, kwTypes, kwArg, starArg, node);
        }
    }

    @NotNull
    @Override
    public Type visit(ClassDef node, State s)
    {
        ClassType classType = new ClassType(node.name.id, s);
        List<Type> baseTypes = new ArrayList<>();
        for (Node base : node.bases)
        {
            Type baseType = visit(base, s);
            if (baseType instanceof ClassType)
            {
                classType.addSuper(baseType);
            }
            else if (baseType instanceof UnionType)
            {
                for (Type parent : ((UnionType) baseType).types)
                {
                    classType.addSuper(parent);
                }
            }
            else
            {
                addWarningToNode(base, base + " is not a class");
            }
            baseTypes.add(baseType);
        }

        // XXX: Not sure if we should add "bases", "name" and "dict" here. They
        // must be added _somewhere_ but I'm just not sure if it should be HERE.
        node.addSpecialAttribute(classType.table, "__bases__", new TupleType(baseTypes));
        node.addSpecialAttribute(classType.table, "__name__", Types.StrInstance);
        node.addSpecialAttribute(classType.table, "__dict__",
                                 new DictType(Types.StrInstance, Types.UNKNOWN));
        node.addSpecialAttribute(classType.table, "__module__", Types.StrInstance);
        node.addSpecialAttribute(classType.table, "__doc__", Types.StrInstance);

        // Bind ClassType to name here before resolving the body because the
        // methods need node type as self.
        bind(s, node.name, classType, CLASS);
        if (node.body != null)
        {
            visit(node.body, classType.table);
        }
        return Types.CONT;
    }

    @NotNull
    @Override
    public Type visit(Comprehension node, State s)
    {
        bindIter(s, node.target, node.iter, SCOPE);
        visit(node.ifs, s);
        return visit(node.target, s);
    }

    @NotNull
    @Override
    public Type visit(Continue node, State s)
    {
        return Types.CONT;
    }

    @NotNull
    @Override
    public Type visit(Delete node, State s)
    {
        for (Node n : node.targets)
        {
            visit(n, s);
            if (n instanceof Name)
            {
                s.remove(((Name) n).id);
            }
        }
        return Types.CONT;
    }

    @NotNull
    @Override
    public Type visit(Dict node, State s)
    {
        Type keyType = resolveUnion(node.keys, s);
        Type valType = resolveUnion(node.values, s);
        return new DictType(keyType, valType);
    }

    @NotNull
    @Override
    public Type visit(DictComp node, State s)
    {
        visit(node.generators, s);
        Type keyType = visit(node.key, s);
        Type valueType = visit(node.value, s);
        return new DictType(keyType, valueType);
    }

    @NotNull
    @Override
    public Type visit(Dummy node, State s)
    {
        return Types.UNKNOWN;
    }

    @NotNull
    @Override
    public Type visit(Ellipsis node, State s)
    {
        return Types.NoneInstance;
    }

    @NotNull
    @Override
    public Type visit(Exec node, State s)
    {
        if (node.body != null)
        {
            visit(node.body, s);
        }
        if (node.globals != null)
        {
            visit(node.globals, s);
        }
        if (node.locals != null)
        {
            visit(node.locals, s);
        }
        return Types.CONT;
    }

    @NotNull
    @Override
    public Type visit(Expr node, State s)
    {
        if (node.value != null)
        {
            visit(node.value, s);
        }
        return Types.CONT;

    }

    @NotNull
    @Override
    public Type visit(ExtSlice node, State s)
    {
        for (Node d : node.dims)
        {
            visit(d, s);
        }
        return new ListType();
    }

    @NotNull
    @Override
    public Type visit(For node, State s)
    {
        bindIter(s, node.target, node.iter, SCOPE);
        Type t1 = Types.UNKNOWN;
        Type t2 = Types.UNKNOWN;
        Type t3 = Types.UNKNOWN;

        State s1 = s.copy();
        State s2 = s.copy();

        if (node.body != null)
        {
            t1 = visit(node.body, s1);
            s.merge(s1);
            t2 = visit(node.body, s1);
            s.merge(s1);
        }

        if (node.orelse != null)
        {
            t3 = visit(node.orelse, s2);
            s.merge(s2);
        }

        return UnionType.union(UnionType.union(t1, t2), t3);
    }

    @NotNull
    @Override
    public Type visit(FunctionDef node, State s)
    {
        State env = s.getForwarding();
        FunType fun = new FunType(node, env);
        fun.table.setParent(s);
        fun.table.setPath(s.extendPath(node.name.id));
        fun.setDefaultTypes(visit(node.defaults, s));
        Analyzer.self.addUncalled(fun);
        Binding.Kind funkind;

        if (node.isLamba)
        {
            return fun;
        }
        else
        {
            if (s.stateType == State.StateType.CLASS)
            {
                if ("__init__".equals(node.name.id))
                {
                    funkind = CONSTRUCTOR;
                }
                else
                {
                    funkind = METHOD;
                }
            }
            else
            {
                funkind = FUNCTION;
            }

            Type outType = s.type;
            if (outType instanceof ClassType)
            {
                fun.setCls((ClassType) outType);
            }

            bind(s, node.name, fun, funkind);
            return Types.CONT;
        }
    }

    @NotNull
    @Override
    public Type visit(GeneratorExp node, State s)
    {
        visit(node.generators, s);
        return new ListType(visit(node.elt, s));
    }

    @NotNull
    @Override
    public Type visit(Global node, State s)
    {
        return Types.CONT;
    }

    @NotNull
    @Override
    public Type visit(Handler node, State s)
    {
        Type typeval = Types.UNKNOWN;
        if (node.exceptions != null)
        {
            typeval = resolveUnion(node.exceptions, s);
        }
        if (node.binder != null)
        {
            bind(s, node.binder, typeval);
        }
        if (node.body != null)
        {
            return visit(node.body, s);
        }
        else
        {
            return Types.UNKNOWN;
        }
    }

    @NotNull
    @Override
    public Type visit(If node, State s)
    {
        Type type1, type2;
        State s1 = s.copy();
        State s2 = s.copy();

        // Ignore result because Python can treat anything as bool
        visit(node.test, s);
        inferInstance(node.test, s, s1);

        if (node.body != null)
        {
            type1 = visit(node.body, s1);
        }
        else
        {
            type1 = Types.CONT;
        }

        if (node.orelse != null)
        {
            type2 = visit(node.orelse, s2);
        }
        else
        {
            type2 = Types.CONT;
        }

        boolean cont1 = UnionType.contains(type1, Types.CONT);
        boolean cont2 = UnionType.contains(type2, Types.CONT);

        // decide which branch affects the downstream state
        if (cont1 && cont2)
        {
            s1.merge(s2);
            s.overwrite(s1);
        }
        else if (cont1)
        {
            s.overwrite(s1);
        }
        else if (cont2)
        {
            s.overwrite(s2);
        }

        return UnionType.union(type1, type2);
    }

    /**
     * Helper for branch inference for 'isinstance'
     */
    private void inferInstance(Node test, State s, State s1)
    {
        if (test instanceof Call)
        {
            Call testCall = (Call) test;
            if (testCall.func instanceof Name)
            {
                Name testFunc = (Name) testCall.func;
                if (testFunc.id.equals("isinstance"))
                {
                    if (testCall.args.size() >= 2)
                    {
                        Node id = testCall.args.get(0);
                        if (id instanceof Name)
                        {
                            Node typeExp = testCall.args.get(1);
                            Type type = visit(typeExp, s);
                            if (type instanceof ClassType)
                            {
                                type = ((ClassType) type).getInstance(null, this, test);
                            }
                            s1.insert(((Name) id).id, id, type, VARIABLE);
                        }
                    }

                    if (testCall.args.size() != 2)
                    {
                        addWarningToNode(test, "Incorrect number of arguments for isinstance");
                    }
                }
            }
        }
    }

    @NotNull
    @Override
    public Type visit(IfExp node, State s)
    {
        Type type1, type2;
        visit(node.test, s);

        if (node.body != null)
        {
            type1 = visit(node.body, s);
        }
        else
        {
            type1 = Types.CONT;
        }
        if (node.orelse != null)
        {
            type2 = visit(node.orelse, s);
        }
        else
        {
            type2 = Types.CONT;
        }
        return UnionType.union(type1, type2);
    }

    @NotNull
    @Override
    public Type visit(Import node, State s)
    {
        for (Alias a : node.names)
        {
            Type mod = Analyzer.self.loadModule(a.name, s);
            if (mod == null)
            {
                addWarningToNode(node, "Cannot load module");
            }
            else if (a.asname != null)
            {
                s.insert(a.asname.id, a.asname, mod, VARIABLE);
            }
        }
        return Types.CONT;
    }

    @NotNull
    @Override
    public Type visit(ImportFrom node, State s)
    {
        if (node.module == null)
        {
            return Types.CONT;
        }

        Type mod = Analyzer.self.loadModule(node.module, s);

        if (mod == null)
        {
            addWarningToNode(node, "Cannot load module");
        }
        else if (node.isImportStar())
        {
            node.importStar(s, mod);
        }
        else
        {
            for (Alias a : node.names)
            {
                Name first = a.name.get(0);
                Set<Binding> bs = mod.table.lookup(first.id);
                if (bs != null)
                {
                    if (a.asname != null)
                    {
                        s.update(a.asname.id, bs);
                        Analyzer.self.putRef(a.asname, bs);
                    }
                    else
                    {
                        s.update(first.id, bs);
                        Analyzer.self.putRef(first, bs);
                    }
                }
                else
                {
                    List<Name> ext = new ArrayList<>(node.module);
                    ext.add(first);
                    Type mod2 = Analyzer.self.loadModule(ext, s);
                    if (mod2 != null)
                    {
                        if (a.asname != null)
                        {
                            Binding binding = Binding.createFileBinding(a.asname.id, mod2.file, mod2);
                            s.update(a.asname.id, binding);
                            Analyzer.self.putRef(a.asname, binding);
                        }
                        else
                        {
                            Binding binding = Binding.createFileBinding(first.id, mod2.file, mod2);
                            s.update(first.id, binding);
                            Analyzer.self.putRef(first, binding);
                        }
                    }
                }
            }
        }

        return Types.CONT;
    }

    @NotNull
    @Override
    public Type visit(Index node, State s)
    {
        return visit(node.value, s);
    }

    @NotNull
    @Override
    public Type visit(Keyword node, State s)
    {
        return visit(node.value, s);
    }

    @NotNull
    @Override
    public Type visit(ListComp node, State s)
    {
        visit(node.generators, s);
        return new ListType(visit(node.elt, s));
    }

    @NotNull
    @Override
    public Type visit(Name node, State s)
    {
        Set<Binding> b = s.lookup(node.id);
        if (b != null)
        {
            Analyzer.self.putRef(node, b);
            Analyzer.self.resolved.add(node);
            Analyzer.self.unresolved.remove(node);
            return State.makeUnion(b);
        }
        else
        {
            addWarningToNode(node, "unbound variable " + node.id);
            Analyzer.self.unresolved.add(node);
            Type t = Types.UNKNOWN;
            t.table.setPath(s.extendPath(node.id));
            return t;
        }
    }

    @NotNull
    @Override
    public Type visit(Pass node, State s)
    {
        return Types.CONT;
    }

    @NotNull
    @Override
    public Type visit(Print node, State s)
    {
        if (node.dest != null)
        {
            visit(node.dest, s);
        }
        if (node.values != null)
        {
            visit(node.values, s);
        }
        return Types.CONT;
    }

    @NotNull
    @Override
    public Type visit(PyComplex node, State s)
    {
        return Types.ComplexInstance;
    }

    @NotNull
    @Override
    public Type visit(PyFloat node, State s)
    {
        return Types.FloatInstance;
    }

    @NotNull
    @Override
    public Type visit(PyInt node, State s)
    {
        return Types.IntInstance;
    }

    @NotNull
    @Override
    public Type visit(PyList node, State s)
    {
        if (node.elts.size() == 0)
        {
            return new ListType();  // list<unknown>
        }

        ListType listType = new ListType();
        for (Node elt : node.elts)
        {
            listType.add(visit(elt, s));
            if (elt instanceof Str)
            {
                listType.addValue(((Str) elt).value);
            }
        }

        return listType;
    }

    @NotNull
    @Override
    public Type visit(PySet node, State s)
    {
        if (node.elts.size() == 0)
        {
            return new ListType();
        }

        ListType listType = null;
        for (Node elt : node.elts)
        {
            if (listType == null)
            {
                listType = new ListType(visit(elt, s));
            }
            else
            {
                listType.add(visit(elt, s));
            }
        }

        return listType;
    }

    @NotNull
    @Override
    public Type visit(Raise node, State s)
    {
        if (node.exceptionType != null)
        {
            visit(node.exceptionType, s);
        }
        if (node.inst != null)
        {
            visit(node.inst, s);
        }
        if (node.traceback != null)
        {
            visit(node.traceback, s);
        }
        return Types.CONT;
    }

    @NotNull
    @Override
    public Type visit(Repr node, State s)
    {
        if (node.value != null)
        {
            visit(node.value, s);
        }
        return Types.StrInstance;
    }

    @NotNull
    @Override
    public Type visit(Return node, State s)
    {
        if (node.value == null)
        {
            return Types.NoneInstance;
        }
        else
        {
            Type result = visit(node.value, s);

            CallStackEntry entry = Analyzer.self.callStack.top();
            if (entry != null)
            {
                entry.fun.addMapping(entry.from, result);
            }

            return result;
        }
    }

    @NotNull
    @Override
    public Type visit(SetComp node, State s)
    {
        visit(node.generators, s);
        return new ListType(visit(node.elt, s));
    }

    @NotNull
    @Override
    public Type visit(Slice node, State s)
    {
        if (node.lower != null)
        {
            visit(node.lower, s);
        }
        if (node.step != null)
        {
            visit(node.step, s);
        }
        if (node.upper != null)
        {
            visit(node.upper, s);
        }
        return new ListType();
    }

    @NotNull
    @Override
    public Type visit(Starred node, State s)
    {
        return visit(node.value, s);
    }

    @NotNull
    @Override
    public Type visit(Str node, State s)
    {
        return Types.StrInstance;
    }

    @NotNull
    @Override
    public Type visit(Subscript node, State s)
    {
        Type vt = visit(node.value, s);
        Type st = node.slice == null ? null : visit(node.slice, s);

        if (vt instanceof UnionType)
        {
            Type retType = Types.UNKNOWN;
            for (Type t : ((UnionType) vt).types)
            {
                retType = UnionType.union(retType, getSubscript(node, t, st, s));
            }
            return retType;
        }
        else
        {
            return getSubscript(node, vt, st, s);
        }
    }

    @NotNull
    @Override
    public Type visit(Try node, State s)
    {
        Type tp1 = Types.UNKNOWN;
        Type tp2 = Types.UNKNOWN;
        Type tph = Types.UNKNOWN;
        Type tpFinal = Types.UNKNOWN;

        if (node.handlers != null)
        {
            for (Handler h : node.handlers)
            {
                tph = UnionType.union(tph, visit(h, s));
            }
        }

        if (node.body != null)
        {
            tp1 = visit(node.body, s);
        }

        if (node.orelse != null)
        {
            tp2 = visit(node.orelse, s);
        }

        if (node.finalbody != null)
        {
            tpFinal = visit(node.finalbody, s);
        }

        return new UnionType(tp1, tp2, tph, tpFinal);
    }

    @NotNull
    @Override
    public Type visit(Tuple node, State s)
    {
        TupleType t = new TupleType();
        for (Node e : node.elts)
        {
            t.add(visit(e, s));
        }
        return t;
    }

    @NotNull
    @Override
    public Type visit(UnaryOp node, State s)
    {
        return visit(node.operand, s);
    }

    @NotNull
    @Override
    public Type visit(Unsupported node, State s)
    {
        return Types.NoneInstance;
    }

    @NotNull
    @Override
    public Type visit(Url node, State s)
    {
        return Types.StrInstance;
    }

    @NotNull
    @Override
    public Type visit(While node, State s)
    {
        visit(node.test, s);
        Type t1 = Types.UNKNOWN;
        Type t2 = Types.UNKNOWN;
        Type t3 = Types.UNKNOWN;

        State s1 = s.copy();
        State s2 = s.copy();

        if (node.body != null)
        {
            t1 = visit(node.body, s1);
            s.merge(s1);

            t2 = visit(node.body, s1);
            s.merge(s1);
        }

        if (node.orelse != null)
        {
            t3 = visit(node.orelse, s2);
            s.merge(s2);
        }

        return UnionType.union(UnionType.union(t1, t2), t3);
    }

    @NotNull
    @Override
    public Type visit(With node, State s)
    {
        for (Withitem item : node.items)
        {
            Type val = visit(item.context_expr, s);
            if (item.optional_vars != null)
            {
                bind(s, item.optional_vars, val);
            }
        }
        return visit(node.body, s);
    }

    @NotNull
    @Override
    public Type visit(Withitem node, State s)
    {
        return Types.UNKNOWN;
    }

    @NotNull
    @Override
    public Type visit(Yield node, State s)
    {
        if (node.value != null)
        {
            return new ListType(visit(node.value, s));
        }
        else
        {
            return Types.NoneInstance;
        }
    }

    @NotNull
    @Override
    public Type visit(YieldFrom node, State s)
    {
        if (node.value != null)
        {
            return new ListType(visit(node.value, s));
        }
        else
        {
            return Types.NoneInstance;
        }
    }

    @NotNull
    private Type resolveUnion(@NotNull Collection<? extends Node> nodes, State s)
    {
        Type result = Types.UNKNOWN;
        for (Node node : nodes)
        {
            Type nodeType = visit(node, s);
            result = UnionType.union(result, nodeType);
        }
        return result;
    }

    public void setAttr(Attribute node, State s, @NotNull Type v)
    {
        Type targetType = visit(node.target, s);
        if (targetType instanceof UnionType)
        {
            Set<Type> types = ((UnionType) targetType).types;
            for (Type tp : types)
            {
                setAttrType(node, tp, v);
            }
        }
        else
        {
            setAttrType(node, targetType, v);
        }
    }

    private void setAttrType(Attribute node, @NotNull Type targetType, @NotNull Type v)
    {
        if (targetType.isUnknownType())
        {
            addWarningToNode(node, "Can't set attribute for UnknownType");
            return;
        }

        Set<Binding> bs = targetType.table.lookupAttr(node.attr.id);
        if (bs != null)
        {
            for (Binding b : bs)
            {
                b.addType(v);
                Analyzer.self.putRef(node.attr, b);
            }
        }
        else
        {
            targetType.table.insert(node.attr.id, node.attr, v, ATTRIBUTE);
        }
    }

    public Type getAttrType(Attribute node, @NotNull Type targetType)
    {
        Set<Binding> bs = targetType.table.lookupAttr(node.attr.id);
        if (bs == null)
        {
            addWarningToNode(node.attr, "attribute not found in type: " + targetType);
            Type t = Types.UNKNOWN;
            t.table.setPath(targetType.table.extendPath(node.attr.id));
            return t;
        }
        else
        {
            for (Binding b : bs)
            {
                Analyzer.self.putRef(node.attr, b);
            }
            return State.makeUnion(bs);
        }
    }

    @NotNull
    public Type resolveCall(@NotNull Type fun,
                            @Nullable Type selfType,
                            @NotNull List<Type> positional,
                            @NotNull Map<String, Type> kwTypes,
                            @Nullable Type kwArg,
                            @Nullable Type starArg,
                            @NotNull Call node)
    {
        if (fun instanceof FunType)
        {
            return apply((FunType) fun, selfType, positional, kwTypes, kwArg, starArg, node);
        }
        else if (fun instanceof ClassType)
        {
            return new InstanceType(fun, positional, this, node);
        }
        else
        {
            addWarningToNode(node, "calling non-function and non-class: " + fun);
            return Types.UNKNOWN;
        }
    }

    @NotNull
    public Type apply(@NotNull FunType func,
                      @Nullable Type selfType,
                      @Nullable List<Type> positional,
                      @Nullable Map<String, Type> kwTypes,
                      @Nullable Type kwArg,
                      @Nullable Type starArg,
                      @Nullable Node call)
    {
        if (call instanceof Call &&
            ((Call) call).func instanceof Attribute &&
            ((Attribute) ((Call) call).func).attr.id.equals("append"))
        {
            if (selfType instanceof ListType)
            {
                ListType listType = (ListType) selfType;
                if (positional != null && positional.size() == 1)
                {
                    listType.add(positional.get(0));
                }
                else
                {
                    Analyzer.self.putProblem(call, "Calling append with wrong argument types");
                }
            }
        }

        if (call instanceof Call &&
            ((Call) call).func instanceof Attribute &&
            ((Attribute) ((Call) call).func).attr.id.equals("update"))
        {
            if (selfType instanceof DictType)
            {
                DictType dict = (DictType) selfType;
                if (positional != null && positional.size() == 1)
                {
                    Type argType = positional.get(0);
                    if (argType instanceof DictType)
                    {
                        dict.keyType = UnionType.union(dict.keyType, ((DictType) argType).keyType);
                        dict.valueType = UnionType.union(dict.valueType, ((DictType) argType).valueType);
                    }
                }
                else
                {
                    Analyzer.self.putProblem(call, "Calling update with wrong argument types");
                }
            }
        }

        Analyzer.self.removeUncalled(func);

        if (func.func != null && !func.func.called)
        {
            Analyzer.self.nCalled++;
            func.func.called = true;
        }

        if (func.func == null)
        {
            // func without definition (possibly builtins)
            return func.getReturnType();
        }

        List<Type> argTypes = new ArrayList<>();

        // Add class or object as first argument if it is not static method
        if (!func.func.isStaticMethod())
        {
            if (func.func.isClassMethod())
            {
                if (func.cls != null)
                {
                    argTypes.add(func.cls);
                }
                else if (selfType != null && selfType instanceof InstanceType)
                {
                    argTypes.add(((InstanceType) selfType).classType);
                }
            }
            else
            {
                // usual method
                if (selfType != null)
                {
                    argTypes.add(selfType);
                }
                else
                {
                    if (func.cls != null)
                    {
                        if (!func.func.name.id.equals("__init__"))
                        {
                            argTypes.add(func.cls.getInstance(null, this, call));
                        }
                        else
                        {
                            argTypes.add(func.cls.getInstance());
                        }
                    }
                }
            }
        }

        // Put in positional arguments
        if (positional != null)
        {
            argTypes.addAll(positional);
        }

        bindMethodAttrs(func);

        State callState = new State(func.env, State.StateType.FUNCTION);

        if (func.table.parent != null)
        {
            callState.setPath(func.table.parent.extendPath(func.func.name.id));
        }
        else
        {
            callState.setPath(func.func.name.id);
        }

        Type fromType = bindParams(callState, func.func, argTypes, func.defaultTypes, kwTypes, kwArg, starArg);
        Type cachedTo = func.getMapping(fromType);

        if (cachedTo != null)
        {
            return cachedTo;
        }
        else if (func.oversized())
        {
            return Types.UNKNOWN;
        }
        else
        {
            func.addMapping(fromType, Types.UNKNOWN);
            Analyzer.self.callStack.push(new CallStackEntry(func, fromType));
            Type toType = visit(func.func.body, callState);
            Analyzer.self.callStack.pop();
            if (missingReturn(toType))
            {
                addWarningToNode(func.func.name, "Function not always return a value");

                if (call != null)
                {
                    addWarningToNode(call, "Call not always return a value");
                }
            }

            toType = UnionType.remove(toType, Types.CONT);
            if (!func.func.name.id.equals("__init__"))
            {
                func.addMapping(fromType, toType);
            }
            else
            {
                func.removeMapping(fromType);
            }

            return toType;
        }
    }

    @NotNull
    private Type bindParams(@NotNull State state,
                            @NotNull FunctionDef func,
                            @Nullable List<Type> pTypes,
                            @Nullable List<Type> dTypes,
                            @Nullable Map<String, Type> hash,
                            @Nullable Type kw,
                            @Nullable Type star)
    {

        List<Node> args = func.args;
        Name rest = func.vararg;
        Name restKw = func.kwarg;

        TupleType fromType = new TupleType();
        int pSize = args == null ? 0 : args.size();
        int aSize = pTypes == null ? 0 : pTypes.size();
        int dSize = dTypes == null ? 0 : dTypes.size();
        int nPos = pSize - dSize;

        if (star != null && star instanceof ListType)
        {
            star = ((ListType) star).toTupleType();
        }

        for (int i = 0, j = 0; i < pSize; i++)
        {
            Node arg = args.get(i);
            Type aType;
            if (i < aSize)
            {
                aType = pTypes.get(i);
            }
            else if (i - nPos >= 0 && i - nPos < dSize)
            {
                aType = dTypes.get(i - nPos);
            }
            else
            {
                if (hash != null && args.get(i) instanceof Name &&
                    hash.containsKey(((Name) args.get(i)).id))
                {
                    aType = hash.get(((Name) args.get(i)).id);
                    hash.remove(((Name) args.get(i)).id);
                }
                else
                {
                    if (star != null && star instanceof TupleType &&
                        j < ((TupleType) star).eltTypes.size())
                    {
                        aType = ((TupleType) star).get(j++);
                    }
                    else
                    {
                        aType = Types.UNKNOWN;
                        addWarningToNode(args.get(i), "unable to bind argument:" + args.get(i));
                    }
                }
            }
            bind(state, arg, aType, PARAMETER);
            fromType.add(aType);
        }

        if (restKw != null)
        {
            if (hash != null && !hash.isEmpty())
            {
                Type hashType = UnionType.newUnion(hash.values());
                bind(state, restKw, new DictType(Types.StrInstance, hashType), PARAMETER);
            }
            else
            {
                bind(state, restKw, Types.UNKNOWN, PARAMETER);
            }
        }

        if (rest != null)
        {
            if (pTypes.size() > pSize)
            {
                if (func.afterRest != null)
                {
                    int nAfter = func.afterRest.size();
                    for (int i = 0; i < nAfter; i++)
                    {
                        bind(state, func.afterRest.get(i), pTypes.get(pTypes.size() - nAfter + i), PARAMETER);
                    }
                    if (pTypes.size() - nAfter > 0)
                    {
                        Type restType = new TupleType(pTypes.subList(pSize, pTypes.size() - nAfter));
                        bind(state, rest, restType, PARAMETER);
                    }
                }
                else
                {
                    Type restType = new TupleType(pTypes.subList(pSize, pTypes.size()));
                    bind(state, rest, restType, PARAMETER);
                }
            }
            else
            {
                bind(state, rest, Types.UNKNOWN, PARAMETER);
            }
        }

        return fromType;
    }

    static void bindMethodAttrs(@NotNull FunType cl)
    {
        if (cl.table.parent != null)
        {
            Type cls = cl.table.parent.type;
            if (cls != null && cls instanceof ClassType)
            {
                addReadOnlyAttr(cl, "im_class", cls, CLASS);
                addReadOnlyAttr(cl, "__class__", cls, CLASS);
                addReadOnlyAttr(cl, "im_self", cls, ATTRIBUTE);
                addReadOnlyAttr(cl, "__self__", cls, ATTRIBUTE);
            }
        }
    }

    static void addReadOnlyAttr(@NotNull FunType fun,
                                String name,
                                @NotNull Type type,
                                Binding.Kind kind)
    {
        Node loc = Builtins.newDataModelUrl("the-standard-type-hierarchy");
        Binding b = new Binding(name, loc, type, kind);
        fun.table.update(name, b);
        b.markSynthetic();
        b.markStatic();
    }

    static boolean missingReturn(@NotNull Type toType)
    {
        boolean hasNone = false;
        boolean hasOther = false;

        if (toType instanceof UnionType)
        {
            for (Type t : ((UnionType) toType).types)
            {
                if (t == Types.NoneInstance || t == Types.CONT)
                {
                    hasNone = true;
                }
                else
                {
                    hasOther = true;
                }
            }
        }

        return hasNone && hasOther;
    }

    @NotNull
    public Type getSubscript(Node node, @NotNull Type vt, @Nullable Type st, State s)
    {
        if (vt.isUnknownType())
        {
            return Types.UNKNOWN;
        }
        else
        {
            if (vt instanceof ListType)
            {
                return getListSubscript(node, vt, st, s);
            }
            else if (vt instanceof TupleType)
            {
                return getListSubscript(node, ((TupleType) vt).toListType(), st, s);
            }
            else if (vt instanceof DictType)
            {
                DictType dt = (DictType) vt;
                if (!dt.keyType.equals(st))
                {
                    addWarningToNode(node, "Possible KeyError (wrong type for subscript)");
                }
                return ((DictType) vt).valueType;
            }
            else if (vt == Types.StrInstance)
            {
                if (st != null && (st instanceof ListType || st.isNumType()))
                {
                    return vt;
                }
                else
                {
                    addWarningToNode(node, "Possible KeyError (wrong type for subscript)");
                    return Types.UNKNOWN;
                }
            }
            else
            {
                return Types.UNKNOWN;
            }
        }
    }

    @NotNull
    private Type getListSubscript(Node node, @NotNull Type vt, @Nullable Type st, State s)
    {
        if (vt instanceof ListType)
        {
            if (st != null && st instanceof ListType)
            {
                return vt;
            }
            else if (st == null || st.isNumType())
            {
                return ((ListType) vt).eltType;
            }
            else
            {
                Type sliceFunc = vt.table.lookupAttrType("__getslice__");
                if (sliceFunc == null)
                {
                    addError(node, "The type can't be sliced: " + vt);
                    return Types.UNKNOWN;
                }
                else if (sliceFunc instanceof FunType)
                {
                    return apply((FunType) sliceFunc, null, null, null, null, null, node);
                }
                else
                {
                    addError(node, "The type's __getslice__ method is not a function: " + sliceFunc);
                    return Types.UNKNOWN;
                }
            }
        }
        else
        {
            return Types.UNKNOWN;
        }
    }

    public void bind(@NotNull State s, Node target, @NotNull Type rvalue, Binding.Kind kind)
    {
        if (target instanceof Name)
        {
            bind(s, (Name) target, rvalue, kind);
        }
        else if (target instanceof Tuple)
        {
            bind(s, ((Tuple) target).elts, rvalue, kind);
        }
        else if (target instanceof PyList)
        {
            bind(s, ((PyList) target).elts, rvalue, kind);
        }
        else if (target instanceof Attribute)
        {
            setAttr(((Attribute) target), s, rvalue);
        }
        else if (target instanceof Subscript)
        {
            Subscript sub = (Subscript) target;
            Type sliceType = sub.slice == null ? null : visit(sub.slice, s);
            Type valueType = visit(sub.value, s);
            if (valueType instanceof ListType)
            {
                ListType t = (ListType) valueType;
                t.setElementType(UnionType.union(t.eltType, rvalue));
            }
            else if (valueType instanceof DictType)
            {
                DictType t = (DictType) valueType;
                if (sliceType != null)
                {
                    t.setKeyType(UnionType.union(t.keyType, sliceType));
                }
                t.setValueType(UnionType.union(t.valueType, rvalue));
            }
        }
        else if (target != null)
        {
            addWarningToNode(target, "invalid location for assignment");
        }
    }

    /**
     * Without specifying a kind, bind determines the kind according to the type
     * of the scope.
     */
    public void bind(@NotNull State s, Node target, @NotNull Type rvalue)
    {
        Binding.Kind kind;
        if (s.stateType == State.StateType.FUNCTION)
        {
            kind = VARIABLE;
        }
        else if (s.stateType == State.StateType.CLASS ||
                 s.stateType == State.StateType.INSTANCE)
        {
            kind = ATTRIBUTE;
        }
        else
        {
            kind = SCOPE;
        }
        bind(s, target, rvalue, kind);
    }

    public void bind(@NotNull State s, @NotNull List<Node> xs, @NotNull Type rvalue, Binding.Kind kind)
    {
        if (rvalue instanceof TupleType)
        {
            List<Type> vs = ((TupleType) rvalue).eltTypes;
            if (xs.size() != vs.size())
            {
                reportUnpackMismatch(xs, vs.size());
            }
            else
            {
                for (int i = 0; i < xs.size(); i++)
                {
                    bind(s, xs.get(i), vs.get(i), kind);
                }
            }
        }
        else if (rvalue instanceof ListType)
        {
            bind(s, xs, ((ListType) rvalue).toTupleType(xs.size()), kind);
        }
        else if (rvalue instanceof DictType)
        {
            bind(s, xs, ((DictType) rvalue).toTupleType(xs.size()), kind);
        }
        else if (xs.size() > 0)
        {
            for (Node x : xs)
            {
                bind(s, x, Types.UNKNOWN, kind);
            }
            addWarningToFile(xs.get(0).file,
                             xs.get(0).start,
                             xs.get(xs.size() - 1).end,
                             "unpacking non-iterable: " + rvalue);
        }
    }

    public static void bind(@NotNull State s, @NotNull Name name, @NotNull Type rvalue, Binding.Kind kind)
    {
        if (s.isGlobalName(name.id))
        {
            Set<Binding> bs = s.lookup(name.id);
            if (bs != null)
            {
                for (Binding b : bs)
                {
                    b.addType(rvalue);
                    Analyzer.self.putRef(name, b);
                }
            }
        }
        else
        {
            s.insert(name.id, name, rvalue, kind);
        }
    }

    // iterator
    public void bindIter(@NotNull State s, Node target, @NotNull Node iter, Binding.Kind kind)
    {
        Type iterType = visit(iter, s);

        if (iterType instanceof ListType)
        {
            bind(s, target, ((ListType) iterType).eltType, kind);
        }
        else if (iterType instanceof TupleType)
        {
            bind(s, target, ((TupleType) iterType).toListType().eltType, kind);
        }
        else
        {
            Set<Binding> ents = iterType.table.lookupAttr("__iter__");
            if (ents != null)
            {
                for (Binding ent : ents)
                {
                    if (ent == null || !(ent.type instanceof FunType))
                    {
                        if (!iterType.isUnknownType())
                        {
                            addWarningToNode(iter, "not an iterable type: " + iterType);
                        }
                        bind(s, target, Types.UNKNOWN, kind);
                    }
                    else
                    {
                        bind(s, target, ((FunType) ent.type).getReturnType(), kind);
                    }
                }
            }
            else
            {
                bind(s, target, Types.UNKNOWN, kind);
            }
        }
    }

    private static void reportUnpackMismatch(@NotNull List<Node> xs, int vsize)
    {
        int xsize = xs.size();
        int beg = xs.get(0).start;
        int end = xs.get(xs.size() - 1).end;
        int diff = xsize - vsize;
        String msg;
        if (diff > 0)
        {
            msg = "ValueError: need more than " + vsize + " values to unpack";
        }
        else
        {
            msg = "ValueError: too many values to unpack";
        }
        addWarningToFile(xs.get(0).file, beg, end, msg);
    }

    public static void addWarningToNode(Node node, String msg)
    {
        Analyzer.self.putProblem(node, msg);
    }

    public static void addWarningToFile(String file, int begin, int end, String msg)
    {
        Analyzer.self.putProblem(file, begin, end, msg);
    }

    public void addError(Node node, String msg)
    {
        Analyzer.self.putProblem(node, msg);
    }
}
