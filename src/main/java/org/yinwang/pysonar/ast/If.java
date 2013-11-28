package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Indexer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.Type;
import org.yinwang.pysonar.types.UnionType;


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


    @NotNull
    @Override
    public Type resolve(@NotNull Scope s) {
        Type type1, type2;
        resolveExpr(test, s);
        Scope s1 = s.copy();
        Scope s2 = s.copy();

        if (body != null && !body.isEmpty()) {
            type1 = resolveExpr(body, s1);
        } else {
            type1 = Indexer.idx.builtins.Cont;
        }

        if (orelse != null && !orelse.isEmpty()) {
            type2 = resolveExpr(orelse, s2);
        } else {
            type2 = Indexer.idx.builtins.Cont;
        }

        boolean cont1 = UnionType.contains(type1, Indexer.idx.builtins.Cont);
        boolean cont2 = UnionType.contains(type2, Indexer.idx.builtins.Cont);

        if (cont1 && cont2) {
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
