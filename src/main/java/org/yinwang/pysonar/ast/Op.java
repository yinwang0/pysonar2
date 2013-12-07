package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Analyzer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar._;
import org.yinwang.pysonar.types.Type;


public class Op extends Node {

    public String name;


    public Op(@NotNull Object name, int start, int end) {
        super(start, end);
        this.name = name.toString();
    }


    public String getName() {
        return name;
    }


    public boolean isNumberComparisonOp() {
        return name.equals("<") || name.equals(">") || name.equals("<=") || name.equals(">=");
    }


    public static String invert(String name) {
        if (name.equals("<")) {
            return ">=";
        }
        if (name.equals("<=")) {
            return ">";
        }
        if (name.equals(">")) {
            return "<=";
        }
        if (name.equals(">=")) {
            return "<";
        }
        if (name.equals("==")) {
            return "==";
        }
        _.die("invalid operator name for invert: " + name);
        return "";  // unreacheable
    }


    @NotNull
    @Override
    public Type resolve(Scope s) {
        return Analyzer.self.builtins.unknown;   // will never be used
    }


    @NotNull
    @Override
    public String toString() {
        return name;
    }


    @Override
    public void visit(@NotNull NodeVisitor v) {
        v.visit(this);
    }
}
