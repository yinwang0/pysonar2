package org.yinwang.pysonar.visitor;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.*;
import org.yinwang.pysonar.ast.*;
import org.yinwang.pysonar.types.*;

import java.util.*;

public class TypeInferencer implements Visitor1<Type, State> {

    @NotNull
    @Override
    public Type visit(Alias node, State s) {
        return Type.UNKNOWN;
    }

    @NotNull
    @Override
    public Type visit(Assert node, State s) {
        if (node.test != null) {
            visit(node.test, s);
        }
        if (node.msg != null) {
            visit(node.msg, s);
        }
        return Type.CONT;
    }

    @NotNull
    @Override
    public Type visit(Assign node, State s) {
        Type valueType = visit(node.value, s);
        Binder.bind(s, node.target, valueType);
        return Type.CONT;
    }

    @NotNull
    @Override
    public Type visit(Attribute node, State s) {
        // the form of ::A in ruby
        if (node.target == null) {
            return visit(node.attr, s);
        }

        Type targetType = visit(node.target, s);
        if (targetType instanceof UnionType) {
            Set<Type> types = ((UnionType) targetType).types;
            Type retType = Type.UNKNOWN;
            for (Type tt : types) {
                retType = UnionType.union(retType, node.getAttrType(tt));
            }
            return retType;
        } else {
            return node.getAttrType(targetType);
        }
    }

    @NotNull
    @Override
    public Type visit(Await node, State s) {
        if (node.value == null) {
            return Type.NONE;
        } else {
            return visit(node.value, s);
        }
    }

    @NotNull
    @Override
    public Type visit(BinOp node, State s) {
        Type ltype = visit(node.left, s);
        Type rtype = visit(node.right, s);

        if (Op.isBoolean(node.op)) {
            return Type.BOOL;
        } else {
            return UnionType.union(ltype, rtype);
        }
    }

    @NotNull
    @Override
    public Type visit(Block node, State s) {
        // first pass: mark global names
        for (Node n : node.seq) {
            if (n instanceof Global) {
                for (Name name : ((Global) n).names) {
                    s.addGlobalName(name.id);
                    Set<Binding> nb = s.lookup(name.id);
                    if (nb != null) {
                        Analyzer.self.putRef(name, nb);
                    }
                }
            }
        }

        boolean returned = false;
        Type retType = Type.UNKNOWN;

        for (Node n : node.seq) {
            Type t = visit(n, s);
            if (!returned) {
                retType = UnionType.union(retType, t);
                if (!UnionType.contains(t, Type.CONT)) {
                    returned = true;
                    retType = UnionType.remove(retType, Type.CONT);
                }
            }
        }

        return retType;
    }

    @NotNull
    @Override
    public Type visit(Break node, State s) {
        return Type.NONE;
    }

    @NotNull
    @Override
    public Type visit(Bytes node, State s) {
        return Type.STR;
    }

    @NotNull
    @Override
    public Type visit(Call node, State s) {
        Type fun = visit(node.func, s);
        List<Type> pos = visit(node.args, s);
        Map<String, Type> hash = new HashMap<>();

        if (node.keywords != null) {
            for (Keyword kw : node.keywords) {
                hash.put(kw.arg, visit(kw.value, s));
            }
        }

        Type kw = node.kwargs == null ? null : visit(node.kwargs, s);
        Type star = node.starargs == null ? null : visit(node.starargs, s);

        if (fun instanceof UnionType) {
            Set<Type> types = ((UnionType) fun).types;
            Type retType = Type.UNKNOWN;
            for (Type ft : types) {
                Type t = node.resolveCall(ft, pos, hash, kw, star);
                retType = UnionType.union(retType, t);
            }
            return retType;
        } else {
            return node.resolveCall(fun, pos, hash, kw, star);
        }
    }

    @NotNull
    @Override
    public Type visit(ClassDef node, State s) {
        ClassType classType = new ClassType(node.name.id, s);
        List<Type> baseTypes = new ArrayList<>();
        for (Node base : node.bases) {
            Type baseType = visit(base, s);
            if (baseType instanceof ClassType) {
                classType.addSuper(baseType);
            } else if (baseType instanceof UnionType) {
                for (Type parent : ((UnionType) baseType).types) {
                    classType.addSuper(parent);
                }
            } else {
                Analyzer.self.putProblem(base, base + " is not a class");
            }
            baseTypes.add(baseType);
        }

        // XXX: Not sure if we should add "bases", "name" and "dict" here. They
        // must be added _somewhere_ but I'm just not sure if it should be HERE.
        node.addSpecialAttribute(classType.table, "__bases__", new TupleType(baseTypes));
        node.addSpecialAttribute(classType.table, "__name__", Type.STR);
        node.addSpecialAttribute(classType.table, "__dict__",
                new DictType(Type.STR, Type.UNKNOWN));
        node.addSpecialAttribute(classType.table, "__module__", Type.STR);
        node.addSpecialAttribute(classType.table, "__doc__", Type.STR);

        // Bind ClassType to name here before resolving the body because the
        // methods need node type as self.
        Binder.bind(s, node.name, classType, Binding.Kind.CLASS);
        if (node.body != null) {
            visit(node.body, classType.table);
        }
        return Type.CONT;
    }

    @NotNull
    @Override
    public Type visit(Comprehension node, State s) {
        Binder.bindIter(s, node.target, node.iter, Binding.Kind.SCOPE);
        visit(node.ifs, s);
        return visit(node.target, s);
    }

    @NotNull
    @Override
    public Type visit(Continue node, State s) {
        return Type.CONT;
    }

    @NotNull
    @Override
    public Type visit(Delete node, State s) {
        for (Node n : node.targets) {
            visit(n, s);
            if (n instanceof Name) {
                s.remove(((Name) n).id);
            }
        }
        return Type.CONT;
    }

    @NotNull
    @Override
    public Type visit(Dict node, State s) {
        Type keyType = resolveUnion(node.keys, s);
        Type valType = resolveUnion(node.values, s);
        return new DictType(keyType, valType);
    }

    @NotNull
    @Override
    public Type visit(DictComp node, State s) {
        visit(node.generators, s);
        Type keyType = visit(node.key, s);
        Type valueType = visit(node.value, s);
        return new DictType(keyType, valueType);
    }

    @NotNull
    @Override
    public Type visit(Dummy node, State s) {
        return Type.UNKNOWN;
    }

    @NotNull
    @Override
    public Type visit(Ellipsis node, State s) {
        return Type.NONE;
    }

    @NotNull
    @Override
    public Type visit(Exec node, State s) {
        if (node.body != null) {
            visit(node.body, s);
        }
        if (node.globals != null) {
            visit(node.globals, s);
        }
        if (node.locals != null) {
            visit(node.locals, s);
        }
        return Type.CONT;
    }

    @NotNull
    @Override
    public Type visit(Expr node, State s) {
        if (node.value != null) {
            visit(node.value, s);
        }
        return Type.CONT;

    }

    @NotNull
    @Override
    public Type visit(ExtSlice node, State s) {
        for (Node d : node.dims) {
            visit(d, s);
        }
        return new ListType();
    }

    @NotNull
    @Override
    public Type visit(For node, State s) {
        Binder.bindIter(s, node.target, node.iter, Binding.Kind.SCOPE);

        Type ret;
        if (node.body == null) {
            ret = Type.UNKNOWN;
        } else {
            ret = visit(node.body, s);
        }
        if (node.orelse != null) {
            ret = UnionType.union(ret, visit(node.orelse, s));
        }
        return ret;
    }

    @NotNull
    @Override
    public Type visit(FunctionDef node, State s) {
        State env = s.getForwarding();
        FunType fun = new FunType(node, env);
        fun.table.setParent(s);
        fun.table.setPath(s.extendPath(node.name.id));
        fun.setDefaultTypes(visit(node.defaults, s));
        Analyzer.self.addUncalled(fun);
        Binding.Kind funkind;

        if (node.isLamba) {
            return fun;
        } else {
            if (s.stateType == State.StateType.CLASS) {
                if ("__init__".equals(node.name.id)) {
                    funkind = Binding.Kind.CONSTRUCTOR;
                } else {
                    funkind = Binding.Kind.METHOD;
                }
            } else {
                funkind = Binding.Kind.FUNCTION;
            }

            Type outType = s.type;
            if (outType instanceof ClassType) {
                fun.setCls((ClassType) outType);
            }

            Binder.bind(s, node.name, fun, funkind);
            return Type.CONT;
        }
    }

    @NotNull
    @Override
    public Type visit(GeneratorExp node, State s) {
        visit(node.generators, s);
        return new ListType(visit(node.elt, s));
    }

    @NotNull
    @Override
    public Type visit(Global node, State s) {
        return Type.CONT;
    }

    @NotNull
    @Override
    public Type visit(Handler node, State s) {
        Type typeval = Type.UNKNOWN;
        if (node.exceptions != null) {
            typeval = resolveUnion(node.exceptions, s);
        }
        if (node.binder != null) {
            Binder.bind(s, node.binder, typeval);
        }
        if (node.body != null) {
            return visit(node.body, s);
        } else {
            return Type.UNKNOWN;
        }
    }

    @NotNull
    @Override
    public Type visit(If node, State s) {
        Type type1, type2;
        State s1 = s.copy();
        State s2 = s.copy();

        // ignore condition for now
        visit(node.test, s);

        if (node.body != null) {
            type1 = visit(node.body, s1);
        } else {
            type1 = Type.CONT;
        }

        if (node.orelse != null) {
            type2 = visit(node.orelse, s2);
        } else {
            type2 = Type.CONT;
        }

        boolean cont1 = UnionType.contains(type1, Type.CONT);
        boolean cont2 = UnionType.contains(type2, Type.CONT);

        // decide which branch affects the downstream state
        if (cont1 && cont2) {
            s1.merge(s2);
            s.overwrite(s1);
        } else if (cont1) {
            s.overwrite(s1);
        } else if (cont2) {
            s.overwrite(s2);
        }

        return UnionType.union(type1, type2);
    }

    @NotNull
    @Override
    public Type visit(IfExp node, State s) {
        Type type1, type2;
        visit(node.test, s);

        if (node.body != null) {
            type1 = visit(node.body, s);
        } else {
            type1 = Type.CONT;
        }
        if (node.orelse != null) {
            type2 = visit(node.orelse, s);
        } else {
            type2 = Type.CONT;
        }
        return UnionType.union(type1, type2);
    }

    @NotNull
    @Override
    public Type visit(Import node, State s) {
        for (Alias a : node.names) {
            Type mod = Analyzer.self.loadModule(a.name, s);
            if (mod == null) {
                Analyzer.self.putProblem(node, "Cannot load module");
            } else if (a.asname != null) {
                s.insert(a.asname.id, a.asname, mod, Binding.Kind.VARIABLE);
            }
        }
        return Type.CONT;
    }

    @NotNull
    @Override
    public Type visit(ImportFrom node, State s) {
        if (node.module == null) {
            return Type.CONT;
        }

        Type mod = Analyzer.self.loadModule(node.module, s);

        if (mod == null) {
            Analyzer.self.putProblem(node, "Cannot load module");
        } else if (node.isImportStar()) {
            node.importStar(s, mod);
        } else {
            for (Alias a : node.names) {
                Name first = a.name.get(0);
                Set<Binding> bs = mod.table.lookup(first.id);
                if (bs != null) {
                    if (a.asname != null) {
                        s.update(a.asname.id, bs);
                        Analyzer.self.putRef(a.asname, bs);
                    } else {
                        s.update(first.id, bs);
                        Analyzer.self.putRef(first, bs);
                    }
                } else {
                    List<Name> ext = new ArrayList<>(node.module);
                    ext.add(first);
                    Type mod2 = Analyzer.self.loadModule(ext, s);
                    if (mod2 != null) {
                        if (a.asname != null) {
                            s.insert(a.asname.id, a.asname, mod2, Binding.Kind.VARIABLE);
                        } else {
                            s.insert(first.id, first, mod2, Binding.Kind.VARIABLE);
                        }
                    }
                }
            }
        }

        return Type.CONT;
    }

    @NotNull
    @Override
    public Type visit(Index node, State s) {
        return visit(node.value, s);
    }

    @NotNull
    @Override
    public Type visit(Keyword node, State s) {
        return visit(node.value, s);
    }

    @NotNull
    @Override
    public Type visit(ListComp node, State s) {
        visit(node.generators, s);
        return new ListType(visit(node.elt, s));
    }

    @NotNull
    @Override
    public Type visit(Module node, State s) {
        ModuleType mt = new ModuleType(node.name, node.file, Analyzer.self.globaltable);
        s.insert($.moduleQname(node.file), node, mt, Binding.Kind.MODULE);
        if (node.body != null) {
            visit(node.body, mt.table);
        }
        return mt;
    }

    @NotNull
    @Override
    public Type visit(Name node, State s) {
        Set<Binding> b = s.lookup(node.id);
        if (b != null) {
            Analyzer.self.putRef(node, b);
            Analyzer.self.resolved.add(node);
            Analyzer.self.unresolved.remove(node);
            return State.makeUnion(b);
        } else if (node.id.equals("True") || node.id.equals("False")) {
            return Type.BOOL;
        } else {
            Analyzer.self.putProblem(node, "unbound variable " + node.id);
            Analyzer.self.unresolved.add(node);
            Type t = Type.UNKNOWN;
            t.table.setPath(s.extendPath(node.id));
            return t;
        }
    }

    @NotNull
    @Override
    public Type visit(Pass node, State s) {
        return Type.CONT;
    }

    @NotNull
    @Override
    public Type visit(Print node, State s) {
        if (node.dest != null) {
            visit(node.dest, s);
        }
        if (node.values != null) {
            visit(node.values, s);
        }
        return Type.CONT;
    }

    @NotNull
    @Override
    public Type visit(PyComplex node, State s) {
        return Type.COMPLEX;
    }

    @NotNull
    @Override
    public Type visit(PyFloat node, State s) {
        return Type.FLOAT;
    }

    @NotNull
    @Override
    public Type visit(PyInt node, State s) {
        return Type.INT;
    }

    @NotNull
    @Override
    public Type visit(PyList node, State s) {
        if (node.elts.size() == 0) {
            return new ListType();  // list<unknown>
        }

        ListType listType = new ListType();
        for (Node elt : node.elts) {
            listType.add(visit(elt, s));
            if (elt instanceof Str) {
                listType.addValue(((Str) elt).value);
            }
        }

        return listType;
    }

    @NotNull
    @Override
    public Type visit(PySet node, State s) {
        if (node.elts.size() == 0) {
            return new ListType();
        }

        ListType listType = null;
        for (Node elt : node.elts) {
            if (listType == null) {
                listType = new ListType(visit(elt, s));
            } else {
                listType.add(visit(elt, s));
            }
        }

        return listType;
    }

    @NotNull
    @Override
    public Type visit(Raise node, State s) {
        if (node.exceptionType != null) {
            visit(node.exceptionType, s);
        }
        if (node.inst != null) {
            visit(node.inst, s);
        }
        if (node.traceback != null) {
            visit(node.traceback, s);
        }
        return Type.CONT;
    }

    @NotNull
    @Override
    public Type visit(Repr node, State s) {
        if (node.value != null) {
            visit(node.value, s);
        }
        return Type.STR;
    }

    @NotNull
    @Override
    public Type visit(Return node, State s) {
        if (node.value == null) {
            return Type.NONE;
        } else {
            return visit(node.value, s);
        }
    }

    @NotNull
    @Override
    public Type visit(SetComp node, State s) {
        visit(node.generators, s);
        return new ListType(visit(node.elt, s));
    }

    @NotNull
    @Override
    public Type visit(Slice node, State s) {
        if (node.lower != null) {
            visit(node.lower, s);
        }
        if (node.step != null) {
            visit(node.step, s);
        }
        if (node.upper != null) {
            visit(node.upper, s);
        }
        return new ListType();
    }

    @NotNull
    @Override
    public Type visit(Starred node, State s) {
        return visit(node.value, s);
    }

    @NotNull
    @Override
    public Type visit(Str node, State s) {
        return Type.STR;
    }

    @NotNull
    @Override
    public Type visit(Subscript node, State s) {
        Type vt = visit(node.value, s);
        Type st = node.slice == null ? null : visit(node.slice, s);

        if (vt instanceof UnionType) {
            Type retType = Type.UNKNOWN;
            for (Type t : ((UnionType) vt).types) {
                retType = UnionType.union(retType, node.getSubscript(t, st, s));
            }
            return retType;
        } else {
            return node.getSubscript(vt, st, s);
        }
    }

    @NotNull
    @Override
    public Type visit(Try node, State s) {
        Type tp1 = Type.UNKNOWN;
        Type tp2 = Type.UNKNOWN;
        Type tph = Type.UNKNOWN;
        Type tpFinal = Type.UNKNOWN;

        if (node.handlers != null) {
            for (Handler h : node.handlers) {
                tph = UnionType.union(tph, visit(h, s));
            }
        }

        if (node.body != null) {
            tp1 = visit(node.body, s);
        }

        if (node.orelse != null) {
            tp2 = visit(node.orelse, s);
        }

        if (node.finalbody != null) {
            tpFinal = visit(node.finalbody, s);
        }

        return new UnionType(tp1, tp2, tph, tpFinal);
    }

    @NotNull
    @Override
    public Type visit(Tuple node, State s) {
        TupleType t = new TupleType();
        for (Node e : node.elts) {
            t.add(visit(e, s));
        }
        return t;
    }

    @NotNull
    @Override
    public Type visit(UnaryOp node, State s) {
        return visit(node.operand, s);
    }

    @NotNull
    @Override
    public Type visit(Unsupported node, State s) {
        return Type.NONE;
    }

    @NotNull
    @Override
    public Type visit(Url node, State s) {
        return Type.STR;
    }

    @NotNull
    @Override
    public Type visit(While node, State s) {
        visit(node.test, s);
        Type t = Type.UNKNOWN;

        if (node.body != null) {
            t = visit(node.body, s);
        }

        if (node.orelse != null) {
            t = UnionType.union(t, visit(node.orelse, s));
        }

        return t;
    }

    @NotNull
    @Override
    public Type visit(With node, State s) {
        for (Withitem item : node.items) {
            Type val = visit(item.context_expr, s);
            if (item.optional_vars != null) {
                Binder.bind(s, item.optional_vars, val);
            }
        }
        return visit(node.body, s);
    }

    @NotNull
    @Override
    public Type visit(Withitem node, State s) {
        return Type.UNKNOWN;
    }

    @NotNull
    @Override
    public Type visit(Yield node, State s) {
        if (node.value != null) {
            return new ListType(visit(node.value, s));
        } else {
            return Type.NONE;
        }
    }

    @NotNull
    @Override
    public Type visit(YieldFrom node, State s) {
        if (node.value != null) {
            return new ListType(visit(node.value, s));
        } else {
            return Type.NONE;
        }
    }


    @NotNull
    private Type resolveUnion(@NotNull Collection<? extends Node> nodes, State s) {
        Type result = Type.UNKNOWN;
        for (Node node : nodes) {
            Type nodeType = visit(node, s);
            result = UnionType.union(result, nodeType);
        }
        return result;
    }
}
