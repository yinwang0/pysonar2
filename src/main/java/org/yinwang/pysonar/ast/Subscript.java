package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.pysonar.Analyzer;
import org.yinwang.pysonar.State;
import org.yinwang.pysonar.types.DictType;
import org.yinwang.pysonar.types.Type;
import org.yinwang.pysonar.types.UnionType;


public class Subscript extends Node {

    @NotNull
    public Node value;
    @Nullable
    public Node slice;  // an NIndex or NSlice


    public Subscript(@NotNull Node value, @Nullable Node slice, int start, int end) {
        super(start, end);
        this.value = value;
        this.slice = slice;
        addChildren(value, slice);
    }


    @NotNull
    @Override
    public Type transform(State s) {
        Type vt = transformExpr(value, s);
        Type st = slice == null? null : transformExpr(slice, s);

        if (vt.isUnionType()) {
            Type retType = Type.UNKNOWN;
            for (Type t : vt.asUnionType().getTypes()) {
                retType = UnionType.union(retType, getSubscript(t, st, s));
            }
            return retType;
        } else {
            return getSubscript(vt, st, s);
        }
    }


    @NotNull
    private Type getSubscript(@NotNull Type vt, @Nullable Type st, State s) {
        if (vt.isUnknownType()) {
            return Type.UNKNOWN;
        } else if (vt.isListType()) {
            return getListSubscript(vt, st, s);
        } else if (vt.isTupleType()) {
            return getListSubscript(vt.asTupleType().toListType(), st, s);
        } else if (vt.isDictType()) {
            DictType dt = vt.asDictType();
            if (!dt.keyType.equals(st)) {
                addWarning("Possible KeyError (wrong type for subscript)");
            }
            return vt.asDictType().valueType;
        } else if (vt.isStrType()) {
            if (st != null && (st.isListType() || st.isNumType())) {
                return vt;
            } else {
                addWarning("Possible KeyError (wrong type for subscript)");
                return Type.UNKNOWN;
            }
        } else {
            return Type.UNKNOWN;
        }
    }


    @NotNull
    private Type getListSubscript(@NotNull Type vt, @Nullable Type st, State s) {
        if (vt.isListType()) {
            if (st != null && st.isListType()) {
                return vt;
            } else if (st == null || st.isNumType()) {
                return vt.asListType().getElementType();
            } else {
                Type sliceFunc = vt.getTable().lookupAttrType("__getslice__");
                if (sliceFunc == null) {
                    addError("The type can't be sliced: " + vt);
                    return Type.UNKNOWN;
                } else if (sliceFunc.isFuncType()) {
                    return Call.apply(sliceFunc.asFuncType(), null, null, null, null, this);
                } else {
                    addError("The type's __getslice__ method is not a function: " + sliceFunc);
                    return Type.UNKNOWN;
                }
            }
        } else {
            return Type.UNKNOWN;
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
