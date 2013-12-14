package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Analyzer;
import org.yinwang.pysonar.State;
import org.yinwang.pysonar.types.Type;
import org.yinwang.pysonar.types.UnionType;


public class If extends Node {

    @NotNull
    public Node test;
    public Node body;
    public Node orelse;


    public If(@NotNull Node test, Node body, Node orelse, int start, int end) {
        super(start, end);
        this.test = test;
        this.body = body;
        this.orelse = orelse;
        addChildren(test, body, orelse);
    }


    @NotNull
    @Override
    public Type transform(@NotNull State s) {
        Type type1, type2;
        State s1 = s.copy();
        State s2 = s.copy();

        Type conditionType = transformExpr(test, s);
        if (conditionType.isUndecidedBool()) {
            s1 = conditionType.asBool().getS1();
            s2 = conditionType.asBool().getS2();
        }

        if (body != null) {
            type1 = transformExpr(body, s1);
        } else {
            type1 = Analyzer.self.builtins.Cont;
        }

        if (orelse != null) {
            type2 = transformExpr(orelse, s2);
        } else {
            type2 = Analyzer.self.builtins.Cont;
        }

        boolean cont1 = UnionType.contains(type1, Analyzer.self.builtins.Cont);
        boolean cont2 = UnionType.contains(type2, Analyzer.self.builtins.Cont);

        // decide which branch affects the downstream state
        if (conditionType.isTrue() && cont1) {
            s.overwrite(s1);
        } else if (conditionType.isFalse() && cont2) {
            s.overwrite(s2);
        } else if (cont1 && cont2) {
            s.overwrite(State.merge(s1, s2));
        } else if (cont1) {
            s.overwrite(s1);
        } else if (cont2) {
            s.overwrite(s2);
        }

        // determine return type
        if (conditionType == Analyzer.self.builtins.True) {
            return type1;
        } else if (conditionType == Analyzer.self.builtins.False) {
            return type2;
        } else {
            return UnionType.union(type1, type2);
        }
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
