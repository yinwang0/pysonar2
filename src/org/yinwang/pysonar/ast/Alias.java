package org.yinwang.pysonar.ast;

import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.Type;

/**
 * A name alias.  Used for the components of import and import-from statements.
 */
public class Alias extends Node {

    static final long serialVersionUID = 4127878954298987559L;

    public Node name;
    public Name asname;

    public Alias(Node name, String asname, int start, int end) {
        super(start, end);
        this.name = name;
        this.asname = asname == null? null : new Name(asname);
    }

    /**
     * Resolves and returns the referenced
     * {@link org.yinwang.pysonar.types.ModuleType} in an import or
     * or import-from statement.  NImportFrom statements manually
     * resolve their child NAliases.
     */
    @Override
    public Type resolve(Scope s, int tag) throws Exception {
        Type t = resolveExpr(name, s, tag);

        // "import a.b.c" defines 'a' (the top module) in the scope, whereas
        // "import a.b.c as x" defines 'x', which refers to the bottom module.
        return t;
    }

    @Override
    public String toString() {
        return "<Alias:" + name + " as " + asname + ">";
    }

    @Override
    public void visit(NodeVisitor v) {
        if (v.visit(this)) {
            visitNode(name, v);
            visitNode(asname, v);
        }
    }
}
