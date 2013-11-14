package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.pysonar.Indexer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.ListType;
import org.yinwang.pysonar.types.Type;
import org.yinwang.pysonar.types.UnionType;

public class Subscript extends Node {

    static final long serialVersionUID = -493854491438387425L;

    public Node value;
    public Node slice;  // an NIndex or NSlice


    public Subscript(Node value, Node slice, int start, int end) {
        super(start, end);
        this.value = value;
        this.slice = slice;
        addChildren(value, slice);
    }

    @Override
    public Type resolve(Scope s, int tag) {
        Type vt = resolveExpr(value, s, tag);
        Type st = slice == null? null : resolveExpr(slice, s, tag);

        if (vt.isUnionType()) {
            Type retType = Indexer.idx.builtins.unknown;
            for (Type t: vt.asUnionType().getTypes()) {
                retType = UnionType.union(retType, getSubscript(t, st, s, tag));
            }
            return retType;
        } else {
            return getSubscript(vt, st, s, tag);
        }
    }


    @Nullable
    private Type getSubscript(@NotNull Type vt, @Nullable Type st, Scope s, int tag) {
        if (vt.isUnknownType()) {
            return Indexer.idx.builtins.unknown;
        } else if (vt.isListType()) {
            return getListSubscript(vt, st, s, tag);
        } else if (vt.isTupleType()) {
            return getListSubscript(vt.asTupleType().toListType(), st, s, tag);
        } else if (vt.isDictType()) {
            ListType nl = new ListType(vt.asDictType().valueType);
            return getListSubscript(nl, st, s, tag);
        } else if (vt.isStrType()) {
            if (st == null || st.isListType() || st.isNumType()) {
                return vt;
            } else {
                addWarning("Possible KeyError (wrong type for subscript)");
                return Indexer.idx.builtins.unknown;
            }
        } else {
            return Indexer.idx.builtins.unknown;
        }
    }

    @NotNull
    private Type getListSubscript(@NotNull Type vt, @NotNull Type st, Scope s, int tag) {
        if (vt.isListType()) {
            if (st.isListType()) {
                return vt;
            } else if (st.isNumType()) {
                return vt.asListType().getElementType();
            } else {
                Type sliceFunc = vt.getTable().lookupAttrType("__getslice__");
                if (sliceFunc == null) {
                    addError("The type can't be sliced: " + vt);
                    return Indexer.idx.builtins.unknown;
                } else if (sliceFunc.isFuncType()) {
                    return Call.apply(sliceFunc.asFuncType(), null, null, null, null, this, tag);
                } else {
                    addError("The type's __getslice__ method is not a function: " + sliceFunc);
                    return Indexer.idx.builtins.unknown;
                }
            }
        } else {
            return Indexer.idx.builtins.unknown;
        }
    }

    @NotNull
    @Override
    public String toString() {
        return "<Subscript:" + value + ":" + slice + ">";
    }

    @Override
    public void visit(@NotNull NodeVisitor v) {
        if (v.visit(this)) {
            visitNode(value, v);
            visitNode(slice, v);
        }
    }
}
