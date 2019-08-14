package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.$;

public class PyModule extends Node {

    public Block body;

    public PyModule(Block body, String file, int start, int end, int line, int col) {
        super(NodeType.MODULE, file, start, end, line, col);
        this.name = $.moduleName(file);
        this.body = body;
        addChildren(this.body);
    }

    @NotNull
    @Override
    public String toString() {
        return "(module:" + file + ")";
    }

}
