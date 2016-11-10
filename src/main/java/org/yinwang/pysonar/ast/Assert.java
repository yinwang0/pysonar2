package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

public class Assert extends Node {

    public Node test;
    public Node msg;

    public Assert(Node test, Node msg, String file, int start, int end, int line, int col) {
        super(NodeType.ASSERT, file, start, end, line, col);
        this.test = test;
        this.msg = msg;
        addChildren(test, msg);
    }

    @NotNull
    @Override
    public String toString() {
        return "<Assert:" + test + ":" + msg + ">";
    }

}
