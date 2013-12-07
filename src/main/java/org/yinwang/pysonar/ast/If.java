package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Analyzer;
import org.yinwang.pysonar.Binding;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.NumType;
import org.yinwang.pysonar.types.Type;
import org.yinwang.pysonar.types.UnionType;

import java.util.List;


public class If extends Node {

    @NotNull
    public Node test;
    public Block body;
    public Block orelse;


    public If(@NotNull Node test, Block body, Block orelse, int start, int end) {
        super(start, end);
        this.test = test;
        this.body = body;
        this.orelse = orelse;
        addChildren(test, body, orelse);
    }


    public void restrictNumType(Compare compare, Scope s1, Scope s2) {
        List<Node> ops = compare.ops;

        if (ops.size() > 0 && ops.get(0) instanceof Op) {
            Op op = ((Op) ops.get(0));
            String opname = op.name;
            if (op.isNumberComparisonOp()) {

                Node left = compare.left;
                Node right = compare.comparators.get(0);
                if (!left.isName()) {
                    Node tmp = right;
                    right = left;
                    left = tmp;
                    opname = Op.invert(opname);
                }

                if (left.isName()) {
                    Name leftName = left.asName();
                    Type leftType = left.resolve(s1);
                    Type rightType = right.resolve(s1);
                    NumType trueType = Analyzer.self.builtins.BaseNum;
                    NumType falseType = Analyzer.self.builtins.BaseNum;

                    if (opname.equals("<") || opname.equals("<=")) {
                        if (leftType.isNumType() && rightType.isNumType()) {
                            NumType newUpper = rightType.asNumType();
                            trueType = new NumType(leftType.asNumType());
                            trueType.setUpper(newUpper.getUpper());
                            falseType = new NumType(leftType.asNumType());
                            falseType.setLower(newUpper.getUpper());
                        } else {
                            Analyzer.self.putProblem(test, "comparing non-numbers: " + leftType + " and " + rightType);
                        }
                    } else if (opname.equals(">") || opname.equals(">=")) {
                        if (leftType.isNumType() && rightType.isNumType()) {
                            NumType newLower = rightType.asNumType();
                            trueType = new NumType(leftType.asNumType());
                            trueType.setLower(newLower.getLower());
                            falseType = new NumType(leftType.asNumType());
                            falseType.setUpper(newLower.getLower());
                        } else {
                            Analyzer.self.putProblem(test, "comparing non-numbers: " + leftType + " and " + rightType);
                        }
                    }

                    Node loc;
                    List<Binding> bs = s1.lookup(leftName.id);
                    if (bs != null && bs.size() > 0) {
                        loc = bs.get(0).getNode();
                    } else {
                        loc = leftName;
                    }

                    s1.update(leftName.id, new Binding(leftName.id, loc, trueType, Binding.Kind.SCOPE));
                    s2.update(leftName.id, new Binding(leftName.id, loc, falseType, Binding.Kind.SCOPE));
                }
            }
        }
    }


    @NotNull
    @Override
    public Type resolve(@NotNull Scope s) {
        Type type1, type2;
        resolveExpr(test, s);
        Scope s1 = s.copy();
        Scope s2 = s.copy();
        Type conditionType = Analyzer.self.builtins.unknown;

        if (test instanceof Compare) {
            conditionType = resolveExpr(test, s);
            if (conditionType == Analyzer.self.builtins.BaseBool) {
                restrictNumType((Compare) test, s1, s2);
            }
        }

        if (body != null && !body.isEmpty()) {
            type1 = resolveExpr(body, s1);
        } else {
            type1 = Analyzer.self.builtins.Cont;
        }

        if (orelse != null && !orelse.isEmpty()) {
            type2 = resolveExpr(orelse, s2);
        } else {
            type2 = Analyzer.self.builtins.Cont;
        }

        boolean cont1 = UnionType.contains(type1, Analyzer.self.builtins.Cont);
        boolean cont2 = UnionType.contains(type2, Analyzer.self.builtins.Cont);

        if (conditionType == Analyzer.self.builtins.True && cont1) {
            s.overwrite(s1);
        } else if (conditionType == Analyzer.self.builtins.False && cont2) {
            s.overwrite(s2);
        } else if (cont1 && cont2) {
            s.overwrite(Scope.merge(s1, s2));
        } else if (cont1) {
            s.overwrite(s1);
        } else if (cont2) {
            s.overwrite(s2);
        }

        return UnionType.union(type1, type2);
    }


    @NotNull
    @Override
    public String toString() {
        return "<If:" + start + ":" + test + ":" + body + ":" + orelse + ">";
    }


    @Override
    public void visit(@NotNull NodeVisitor v) {
        if (v.visit(this)) {
            visitNode(test, v);
            visitNode(body, v);
            visitNode(orelse, v);
        }
    }
}
