package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.DictType;
import org.yinwang.pysonar.types.Type;

import java.util.List;

public class Dict extends Node {

    public List<Node> keys;
    public List<Node> values;


    public Dict(List<Node> keys, List<Node> values, int start, int end) {
        super(start, end);
        this.keys = keys;
        this.values = values;
        addChildren(keys);
        addChildren(values);
    }

    @NotNull
    @Override
    public Type resolve(Scope s) {
        Type keyType = resolveListAsUnion(keys, s);
        Type valType = resolveListAsUnion(values, s);
        return new DictType(keyType, valType);
    }

    @NotNull
    @Override
    public String toString() {
        return "<Dict>";
    }

    @Override
    public void visit(@NotNull NodeVisitor v) {
        if (v.visit(this)) {
            // XXX:  should visit in alternating order
            visitNodeList(keys, v);
            visitNodeList(values, v);
        }
    }
}
