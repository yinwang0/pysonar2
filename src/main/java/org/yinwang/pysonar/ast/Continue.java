package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

public class Continue extends Node {

    public Continue(String file, int start, int end) {
        super(NodeType.CONTINUE, file, start, end);
    }

    @NotNull
    @Override
    public String toString() {
        return "(continue)";
    }

}
