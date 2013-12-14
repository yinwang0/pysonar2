package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Analyzer;
import org.yinwang.pysonar.State;
import org.yinwang.pysonar.types.ListType;
import org.yinwang.pysonar.types.Type;


public class Yield extends Node {

    public Node value;


    public Yield(Node n, int start, int end) {
        super(start, end);
        this.value = n;
        addChildren(n);
    }


    @NotNull
    @Override
    public Type transform(State s) {
        if (value != null) {
            return new ListType(transformExpr(value, s));
        } else {
            return Analyzer.self.builtins.None;
        }
    }


    @NotNull
    @Override
    public String toString() {
        return "<Yield:" + start + ":" + value + ">";
    }


    @Override
    public void visit(@NotNull NodeVisitor v) {
        if (v.visit(this)) {
            visitNode(value, v);
        }
    }
}
