package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.pysonar.Binding;
import org.yinwang.pysonar.Indexer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.ListType;
import org.yinwang.pysonar.types.ModuleType;
import org.yinwang.pysonar.types.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;


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
    public Type resolve(@NotNull Scope s, int tag) {
        if (module == null) {
            return Indexer.idx.builtins.Cont;
        }

        ModuleType mod = Indexer.idx.loadModule(module, s, tag);

        if (mod == null) {
            Indexer.idx.putProblem(this, "Cannot load module");
        } else if (isImportStar()) {
            importStar(s, mod, tag);
        } else {
            for (Alias a : names) {
                Type t = mod.getTable().lookupType(a.name.get(0).id);
                if (t == null) return Indexer.idx.builtins.Cont;

                if (a.asname != null) {
                    s.put(a.asname.id, a.asname, t, Binding.Kind.MODULE, tag);
                } else {
                    Binding b = mod.getTable().lookup(a.name.get(0).id);
                    if (b != null) {
                        s.put(a.name.get(0).id, b);
                    }
                }
            }
        }

        return Indexer.idx.builtins.Cont;
    }


    public boolean isImportStar() {
        return names.size() == 1 && "*".equals(names.get(0).name.get(0).id);
    }


    private void importStar(@NotNull Scope s, @Nullable ModuleType mt, int tag) {
        if (mt == null || mt.getFile() == null) {
            return;
        }

        Module mod = Indexer.idx.getAstForFile(mt.getFile());
        if (mod == null) {
            return;
        }

        List<String> names = new ArrayList<String>();
        Type allType = mt.getTable().lookupType("__all__");

        if (allType == null || !allType.isListType()) {
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

    @NotNull
    @Override
    public String toString() {
        return "<FromImport:" + module + ":" + names + ">";
    }

    @Override
    public void visit(@NotNull NodeVisitor v) {
        if (v.visit(this)) {
            visitNodeList(names, v);
        }
    }
}
