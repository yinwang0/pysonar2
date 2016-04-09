package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.$;

public class Module extends Node {

    public Block body;

    public Module(Block body, String file, int start, int end) {
        super(NodeType.MODULE, file, start, end);
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
