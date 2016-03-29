package org.yinwang.pysonar.ast;

import org.yinwang.pysonar.$;

public enum Op {
    // numeral
    Add,
    Sub,
    Mul,
    MatMult,
    Div,
    Mod,
    Pow,
    FloorDiv,

    // comparison
    Eq,
    Eqv,
    Equal,
    Lt,
    Gt,

    // bit
    BitAnd,
    BitOr,
    BitXor,
    In,
    LShift,
    RShift,
    Invert,

    // boolean
    And,
    Or,
    Not,

    // synthetic
    NotEqual,
    NotEq,
    LtE,
    GtE,
    NotIn,

    // unsupported new operator
    Unsupported;

    public static Op invert(Op op) {
        if (op == Op.Lt) {
            return Op.Gt;
        }

        if (op == Op.Gt) {
            return Op.Lt;
        }

        if (op == Op.Eq) {
            return Op.Eq;
        }

        if (op == Op.And) {
            return Op.Or;
        }

        if (op == Op.Or) {
            return Op.And;
        }

        $.die("invalid operator name for invert: " + op);
        return null;  // unreacheable
    }

    public static boolean isBoolean(Op op) {
        return op == Eq ||
               op == Eqv ||
               op == Equal ||
               op == Lt ||
               op == Gt ||
               op == NotEqual ||
               op == NotEq ||
               op == LtE ||
               op == GtE ||
               op == In ||
               op == NotIn;
    }
}
