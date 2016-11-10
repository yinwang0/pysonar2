package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

public class Break extends Node {

    public Break(String file, int start, int end, int line, int col) {
        super(NodeType.BREAK, file, start, end, line, col);
    }

    @NotNull
    @Override
    public String toString() {
        return "(break)";
    }
}
