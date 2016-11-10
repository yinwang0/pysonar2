package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Try extends Node {

    public List<Handler> handlers;
    public Block body;
    public Block orelse;
    public Block finalbody;

    public Try(List<Handler> handlers, Block body, Block orelse, Block finalbody,
        String file, int start, int end, int line, int col) {
        super(NodeType.TRY, file, start, end, line, col);
        this.handlers = handlers;
        this.body = body;
        this.orelse = orelse;
        this.finalbody = finalbody;
        addChildren(handlers);
        addChildren(body, orelse);
    }

    @NotNull
    @Override
    public String toString() {
        return "<Try:" + handlers + ":" + body + ":" + orelse + ">";
    }

}
