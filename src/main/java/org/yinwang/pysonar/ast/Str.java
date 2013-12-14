package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Analyzer;
import org.yinwang.pysonar.State;
import org.yinwang.pysonar.types.Type;


public class Str extends Node {

    private String value;


    public Str(@NotNull Object value, int start, int end) {
        super(start, end);
        this.value = value.toString();
    }


    public String getStr() {
        return value;
    }


    @NotNull
    @Override
    public Type transform(State s) {
        return Analyzer.self.builtins.BaseStr;
    }


    @NotNull
    @Override
    public String toString() {
        return "<Str>";
    }


    @Override
    public void visit(@NotNull NodeVisitor v) {
        v.visit(this);
    }
}
