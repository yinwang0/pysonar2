package org.yinwang.pysonar;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.pysonar.ast.Node;
import org.yinwang.pysonar.types.Type;
import org.yinwang.pysonar.types.UnionType;

import java.util.*;
import java.util.Map.Entry;


public class State {
    public enum StateType {
        CLASS,
        INSTANCE,
        FUNCTION,
        MODULE,
        GLOBAL,
        SCOPE
    }


    @Nullable
    private Map<String, List<Binding>> table;  // stays null for most scopes (mem opt)
    @Nullable
    public State parent;      // all are non-null except global table
    @Nullable
    private State forwarding; // link to the closest non-class scope, for lifting functions out
    @Nullable
    private List<State> supers;
    @Nullable
    private Set<String> globalNames;
    private StateType stateType;
    private Type type;
    @NotNull
    private String path = "";


    public State(@Nullable State parent, StateType type) {
        this.parent = parent;
        this.stateType = type;

        if (type == StateType.CLASS) {
            this.forwarding = parent == null ? null : parent.getForwarding();
        } else {
            this.forwarding = this;
        }
    }


    public State(@NotNull State s) {
        if (s.table != null) {
            this.table = new HashMap<>();
            this.table.putAll(s.table);
        }
        this.parent = s.parent;
        this.stateType = s.stateType;
        this.forwarding = s.forwarding;
        this.supers = s.supers;
        this.globalNames = s.globalNames;
        this.type = s.type;
        this.path = s.path;
    }


    // erase and overwrite this to s's contents
    public void overwrite(@NotNull State s) {
        this.table = s.table;
        this.parent = s.parent;
        this.stateType = s.stateType;
        this.forwarding = s.forwarding;
        this.supers = s.supers;
        this.globalNames = s.globalNames;
        this.type = s.type;
        this.path = s.path;
    }


    @NotNull
    public State copy() {
        return new State(this);
    }


    public void merge(State other) {
        for (Map.Entry<String, List<Binding>> e1 : getInternalTable().entrySet()) {
            List<Binding> b1 = e1.getValue();
            List<Binding> b2 = other.getInternalTable().get(e1.getKey());

            // both branch have the same name, need merge
            if (b2 != null && b1 != b2) {
                b1.addAll(b2);
            }
        }

        for (Map.Entry<String, List<Binding>> e2 : other.getInternalTable().entrySet()) {
            List<Binding> b1 = getInternalTable().get(e2.getKey());
            List<Binding> b2 = e2.getValue();

            // both branch have the same name, need merge
            if (b1 == null && b1 != b2) {
                this.update(e2.getKey(), b2);
            }
        }
    }


    public static State merge(State state1, State state2) {
        State ret = state1.copy();
        ret.merge(state2);
        return ret;
    }


    public void setParent(@Nullable State parent) {
        this.parent = parent;
    }


    public State getForwarding() {
        if (forwarding != null) {
            return forwarding;
        } else {
            return this;
        }
    }


    public void addSuper(State sup) {
        if (supers == null) {
            supers = new ArrayList<>();
        }
        supers.add(sup);
    }


    public void setStateType(StateType type) {
        this.stateType = type;
    }


    public StateType getStateType() {
        return stateType;
    }


    public void addGlobalName(@NotNull String name) {
        if (globalNames == null) {
            globalNames = new HashSet<>();
        }
        globalNames.add(name);
    }


    public boolean isGlobalName(@NotNull String name) {
        if (globalNames != null) {
            return globalNames.contains(name);
        } else if (parent != null) {
            return parent.isGlobalName(name);
        } else if (Analyzer.self.language == Language.RUBY && name.startsWith("$")) {
            return true;
        } else {
            return false;
        }
    }


    public void remove(String id) {
        if (table != null) {
            table.remove(id);
        }
    }


    // create new binding and insert
    public void insert(String id, Node node, Type type, Binding.Kind kind) {
        Binding b = new Binding(id, node, type, kind);
        if (type.isModuleType()) {
            b.setQname(type.asModuleType().getQname());
        } else {
            b.setQname(extendPath(id));
        }
        update(id, b);
    }


    // directly insert a given binding
    @NotNull
    public List<Binding> update(String id, @NotNull List<Binding> bs) {
        getInternalTable().put(id, bs);
        return bs;
    }


    @NotNull
    public List<Binding> update(String id, @NotNull Binding b) {
        List<Binding> bs = new ArrayList<>();
        bs.add(b);
        getInternalTable().put(id, bs);
        return bs;
    }


    public void setPath(@NotNull String path) {
        this.path = path;
    }


    @NotNull
    public String getPath() {
        return path;
    }


    public Type getType() {
        return type;
    }


    public void setType(Type type) {
        this.type = type;
    }


    /**
     * Look up a name in the current symbol table only. Don't recurse on the
     * parent table.
     */
    @Nullable
    public List<Binding> lookupLocal(String name) {
        if (table == null) {
            return null;
        } else {
            return table.get(name);
        }
    }


    /**
     * Look up a name (String) in the current symbol table.  If not found,
     * recurse on the parent table.
     */
    @Nullable
    public List<Binding> lookup(@NotNull String name) {
        List<Binding> b = getModuleBindingIfGlobal(name);
        if (b != null) {
            return b;
        } else {
            List<Binding> ent = lookupLocal(name);
            if (ent != null) {
                return ent;
            } else {
                if (parent != null) {
                    return parent.lookup(name);
                } else {
                    return null;
                }
            }
        }
    }


    /**
     * Look up a name in the module if it is declared as global, otherwise look
     * it up locally.
     */
    @Nullable
    public List<Binding> lookupScope(String name) {
        List<Binding> b = getModuleBindingIfGlobal(name);
        if (b != null) {
            return b;
        } else {
            return lookupLocal(name);
        }
    }


    /**
     * Look up an attribute in the type hierarchy.  Don't look at parent link,
     * because the enclosing scope may not be a super class. The search is
     * "depth first, left to right" as in Python's (old) multiple inheritance
     * rule. The new MRO can be implemented, but will probably not introduce
     * much difference.
     */
    @NotNull
    private static Set<State> looked = new HashSet<>();    // circularity prevention


    @Nullable
    public List<Binding> lookupAttr(String attr) {
        if (looked.contains(this)) {
            return null;
        } else {
            List<Binding> b = lookupLocal(attr);
            if (b != null) {
                return b;
            } else {
                if (supers != null && !supers.isEmpty()) {
                    looked.add(this);
                    for (State p : supers) {
                        b = p.lookupAttr(attr);
                        if (b != null) {
                            looked.remove(this);
                            return b;
                        }
                    }
                    looked.remove(this);
                    return null;
                } else {
                    return null;
                }
            }
        }
    }


    /**
     * Look for a binding named {@code name} and if found, return its type.
     */
    @Nullable
    public Type lookupType(String name) {
        List<Binding> bs = lookup(name);
        if (bs == null) {
            return null;
        } else {
            return makeUnion(bs);
        }
    }


    /**
     * Look for a attribute named {@code attr} and if found, return its type.
     */
    @Nullable
    public Type lookupAttrType(String attr) {
        List<Binding> bs = lookupAttr(attr);
        if (bs == null) {
            return null;
        } else {
            return makeUnion(bs);
        }
    }


    public static Type makeUnion(List<Binding> bs) {
        Type t = Analyzer.self.builtins.unknown;
        for (Binding b : bs) {
            t = UnionType.union(t, b.getType());
        }
        return t;
    }


    /**
     * Find a symbol table of a certain type in the enclosing scopes.
     */
    @Nullable
    public State getStateOfType(StateType type) {
        if (stateType == type) {
            return this;
        } else if (parent == null) {
            return null;
        } else {
            return parent.getStateOfType(type);
        }
    }


    /**
     * Returns the global scope (i.e. the module scope for the current module).
     */
    @NotNull
    public State getGlobalTable() {
        State result = null;

        if (Analyzer.self.language == Language.PYTHON) {
            result = getStateOfType(StateType.MODULE);
        } else if (Analyzer.self.language == Language.RUBY) {
            result = getStateOfType(StateType.GLOBAL);
        }

        if (result != null) {
            return result;
        } else {
            _.die("Couldn't find global table. Shouldn't happen");
            return this;
        }
    }


    /**
     * If {@code name} is declared as a global, return the module binding.
     */
    @Nullable
    private List<Binding> getModuleBindingIfGlobal(@NotNull String name) {
        if (isGlobalName(name)) {
            State module = getGlobalTable();
            if (module != this) {
                return module.lookupLocal(name);
            }
        }
        return null;
    }


    public void putAll(@NotNull State other) {
        getInternalTable().putAll(other.getInternalTable());
    }


    @NotNull
    public Set<String> keySet() {
        if (table != null) {
            return table.keySet();
        } else {
            return Collections.emptySet();
        }
    }


    @NotNull
    public Collection<Binding> values() {
        if (table != null) {
            List<Binding> ret = new ArrayList<>();
            for (List<Binding> bs : table.values()) {
                ret.addAll(bs);
            }
            return ret;
        }
        return Collections.emptySet();
    }


    @NotNull
    public Set<Entry<String, List<Binding>>> entrySet() {
        if (table != null) {
            return table.entrySet();
        }
        return Collections.emptySet();
    }


    public boolean isEmpty() {
        return table == null || table.isEmpty();
    }


    @NotNull
    public String extendPath(@NotNull String name) {
        name = _.moduleName(name);
        if (path.equals("")) {
            return name;
        }
        return path + "." + name;
    }


    @NotNull
    private Map<String, List<Binding>> getInternalTable() {
        if (this.table == null) {
            this.table = new HashMap<>();
        }
        return this.table;
    }


    @NotNull
    @Override
    public String toString() {
        return "<State:" + getStateType() + ":" +
                (table == null ? "{}" : table.keySet()) + ">";
    }

}
