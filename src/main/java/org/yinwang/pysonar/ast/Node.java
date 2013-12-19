package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.pysonar.Analyzer;
import org.yinwang.pysonar.State;
import org.yinwang.pysonar._;
import org.yinwang.pysonar.types.Type;
import org.yinwang.pysonar.types.UnionType;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * A Node is a junction in the program.
 * Since there is no way to put different things in the same segment of the same file,
 * a node is uniquely identified by a file, a start and end point.
 */
public abstract class Node implements java.io.Serializable, Comparable<Object> {

    public String file;
    public int start;
    public int end;

    public String name;
    private String sha1;   // input source file sha
    protected Node parent = null;


    public Node() {
    }


    public Node(String file, int start, int end) {
        this.file = file;
        this.start = start;
        this.end = end;
    }


    public void setParent(Node parent) {
        this.parent = parent;
    }


    @Nullable
    public Node getParent() {
        return parent;
    }


    public String getSHA1() {
        return sha1;
    }


    @NotNull
    public Node getAstRoot() {
        if (parent == null) {
            return this;
        }
        return parent.getAstRoot();
    }


    public int length() {
        return end - start;
    }


    public void addChildren(@Nullable Node... nodes) {
        if (nodes != null) {
            for (Node n : nodes) {
                if (n != null) {
                    n.setParent(this);
                }
            }
        }
    }


    public void addChildren(@Nullable Collection<? extends Node> nodes) {
        if (nodes != null) {
            for (Node n : nodes) {
                if (n != null) {
                    n.setParent(this);
                }
            }
        }
    }


    public void setFile(String file) {
        if (file.startsWith(Analyzer.self.projectDir)) {
            file = file.substring(Analyzer.self.projectDir.length() + 1);
        }

        this.file = file;
        this.name = _.moduleName(file);
        this.sha1 = _.getSHA1(new File(file));
    }


    @Nullable
    public Str getDocString() {
        Node body = null;
        if (this instanceof Function) {
            body = ((Function) this).body;
        } else if (this instanceof Class) {
            body = ((Class) this).body;
        } else if (this instanceof Module) {
            body = ((Module) this).body;
        }

        if (body instanceof Block && ((Block) body).seq.size() >= 1) {
            Node firstExpr = ((Block) body).seq.get(0);
            if (firstExpr instanceof Expr) {
                Node docstrNode = ((Expr) firstExpr).value;
                if (docstrNode != null && docstrNode instanceof Str) {
                    return (Str) docstrNode;
                }
            }
        }
        return null;
    }


    @NotNull
    public static Type transformExpr(@NotNull Node n, State s) {
        return n.transform(s);
    }


    @NotNull
    protected abstract Type transform(State s);


    public boolean isCall() {
        return this instanceof Call;
    }


    public boolean isModule() {
        return this instanceof Module;
    }


    public boolean isClassDef() {
        return false;
    }


    public boolean isFunctionDef() {
        return false;
    }


    public boolean isLambda() {
        return false;
    }


    public boolean isName() {
        return this instanceof Name;
    }


    public boolean isAssign() {
        return this instanceof Assign;
    }


    public boolean isGlobal() {
        return this instanceof Global;
    }


    public boolean isBinOp() {
        return this instanceof BinOp;
    }


    @NotNull
    public BinOp asBinOp() {
        return (BinOp) this;
    }


    @NotNull
    public Call asCall() {
        return (Call) this;
    }


    @NotNull
    public Module asModule() {
        return (Module) this;
    }


    @NotNull
    public Class asClassDef() {
        return (Class) this;
    }


    @NotNull
    public Function asFunctionDef() {
        return (Function) this;
    }


    @NotNull
    public Name asName() {
        return (Name) this;
    }


    @NotNull
    public Assign asAssign() {
        return (Assign) this;
    }


    @NotNull
    public Global asGlobal() {
        return (Global) this;
    }


    protected void addWarning(String msg) {
        Analyzer.self.putProblem(this, msg);
    }


    protected void addError(String msg) {
        Analyzer.self.putProblem(this, msg);
    }


    /**
     * Utility method to resolve every node in {@code nodes} and
     * return the union of their types.  If {@code nodes} is empty or
     * {@code null}, returns a new {@link org.yinwang.pysonar.types.UnknownType}.
     */
    @NotNull
    protected Type resolveUnion(@NotNull Collection<? extends Node> nodes, State s) {
        Type result = Type.UNKNOWN;
        for (Node node : nodes) {
            Type nodeType = transformExpr(node, s);
            result = UnionType.union(result, nodeType);
        }
        return result;
    }


    /**
     * Resolves each element, also construct a result list.
     */
    @Nullable
    static protected List<Type> resolveList(@Nullable Collection<? extends Node> nodes, State s) {
        if (nodes == null) {
            return null;
        } else {
            List<Type> ret = new ArrayList<>();
            for (Node n : nodes) {
                ret.add(transformExpr(n, s));
            }
            return ret;
        }
    }


    // nodes are equal if they are from the same file and same starting point
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Node)) {
            return false;
        } else {
            Node node = (Node) obj;
            String file = this.file;
            return (start == node.start &&
                    end == node.end &&
                    (file == null && node.file == null) ||
                    (file != null && node.file != null && file.equals(node.file)));
        }
    }


    @Override
    public int hashCode() {
        return (file + ":" + start + ":" + end).hashCode();
    }


    @Override
    public int compareTo(@NotNull Object o) {
        if (o instanceof Node) {
            return start - ((Node) o).start;
        } else {
            return -1;
        }
    }


    public String toDisplay() {
        return "";
    }


    @NotNull
    @Override
    public String toString() {
        return "(node:" + file + ":" + name + ":" + start + ")";
    }

}
