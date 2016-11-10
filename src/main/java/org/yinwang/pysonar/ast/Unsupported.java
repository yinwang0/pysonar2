package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

public class Unsupported extends Node {

    public Unsupported(String file, int start, int end, int line, int col) {
        super(NodeType.UNSUPPORTED, file, start, end, line, col);
    }

    @NotNull
    @Override
    public String toString() {
        return "(unsupported)";
    }
}
