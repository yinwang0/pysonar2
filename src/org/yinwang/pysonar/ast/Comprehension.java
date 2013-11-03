package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.pysonar.Binding;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.Type;

import java.util.List;

public class Comprehension extends Node {

    static final long serialVersionUID = -598250664243757218L;

    public Node target;
    public Node iter;
    public List<Node> ifs;


    public Comprehension(Node target, Node iter, List<Node> ifs, int start, int end) {
        super(start, end);
        this.target = target;
        this.iter = iter;
        this.ifs = ifs;
        addChildren(target, iter);
        addChildren(ifs);
    }

    @Override
    public boolean bindsName() {
        return true;
    }

    @Nullable
    @Override
    public Type resolve(@NotNull Scope s, int tag) {
        NameBinder.bindIter(s, target, iter, Binding.Kind.SCOPE, tag);
        resolveList(ifs, s, tag);
        return resolveExpr(target, s, tag);
    }

    @NotNull
    @Override
    public String toString() {
        return "<Comprehension:" + start + ":" + target + ":" + iter + ":" + ifs + ">";
    }

    @Override
    public void visit(@NotNull NodeVisitor v) {
        if (v.visit(this)) {
            visitNode(target, v);
            visitNode(iter, v);
            visitNodeList(ifs, v);
        }
    }
}
