package org.yinwang.pysonar.ast;

import org.yinwang.pysonar.Indexer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.Type;
import org.yinwang.pysonar.types.UnionType;

public class BinOp extends Node {

    static final long serialVersionUID = -8961832251782335108L;

    public Node left;
    public Node right;
    public Node op;

    public BinOp(Node target, Node value, Node op) {
        this(target, value, op, 0, 1);
    }

    public BinOp(Node left, Node right, Node op, int start, int end) {
        super(start, end);
        this.left = left;
        this.right = right;
        this.op = op;
        addChildren(left, right);
    }

    @Override
    public Type resolve(Scope s, int tag) throws Exception {
        Type ltype = null, rtype = null;
        if (left != null) {
            ltype = resolveExpr(left, s, tag);
        }
        if (right != null) {
            rtype = resolveExpr(right, s, tag);
        }

        // If either non-null operand is a string, assume the result is a string.
        if (ltype == Indexer.idx.builtins.BaseStr || rtype == Indexer.idx.builtins.BaseStr) {
            return Indexer.idx.builtins.BaseStr;
        }
        // If either non-null operand is a number, assume the result is a number.
        if (ltype == Indexer.idx.builtins.BaseNum || rtype == Indexer.idx.builtins.BaseNum) {
            return Indexer.idx.builtins.BaseNum;
        }

        if (ltype == null) {
            return (rtype == null ? Indexer.idx.builtins.unknown : rtype);
        }

        if (rtype == null) {
            return (ltype == null ? Indexer.idx.builtins.unknown : ltype);
        }

        return UnionType.union(ltype, rtype);
    }

    @Override
    public String toString() {
        return "<BinOp:" + left + " " + op + " " + right + ">";
    }

    @Override
    public void visit(NodeVisitor v) {
        if (v.visit(this)) {
            visitNode(left, v);
            visitNode(right, v);
        }
    }
}
