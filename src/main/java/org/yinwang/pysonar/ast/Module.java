package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Analyzer;
import org.yinwang.pysonar.Binding;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar._;
import org.yinwang.pysonar.types.ModuleType;
import org.yinwang.pysonar.types.Type;


public class Module extends Node {

    public Block body;

    public Module(Block body, int start, int end) {
        super(start, end);
        this.body = body;
        addChildren(this.body);
    }


    @NotNull
    @Override
    public Type resolve(@NotNull Scope s) {
        ModuleType mt = new ModuleType(name, file, Analyzer.self.globaltable);
        s.insert(_.moduleQname(file), this, mt, Binding.Kind.MODULE);
        resolveExpr(body, mt.getTable());
        return mt;
    }


    @NotNull
    @Override
    public String toString() {
        return "<Module:" + file + ">";
    }


    @Override
    public void visit(@NotNull NodeVisitor v) {
        if (v.visit(this)) {
            visitNode(body, v);
        }
    }
}
