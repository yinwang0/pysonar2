package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

public class Str extends Node {

    public String value;

    public Str(@NotNull Object value, String file, int start, int end, int line, int col) {
        super(NodeType.STR, file, start, end, line, col);
        this.value = value.toString();
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

}
