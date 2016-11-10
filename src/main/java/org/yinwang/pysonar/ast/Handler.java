package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Handler extends Node {

    public List<Node> exceptions;
    public Node binder;
    public Block body;

    public Handler(List<Node> exceptions, Node binder, Block body, String file, int start, int end, int line, int col) {
        super(NodeType.HANDLER, file, start, end, line, col);
        this.binder = binder;
        this.exceptions = exceptions;
        this.body = body;
        addChildren(binder, body);
        addChildren(exceptions);
    }

    @NotNull
    @Override
    public String toString() {
        return "(handler:" + start + ":" + exceptions + ":" + binder + ")";
    }

}
