package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Analyzer;
import org.yinwang.pysonar.Binder;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.Type;


public class ExceptHandler extends Node {

    public Node name;
    public Node exceptionType;
    public Block body;


    public ExceptHandler(Node name, Node exceptionType, Block body, int start, int end) {
        super(start, end);
        this.name = name;
        this.exceptionType = exceptionType;
        this.body = body;
        addChildren(name, exceptionType, body);
    }


    @Override
    public boolean bindsName() {
        return true;
    }


    @NotNull
    @Override
    public Type resolve(@NotNull Scope s) {
        Type typeval = Analyzer.self.builtins.unknown;
        if (exceptionType != null) {
            typeval = resolveExpr(exceptionType, s);
        }
        if (name != null) {
            Binder.bind(s, name, typeval);
        }
        if (body != null) {
            return resolveExpr(body, s);
        } else {
            return Analyzer.self.builtins.unknown;
        }
    }


    @NotNull
    @Override
    public String toString() {
        return "<ExceptHandler:" + start + ":" + name + ":" + exceptionType + ">";
    }


    @Override
    public void visit(@NotNull NodeVisitor v) {
        if (v.visit(this)) {
            visitNode(name, v);
            visitNode(exceptionType, v);
            visitNode(body, v);
        }
    }
}
