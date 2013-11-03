package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Indexer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.Type;

public class Raise extends Node {

    static final long serialVersionUID = 5384576775167988640L;

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

    @Override
    public Type resolve(Scope s, int tag) throws Exception {
        if (exceptionType != null) resolveExpr(exceptionType, s, tag);
        if (inst != null) resolveExpr(inst, s, tag);
        if (traceback != null) resolveExpr(traceback, s, tag);
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
