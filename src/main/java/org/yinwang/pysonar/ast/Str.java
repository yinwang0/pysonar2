package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Analyzer;
import org.yinwang.pysonar.State;
import org.yinwang.pysonar.types.Type;


public class Str extends Node {

    public String value;


    public Str(@NotNull Object value, int start, int end) {
        super(start, end);
        this.value = value.toString();
    }


    @NotNull
    @Override
    public Type transform(State s) {
        return Type.UNKNOWN_STR;
    }


    @NotNull
    @Override
    public String toString() {
        String summary;
        if (value.length() > 10) {
            summary = value.substring(0, 10);
        } else {
            summary = value;
        }
        return "'" + summary + "'";
    }


    @Override
    public void visit(@NotNull NodeVisitor v) {
        v.visit(this);
    }
}
