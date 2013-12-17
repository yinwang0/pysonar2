package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.pysonar.Analyzer;
import org.yinwang.pysonar.Binder;
import org.yinwang.pysonar.Binding;
import org.yinwang.pysonar.State;
import org.yinwang.pysonar.types.ClassType;
import org.yinwang.pysonar.types.FunType;
import org.yinwang.pysonar.types.Type;

import java.util.List;


public class Function extends Node {

    public Name name;
    public List<Node> args;
    public List<Node> defaults;
    public Name vararg;  // *args
    public Name kwarg;   // **kwarg
    public Name blockarg = null;   // block arg of Ruby
    public List<Node> afterRest = null;   // after rest arg of Ruby
    public Node body;
    private List<Node> decoratorList;
    public boolean called = false;
    public boolean isLamba = false;


    public Function(Name name, List<Node> args, Node body, List<Node> defaults,
                    Name vararg, Name kwarg, int start, int end)
    {
        super(start, end);
        if (name != null) {
            this.name = name;
        } else {
            isLamba = true;
            String fn = genLambdaName();
            this.name = new Name(fn, start, start + "lambda".length());
            addChildren(this.name);
        }

        this.args = args;
        this.body = body;
        this.defaults = defaults;
        this.vararg = vararg;
        this.kwarg = kwarg;
        addChildren(name);
        addChildren(args);
        addChildren(defaults);
        addChildren(vararg, kwarg, this.body);
    }


    @NotNull
    @Override
    public Type transform(@NotNull State s) {
        resolveList(decoratorList, s);
        State env = s.getForwarding();
        FunType fun = new FunType(this, env);
        fun.getTable().setParent(s);
        fun.getTable().setPath(s.extendPath(name.id));
        fun.setDefaultTypes(resolveList(defaults, s));
        Analyzer.self.addUncalled(fun);
        Binding.Kind funkind;

        if (isLamba) {
            return fun;
        } else {
            if (s.getStateType() == State.StateType.CLASS) {
                if ("__init__".equals(name.id)) {
                    funkind = Binding.Kind.CONSTRUCTOR;
                } else {
                    funkind = Binding.Kind.METHOD;
                }
            } else {
                funkind = Binding.Kind.FUNCTION;
            }

            Type outType = s.getType();
            if (outType instanceof ClassType) {
                fun.setCls(outType.asClassType());
            }

            Binder.bind(s, name, fun, funkind);
            return Analyzer.self.builtins.Cont;
        }
    }


    private static int lambdaCounter = 0;


    @NotNull
    public static String genLambdaName() {
        lambdaCounter = lambdaCounter + 1;
        return "lambda%" + lambdaCounter;
    }


    @NotNull
    @Override
    public String toString() {
        return "(func:" + start + ":" + name + ")";
    }


    @Override
    public void visit(@NotNull NodeVisitor v) {
        if (v.visit(this)) {
            visitNode(name, v);
            visitNodes(args, v);
            visitNodes(defaults, v);
            visitNode(kwarg, v);
            visitNode(vararg, v);
            visitNode(body, v);
        }
    }


    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Function) {
            Function fo = (Function) obj;
            return (fo.getFile().equals(getFile()) && fo.start == start);
        } else {
            return false;
        }
    }

}
