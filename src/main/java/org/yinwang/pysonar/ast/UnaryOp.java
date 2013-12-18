package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Analyzer;
import org.yinwang.pysonar.State;
import org.yinwang.pysonar.types.Type;


public class UnaryOp extends Node {

    public Op op;
    public Node operand;


    public UnaryOp(Op op, Node operand, int start, int end) {
        super(start, end);
        this.op = op;
        this.operand = operand;
        addChildren(operand);
    }


    @NotNull
    @Override
    public Type transform(State s) {
        Type valueType = transformExpr(operand, s);

        if (op == Op.Add) {
            if (valueType.isNumType()) {
                return valueType;
            } else {
                Analyzer.self.putProblem(this, "+ can't be applied to type: " + valueType);
                return Type.UNKNOWN_INT;
            }
        }

        if (op == Op.Sub) {
            if (valueType.isIntType()) {
                return valueType.asIntType().negate();
            } else {
                Analyzer.self.putProblem(this, "- can't be applied to type: " + valueType);
                return Type.UNKNOWN_INT;
            }
        }

        if (op == Op.Not) {
            if (valueType.isTrue()) {
                return Analyzer.self.builtins.False;
            }
            if (valueType.isFalse()) {
                return Analyzer.self.builtins.True;
            }
            if (valueType.isUndecidedBool()) {
                return valueType.asBool().swap();
            }
        }

        Analyzer.self.putProblem(this, "operator " + op + " cannot be applied to type: " + valueType);
        return Type.UNKNOWN;

    }


    @NotNull
    @Override
    public String toString() {
        return "(" + op + " " + operand + ")";
    }


    @Override
    public void visit(@NotNull NodeVisitor v) {
        if (v.visit(this)) {
            visitNode(operand, v);
        }
    }
}
