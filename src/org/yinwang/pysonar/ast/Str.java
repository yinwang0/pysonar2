package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.pysonar.Indexer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.Type;

public class Str extends Node {

    static final long serialVersionUID = -6092297133232624953L;

    private String value;


    public Str(@Nullable Object value, int start, int end) {
        super(start, end);

        if (value == null) {
            this.value = "";
        } else {
            this.value = value.toString();
        }
    }

    public String getStr() {
        return value;
    }

    @Override
    public Type resolve(Scope s, int tag) throws Exception {
        return Indexer.idx.builtins.BaseStr;
    }

    @NotNull
    @Override
    public String toString() {
        return "<Str>";
    }

    @Override
    public void visit(@NotNull NodeVisitor v) {
        v.visit(this);
    }
}
