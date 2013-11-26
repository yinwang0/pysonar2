package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Indexer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.Type;


public class Raise extends Node {

    public Node exceptionType;
    public Node inst;
    public Node traceback;


    public Raise(Node exceptionType, Node inst, Node traceback, int start, int end) {
        super(start, end);
        this.exceptionType = exceptionType;
        this.inst = inst;
        this.traceback = traceback;
        addChildren(exceptionType, inst, traceback);
    }


    @NotNull
    @Override
    public Type resolve(Scope s) {
        if (exceptionType != null) {
            resolveExpr(exceptionType, s);
        }
        if (inst != null) {
            resolveExpr(inst, s);
        }
        if (traceback != null) {
            resolveExpr(traceback, s);
        }
        return Indexer.idx.builtins.Cont;
    }


    @NotNull
    @Override
    public String toString() {
        return "<Raise:" + traceback + ":" + exceptionType + ">";
    }


    @Override
    public void visit(@NotNull NodeVisitor v) {
        if (v.visit(this)) {
            visitNode(exceptionType, v);
            visitNode(inst, v);
            visitNode(traceback, v);
        }
    }
}
