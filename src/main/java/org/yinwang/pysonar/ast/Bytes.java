package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.State;
import org.yinwang.pysonar.types.Type;


public class Bytes extends Node {

    private Object value;


    public Bytes(@NotNull Object value, int start, int end) {
        super(start, end);
        this.value = value.toString();
    }


    public Object getStr() {
        return value;
    }


    @NotNull
    @Override
    public Type transform(State s) {
        return Type.STR;
    }


    @NotNull
    @Override
    public String toString() {
        return "<Bytpes: " + value + ">";
    }

}
