package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

public class Break extends Node {

    public Break(String file, int start, int end) {
        super(NodeType.BREAK, file, start, end);
    }

    @NotNull
    @Override
    public String toString() {
        return "(break)";
    }
}
