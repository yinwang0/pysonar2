package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

public class For extends Node {

    public Node target;
    public Node iter;
    public Block body;
    public Block orelse;
    public boolean isAsync = false;

    public For(Node target, Node iter, Block body, Block orelse, boolean isAsync,
        String file, int start, int end, int line, int col) {
        super(NodeType.FOR, file, start, end, line, col);
        this.target = target;
        this.iter = iter;
        this.body = body;
        this.orelse = orelse;
        this.isAsync = isAsync;
        addChildren(target, iter, body, orelse);
    }

    @NotNull
    @Override
    public String toString() {
        return "<For:" + target + ":" + iter + ":" + body + ":" + orelse + ">";
    }

}
