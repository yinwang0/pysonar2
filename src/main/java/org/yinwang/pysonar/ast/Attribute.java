package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.pysonar.Binding;
import org.yinwang.pysonar.Indexer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.Type;
import org.yinwang.pysonar.types.UnionType;

import java.util.List;
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
        return attr.id;
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


    public void setAttr(Scope s, @NotNull Type v) {
        Type targetType = resolveExpr(target, s);
        if (targetType.isUnionType()) {
            Set<Type> types = targetType.asUnionType().getTypes();
            for (Type tp : types) {
                setAttrType(tp, v);
            }
        } else {
            setAttrType(targetType, v);
        }
    }


    private void setAttrType(@NotNull Type targetType, @NotNull Type v) {
        if (targetType.isUnknownType()) {
            Indexer.idx.putProblem(this, "Can't set attribute for UnknownType");
            return;
        }
        targetType.getTable().insert(attr.id, attr, v, ATTRIBUTE);
    }


    @NotNull
    @Override
    public Type resolve(Scope s) {
        Type targetType = resolveExpr(target, s);
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
        List<Binding> bs = targetType.getTable().lookupAttr(attr.id);
        if (bs == null) {
            Indexer.idx.putProblem(attr, "attribute not found in type: " + targetType);
            Type t = Indexer.idx.builtins.unknown;
            t.getTable().setPath(targetType.getTable().extendPath(attr.id));
            return t;
        } else {
            for (Binding b : bs) {
                Indexer.idx.putRef(attr, b);
                if (getParent() != null && getParent().isCall() &&
                        b.getType().isFuncType() && targetType.isInstanceType())
                {  // method call
                    b.getType().asFuncType().setSelfType(targetType);
                }
            }

            return Scope.makeUnion(bs);
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
