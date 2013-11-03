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

    static final long serialVersionUID = 5212954751978100639L;

    public Node func;
    public List<Node> args;
    public List<Keyword> keywords;
    public Node kwargs;
    public Node starargs;


    public Call(Node func, List<Node> args, List<Keyword> keywords,
                Node kwargs, Node starargs, int start, int end) {
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
    @Override
    public Type resolve(Scope s, int tag) {

        Type opType = resolveExpr(func, s, tag);
        List<Type> aTypes = resolveAndConstructList(args, s, tag);
        Map<String, Type> kwTypes = new HashMap<String, Type>();

        for (Keyword kw : keywords) {
            kwTypes.put(kw.getArg(), kw.getValue().resolve(s, 0));
        }

        Type kwargsType = kwargs == null ? null : resolveExpr(kwargs, s, tag);
        Type starargsType = starargs == null ? null : resolveExpr(starargs, s, tag);

        if (opType.isUnionType()) {
            Set<Type> types = opType.asUnionType().getTypes();
            Type retType = Indexer.idx.builtins.unknown;
            for (Type funcType : types) {
                Type t = resolveCall(funcType, aTypes, kwTypes, kwargsType, starargsType, tag);
                retType = UnionType.union(retType, t);
            }
            return retType;
        } else {
            return resolveCall(opType, aTypes, kwTypes, kwargsType, starargsType, tag);
        }
    }


    @Nullable
    private Type resolveCall(@NotNull Type rator, List<Type> aTypes, Map<String, Type> kwTypes, Type kwargsType, Type starargsType, int tag) {
        if (rator.isFuncType()) {
            FunType ft = rator.asFuncType();
            return apply(ft, aTypes, kwTypes, kwargsType, starargsType, this, tag);
        } else if (rator.isClassType()) {
            return new InstanceType(rator, this, aTypes, tag);
        } else {
            addWarning("calling non-function and non-class: " + rator);
            return Indexer.idx.builtins.unknown;
        }
    }


    @Nullable
    public static Type apply(@NotNull FunType func, @Nullable List<Type> aTypes, Map<String, Type> kTypes, Type kwargsType, Type starargsType, @Nullable Node call, int tag) {

        Indexer.idx.removeUncalled(func);

        if (func.func != null && !func.func.called) {
            Indexer.idx.nCalled++;
            func.func.called = true;
        }

        if (func.getFunc() == null) {           // func without definition (possibly builtins)
            return Indexer.idx.builtins.unknown;
        } else if (call != null && Indexer.idx.inStack(call)) {
            func.setSelfType(null);
            return Indexer.idx.builtins.unknown;
        }

        if (call != null) {
            Indexer.idx.pushStack(call);
        }

        List<Type> argTypeList = new ArrayList<Type>();
        if (func.getSelfType() != null) {
            argTypeList.add(func.getSelfType());
        } else if (func.getCls() != null) {
            argTypeList.add(func.getCls().getCanon());
        }


        if (aTypes != null) {
            argTypeList.addAll(aTypes);
        }

        bindMethodAttrs(func, tag);

        Scope funcTable = new Scope(func.getEnv(), Scope.ScopeType.FUNCTION);

        if (func.getTable().getParent() != null) {
            funcTable.setPath(func.getTable().getParent().extendPath(func.func.name.id));
        } else {
            funcTable.setPath(func.func.name.id);
        }

        Type fromType = bindParams(call, funcTable, func.func.args,
                func.func.vararg, func.func.kwarg,
                argTypeList, func.defaultTypes, kTypes, kwargsType, starargsType, tag);

        Type cachedTo = func.getMapping(fromType);
        if (cachedTo != null) {
            func.setSelfType(null);
            return cachedTo;
        } else {
            Type toType = resolveExpr(func.func.body, funcTable, tag);
            if (missingReturn(toType)) {
                Indexer.idx.putProblem(func.func.name, "Function not always return a value");

                if (call != null) {
                    Indexer.idx.putProblem(call, "Call not always return a value");
                }
            }

            func.setMapping(fromType, toType);
            func.setSelfType(null);
            return toType;
        }
    }

    @NotNull
    static private Type bindParams(@Nullable Node call, @NotNull Scope funcTable, @NotNull List<Node> args, Name fvarargs, Name fkwargs,
                                   @Nullable List<Type> aTypes, @Nullable List<Type> dTypes, @Nullable Map<String, Type> kwTypes,
                                   Type kwargsType, @Nullable Type starargsType, int tag) {

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
                    kwTypes.containsKey(args.get(i).asName().getId())) {
                aType = kwTypes.get(args.get(i).asName().getId());
                kwTypes.remove(args.get(i).asName().getId());
            } else if (starargsType != null && starargsType.isTupleType() &&
                    j < starargsType.asTupleType().getElementTypes().size()) {
                aType = starargsType.asTupleType().get(j++);
            } else {
                aType = Indexer.idx.builtins.unknown;
                if (call != null) {
                    Indexer.idx.putProblem(args.get(i), "unable to bind argument:" + args.get(i));
                }
            }
            NameBinder.bind(funcTable, arg, aType, Binding.Kind.PARAMETER, tag);
            fromType.add(aType);
        }

        if (kwTypes != null && !kwTypes.isEmpty()) {
            Type kwValType = UnionType.newUnion(kwTypes.values());
            NameBinder.bind(funcTable, fkwargs, new DictType(Indexer.idx.builtins.BaseStr, kwValType),
                    Binding.Kind.PARAMETER, tag);
        } else {
            NameBinder.bind(funcTable, fkwargs, Indexer.idx.builtins.unknown,
                    Binding.Kind.PARAMETER, tag);
        }

        if (aTypes.size() > args.size()) {
            Type starType = new TupleType(aTypes.subList(args.size(), aTypes.size()));
            NameBinder.bind(funcTable, fvarargs, starType, Binding.Kind.PARAMETER, tag);
        } else {
            NameBinder.bind(funcTable, fvarargs, Indexer.idx.builtins.unknown, Binding.Kind.PARAMETER, tag);
        }

        return fromType;
    }


    static void bindMethodAttrs(@NotNull FunType cl, int tag) {
        if (cl.getTable().getParent() != null) {
            Type cls = cl.getTable().getParent().getType();
            if (cls != null && cls.isClassType()) {
                addReadOnlyAttr(cl, "im_class", cls, CLASS, tag);
                addReadOnlyAttr(cl, "__class__", cls, CLASS, tag);
                addReadOnlyAttr(cl, "im_self", cls, ATTRIBUTE, tag);
                addReadOnlyAttr(cl, "__self__", cls, ATTRIBUTE, tag);
            }
        }
    }

    @Nullable
    static Binding addReadOnlyAttr(@NotNull FunType cl, String name, @NotNull Type type, Binding.Kind kind, int tag) {
        Binding b = cl.getTable().put(name,
                Builtins.newDataModelUrl("the-standard-type-hierarchy"),
                type, kind, tag);
        b.markSynthetic();
        b.markStatic();
        b.markReadOnly();
        return b;
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
