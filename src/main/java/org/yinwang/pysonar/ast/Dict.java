package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Dict extends Node {

    public List<Node> keys;
    public List<Node> values;

    public Dict(List<Node> keys, List<Node> values, String file, int start, int end, int line, int col) {
        super(NodeType.DICT, file, start, end, line, col);
        this.keys = keys;
        this.values = values;
        addChildren(keys);
        addChildren(values);
    }

    @NotNull
    @Override
    public String toString() {
        return "<Dict>";
    }

}
