package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

public class Ellipsis extends Node {

    public Ellipsis(String file, int start, int end, int line, int col) {
        super(NodeType.ELLIPSIS, file, start, end, line, col);
    }

    @NotNull
    @Override
    public String toString() {
        return "...";
    }

}
