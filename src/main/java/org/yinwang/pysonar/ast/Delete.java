package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Delete extends Node {

    public List<Node> targets;

    public Delete(List<Node> elts, String file, int start, int end) {
        super(NodeType.DELETE, file, start, end);
        this.targets = elts;
        addChildren(elts);
    }

    @NotNull
    @Override
    public String toString() {
        return "<Delete:" + targets + ">";
    }

}
