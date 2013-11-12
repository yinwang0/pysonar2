package org.yinwang.pysonar;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.pysonar.ast.Node;
import org.yinwang.pysonar.types.Type;
import org.yinwang.pysonar.types.UnionType;

import java.util.*;
import java.util.Map.Entry;


public class Scope {

    public enum ScopeType {
        CLASS,
        INSTANCE,
        FUNCTION,
        MODULE,
        GLOBAL,
        SCOPE
    }


    @Nullable
    private Map<String, Binding> table;  // stays null for most scopes (mem opt)
    @Nullable
    public Scope parent;
    private Scope forwarding;       // link to the closest non-class scope, for lifting functions out
    @Nullable
    private List<Scope> supers;
    @Nullable
    private Set<String> globalNames;
    private ScopeType scopeType;
    private Type type;
    @Nullable
    private String path = "";


    public Scope(Scope parent, ScopeType type) {
        this.parent = parent;
        this.scopeType = type;

        if (type == ScopeType.CLASS) {
            this.forwarding = parent.getForwarding();
        } else {
            this.forwarding = this;
        }
    }


    public Scope(@NotNull Scope s) {
        if (s.table != null) {
            this.table = new HashMap<String, Binding>(s.table);
        }
        this.parent = s.parent;
        this.scopeType = s.scopeType;
        this.forwarding = s.forwarding;
        this.supers = s.supers;
        this.globalNames = s.globalNames;
        this.type = s.type;
        this.path = s.path;
    }


    public void setParent(Scope parent) {
        this.parent = parent;
    }

    @Nullable
    public Scope getParent() {
        return parent;
    }

    public Scope getForwarding() {
        if (forwarding != null) {
            return forwarding;
        } else {
            return this;
        }
    }

    public void addSuper(Scope sup) {
        if (supers == null) {
            supers = new ArrayList<Scope>();
        }
        supers.add(sup);
    }

    public void setScopeType(ScopeType type) {
        this.scopeType = type;
    }

    public ScopeType getScopeType() {
        return scopeType;
    }

    /**
     * Mark a name as being global (i.e. module scoped) for name-binding and
     * name-lookup operations in this code block and any nested scopes.
     */
    public void addGlobalName(@Nullable String name) {
        if (name == null) {
            throw new IllegalArgumentException("name shouldn't be null");
        }
        if (globalNames == null) {
            globalNames = new HashSet<String>();
        }
        globalNames.add(name);
    }

    /**
     * Returns {@code true} if {@code name} appears in a {@code global}
     * statement in this scope or any enclosing scope.
     */
    public boolean isGlobalName(String name) {
        if (globalNames != null) {
            return globalNames.contains(name);
        } else if (parent != null) {
            return parent.isGlobalName(name);
        } else {
            return false;
        }
    }

    /**
     * Directly assigns a binding to a name in this table.  Does not add a new
     * definition or reference to the binding.  This form of {@code put} is
     * often followed by a call to {@link putLocation} to create a reference to
     * the binding.  When there is no code location associated with {@code id},
     * or it is otherwise undesirable to create a reference, the
     * {@link putLocation} call is omitted.
     */
    public void put(String id, @NotNull Binding b) {
        getInternalTable().put(id, b);
    }


    @Nullable
    public Binding put(String id, Node loc, @NotNull Type type, Binding.Kind kind, int tag) {
        Binding b = lookupScope(id);
        return insertOrUpdate(b, id, loc, type, kind, tag);
    }


    @Nullable
    public Binding putAttr(String id, Node loc, @NotNull Type type, Binding.Kind kind, int tag) {
        // Attributes are always part of a qualified name.  If there is no qname
        // on the target type, it's a bug (we forgot to set the path somewhere.)
        if ("".equals(path)) {
            return null;
        } else {
            Binding b = lookupAttr(id);
            return insertOrUpdate(b, id, loc, type, kind, tag);
        }
    }


    /**
     * If no bindings are found, or it is rebinding in the same thread of
     * control to a new type, then create a new binding and rewrite/shadow the
     * old one. Otherwise, use the exisitng binding and update the type.
     */
    @Nullable
    private Binding insertOrUpdate(@Nullable Binding b, String id, Node loc, @NotNull Type t, Binding.Kind k, int tag) {
        if (b == null) {
            b = insertBinding(id, new Binding(id, loc, t, k, tag));
        } else if (tag == b.tag && !b.getType().equals(t)) {
            b = insertBinding(id, new Binding(id, loc, t, k, tag));
        } else {
            b.addDef(loc);
            b.setType(UnionType.union(t, b.getType()));
        }
        return b;
    }


    @NotNull
    private Binding insertBinding(String id, @NotNull Binding b) {
        switch (b.getKind()) {
            case MODULE:
                b.setQname(b.getType().getTable().getPath());
                break;
            case PARAMETER:
                b.setQname(extendPathForParam(b.getName()));
                break;
            default:
                b.setQname(extendPath(b.getName()));
                break;
        }
        Indexer.idx.putBinding(b);
        put(id, b);
        return b;
    }


    public void remove(String id) {
        if (table != null) {
            table.remove(id);
        }
    }


    /**
     * Adds a new binding for {@code id}.  If a binding already existed,
     * replaces its previous definitions, if any, with {@code loc}.  Sets the
     * binding's type to {@code type} (not a union with the previous type).
     */
    @Nullable
    public Binding update(String id, Node node, Type type, Binding.Kind kind) {
        Binding b = lookupScope(id);
        if (b == null) {
            return insertBinding(id, new Binding(id, node, type, kind));
        } else {
            b.getDefs().clear();
            b.addDef(node);
            b.setType(type);
            b.setKind(kind);
            return b;
        }
    }


    /**
     * Create a copy of the symbol table but without the links to parent, supers
     * and children. Useful for creating instances.
     *
     * @return the symbol table for use by the instance.
     */
    @Nullable
    public Scope copy(ScopeType tableType) {
        Scope ret = new Scope(null, tableType);
        if (table != null) {
            ret.getInternalTable().putAll(table);
        }
        return ret;
    }

    public void setPath(@Nullable String path) {
        if (path == null) {
            throw new IllegalArgumentException("'path' param cannot be null");
        }
        this.path = path;
    }

    @Nullable
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
    public Binding lookupLocal(String name) {
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
    public Binding lookup(String name) {
        Binding b = getModuleBindingIfGlobal(name);
        if (b != null) {
            return b;
        } else {
            Binding ent = lookupLocal(name);
            if (ent != null) {
                return ent;
            } else if (getParent() != null) {
                return getParent().lookup(name);
            } else {
                return null;
            }
        }
    }

    /**
     * Look up an attribute in the type hierarchy.  Don't look at parent link,
     * because the enclosing scope may not be a super class. The search is
     * "depth first, left to right" as in Python's (old) multiple inheritance
     * rule. The new MRO can be implemented, but will probably not introduce
     * much difference.
     */
    @Nullable
    private static Set<Scope> looked = new HashSet<Scope>();    // circularity prevention

    @Nullable
    public Binding lookupAttr(String attr) {
        if (looked.contains(this)) {
            return null;
        } else {
            Binding b = lookupLocal(attr);
            if (b != null) {
                return b;
            } else {
                if (supers != null && !supers.isEmpty()) {
                    looked.add(this);
                    for (Scope p : supers) {
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
        Binding b = lookup(name);
        if (b == null) {
            return null;
        } else {
            return b.getType();
        }
    }

    /**
     * Look for a attribute named {@code attr} and if found, return its type.
     */
    @Nullable
    public Type lookupAttrType(String attr) {
        Binding b = lookupAttr(attr);
        if (b == null) {
            return null;
        } else {
            return b.getType();
        }
    }

    /**
     * Look up a name in the module if it is declared as global, otherwise look
     * it up locally.
     */
    @Nullable
    public Binding lookupScope(String name) {
        Binding b = getModuleBindingIfGlobal(name);
        if (b != null) {
            return b;
        } else {
            return lookupLocal(name);
        }
    }

    /**
     * Find a symbol table of a certain type in the enclosing scopes.
     */
    @Nullable
    private Scope getSymtabOfType(ScopeType type) {
        if (scopeType == type) {
            return this;
        } else if (parent == null) {
            return null;
        } else {
            return parent.getSymtabOfType(type);
        }
    }

    /**
     * Returns the global scope (i.e. the module scope for the current module).
     */
    @Nullable
    public Scope getGlobalTable() {
        Scope result = getSymtabOfType(ScopeType.MODULE);
        if (result == null) {
            result = this;
        }
        return result;
    }

    /**
     * If {@code name} is declared as a global, return the module binding.
     */
    @Nullable
    private Binding getModuleBindingIfGlobal(String name) {
        if (isGlobalName(name)) {
            Scope module = getGlobalTable();
            if (module != null && module != this) {
                return module.lookupLocal(name);
            }
        }
        return null;
    }

    /**
     * Merge all records from another symbol table. Used by {@code import from *}.
     */
    public void merge(@NotNull Scope other) {
        getInternalTable().putAll(other.table);
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
            return table.values();
        }
        Collection<Binding> result = Collections.emptySet();
        return result;
    }

    @NotNull
    public Set<Entry<String, Binding>> entrySet() {
        if (table != null) {
            return table.entrySet();
        }
        Set<Entry<String, Binding>> result = Collections.emptySet();
        return result;
    }

    public boolean isEmpty() {
        return table == null ? true : table.isEmpty();
    }


    @NotNull
    public String extendPathForParam(String name) {
        assert(!path.isEmpty());

        StringBuilder sb = new StringBuilder();
        sb.append(path).append("@").append(name);
        return sb.toString();
    }


    @Nullable
    public String extendPath(@NotNull String name) {
        if (name.endsWith(".py")) {
            name = Util.moduleNameFor(name);
        }
        if (path.equals("")) {
            return name;
        }
        String sep;
        switch (scopeType) {
            case MODULE:
            case CLASS:
            case INSTANCE:
            case SCOPE:
                sep = ".";
                break;
            case FUNCTION:
                sep = "&";
                break;
            default:
                System.err.println("unsupported context for extendPath: " + scopeType);
                return path;
        }
        return path + sep + name;
    }


    @Nullable
    private Map<String, Binding> getInternalTable() {
        if (this.table == null) {
            this.table = new HashMap<String, Binding>();
        }
        return this.table;
    }


    @NotNull
    @Override
    public String toString() {
        return "<Scope:" + getScopeType() + ":" + path + ":" +
                (table == null ? "{}" : table.keySet()) + ">";
    }


    @NotNull
    public String toShortString() {
        return "<Scope:" + getScopeType() + ":" + path + ">";
    }
}
