package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.pysonar.Analyzer;
import org.yinwang.pysonar.Binding;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.ListType;
import org.yinwang.pysonar.types.ModuleType;
import org.yinwang.pysonar.types.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;


public class ImportFrom extends Node {

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


    @NotNull
    @Override
    public Type resolve(@NotNull Scope s) {
        if (module == null) {
            return Analyzer.self.builtins.Cont;
        }

        ModuleType mod = Analyzer.self.loadModule(module, s);

        if (mod == null) {
            Analyzer.self.putProblem(this, "Cannot load module");
        } else if (isImportStar()) {
            importStar(s, mod);
        } else {
            for (Alias a : names) {
                Name first = a.name.get(0);
                List<Binding> bs = mod.getTable().lookup(first.id);
                if (bs != null) {
                    if (a.asname != null) {
                        s.update(a.asname.id, bs);
                        Analyzer.self.putRef(a.asname, bs);
                    } else {
                        s.update(first.id, bs);
                        Analyzer.self.putRef(first, bs);
                    }
                } else {
                    List<Name> ext = new ArrayList<>(module);
                    ext.add(first);
                    ModuleType mod2 = Analyzer.self.loadModule(ext, s);
                    if (mod2 != null) {
                        if (a.asname != null) {
                            s.insert(a.asname.id, a.asname, mod2, Binding.Kind.VARIABLE);
                        } else {
                            s.insert(first.id, first, mod2, Binding.Kind.VARIABLE);
                        }
                    }
                }
            }
        }

        return Analyzer.self.builtins.Cont;
    }


    public boolean isImportStar() {
        return names.size() == 1 && "*".equals(names.get(0).name.get(0).id);
    }


    private void importStar(@NotNull Scope s, @Nullable ModuleType mt) {
        if (mt == null || mt.getFile() == null) {
            return;
        }

        Module mod = Analyzer.self.getAstForFile(mt.getFile());
        if (mod == null) {
            return;
        }

        List<String> names = new ArrayList<>();
        Type allType = mt.getTable().lookupType("__all__");

        if (allType != null && allType.isListType()) {
            ListType lt = allType.asListType();

            for (Object o : lt.values) {
                if (o instanceof String) {
                    names.add((String) o);
                }
            }
        }

        if (!names.isEmpty()) {
            for (String name : names) {
                List<Binding> b = mt.getTable().lookupLocal(name);
                if (b != null) {
                    s.update(name, b);
                } else {
                    List<Name> m2 = new ArrayList<>(module);
                    m2.add(new Name(name));
                    ModuleType mod2 = Analyzer.self.loadModule(m2, s);
                    if (mod2 != null) {
                        s.insert(name, null, mod2, Binding.Kind.VARIABLE);
                    }
                }
            }
        } else {
            // Fall back to importing all names not starting with "_".
            for (Entry<String, List<Binding>> e : mt.getTable().entrySet()) {
                if (!e.getKey().startsWith("_")) {
                    s.update(e.getKey(), e.getValue());
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
