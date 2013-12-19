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


    public Assign(@NotNull Node target, @NotNull Node value, String file, int start, int end) {
        super(file, start, end);
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
            Type thisType = s.lookupType(Constants.rbSelfName);
            if (thisType == null) {
                Analyzer.self.putProblem(this, "Instance variable assignment not within class");
            } else {
                Binder.bind(thisType.table, target, valueType);
            }
        } else {
            Binder.bind(s, target, valueType);
        }
        return Type.CONT;
    }


    @NotNull
    @Override
    public String toString() {
        return "(" + target + " = " + value + ")";
    }

}
