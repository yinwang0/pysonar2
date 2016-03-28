package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Binder;
import org.yinwang.pysonar.State;
import org.yinwang.pysonar.types.Type;

import java.util.List;


public class With extends Node {

    @NotNull
    public List<Withitem> items;
    public Block body;
    public boolean isAsync = false;


    public With(@NotNull List<Withitem> items, Block body, String file, boolean isAsync, int start, int end) {
        super(NodeType.WITH, file, start, end);
        this.items = items;
        this.body = body;
        this.isAsync = isAsync;
        addChildren(items);
        addChildren(body);
    }


    @NotNull
    @Override
    public Type transform(@NotNull State s) {
        for (Withitem item : items) {
            Type val = transformExpr(item.context_expr, s);
            if (item.optional_vars != null) {
                Binder.bind(s, item.optional_vars, val);
            }
        }
        return transformExpr(body, s);
    }


    @NotNull
    @Override
    public String toString() {
        return "<With:" + items + ":" + body + ">";
    }

}
