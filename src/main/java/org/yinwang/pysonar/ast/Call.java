package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.pysonar.Binding;
import org.yinwang.pysonar.Builtins;
import org.yinwang.pysonar.Indexer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.*;

import java.util.*;
import java.util.Set;

import static org.yinwang.pysonar.Binding.Kind.ATTRIBUTE;
import static org.yinwang.pysonar.Binding.Kind.CLASS;


public class Call extends Node {

    public Node func;
    public List<Node> args;
    public List<Keyword> keywords;
    public Node kwargs;
    public Node starargs;


    public Call(Node func, List<Node> args, List<Keyword> keywords,
                Node kwargs, Node starargs, int start, int end)
    {
        super(start, end);
        this.func = func;
        this.args = args;
        this.keywords = keywords;
        this.kwargs = kwargs;
        this.starargs = starargs;
        addChildren(func, kwargs, starargs);
        addChildren(args);
        addChildren(keywords);
    }


    /**
     * Most of the work here is done by the static method invoke, which is also
     * used by Indexer.applyUncalled. By using a static method we avoid building
     * a NCall node for those dummy calls.
     */
    @NotNull
    @Override
    public Type resolve(Scope s) {

//// experiment with isinstance
//        if (func.isName() && func.asName().id.equals("isinstance")) {
//            if (args.size() == 2) {
//                if (args.get(0).isName()) {
//                    Type rType = resolveExpr(args.get(1), s);
//                    s.put(args.get(0).asName().id, args.get(0), rType, SCOPE);
//                }
//            }
//        }

        Type opType = resolveExpr(func, s);
        List<Type> aTypes = resolveAndConstructList(args, s);
        Map<String, Type> kwTypes = new HashMap<>();

        for (Keyword kw : keywords) {
            kwTypes.put(kw.getArg(), resolveExpr(kw.getValue(), s));
        }

        Type kwargsType = kwargs == null ? null : resolveExpr(kwargs, s);
        Type starargsType = starargs == null ? null : resolveExpr(starargs, s);

        if (opType.isUnionType()) {
            Set<Type> types = opType.asUnionType().getTypes();
            Type retType = Indexer.idx.builtins.unknown;
            for (Type funcType : types) {
                Type t = resolveCall(funcType, aTypes, kwTypes, kwargsType, starargsType);
                retType = UnionType.union(retType, t);
            }
            return retType;
        } else {
            return resolveCall(opType, aTypes, kwTypes, kwargsType, starargsType);
        }
    }


    @NotNull
    private Type resolveCall(@NotNull Type rator,
                             List<Type> aTypes,
                             Map<String, Type> kwTypes,
                             Type kwargsType,
                             Type starargsType)
    {
        if (rator.isFuncType()) {
            FunType ft = rator.asFuncType();
            return apply(ft, aTypes, kwTypes, kwargsType, starargsType, this);
        } else if (rator.isClassType()) {
            return new InstanceType(rator, this, aTypes);
        } else {
            addWarning("calling non-function and non-class: " + rator);
            return Indexer.idx.builtins.unknown;
        }
    }


    @NotNull
    public static Type apply(@NotNull FunType func,
                             @Nullable List<Type> aTypes,
                             Map<String, Type> kTypes,
                             Type kwargsType,
                             Type starargsType,
                             @Nullable Node call)
    {
        Indexer.idx.removeUncalled(func);

        if (func.func != null && !func.func.called) {
            Indexer.idx.nCalled++;
            func.func.called = true;
        }

        if (func.getFunc() == null) {           // func without definition (possibly builtins)
            return func.getReturnType();
        } else if (call != null && Indexer.idx.inStack(call)) {
            func.setSelfType(null);
            return Indexer.idx.builtins.unknown;
        }

        if (call != null) {
            Indexer.idx.pushStack(call);
        }

        List<Type> argTypeList = new ArrayList<>();
        if (func.getSelfType() != null) {
            argTypeList.add(func.getSelfType());
        } else if (func.getCls() != null) {
            argTypeList.add(func.getCls().getCanon());
        }


        if (aTypes != null) {
            argTypeList.addAll(aTypes);
        }

        bindMethodAttrs(func);

        Scope funcTable = new Scope(func.getEnv(), Scope.ScopeType.FUNCTION);

        if (func.getTable().getParent() != null) {
            funcTable.setPath(func.getTable().getParent().extendPath(func.func.name.id));
        } else {
            funcTable.setPath(func.func.name.id);
        }

        Type fromType = bindParams(call, funcTable, func.func.args,
                func.func.vararg, func.func.kwarg,
                argTypeList, func.defaultTypes, kTypes, kwargsType, starargsType);

        Type cachedTo = func.getMapping(fromType);
        if (cachedTo != null) {
            func.setSelfType(null);
            return cachedTo;
        } else {
            Type toType = resolveExpr(func.func.body, funcTable);
            if (missingReturn(toType)) {
                Indexer.idx.putProblem(func.func.name, "Function not always return a value");

                if (call != null) {
                    Indexer.idx.putProblem(call, "Call not always return a value");
                }
            }

            func.addMapping(fromType, toType);
            func.setSelfType(null);
            return toType;
        }
    }


    @NotNull
    static private Type bindParams(@Nullable Node call, @NotNull Scope funcTable, @NotNull List<Node> args, Name fvarargs, Name fkwargs,
                                   @Nullable List<Type> aTypes, @Nullable List<Type> dTypes, @Nullable Map<String, Type> kwTypes,
                                   Type kwargsType, @Nullable Type starargsType)
    {

        TupleType fromType = new TupleType();
        int aSize = aTypes == null ? 0 : aTypes.size();
        int dSize = dTypes == null ? 0 : dTypes.size();
        int nPositional = args.size() - dSize;

        if (starargsType != null && starargsType.isListType()) {
            starargsType = starargsType.asListType().toTupleType();
        }

        for (int i = 0, j = 0; i < args.size(); i++) {
            Node arg = args.get(i);
            Type aType;
            if (i < aSize) {
                aType = aTypes.get(i);
            } else if (i - nPositional >= 0 && i - nPositional < dSize) {
                aType = dTypes.get(i - nPositional);
            } else if (kwTypes != null && args.get(i).isName() &&
                    kwTypes.containsKey(args.get(i).asName().getId()))
            {
                aType = kwTypes.get(args.get(i).asName().getId());
                kwTypes.remove(args.get(i).asName().getId());
            } else if (starargsType != null && starargsType.isTupleType() &&
                    j < starargsType.asTupleType().getElementTypes().size())
            {
                aType = starargsType.asTupleType().get(j++);
            } else {
                aType = Indexer.idx.builtins.unknown;
                if (call != null) {
                    Indexer.idx.putProblem(args.get(i), "unable to bind argument:" + args.get(i));
                }
            }
            NameBinder.bind(funcTable, arg, aType, Binding.Kind.PARAMETER);
            fromType.add(aType);
        }

        if (kwTypes != null && !kwTypes.isEmpty()) {
            Type kwValType = UnionType.newUnion(kwTypes.values());
            NameBinder.bind(funcTable, fkwargs, new DictType(Indexer.idx.builtins.BaseStr, kwValType),
                    Binding.Kind.PARAMETER);
        } else {
            NameBinder.bind(funcTable, fkwargs, Indexer.idx.builtins.unknown,
                    Binding.Kind.PARAMETER);
        }

        if (aTypes.size() > args.size()) {
            Type starType = new TupleType(aTypes.subList(args.size(), aTypes.size()));
            NameBinder.bind(funcTable, fvarargs, starType, Binding.Kind.PARAMETER);
        } else {
            NameBinder.bind(funcTable, fvarargs, Indexer.idx.builtins.unknown, Binding.Kind.PARAMETER);
        }

        return fromType;
    }


    static void bindMethodAttrs(@NotNull FunType cl) {
        if (cl.getTable().getParent() != null) {
            Type cls = cl.getTable().getParent().getType();
            if (cls != null && cls.isClassType()) {
                addReadOnlyAttr(cl, "im_class", cls, CLASS);
                addReadOnlyAttr(cl, "__class__", cls, CLASS);
                addReadOnlyAttr(cl, "im_self", cls, ATTRIBUTE);
                addReadOnlyAttr(cl, "__self__", cls, ATTRIBUTE);
            }
        }
    }


    @Nullable
    static void addReadOnlyAttr(@NotNull FunType cl, String name, @NotNull Type type, Binding.Kind kind) {
        Binding b = new Binding(name, Builtins.newDataModelUrl("the-standard-type-hierarchy"), type, kind);
        cl.getTable().update(name, b);
        b.markSynthetic();
        b.markStatic();
        b.markReadOnly();
    }


    static boolean missingReturn(@NotNull Type toType) {
        boolean hasNone = false;
        boolean hasOther = false;

        if (toType.isUnionType()) {
            for (Type t : toType.asUnionType().getTypes()) {
                if (t == Indexer.idx.builtins.None || t == Indexer.idx.builtins.Cont) {
                    hasNone = true;
                } else {
                    hasOther = true;
                }
            }
        }

        return hasNone && hasOther;
    }


    @NotNull
    @Override
    public String toString() {
        return "<Call:" + func + ":" + args + ":" + start + ">";
    }


    @Override
    public void visit(@NotNull NodeVisitor v) {
        if (v.visit(this)) {
            visitNode(func, v);
            visitNodeList(args, v);
            visitNodeList(keywords, v);
            visitNode(kwargs, v);
            visitNode(starargs, v);
        }
    }
}
