package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Analyzer;
import org.yinwang.pysonar.State;
import org.yinwang.pysonar.types.Type;


public class Control extends Node {

    public String command;

    public Control(String command, int start, int end) {
        super(start, end);
        this.command = command;
    }


    @NotNull
    @Override
    public String toString() {
        return "(" + command + ")";
    }


    @Override
    public void visit(@NotNull NodeVisitor v) {
        v.visit(this);
    }


    @NotNull
    @Override
    public Type transform(State s) {
        return Analyzer.self.builtins.None;
    }
}
