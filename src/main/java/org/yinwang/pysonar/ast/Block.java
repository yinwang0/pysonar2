package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Analyzer;
import org.yinwang.pysonar.Binding;
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
                    scope.addGlobalName(name.id);
                    List<Binding> nb = scope.lookup(name.id);
                    if (nb != null) {
                        Analyzer.self.putRef(name, nb);
                    }
                }
            }
        }

        boolean returned = false;
        Type retType = Analyzer.self.builtins.unknown;

        for (Node n : seq) {
            Type t = resolveExpr(n, scope);
            if (!returned) {
                retType = UnionType.union(retType, t);
                if (!UnionType.contains(t, Analyzer.self.builtins.Cont)) {
                    returned = true;
                    retType = UnionType.remove(retType, Analyzer.self.builtins.Cont);
                }
            } else if (scope.getScopeType() != Scope.ScopeType.GLOBAL &&
                    scope.getScopeType() != Scope.ScopeType.MODULE)
            {
                Analyzer.self.putProblem(n, "unreachable code");
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
            visitNodes(seq, v);
        }
    }
}
