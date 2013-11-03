package org.yinwang.pysonar.ast;

import org.yinwang.pysonar.Binding;
import org.yinwang.pysonar.Indexer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.Util;
import org.yinwang.pysonar.types.ListType;
import org.yinwang.pysonar.types.ModuleType;
import org.yinwang.pysonar.types.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

/**
 * Handles import from statements such as {@code from moduleA import a, b as c, d}
 * and {@code from foo.bar.moduleB import *}. <p>
 *
 * The indexer's implementation of import * uses different semantics from
 * all the other forms of import.  It's basically a bug, although the jury
 * is still out as to which implementation is better. <p>
 *
 * For the others we define name bindings anywhere an actual name is
 * introduced into the scope containing the import statement, and references
 * to the imported module or name everywhere else.  This mimics the behavior
 * of Python at runtime, but it may be confusing to anyone with only a casual
 * understanding of Python's data model, who might think it works more like
 * Java. <p>
 *
 * For import * we just enter the imported names into the symbol table,
 * which lets other code reference them, but the references "pass through"
 * automatically to the module from which the names were imported. <p>
 *
 * To illustate the difference, consider the following four modules:
 * <pre>
 *  moduleA.py:
 *     a = 1
 *     b = 2
 *
 *  moduleB.py:
 *     c = 3
 *     d = 4
 *
 *  moduleC.py:
 *     from moduleA import a, b
 *     from moduleB import *
 *     print a  # indexer finds definition of 'a' 2 lines up
 *     print b  # indexer finds definition of 'b' 3 lines up
 *     print c  # indexer finds definition of 'c' in moduleB
 *     print d  # indexer finds definition of 'd' in moduleB
 *
 *  moduleD.py:
 *     import moduleC
 *     print moduleC.a  # indexer finds definition of 'a' in moduleC
 *     print moduleC.b  # indexer finds definition of 'b' in moduleC
 *     print moduleC.c  # indexer finds definition of 'c' in moduleB
 *     print moduleC.c  # indexer finds definition of 'd' in moduleB
 * </pre>
 * To make import * work like the others, we need only create bindings
 * for the imported names.  But where would the bindings be located?
 * Assuming that we were to co-locate them all at the "*" name node,
 * clicking on a reference to any of the names would jump to the "*".
 * It's not clear that this is a better user experience. <p>
 *
 * We could make the other import statement forms work like {@code import *},
 * but that path is even more fraught with confusing inconsistencies.
 */
public class ImportFrom extends Node {

    static final long serialVersionUID = 5070549408963950138L;

    public List<Name> module;
    public List<Alias> names;
    public int level;


    public ImportFrom(List<Name> module, List<Alias> names, int level, int start, int end) {
        super(start, end);
        this.module = module;
        this.level = level;
        this.names = names;
        addChildren(names);
    }

    @Override
    public boolean bindsName() {
        return true;
    }

    @Override
    public Type resolve(Scope s, int tag) throws Exception {
        if (module == null) {
            return Indexer.idx.builtins.Cont;
        }

        ModuleType mod = Indexer.idx.loadModule(module, s, tag);

        if (mod == null) {
            Indexer.idx.putProblem(this, "Can't load module");
        } else if (isImportStar()) {
            importStar(s, mod, tag);
        } else {
            for (Alias a : names) {
                Type t = mod.table.lookupType(a.name.get(0).id);
                Binding b = mod.table.lookup(a.name.get(0).id);

                if (a.asname != null) {
                    s.put(a.asname.id, a.asname, t, Binding.Kind.MODULE, tag);
                } else {
                    s.put(a.name.get(0).id, b);
                }
            }
        }

        return Indexer.idx.builtins.Cont;
    }


    public boolean isImportStar() {
        return names.size() == 1 && "*".equals(names.get(0).name.get(0).id);
    }


    private void importStar(Scope s, ModuleType mt, int tag) throws Exception {
        if (mt == null || mt.getFile() == null) {
            return;
        }

        Module mod = Indexer.idx.getAstForFile(mt.getFile());
        if (mod == null) {
            return;
        }

        List<String> names = new ArrayList<String>();
        Type allType = mt.table.lookupType("__all__");
        if (!allType.isListType()) {
            return;
        } else {
            ListType lt = allType.asListType();

            for (Object o: lt.values) {
                if (o instanceof String) {
                    names.add((String) o);
                }
            }
        }

        if (!names.isEmpty()) {
            for (String name : names) {
                Binding nb = mt.getTable().lookupLocal(name);
                if (nb != null) {
                    s.put(name, nb);
                } else {
                    List<Name> m2 = new ArrayList<Name>(module);
                    m2.add(new Name(name));
                    ModuleType mod2 = Indexer.idx.loadModule(m2, s, tag);
                    s.put(name, null, mod2, Binding.Kind.MODULE, tag);
                }
            }
        } else {
            // Fall back to importing all names not starting with "_".
            for (Entry<String, Binding> e : mt.getTable().entrySet()) {
                if (!e.getKey().startsWith("_")) {
                    s.put(e.getKey(), e.getValue());
                }
            }
        }
    }

    @Override
    public String toString() {
        return "<FromImport:" + module + ":" + names + ">";
    }

    @Override
    public void visit(NodeVisitor v) {
        if (v.visit(this)) {
            visitNodeList(names, v);
        }
    }
}
