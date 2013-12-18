package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
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


    @NotNull
    @Override
    public Type transform(State s) {
        return Type.NONE;
    }
}
