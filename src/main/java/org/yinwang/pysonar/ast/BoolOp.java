package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.pysonar.Indexer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.Type;

import java.util.List;

/**
 * Represents the "and"/"or" operators.
 */
public class BoolOp extends Node {

    public List<Node> values;
    public Name op;

    public BoolOp(Name op, List<Node> values, int start, int end) {
        super(start, end);
        this.op = op;
        this.values = values;
        addChildren(values);
    }

    @NotNull
    @Override
    public Type resolve(Scope s, int tag) {
        if (op.id.equals("and")) {
            Type last = null;
            for (Node e : values) {
                last = resolveExpr(e, s, tag);
            }
            return (last == null ? Indexer.idx.builtins.unknown : last);
        }

        // OR
        return resolveListAsUnion(values, s, tag);
    }

    @NotNull
    @Override
    public String toString() {
        return "<BoolOp:" + op + ":" + values + ">";
    }

    @Override
    public void visit(@NotNull NodeVisitor v) {
        if (v.visit(this)) {
            visitNodeList(values, v);
        }
    }
}
