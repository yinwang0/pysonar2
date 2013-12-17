package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.pysonar.*;
import org.yinwang.pysonar.types.*;

import java.util.*;
import java.util.Set;

import static org.yinwang.pysonar.Binding.Kind.ATTRIBUTE;
import static org.yinwang.pysonar.Binding.Kind.CLASS;


public class Call extends Node {

    public Node func;
    public List<Node> args;
    @Nullable
    public List<Keyword> keywords;
    public Node kwargs;
    public Node starargs;


    public Call(Node func, List<Node> args, @Nullable List<Keyword> keywords,
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
     * used by Analyzer.applyUncalled. By using a static method we avoid building
     * a NCall node for those dummy calls.
     */
    @NotNull
    @Override
    public Type transform(State s) {

        // Ruby's Class.new
        if (func instanceof Attribute) {
            Attribute afun = (Attribute) func;
            if (afun.attr.id.equals("new")) {
                func = afun.target;
            }
        }

        Type fun = transformExpr(func, s);
        List<Type> pos = resolveList(args, s);
        Map<String, Type> hash = new HashMap<>();

        if (keywords != null) {
            for (Keyword kw : keywords) {
                hash.put(kw.getArg(), transformExpr(kw.getValue(), s));
            }
        }

        Type kw = kwargs == null ? null : transformExpr(kwargs, s);
        Type star = starargs == null ? null : transformExpr(starargs, s);

        if (fun.isUnionType()) {
            Set<Type> types = fun.asUnionType().getTypes();
            Type retType = Analyzer.self.builtins.unknown;
            for (Type ft : types) {
                Type t = resolveCall(ft, pos, hash, kw, star);
                retType = UnionType.union(retType, t);
            }
            return retType;
        } else {
            return resolveCall(fun, pos, hash, kw, star);
        }
    }


    @NotNull
    private Type resolveCall(@NotNull Type fun,
                             List<Type> pos,
                             Map<String, Type> hash,
                             Type kw,
                             Type star)
    {
        if (fun.isFuncType()) {
            FunType ft = fun.asFuncType();
            return apply(ft, pos, hash, kw, star, this);
        } else if (fun.isClassType()) {
            return new InstanceType(fun, this, pos);
        } else {
            addWarning("calling non-function and non-class: " + fun);
            return Analyzer.self.builtins.unknown;
        }
    }


    @NotNull
    public static Type apply(@NotNull FunType func,
                             @Nullable List<Type> pos,
                             Map<String, Type> hash,
                             Type kw,
                             Type star,
                             @Nullable Node call)
    {
        Analyzer.self.removeUncalled(func);

        if (func.func != null && !func.func.called) {
            Analyzer.self.nCalled++;
            func.func.called = true;
        }

        if (func.getFunc() == null) {
            // func without definition (possibly builtins)
            return func.getReturnType();
        } else if (call != null && Analyzer.self.inStack(call)) {
            func.setSelfType(null);
            return Analyzer.self.builtins.unknown;
        }

        if (call != null) {
            Analyzer.self.pushStack(call);
        }

        List<Type> pTypes = new ArrayList<>();

        // Python: bind first parameter to self type
        if (func.getSelfType() != null) {
            pTypes.add(func.getSelfType());
        } else if (func.getCls() != null) {
            pTypes.add(func.getCls().getCanon());
        }

        if (pos != null) {
            pTypes.addAll(pos);
        }

        bindMethodAttrs(func);

        State funcTable = new State(func.getEnv(), State.StateType.FUNCTION);

        if (func.getTable().parent != null) {
            funcTable.setPath(func.getTable().parent.extendPath(func.func.name.id));
        } else {
            funcTable.setPath(func.func.name.id);
        }

        Type fromType = bindParams(call, func.func, funcTable, func.func.args,
                func.func.vararg, func.func.kwarg,
                pTypes, func.defaultTypes, hash, kw, star);

        Type cachedTo = func.getMapping(fromType);
        if (cachedTo != null) {
            func.setSelfType(null);
            return cachedTo;
        } else {
            Type toType = transformExpr(func.func.body, funcTable);
            if (missingReturn(toType)) {
                Analyzer.self.putProblem(func.func.name, "Function not always return a value");

                if (call != null) {
                    Analyzer.self.putProblem(call, "Call not always return a value");
                }
            }

            func.addMapping(fromType, toType);
            func.setSelfType(null);
            return toType;
        }
    }


    @NotNull
    static private Type bindParams(@Nullable Node call,
                                   @NotNull Function func,
                                   @NotNull State funcTable,
                                   @Nullable List<Node> args,
                                   @Nullable Name rest,
                                   @Nullable Name restKw,
                                   @Nullable List<Type> pTypes,
                                   @Nullable List<Type> dTypes,
                                   @Nullable Map<String, Type> hash,
                                   @Nullable Type kw,
                                   @Nullable Type star)
    {
        TupleType fromType = new TupleType();
        int pSize = args == null ? 0 : args.size();
        int aSize = pTypes == null ? 0 : pTypes.size();
        int dSize = dTypes == null ? 0 : dTypes.size();
        int nPos = pSize - dSize;

        if (star != null && star.isListType()) {
            star = star.asListType().toTupleType();
        }

        for (int i = 0, j = 0; i < pSize; i++) {
            Node arg = args.get(i);
            Type aType;
            if (i < aSize) {
                aType = pTypes.get(i);
            } else if (i - nPos >= 0 && i - nPos < dSize) {
                aType = dTypes.get(i - nPos);
            } else {
                if (hash != null && args.get(i).isName() &&
                        hash.containsKey(args.get(i).asName().id))
                {
                    aType = hash.get(args.get(i).asName().id);
                    hash.remove(args.get(i).asName().id);
                } else if (star != null && star.isTupleType() &&
                        j < star.asTupleType().getElementTypes().size())
                {
                    aType = star.asTupleType().get(j++);
                } else {
                    aType = Analyzer.self.builtins.unknown;
                    if (call != null) {
                        Analyzer.self.putProblem(args.get(i),
                                "unable to bind argument:" + args.get(i));
                    }
                }
            }
            Binder.bind(funcTable, arg, aType, Binding.Kind.PARAMETER);
            fromType.add(aType);
        }

        if (restKw != null) {
            if (hash != null && !hash.isEmpty()) {
                Type hashType = UnionType.newUnion(hash.values());
                Binder.bind(
                        funcTable,
                        restKw,
                        new DictType(Analyzer.self.builtins.BaseStr, hashType),
                        Binding.Kind.PARAMETER);
            } else {
                Binder.bind(funcTable,
                        restKw,
                        Analyzer.self.builtins.unknown,
                        Binding.Kind.PARAMETER);
            }
        }

        if (rest != null) {
            if (pTypes.size() > pSize) {
                if (func.afterRest != null) {
                    int nAfter = func.afterRest.size();
                    for (int i = 0; i < nAfter; i++) {
                        Binder.bind(funcTable, func.afterRest.get(i),
                                pTypes.get(pTypes.size() - nAfter + i),
                                Binding.Kind.PARAMETER);
                    }
                    if (pTypes.size() - nAfter > 0) {
                        Type restType = new TupleType(pTypes.subList(pSize, pTypes.size() - nAfter));
                        Binder.bind(funcTable, rest, restType, Binding.Kind.PARAMETER);
                    }
                } else {
                    Type restType = new TupleType(pTypes.subList(pSize, pTypes.size()));
                    Binder.bind(funcTable, rest, restType, Binding.Kind.PARAMETER);
                }
            } else {
                Binder.bind(funcTable,
                        rest,
                        Analyzer.self.builtins.unknown,
                        Binding.Kind.PARAMETER);
            }
        }

        return fromType;
    }


    static void bindMethodAttrs(@NotNull FunType cl) {
        if (cl.getTable().parent != null) {
            Type cls = cl.getTable().parent.getType();
            if (cls != null && cls.isClassType()) {
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
        fun.getTable().update(name, b);
        b.markSynthetic();
        b.markStatic();
        b.markReadOnly();
    }


    static boolean missingReturn(@NotNull Type toType) {
        boolean hasNone = false;
        boolean hasOther = false;

        if (toType.isUnionType()) {
            for (Type t : toType.asUnionType().getTypes()) {
                if (t == Analyzer.self.builtins.None || t == Analyzer.self.builtins.Cont) {
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
        return "(call:" + func + ":" + args + ":" + start + ")";
    }


    @Override
    public void visit(@NotNull NodeVisitor v) {
        if (v.visit(this)) {
            visitNode(func, v);
            visitNodes(args, v);
            visitNodes(keywords, v);
            visitNode(kwargs, v);
            visitNode(starargs, v);
        }
    }
}
