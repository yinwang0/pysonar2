package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

public class Bytes extends Node {

    public Object value;

    public Bytes(@NotNull Object value, String file, int start, int end) {
        super(NodeType.BYTES, file, start, end);
        this.value = value.toString();
    }

    @NotNull
    @Override
    public String toString() {
        return "(bytes: " + value + ")";
    }

}
