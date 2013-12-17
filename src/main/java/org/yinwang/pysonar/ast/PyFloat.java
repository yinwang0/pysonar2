package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.State;
import org.yinwang.pysonar.types.FloatType;
import org.yinwang.pysonar.types.Type;


public class PyFloat extends Node {

    public double value;


    public PyFloat(String s, int start, int end) {
        super(start, end);
        s = s.replaceAll("_", "");
        this.value = Double.parseDouble(s);
    }


    @NotNull
    @Override
    public Type transform(State s) {
        return new FloatType(value);
    }


    @NotNull
    @Override
    public String toString() {
        return "(float:" + value + ")";
    }


    @Override
    public void visit(@NotNull NodeVisitor v) {
        v.visit(this);
    }
}
