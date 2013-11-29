package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Analyzer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.Type;
import org.yinwang.pysonar.types.UnionType;


public class BinOp extends Node {

    public Node left;
    public Node right;
    public Node op;


    public BinOp(Node left, Node right, Node op, int start, int end) {
        super(start, end);
        this.left = left;
        this.right = right;
        this.op = op;
        addChildren(left, right);
    }


    @NotNull
    @Override
    public Type resolve(Scope s) {
        Type ltype = null, rtype = null;
        if (left != null) {
            ltype = resolveExpr(left, s);
        }
        if (right != null) {
            rtype = resolveExpr(right, s);
        }

        // If either non-null operand is a string, assume the result is a string.
        if (ltype == Analyzer.self.builtins.BaseStr || rtype == Analyzer.self.builtins.BaseStr) {
            return Analyzer.self.builtins.BaseStr;
        }
        // If either non-null operand is a number, assume the result is a number.
        if (ltype == Analyzer.self.builtins.BaseNum || rtype == Analyzer.self.builtins.BaseNum) {
            return Analyzer.self.builtins.BaseNum;
        }

        if (ltype == null) {
            return (rtype == null ? Analyzer.self.builtins.unknown : rtype);
        }

        if (rtype == null) {
            return (ltype == null ? Analyzer.self.builtins.unknown : ltype);
        }

        return UnionType.union(ltype, rtype);
    }


    @NotNull
    @Override
    public String toString() {
        return "<BinOp:" + left + " " + op + " " + right + ">";
    }


    @Override
    public void visit(@NotNull NodeVisitor v) {
        if (v.visit(this)) {
            visitNode(left, v);
            visitNode(right, v);
        }
    }
}
