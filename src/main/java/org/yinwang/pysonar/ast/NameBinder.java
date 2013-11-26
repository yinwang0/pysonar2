package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.pysonar.Binding;
import org.yinwang.pysonar.Indexer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.ListType;
import org.yinwang.pysonar.types.Type;
import org.yinwang.pysonar.types.UnionType;

import java.util.List;


/**
 * Handles binding names to scopes, including destructuring assignment.
 */
public class NameBinder {

    public static void bind(@NotNull Scope s, Node target, @NotNull Type rvalue, Binding.Kind kind) {
        if (target instanceof Name) {
            bindName(s, (Name) target, rvalue, kind);
        } else if (target instanceof Tuple) {
            bind(s, ((Tuple) target).elts, rvalue, kind);
        } else if (target instanceof NList) {
            bind(s, ((NList) target).elts, rvalue, kind);
        } else if (target instanceof Attribute) {
            ((Attribute) target).setAttr(s, rvalue);
        } else if (target instanceof Subscript) {
            Subscript sub = (Subscript) target;
            Type valueType = Node.resolveExpr(sub.value, s);
            if (sub.slice != null) {
                Node.resolveExpr(sub.slice, s);
            }
            if (valueType instanceof ListType) {
                ListType t = (ListType) valueType;
                t.setElementType(UnionType.union(t.getElementType(), rvalue));
            }
        } else if (target != null) {
            Indexer.idx.putProblem(target, "invalid location for assignment");
        }
    }


    /**
     * Without specifying a kind, bind determines the kind according to the type
     * of the scope.
     */
    public static void bind(@NotNull Scope s, Node target, @NotNull Type rvalue) {
        Binding.Kind kind;
        if (s.getScopeType() == Scope.ScopeType.FUNCTION) {
            kind = Binding.Kind.VARIABLE;
        } else {
            kind = Binding.Kind.SCOPE;
        }
        bind(s, target, rvalue, kind);
    }


    public static void bind(@NotNull Scope s, @NotNull List<Node> xs, @NotNull Type rvalue, Binding.Kind kind) {
        if (rvalue.isTupleType()) {
            List<Type> vs = rvalue.asTupleType().getElementTypes();
            if (xs.size() != vs.size()) {
                reportUnpackMismatch(xs, vs.size());
            } else {
                for (int i = 0; i < xs.size(); i++) {
                    bind(s, xs.get(i), vs.get(i), kind);
                }
            }
        } else if (rvalue.isListType()) {
            bind(s, xs, rvalue.asListType().toTupleType(xs.size()), kind);
        } else if (rvalue.isDictType()) {
            bind(s, xs, rvalue.asDictType().toTupleType(xs.size()), kind);
        } else if (rvalue.isUnknownType()) {
            for (Node x : xs) {
                bind(s, x, Indexer.idx.builtins.unknown, kind);
            }
        } else {
            Indexer.idx.putProblem(xs.get(0).getFile(),
                    xs.get(0).start,
                    xs.get(xs.size() - 1).end,
                    "unpacking non-iterable: " + rvalue);
        }
    }


    @Nullable
    public static void bindName(@NotNull Scope s, @NotNull Name name, @NotNull Type rvalue,
                                Binding.Kind kind)
    {
        if (s.isGlobalName(name.getId())) {
            Binding b = new Binding(name.getId(), name, rvalue, kind);
            s.getGlobalTable().update(name.getId(), b);
            Indexer.idx.putRef(name, b);
        } else {
            s.insert(name.getId(), name, rvalue, kind);
        }
    }


    public static void bindIter(@NotNull Scope s, Node target, @NotNull Node iter, Binding.Kind kind) {
        Type iterType = Node.resolveExpr(iter, s);

        if (iterType.isListType()) {
            bind(s, target, iterType.asListType().getElementType(), kind);
        } else if (iterType.isTupleType()) {
            bind(s, target, iterType.asTupleType().toListType().getElementType(), kind);
        } else {
            List<Binding> ents = iterType.getTable().lookupAttr("__iter__");
            if (ents != null) {
                for (Binding ent : ents) {
                    if (ent.getType().isFuncType()) {
                        bind(s, target, ent.getType().asFuncType().getReturnType(), kind);
                    } else {
                        iter.addWarning("not an iterable type: " + iterType);
                        bind(s, target, Indexer.idx.builtins.unknown, kind);
                    }
                }
            }
        }
    }


    private static void reportUnpackMismatch(@NotNull List<Node> xs, int vsize) {
        int xsize = xs.size();
        int beg = xs.get(0).start;
        int end = xs.get(xs.size() - 1).end;
        int diff = xsize - vsize;
        String msg;
        if (diff > 0) {
            msg = "ValueError: need more than " + vsize + " values to unpack";
        } else {
            msg = "ValueError: too many values to unpack";
        }
        Indexer.idx.putProblem(xs.get(0).getFile(), beg, end, msg);
    }
}
