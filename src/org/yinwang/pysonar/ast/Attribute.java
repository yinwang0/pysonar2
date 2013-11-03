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

    static final long serialVersionUID = -1120979305017812255L;

    @Nullable
    public Node target;
    @Nullable
    public Name attr;

    public Attribute(Node target, Name attr, int start, int end) {
        super(start, end);
        this.target = target;
        this.attr = attr;
        addChildren(target, attr);
    }

    @Nullable
    public String getAttributeName() {
        return attr.getId();
    }

    /**
     * Sets the attribute node.  Used when constructing the AST.
     * @throws IllegalArgumentException if the param is null
     */
    public void setAttr(@Nullable Name attr) {
        if (attr == null) {
            throw new IllegalArgumentException("param cannot be null");
        }
        this.attr = attr;
    }

    @Nullable
    public Name getAttr() {
        return attr;
    }

    /**
     * Sets the target node.  Used when constructing the AST.
     * @throws IllegalArgumentException if the param is null
     */
    public void setTarget(@Nullable Node target) {
        if (target == null) {
            throw new IllegalArgumentException("param cannot be null");
        }
        this.target = target;
    }

    @Nullable
    public Node getTarget() {
        return target;
    }

    /**
     * Assign some definite value to the attribute.  Used during the name
     * resolution pass.  This method is called when this node is in the lvalue of
     * an assignment, in which case it is called in lieu of {@link #resolve}.<p>
     */
    public void setAttr(Scope s, @NotNull Type v, int tag) throws Exception {
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

    @Override
    public Type resolve(Scope s, int tag) throws Exception {
        if (target == null) {
            Util.msg("target is null!");
        }
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
        if (attr == null) {
            Util.msg("attr is null");
        }
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
            if (targetType == null) {
                Util.msg("targetType is null");
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
