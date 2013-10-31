package org.yinwang.pysonar.ast;

import org.yinwang.pysonar.Indexer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.Type;

import java.util.List;

/**
 * Represents the "and"/"or" operators.
 */
public class BoolOp extends Node {

    static final long serialVersionUID = -5261954056600388069L;

    public List<Node> values;
    public Name op;

    public BoolOp(Name op, List<Node> values, int start, int end) {
        super(start, end);
        this.op = op;
        this.values = values;
        addChildren(values);
    }

    @Override
    public Type resolve(Scope s, int tag) throws Exception {
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

    @Override
    public String toString() {
        return "<BoolOp:" + op + ":" + values + ">";
    }

    @Override
    public void visit(NodeVisitor v) {
        if (v.visit(this)) {
            visitNodeList(values, v);
        }
    }
}
