package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Analyzer;
import org.yinwang.pysonar.Binding;
import org.yinwang.pysonar.State;
import org.yinwang.pysonar.types.BoolType;
import org.yinwang.pysonar.types.IntType;
import org.yinwang.pysonar.types.Type;


public class BinOp extends Node {

    @NotNull
    public Node left;
    @NotNull
    public Node right;
    @NotNull
    public Op op;


    public BinOp(@NotNull Op op, @NotNull Node left, @NotNull Node right, int start, int end) {
        super(start, end);
        this.left = left;
        this.right = right;
        this.op = op;
        addChildren(left, right);
    }


    @NotNull
    @Override
    public Type transform(State s) {

        Type ltype = transformExpr(left, s);
        Type rtype;

        // boolean operations
        if (op == Op.And) {
            if (ltype.isUndecidedBool()) {
                rtype = transformExpr(right, ltype.asBool().getS1());
            } else {
                rtype = transformExpr(right, s);
            }

            if (ltype.isTrue() && rtype.isTrue()) {
                return Type.TRUE;
            } else if (ltype.isFalse() || rtype.isFalse()) {
                return Type.FALSE;
            } else if (ltype.isUndecidedBool() && rtype.isUndecidedBool()) {
                State falseState = State.merge(ltype.asBool().getS2(), rtype.asBool().getS2());
                return new BoolType(rtype.asBool().getS1(), falseState);
            } else {
                return Type.UNKNOWN_BOOL;
            }
        }

        if (op == Op.Or) {
            if (ltype.isUndecidedBool()) {
                rtype = transformExpr(right, ltype.asBool().getS2());
            } else {
                rtype = transformExpr(right, s);
            }

            if (ltype.isTrue() || rtype.isTrue()) {
                return Type.TRUE;
            } else if (ltype.isFalse() && rtype.isFalse()) {
                return Type.FALSE;
            } else if (ltype.isUndecidedBool() && rtype.isUndecidedBool()) {
                State trueState = State.merge(ltype.asBool().getS1(), rtype.asBool().getS1());
                return new BoolType(trueState, rtype.asBool().getS2());
            } else {
                return Type.UNKNOWN_BOOL;
            }
        }

        rtype = transformExpr(right, s);

        if (ltype.isUnknownType() || rtype.isUnknownType()) {
            return Type.UNKNOWN;
        }

        // Don't do specific things about string types at the moment
        if (ltype == Type.UNKNOWN_STR && rtype == Type.UNKNOWN_STR) {
            return Type.UNKNOWN_STR;
        }

        // try to figure out actual result
        if (ltype.isIntType() && rtype.isIntType()) {
            IntType leftNum = ltype.asIntType();
            IntType rightNum = rtype.asIntType();

            if (op == Op.Add) {
                return IntType.add(leftNum, rightNum);
            }

            if (op == Op.Sub) {
                return IntType.sub(leftNum, rightNum);
            }

            if (op == Op.Mul) {
                return IntType.mul(leftNum, rightNum);
            }

            if (op == Op.Div) {
                return IntType.div(leftNum, rightNum);
            }

            // comparison
            if (op == Op.Lt || op == Op.Gt) {
                Node leftNode = left;
                IntType trueType, falseType;
                Op op1 = op;

                if (!left.isName()) {
                    leftNode = right;

                    IntType tmpNum = rightNum;
                    rightNum = leftNum;
                    leftNum = tmpNum;

                    op1 = Op.invert(op1);
                }

                if (op1 == Op.Lt) {
                    if (leftNum.lt(rightNum)) {
                        return Type.TRUE;
                    } else if (leftNum.gt(rightNum)) {
                        return Type.FALSE;
                    } else {
                        // transfer bound information
                        State s1 = s.copy();
                        State s2 = s.copy();

                        if (leftNode.isName()) {
                            // true branch: if l < r, then l's upper bound is r's upper bound
                            trueType = new IntType(leftNum);
                            trueType.setUpper(rightNum.getUpper());

                            // false branch: if l > r, then l's lower bound is r's lower bound
                            falseType = new IntType(leftNum);
                            falseType.setLower(rightNum.getLower());
                            String id = leftNode.asName().id;

                            for (Binding b : s.lookup(id)) {
                                Node loc = b.getNode();
                                s1.update(id, new Binding(id, loc, trueType, b.getKind()));
                                s2.update(id, new Binding(id, loc, falseType, b.getKind()));
                            }
                        }
                        return new BoolType(s1, s2);
                    }
                }

                if (op1 == Op.Gt) {
                    if (leftNum.gt(rightNum)) {
                        return Type.TRUE;
                    } else if (leftNum.lt(rightNum)) {
                        return Type.FALSE;
                    } else {
                        // undecided, need to transfer bound information
                        State s1 = s.copy();
                        State s2 = s.copy();

                        if (leftNode.isName()) {
                            // true branch: if l > r, then l's lower bound is r's lower bound
                            trueType = new IntType(leftNum);
                            trueType.setLower(rightNum.getLower());

                            // false branch: if l < r, then l's upper bound is r's upper bound
                            falseType = new IntType(leftNum);
                            falseType.setUpper(rightNum.getUpper());
                            String id = leftNode.asName().id;

                            for (Binding b : s.lookup(id)) {
                                Node loc = b.getNode();
                                s1.update(id, new Binding(id, loc, trueType, b.getKind()));
                                s2.update(id, new Binding(id, loc, falseType, b.getKind()));
                            }
                        }
                        return new BoolType(s1, s2);
                    }
                }
            }
        }


        Analyzer.self.putProblem(this, "operator " + op + " cannot be applied on operands " + ltype + " and " + rtype);
        return ltype;
    }


    @NotNull
    @Override
    public String toString() {
        return "(" + left + " " + op + " " + right + ")";
    }


    @Override
    public void visit(@NotNull NodeVisitor v) {
        if (v.visit(this)) {
            visitNode(left, v);
            visitNode(right, v);
        }
    }
}
