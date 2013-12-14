package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.State;
import org.yinwang.pysonar.types.ListType;
import org.yinwang.pysonar.types.Type;

import java.util.List;


public class ListComp extends Node {

    public Node elt;
    public List<Comprehension> generators;


    public ListComp(Node elt, List<Comprehension> generators, int start, int end) {
        super(start, end);
        this.elt = elt;
        this.generators = generators;
        addChildren(elt);
        addChildren(generators);
    }


    /**
     * Python's list comprehension will bind the variables used in generators.
     * This will erase the original values of the variables even after the
     * comprehension.
     */
    @NotNull
    @Override
    public Type transform(State s) {
        resolveList(generators, s);
        return new ListType(transformExpr(elt, s));
    }


    @NotNull
    @Override
    public String toString() {
        return "<NListComp:" + start + ":" + elt + ">";
    }


    @Override
    public void visit(@NotNull NodeVisitor v) {
        if (v.visit(this)) {
            visitNode(elt, v);
            visitNodes(generators, v);
        }
    }
}
