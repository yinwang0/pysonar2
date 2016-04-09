package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

public class Exec extends Node {

    public Node body;
    public Node globals;
    public Node locals;

    public Exec(Node body, Node globals, Node locals, String file, int start, int end) {
        super(NodeType.EXEC, file, start, end);
        this.body = body;
        this.globals = globals;
        this.locals = locals;
        addChildren(body, globals, locals);
    }

    @NotNull
    @Override
    public String toString() {
        return "<Exec:" + start + ":" + end + ">";
    }

}
