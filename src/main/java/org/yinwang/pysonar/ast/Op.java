package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.Nullable;
import org.yinwang.pysonar.$;

public enum Op {
    // numeral
    Add("+", "__add__"),
    Sub("-", "__sub__"),
    Mul("*", "__mul__"),
    MatMult("@", "__matmult__"),
    Div("/", "__div__"),
    Mod("%", "__mod__"),
    Pow("**", "__pow__"),
    FloorDiv("//", "__floordiv__"),

    // comparison
    Eq("is"),
    Equal("==", "__eq__"),
    Lt("<", "__lt__"),
    Gt(">", "__gt__"),

    // bit
    BitAnd("&", "__and__"),
    BitOr("|", "__or__"),
    BitXor("^", "__xor__"),
    In("in"),
    LShift("<<", "__lshift__"),
    RShift(">>", "__rshift__"),
    Invert("~", "__invert__"),

    // boolean
    And("and"),
    Or("or"),
    Not("not"),

    // synthetic
    NotEqual("!=", "__neq__"),
    NotEq("is not"),
    LtE("<=", "__lte__"),
    GtE(">=", "__gte__"),
    NotIn("not in"),

    // unsupported new operator
    Unsupported("??");

    private String rep;

    @Nullable
    private String method;

    Op(String rep, @Nullable String method) {
        this.rep = rep;
        this.method = method;
    }

    Op(String rep) {
        this.rep = rep;
        this.method = null;
    }

    public String getRep() {
        return rep;
    }

    @Nullable
    public String getMethod() {
        return method;
    }

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
