package org.yinwang.pysonar;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.pysonar.types.ModuleType;
import org.yinwang.pysonar.types.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Generates a file outline from the index: a structure representing the
 * variable and attribute definitions in a file.
 */
public class Outliner {

    public static abstract class Entry {
        @Nullable
        protected String qname;  // entry qualified name
        protected int offset;  // file offset of referenced declaration
        @Nullable
        protected Binding.Kind kind;  // binding kind of outline entry

        public Entry() {
        }

        public Entry(String qname, int offset, Binding.Kind kind) {
            this.qname = qname;
            this.offset = offset;
            this.kind = kind;
        }

        public abstract boolean isLeaf();

        @NotNull
        public Leaf asLeaf() {
            return (Leaf) this;
        }

        public abstract boolean isBranch();

        @NotNull
        public Branch asBranch() {
            return (Branch) this;
        }

        public abstract boolean hasChildren();

        public abstract List<Entry> getChildren();

        public abstract void setChildren(List<Entry> children);

        @Nullable
        public String getQname() {
            return qname;
        }

        public void setQname(@Nullable String qname) {
            if (qname == null) {
                throw new IllegalArgumentException("qname param cannot be null");
            }
            this.qname = qname;
        }

        /**
         * Returns the file offset of the beginning of the identifier referenced
         * by this outline entry.
         */
        public int getOffset() {
            return offset;
        }

        public void setOffset(int offset) {
            this.offset = offset;
        }

        @Nullable
        public Binding.Kind getKind() {
            return kind;
        }

        public void setKind(@Nullable Binding.Kind kind) {
            if (kind == null) {
                throw new IllegalArgumentException("kind param cannot be null");
            }
            this.kind = kind;
        }

        /**
         * Returns the simple (unqualified) name of the identifier.
         */
        public String getName() {
            String[] parts = qname.split("[.&@%]");
            return parts[parts.length - 1];
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            toString(sb, 0);
            return sb.toString().trim();
        }

        public void toString(@NotNull StringBuilder sb, int depth) {
            for (int i = 0; i < depth; i++) {
                sb.append("  ");
            }
            sb.append(getKind());
            sb.append(" ");
            sb.append(getName());
            sb.append("\n");
            if (hasChildren()) {
                for (Entry e : getChildren()) {
                    e.toString(sb, depth + 1);
                }
            }
        }
    }

    /**
     * An outline entry with children.
     */
    public static class Branch extends Entry {
        private List<Entry> children = new ArrayList<Entry>();

        public Branch() {
        }

        public Branch(String qname, int start, Binding.Kind kind) {
            super(qname, start, kind);
        }

        public boolean isLeaf() {
            return false;
        }

        public boolean isBranch() {
            return true;
        }

        public boolean hasChildren() {
            return children != null && !children.isEmpty();
        }

        public List<Entry> getChildren() {
            return children;
        }

        public void setChildren(List<Entry> children) {
            this.children = children;
        }
    }

    /**
     * An entry with no children.
     */
    public static class Leaf extends Entry {
        public boolean isLeaf() {
            return true;
        }

        public boolean isBranch() {
            return false;
        }

        public Leaf() {
        }

        public Leaf(String qname, int start, Binding.Kind kind) {
            super(qname, start, kind);
        }

        public boolean hasChildren() {
            return false;
        }

        @NotNull
        public List<Entry> getChildren() {
            return new ArrayList<Entry>();
        }

        public void setChildren(List<Entry> children) {
            throw new UnsupportedOperationException("Leaf nodes cannot have children.");
        }
    }

    /**
     * Create an outline for a file in the index.
     *
     * @param scope the file scope
     * @param path  the file for which to build the outline
     * @return a list of entries constituting the file outline.
     *         Returns an empty list if the indexer hasn't indexed that path.
     */
    @NotNull
    public List<Entry> generate(@NotNull Indexer idx, @NotNull String abspath) {
        ModuleType mt = idx.getModuleForFile(abspath);
        if (mt == null) {
            return new ArrayList<Entry>();
        }
        return generate(mt.getTable(), abspath);
    }

    /**
     * Create an outline for a symbol table.
     *
     * @param scope the file scope
     * @param path  the file for which we're building the outline
     * @return a list of entries constituting the outline
     */
    @NotNull
    public List<Entry> generate(@NotNull Scope scope, @NotNull String path) {
        List<Entry> result = new ArrayList<Entry>();

        Set<Binding> entries = new TreeSet<Binding>();
        for (Binding b : scope.values()) {
            if (!b.isSynthetic()
                    && !b.isBuiltin()
                    && !b.getDefs().isEmpty()
                    && path.equals(b.getFirstNode().getFile())) {
                entries.add(b);
            }
        }

        for (Binding nb : entries) {
            Def signode = nb.getFirstNode();
            List<Entry> kids = null;

            if (nb.getKind() == Binding.Kind.CLASS) {
                Type realType = nb.getType();
                if (realType.isUnionType()) {
                    for (Type t : realType.asUnionType().getTypes()) {
                        if (t.isClassType()) {
                            realType = t;
                            break;
                        }
                    }
                }
                kids = generate(realType.getTable(), path);
            }

            Entry kid = kids != null ? new Branch() : new Leaf();
            kid.setOffset(signode.getStart());
            kid.setQname(nb.getQname());
            kid.setKind(nb.getKind());

            if (kids != null) {
                kid.setChildren(kids);
            }
            result.add(kid);
        }
        return result;
    }
}
