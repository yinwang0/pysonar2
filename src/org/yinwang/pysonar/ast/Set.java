package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.ListType;
import org.yinwang.pysonar.types.Type;

import java.util.List;

public class Set extends Sequence {

    static final long serialVersionUID = 6623743056841822992L;


    public Set(List<Node> elts, int start, int end) {
        super(elts, start, end);
    }

    @Nullable
    @Override
    public Type resolve(Scope s, int tag) throws Exception {
        if (elts.size() == 0) {
            return new ListType();
        }

        ListType listType = null;
        for (Node elt : elts) {
            if (listType == null) {
                listType = new ListType(resolveExpr(elt, s, tag));
            } else {
                listType.add(resolveExpr(elt, s, tag));
            }
        }

        return listType;
    }

    @NotNull
    @Override
    public String toString() {
        return "<List:" + start + ":" + elts + ">";
    }

    @Override
    public void visit(@NotNull NodeVisitor v) {
        if (v.visit(this)) {
            visitNodeList(elts, v);
        }
    }
}
