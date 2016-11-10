package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Subscript extends Node {

    @NotNull
    public Node value;
    @Nullable
    public Node slice;  // an NIndex or NSlice

    public Subscript(@NotNull Node value, @Nullable Node slice, String file, int start, int end, int line, int col) {
        super(NodeType.SUBSCRIPT, file, start, end, line, col);
        this.value = value;
        this.slice = slice;
        addChildren(value, slice);
    }

    @NotNull
    @Override
    public String toString() {
        return "<Subscript:" + value + ":" + slice + ">";
    }

}
