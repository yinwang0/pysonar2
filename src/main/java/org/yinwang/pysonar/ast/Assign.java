package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Analyzer;
import org.yinwang.pysonar.Binder;
import org.yinwang.pysonar.Constants;
import org.yinwang.pysonar.State;
import org.yinwang.pysonar.types.Type;


public class Assign extends Node {

    @NotNull
    public Node target;
    @NotNull
    public Node value;


    public Assign(@NotNull Node target, @NotNull Node value, int start, int end) {
        super(start, end);
        this.target = target;
        this.value = value;
        addChildren(target);
        addChildren(value);
    }


    @NotNull
    @Override
    public Type transform(@NotNull State s) {
        Type valueType = transformExpr(value, s);
        if (target.isName() && target.asName().isInstanceVar()) {
            Type thisType = s.lookupType(Constants.thisName);
            if (thisType == null) {
                Analyzer.self.putProblem(this, "Instance variable assignment not within class");
            } else {
                Binder.bind(thisType.table, target, valueType);
            }
        } else {
            Binder.bind(s, target, valueType);
        }
        return Analyzer.self.builtins.Cont;
    }


    @NotNull
    @Override
    public String toString() {
        return "(" + target + " = " + value + ")";
    }


    @Override
    public void visit(@NotNull NodeVisitor v) {
        if (v.visit(this)) {
            visitNode(target, v);
            visitNode(value, v);
        }
    }
}
