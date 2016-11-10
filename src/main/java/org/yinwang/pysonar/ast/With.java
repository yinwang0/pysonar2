package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class With extends Node {

    @NotNull
    public List<Withitem> items;
    public Block body;
    public boolean isAsync = false;

    public With(@NotNull List<Withitem> items, Block body, String file, boolean isAsync, int start, int end, int line, int col) {
        super(NodeType.WITH, file, start, end, line, col);
        this.items = items;
        this.body = body;
        this.isAsync = isAsync;
        addChildren(items);
        addChildren(body);
    }

    @NotNull
    @Override
    public String toString() {
        return "<With:" + items + ":" + body + ">";
    }

}
