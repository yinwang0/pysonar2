package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

public class Attribute extends Node {

    @NotNull
    public Node target;
    @NotNull
    public Name attr;

    public Attribute(@NotNull Node target, @NotNull Name attr, String file, int start, int end) {
        super(NodeType.ATTRIBUTE, file, start, end);
        this.target = target;
        this.attr = attr;
        addChildren(target, attr);
    }

    @NotNull
    @Override
    public String toString() {
        return "<Attribute:" + start + ":" + target + "." + attr.id + ">";
    }

}
