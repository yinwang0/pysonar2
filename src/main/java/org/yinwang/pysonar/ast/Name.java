package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Binding;
import org.yinwang.pysonar.Indexer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.Type;

public class Name extends Node {

    @NotNull
    public final String id;  // identifier

    public Name(String id) {
        this(id, -1, -1);
    }

    public Name(@NotNull String id, int start, int end) {
        super(start, end);
        this.id = id;
    }
    
    /**
     * Returns {@code true} if this name is structurally in a call position.
     */
    @Override
    public boolean isCall() {
        // foo(...)
        if (parent != null && parent.isCall() && this == ((Call)parent).func) {
            return true;
        }

        // <expr>.foo(...)
        Node gramps;
        return parent instanceof Attribute
                && this == ((Attribute)parent).attr
                && (gramps = parent.parent) instanceof Call
                && parent == ((Call)gramps).func;
    }
    
    @NotNull
    @Override
    public Type resolve(@NotNull Scope s) {
        Binding b = s.lookup(id);
        if (b != null) {
            Indexer.idx.putRef(this, b);
            Indexer.idx.stats.inc("resolved");
            return b.getType();
        } else if (id.equals("True") || id.equals("False")) {
            return Indexer.idx.builtins.BaseBool;
        } else {
            Indexer.idx.putProblem(this, "unbound variable " + getId());
            Indexer.idx.stats.inc("unresolved");
            Type t = Indexer.idx.builtins.unknown;
            t.getTable().setPath(s.extendPath(getId()));
            return t;
        }
    }

    /**
     * Returns {@code true} if this name node is the {@code attr} child
     * (i.e. the attribute being accessed) of an {@link Attribute} node.
     */
    public boolean isAttribute() {
        return parent instanceof Attribute
                && ((Attribute)parent).getAttr() == this;
    }
    
    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    @Override
    public String toString() {
        return "<Name:" + start + ":" + id +  ">";
    }

    @NotNull
    @Override
    public String toDisplay() {
        return id;
    }

    @Override
    public void visit(@NotNull NodeVisitor v) {
        v.visit(this);
    }
}
