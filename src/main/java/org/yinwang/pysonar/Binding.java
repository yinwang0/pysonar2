package org.yinwang.pysonar;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.pysonar.ast.*;
import org.yinwang.pysonar.types.ModuleType;
import org.yinwang.pysonar.types.Type;

import java.util.LinkedHashSet;
import java.util.Set;


public class Binding implements Comparable<Object> {

    public enum Kind {
        ATTRIBUTE,    // attr accessed with "." on some other object
        CLASS,        // class definition
        CONSTRUCTOR,  // __init__ functions in classes
        FUNCTION,     // plain function
        METHOD,       // static or instance method
        MODULE,       // file
        PARAMETER,    // function param
        SCOPE,        // top-level variable ("scope" means we assume it can have attrs)
        VARIABLE      // local variable
    }


    private boolean isStatic = false;         // static fields/methods
    private boolean isSynthetic = false;      // auto-generated bindings
    private boolean isReadonly = false;       // non-writable attributes
    private boolean isDeprecated = false;     // documented as deprecated
    private boolean isBuiltin = false;        // not from a source file

    @NotNull
    private String name;     // unqualified name
    @NotNull
    private String qname;    // qualified name
    private Type type;       // inferred type
    public Kind kind;        // name usage context

    @NotNull
    private Def def;   // definitions (may be multiple)
    private Set<Ref> refs;


    // fields from Def
    private int start = -1;
    private int end = -1;
    private int bodyStart = -1;
    private int bodyEnd = -1;

    public String docstring;
    public int docstringStart;
    public int docstringEnd;

    @NotNull
    private Binding binding;
    @Nullable
    private String fileOrUrl;
    @NotNull
    private Node node;


    public Binding(@NotNull String id, Node node, @NotNull Type type, @NotNull Kind kind) {
        this.name = id;
        this.qname = type.getTable().getPath();
        this.type = type;
        this.kind = kind;
        addDef(node);

        this.node = node;

        if (node instanceof Url) {
            String url = ((Url) node).getURL();
            if (url.startsWith("file://")) {
                fileOrUrl = url.substring("file://".length());
            } else {
                fileOrUrl = url;
            }
        } else {
            fileOrUrl = node.getFile();
            if (node instanceof Name) {
                name = node.asName().getId();
            }
        }

        initLocationInfo(node);
        Indexer.idx.registerBinding(this);
    }


    private void initLocationInfo(Node node) {
        start = node.start;
        end = node.end;

        Node parent = node.getParent();
        if ((parent instanceof FunctionDef && ((FunctionDef) parent).name == node) ||
                (parent instanceof ClassDef && ((ClassDef) parent).name == node))
        {
            bodyStart = parent.start;
            bodyEnd = parent.end;
            Str docstring = parent.docstring();
            if (docstring != null) {
                this.docstring = docstring.getStr();
                this.docstringStart = docstring.start;
                this.docstringEnd = docstring.end;
            }
        } else if (node instanceof Module) {
            name = ((Module) node).name;
            start = 0;
            end = 0;
            bodyStart = node.start;
            bodyEnd = node.end;

            Str docstring = node.docstring();
            if (docstring != null) {
                this.docstring = docstring.getStr();
                this.docstringStart = docstring.start;
                this.docstringEnd = docstring.end;
            }
        } else {
            bodyStart = node.start;
            bodyEnd = node.end;
        }

//        Util.msg("start: " + start + ", end: " + end + ", bodystart: " + bodyStart + ", bodyend: " + bodyEnd);

    }


    @NotNull
    public String getName() {
        return name;
    }


    public void setQname(@NotNull String qname) {
        this.qname = qname;
    }


    @NotNull
    public String getQname() {
        return qname;
    }


    public void addDef(@Nullable Node node) {
        if (node != null) {
            Def def = new Def(node, this);
            addDef(def);
        }
    }


    public void addDef(Def def) {
        def.setBinding(this);
        this.def = def;
        if (def.isURL()) {
            markBuiltin();
        }
    }


    public void addRef(Ref ref) {
        getRefs().add(ref);
    }


    // Returns one definition (even if there are many)
    @NotNull
    public Def getSingle() {
        return getDef();
    }


    public void setType(Type type) {
        this.type = type;
    }


    public Type getType() {
        return type;
    }


    public void setKind(Kind kind) {
        this.kind = kind;
    }


    public Kind getKind() {
        return kind;
    }


    public void markStatic() {
        isStatic = true;
    }


    public boolean isStatic() {
        return isStatic;
    }


    public void markSynthetic() {
        isSynthetic = true;
    }


    public boolean isSynthetic() {
        return isSynthetic;
    }


    public void markReadOnly() {
        isReadonly = true;
    }


    public boolean isReadOnly() {
        return isReadonly;
    }


    public boolean isDeprecated() {
        return isDeprecated;
    }


    public void markDeprecated() {
        isDeprecated = true;
    }


    public boolean isBuiltin() {
        return isBuiltin;
    }


    public void markBuiltin() {
        isBuiltin = true;
    }


    @NotNull
    public Def getDef() {
        return def;
    }


    public Set<Ref> getRefs() {
        if (refs == null) {
            refs = new LinkedHashSet<>(1);
        }
        return refs;
    }


    @NotNull
    public String getFirstFile() {
        Type bt = getType();
        if (bt instanceof ModuleType) {
            String file = bt.asModuleType().getFile();
            return file != null ? file : "<built-in module>";
        }

        String file = def.getFile();
        if (file != null) {
            return file;
        }

        return "<built-in binding>";
    }


    @Nullable
    public String getFile() {
        return isURL() ? null : fileOrUrl;
    }


    @Nullable
    public String getURL() {
        return isURL() ? fileOrUrl : null;
    }


    @Nullable
    public String getFileOrUrl() {
        return fileOrUrl;
    }


    public boolean isURL() {
        return fileOrUrl != null && fileOrUrl.startsWith("http://");
    }


    public int getStart() {
        return start;
    }


    public int getEnd() {
        return end;
    }


    public int getLength() {
        return end - start;
    }


    public int getBodyStart() {
        return bodyStart;
    }


    public int getBodyEnd() {
        return bodyEnd;
    }


    public boolean hasName() {
        return name != null;
    }


    @NotNull
    public Node getNode() {
        return node;
    }


    public void setNode(Node node) {
        this.node = node;
    }


    /**
     * Bindings can be sorted by their location for outlining purposes.
     */
    public int compareTo(@NotNull Object o) {
        return getSingle().getStart() - ((Binding) o).getSingle().getStart();
    }


    @NotNull
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("<Binding:");
        sb.append(":qname=").append(qname);
        sb.append(":type=").append(type);
        sb.append(":kind=").append(kind);
        sb.append(":def=").append(def);
        sb.append(":refs=");
        if (getRefs().size() > 10) {
            sb.append("[");
            sb.append(refs.iterator().next());
            sb.append(", ...(");
            sb.append(refs.size() - 1);
            sb.append(" more)]");
        } else {
            sb.append(refs);
        }
        sb.append(">");
        return sb.toString();
    }


    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Binding)) {
            return false;
        } else {
            Binding b = (Binding) obj;
            return (start == b.start
                    && end == b.end
                    && ((fileOrUrl == null && b.fileOrUrl == null)
                    || (fileOrUrl != null && b.fileOrUrl != null &&
                    fileOrUrl.equals(b.fileOrUrl))));
        }
    }


    @Override
    public int hashCode() {
        return ("" + fileOrUrl + start).hashCode();
    }

}
