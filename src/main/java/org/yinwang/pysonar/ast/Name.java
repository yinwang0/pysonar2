package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Binding;
import org.yinwang.pysonar.Indexer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.Type;

import java.util.List;


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
     * We don't always have enough information at this point to know
     * if it's a constructor call or a regular function/method call,
     * so we just determine if it looks like a call or not, and the
     * indexer will convert constructor-calls to NEW in a later pass.
     */
    @Override
    public boolean isCall() {
        // foo(...)
        if (parent != null && parent.isCall() && this == ((Call) parent).func) {
            return true;
        }

        // <expr>.foo(...)
        Node gramps;
        return parent instanceof Attribute
                && this == ((Attribute) parent).attr
                && (gramps = parent.parent) instanceof Call
                && parent == ((Call) gramps).func;
    }


    @NotNull
    @Override
    public Type resolve(@NotNull Scope s) {
        List<Binding> b = s.lookup(id);
        if (b != null) {
            Indexer.idx.putRef(this, b);
            Indexer.idx.stats.inc("resolved");
            return Scope.makeUnion(b);
        } else if (id.equals("True") || id.equals("False")) {
            return Indexer.idx.builtins.BaseBool;
        } else {
            Indexer.idx.putProblem(this, "unbound variable " + id);
            Indexer.idx.stats.inc("unresolved");
            Type t = Indexer.idx.builtins.unknown;
            t.getTable().setPath(s.extendPath(id));
            return t;
        }
    }


    /**
     * Returns {@code true} if this name node is the {@code attr} child
     * (i.e. the attribute being accessed) of an {@link Attribute} node.
     */
    public boolean isAttribute() {
        return parent instanceof Attribute
                && ((Attribute) parent).getAttr() == this;
    }


    @NotNull
    @Override
    public String toString() {
        return "<Name:" + start + ":" + id + ">";
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
