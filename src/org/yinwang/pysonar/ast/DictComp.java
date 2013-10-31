package org.yinwang.pysonar.ast;

import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.Util;
import org.yinwang.pysonar.types.DictType;
import org.yinwang.pysonar.types.ListType;
import org.yinwang.pysonar.types.Type;

import java.util.List;

public class DictComp extends Node {

    static final long serialVersionUID = -150205687457446323L;

    public Node key;
    public Node value;
    public List<Comprehension> generators;


    public DictComp(Node key, Node value, List<Comprehension> generators, int start, int end) {
        super(start, end);
        this.key = key;
        this.value = value;
        this.generators = generators;
        addChildren(key);
        addChildren(generators);
    }

    /**
     * Python's list comprehension will bind the variables used in generators.
     * This will erase the original values of the variables even after the
     * comprehension.
     */
    @Override
    public Type resolve(Scope s, int tag) throws Exception {
        resolveList(generators, s, tag);
        Type keyType = resolveExpr(key, s, tag);
        Type valueType = resolveExpr(value, s, tag);
        return new DictType(keyType, valueType);
    }

    @Override
    public String toString() {
        return "<DictComp:" + start + ":" + key + ">";
    }

    @Override
    public void visit(NodeVisitor v) {
        if (v.visit(this)) {
            visitNode(key, v);
            visitNodeList(generators, v);
        }
    }
}
