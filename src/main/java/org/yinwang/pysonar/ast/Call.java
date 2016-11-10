package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Call extends Node {

    public Node func;
    public List<Node> args;
    @Nullable
    public List<Keyword> keywords;
    public Node kwargs;
    public Node starargs;

    public Call(Node func, List<Node> args, @Nullable List<Keyword> keywords,
        Node kwargs, Node starargs, String file, int start, int end, int line, int col) {
        super(NodeType.CALL, file, start, end, line, col);
        this.func = func;
        this.args = args;
        this.keywords = keywords;
        this.kwargs = kwargs;
        this.starargs = starargs;
        addChildren(func, kwargs, starargs);
        addChildren(args);
        addChildren(keywords);
    }

    @NotNull
    @Override
    public String toString() {
        return "(call:" + func + ":" + args + ":" + start + ")";
    }

}
