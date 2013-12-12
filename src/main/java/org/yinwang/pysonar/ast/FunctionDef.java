package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.pysonar.Analyzer;
import org.yinwang.pysonar.Binder;
import org.yinwang.pysonar.Binding;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.FunType;
import org.yinwang.pysonar.types.Type;

import java.util.List;


public class FunctionDef extends Node {

    public Name name;
    public List<Node> args;
    public List<Node> defaults;
    @Nullable
    public List<Type> defaultTypes;
    public Name vararg;  // *args
    public Name kwarg;   // **kwarg
    public Name blockarg = null;   // block arg of Ruby
    public List<Node> afterRest = null;   // after rest arg of Ruby
    public Node body;
    private List<Node> decoratorList;
    public boolean called = false;


    public FunctionDef(Name name, List<Node> args, Block body, List<Node> defaults,
                       Name vararg, Name kwarg, int start, int end) {
        super(start, end);
        this.name = name;
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
    public Type resolve(@NotNull Scope s) {
        resolveList(decoratorList, s);
        FunType fun = new FunType(this, s.getForwarding());
        fun.getTable().setParent(s);
        fun.getTable().setPath(s.extendPath(name.id));
        fun.setDefaultTypes(resolveList(defaults, s));
        Analyzer.self.addUncalled(fun);
        Binding.Kind funkind;

        if (s.getScopeType() == Scope.ScopeType.CLASS) {
            if ("__init__".equals(name.id) ||
                    "initialize".equals(name.id)) {
                funkind = Binding.Kind.CONSTRUCTOR;
            } else {
                funkind = Binding.Kind.METHOD;
            }
        } else {
            funkind = Binding.Kind.FUNCTION;
        }

        Type outType = s.getType();
        if (outType != null && outType.isClassType()) {
            fun.setCls(outType.asClassType());
        }

        Binder.bind(s, name, fun, funkind);
        return Analyzer.self.builtins.Cont;
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
            visitNodeList(args, v);
            visitNodeList(defaults, v);
            visitNode(kwarg, v);
            visitNode(vararg, v);
            visitNode(body, v);
        }
    }


    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FunctionDef) {
            FunctionDef fo = (FunctionDef) obj;
            return (fo.getFile().equals(getFile()) && fo.start == start);
        } else {
            return false;
        }
    }

}
