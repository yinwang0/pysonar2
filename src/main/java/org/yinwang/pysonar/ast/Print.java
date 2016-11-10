package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Print extends Node {

    public Node dest;
    public List<Node> values;

    public Print(Node dest, List<Node> elts, String file, int start, int end, int line, int col) {
        super(NodeType.PRINT, file, start, end, line, col);
        this.dest = dest;
        this.values = elts;
        addChildren(dest);
        addChildren(elts);
    }

    @NotNull
    @Override
    public String toString() {
        return "<Print:" + values + ">";
    }

}
