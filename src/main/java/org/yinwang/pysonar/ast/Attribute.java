package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.pysonar.Binding;
import org.yinwang.pysonar.Indexer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.Util;
import org.yinwang.pysonar.types.Type;
import org.yinwang.pysonar.types.UnionType;

import java.util.Set;

import static org.yinwang.pysonar.Binding.Kind.ATTRIBUTE;

public class Attribute extends Node {

    @NotNull
    public Node target;
    @NotNull
    public Name attr;

    public Attribute(@NotNull Node target, @NotNull Name attr, int start, int end) {
        super(start, end);
        this.target = target;
        this.attr = attr;
        addChildren(target, attr);
    }

    @Nullable
    public String getAttributeName() {
        return attr.getId();
    }


    public void setAttr(@NotNull Name attr) {
        this.attr = attr;
    }

    @NotNull
    public Name getAttr() {
        return attr;
    }


    public void setTarget(@NotNull Node target) {
        this.target = target;
    }

    @Nullable
    public Node getTarget() {
        return target;
    }


    public void setAttr(Scope s, @NotNull Type v, int tag) {
        Type targetType = resolveExpr(target, s, tag);
        if (targetType.isUnionType()) {
            Set<Type> types = targetType.asUnionType().getTypes();
            for (Type tp : types) {
                setAttrType(tp, v, tag);
            }
        } else {
            setAttrType(targetType, v, tag);
        }
    }

    private void setAttrType(@NotNull Type targetType, @NotNull Type v, int tag) {
        if (targetType.isUnknownType()) {
            Indexer.idx.putProblem(this, "Can't set attribute for UnknownType");
            return;
        }
        targetType.getTable().putAttr(attr.getId(), attr, v, ATTRIBUTE, tag);
    }

    @NotNull
    @Override
    public Type resolve(Scope s, int tag) {
        Type targetType = resolveExpr(target, s, tag);
        if (targetType.isUnionType()) {
            Set<Type> types = targetType.asUnionType().getTypes();
            Type retType = Indexer.idx.builtins.unknown;
            for (Type tt : types) {
                retType = UnionType.union(retType, getAttrType(tt));
            }
            return retType;
        } else {
            return getAttrType(targetType);
        }
    }

    private Type getAttrType(@NotNull Type targetType) {
        Binding b = targetType.getTable().lookupAttr(attr.getId());
        if (b == null) {
            Indexer.idx.putProblem(attr, "attribute not found in type: " + targetType);
            Type t = Indexer.idx.builtins.unknown;
            t.getTable().setPath(targetType.getTable().extendPath(attr.getId()));
            return t;
        } else {
            Indexer.idx.putLocation(attr, b);

            if (b.getType() == null) {
                Util.msg("b.getType() is null!");
            }

            if (getParent() == null) {
                Util.msg("parent is null!");
            }

            if (getParent() != null && getParent().isCall() &&
                    b.getType().isFuncType() && targetType.isInstanceType()) {  // method call
                b.getType().asFuncType().setSelfType(targetType);
            }

            return b.getType();
        }
    }

    @NotNull
    @Override
    public String toString() {
        return "<Attribute:" + start + ":" + target + "." + getAttributeName() + ">";
    }

    @Override
    public void visit(@NotNull NodeVisitor v) {
        if (v.visit(this)) {
            visitNode(target, v);
            visitNode(attr, v);
        }
    }
}
