package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Analyzer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.NumType;
import org.yinwang.pysonar.types.Type;

import java.util.List;


public class Compare extends Node {

    @NotNull
    public Node left;
    public List<Node> ops;
    public List<Node> comparators;


    public Compare(@NotNull Node left, List<Node> ops, List<Node> comparators, int start, int end) {
        super(start, end);
        this.left = left;
        this.ops = ops;
        this.comparators = comparators;
        addChildren(left);
        addChildren(ops);
        addChildren(comparators);
    }


    @NotNull
    @Override
    public Type resolve(Scope s) {
        resolveExpr(left, s);
        resolveList(comparators, s);

        // try to figure out actual result
        if (ops.size() > 0 && ops.get(0) instanceof Op) {
            String opname = ((Op) ops.get(0)).name;
            Node left = this.left;
            Node right = comparators.get(0);
            Type leftType = left.resolve(s);
            Type rightType = right.resolve(s);

            if (leftType.isNumType() && rightType.isNumType()) {
                NumType leftNum = leftType.asNumType();
                NumType rightNum = rightType.asNumType();

                if (!leftNum.isFeasible() || !rightNum.isFeasible()) {
                    return Analyzer.self.builtins.Infeasible;
                }

                if (opname.equals("<") || opname.equals("<=")) {
                    if (leftNum.lt(rightNum)) {
                        return Analyzer.self.builtins.True;
                    } else if (leftNum.gt(rightNum)) {
                        return Analyzer.self.builtins.False;
                    } else {
                        return Analyzer.self.builtins.BaseBool;
                    }
                }

                if (opname.equals(">") || opname.equals(">=")) {
                    if (leftNum.gt(rightNum)) {
                        return Analyzer.self.builtins.True;
                    } else if (leftNum.lt(rightNum)) {
                        return Analyzer.self.builtins.False;
                    } else {
                        return Analyzer.self.builtins.BaseBool;
                    }
                }
            } else {
                Analyzer.self.putProblem(this, "comparing non-numbers: " + leftType + " and " + rightType);
            }
        }

        return Analyzer.self.builtins.BaseBool;
    }


    @NotNull
    @Override
    public String toString() {
        return "<Compare:" + left + ":" + ops + ":" + comparators + ">";
    }


    @Override
    public void visit(@NotNull NodeVisitor v) {
        if (v.visit(this)) {
            visitNode(left, v);
            visitNodeList(ops, v);
            visitNodeList(comparators, v);
        }
    }
}
