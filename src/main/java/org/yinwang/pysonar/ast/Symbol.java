package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Analyzer;
import org.yinwang.pysonar.Binding;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.ClassType;
import org.yinwang.pysonar.types.InstanceType;
import org.yinwang.pysonar.types.SymbolType;
import org.yinwang.pysonar.types.Type;

import java.util.List;


public class Symbol extends Node {

    @NotNull
    public final String id;  // identifier


    public Symbol(@NotNull String id, int start, int end) {
        super(start, end);
        this.id = id;
    }


    @NotNull
    @Override
    public Type resolve(@NotNull Scope s) {
        return new SymbolType(id);
    }


    @NotNull
    @Override
    public String toString() {
        return "(sym:" + id + ":" + start + ")";
    }


    @NotNull
    @Override
    public String toDisplay() {
        return id;
    }


    @Override
    public void visit(@NotNull NodeVisitor v) {
        v.visit(this);
    }
}
