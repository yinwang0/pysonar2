package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Comprehension extends Node {

    public Node target;
    public Node iter;
    public List<Node> ifs;

    public Comprehension(Node target, Node iter, List<Node> ifs, String file, int start, int end, int line, int col) {
        super(NodeType.COMPREHENSION, file, start, end, line, col);
        this.target = target;
        this.iter = iter;
        this.ifs = ifs;
        addChildren(target, iter);
        addChildren(ifs);
    }

    @NotNull
    @Override
    public String toString() {
        return "<Comprehension:" + start + ":" + target + ":" + iter + ":" + ifs + ">";
    }

}
