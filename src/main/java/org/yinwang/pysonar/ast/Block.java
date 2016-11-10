package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Block extends Node {

    @NotNull
    public List<Node> seq;

    public Block(@NotNull List<Node> seq, String file, int start, int end, int line, int col) {
        super(NodeType.BLOCK, file, start, end, line, col);
        this.seq = seq;
        addChildren(seq);
    }

    @NotNull
    @Override
    public String toString() {
        return "(block:" + seq + ")";
    }

}
