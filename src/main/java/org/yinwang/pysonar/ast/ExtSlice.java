package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ExtSlice extends Node {

    public List<Node> dims;

    public ExtSlice(List<Node> dims, String file, int start, int end) {
        super(NodeType.EXTSLICE, file, start, end);
        this.dims = dims;
        addChildren(dims);
    }

    @NotNull
    @Override
    public String toString() {
        return "<ExtSlice:" + dims + ">";
    }

}
