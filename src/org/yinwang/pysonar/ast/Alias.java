package org.yinwang.pysonar.ast;

import org.yinwang.pysonar.Indexer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.Type;

import java.util.List;

/**
 * A name alias.  Used for the components of import and import-from statements.
 */
public class Alias extends Node {

    static final long serialVersionUID = 4127878954298987559L;

    public List<Name> name;
    public Name asname;

    public Alias(List<Name> name, Name asname, int start, int end) {
        super(start, end);
        this.name = name;
        this.asname = asname;
        addChildren(name);
        addChildren(asname);
    }

    /**
     * Resolves and returns the referenced
     * {@link org.yinwang.pysonar.types.ModuleType} in an import or
     * or import-from statement.  NImportFrom statements manually
     * resolve their child NAliases.
     */
    @Override
    public Type resolve(Scope s, int tag) throws Exception {
        return Indexer.idx.builtins.unknown;
    }

    @Override
    public String toString() {
        return "<Alias:" + name + " as " + asname + ">";
    }

    @Override
    public void visit(NodeVisitor v) {
        if (v.visit(this)) {
            for (Name n : name) {
                visitNode(n, v);
            }
            visitNode(asname, v);
        }
    }
}
