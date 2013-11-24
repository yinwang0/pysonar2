package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Indexer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.Type;

import java.util.List;

/**
 * A name alias.  Used for the components of import and import-from statements.
 */
public class Alias extends Node {

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
    @NotNull
    @Override
    public Type resolve(Scope s) {
        return Indexer.idx.builtins.unknown;
    }

    @NotNull
    @Override
    public String toString() {
        return "<Alias:" + name + " as " + asname + ">";
    }

    @Override
    public void visit(@NotNull NodeVisitor v) {
        if (v.visit(this)) {
            for (Name n : name) {
                visitNode(n, v);
            }
            visitNode(asname, v);
        }
    }
}
