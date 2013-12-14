package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Analyzer;
import org.yinwang.pysonar.Binding;
import org.yinwang.pysonar.State;
import org.yinwang.pysonar.types.Type;

import java.util.List;


public class Name extends Node {

    @NotNull
    public final String id;  // identifier
    public NameType type;


    public Name(String id) {
        this(id, -1, -1);
    }


    public Name(@NotNull String id, int start, int end) {
        super(start, end);
        this.id = id;
        this.type = NameType.LOCAL;
    }


    public Name(@NotNull String id, NameType type, int start, int end) {
        super(start, end);
        this.id = id;
        this.type = type;
    }


    /**
     * Returns {@code true} if this name is structurally in a call position.
     * We don't always have enough information at this point to know
     * if it's a constructor call or a regular function/method call,
     * so we just determine if it looks like a call or not, and the
     * analyzer will convert constructor-calls to NEW in a later pass.
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
    public Type transform(@NotNull State s) {
        List<Binding> b = s.lookup(id);
        if (b != null) {
            Analyzer.self.putRef(this, b);
            Analyzer.self.stats.inc("resolved");
            return State.makeUnion(b);
        } else if (id.equals("True") || id.equals("False")) {
            return Analyzer.self.builtins.BaseBool;
        } else {
            Analyzer.self.putProblem(this, "unbound variable " + id);
            Analyzer.self.stats.inc("unresolved");
            Type t = Analyzer.self.builtins.unknown;
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


    public boolean isInstanceVar() {
        return type == NameType.INSTANCE;
    }


    public boolean isGlobal() {
        return type == NameType.GLOBAL;
    }


    @NotNull
    @Override
    public String toString() {
        return "(" + id + ":" + start + ")";
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
