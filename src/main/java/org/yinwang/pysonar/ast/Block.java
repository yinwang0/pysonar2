package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Binding;
import org.yinwang.pysonar.Indexer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.Type;
import org.yinwang.pysonar.types.UnionType;

import java.util.List;


public class Block extends Node {

    @NotNull
    public List<Node> seq;


    public Block(@NotNull List<Node> seq, int start, int end) {
        super(start, end);
        this.seq = seq;
        addChildren(seq);
    }


    @NotNull
    @Override
    public Type resolve(@NotNull Scope scope) {
        // find global names and mark them
        for (Node n : seq) {
            if (n.isGlobal()) {
                for (Name name : n.asGlobal().getNames()) {
                    scope.addGlobalName(name.getId());
                    Binding nb = scope.lookup(name.getId());
                    if (nb != null) {
                        Indexer.idx.putRef(name, nb);
                    }
                }
            }
        }

        boolean returned = false;
        Type retType = Indexer.idx.builtins.unknown;

        for (Node n : seq) {
            Type t = resolveExpr(n, scope);
            if (!returned) {
                retType = UnionType.union(retType, t);
                if (!UnionType.contains(t, Indexer.idx.builtins.Cont)) {
                    returned = true;
                    retType = UnionType.remove(retType, Indexer.idx.builtins.Cont);
                }
            } else if (scope.getScopeType() != Scope.ScopeType.GLOBAL &&
                    scope.getScopeType() != Scope.ScopeType.MODULE)
            {
                Indexer.idx.putProblem(n, "unreachable code");
            }
        }

        return retType;
    }


    public boolean isEmpty() {
        return seq.isEmpty();
    }


    @NotNull
    @Override
    public String toString() {
        return "<Block:" + seq + ">";
    }


    @Override
    public void visit(@NotNull NodeVisitor v) {
        if (v.visit(this)) {
            visitNodeList(seq, v);
        }
    }
}
