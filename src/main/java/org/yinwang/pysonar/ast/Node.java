package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.pysonar.$;
import org.yinwang.pysonar.Analyzer;

import java.util.Collection;

/**
 * A Node is a junction in the program.
 * Since there is no way to put different things in the same segment of the same file,
 * a node is uniquely identified by a file, a start and end point.
 */
public abstract class Node implements java.io.Serializable, Comparable<Object> {

    public NodeType nodeType;
    public String file;
    public int start;
    public int end;
    public int line;
    public int col;

    public String name;
    public Node parent = null;

    public Node() {
    }

    public Node(NodeType nodeType, String file, int start, int end, int line, int col) {
        this.nodeType = nodeType;
        this.file = file;
        this.start = start;
        this.end = end;
        this.line = line;
        this.col = col;
    }

    public String getFullPath() {
        if (!file.startsWith("/")) {
            return $.makePathString(Analyzer.self.projectDir, file);
        } else {
            return file;
        }
    }

    public void setParent(Node parent) {
        this.parent = parent;
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

    @Nullable
    public Str getDocString() {
        Node body = null;
        if (this instanceof FunctionDef) {
            body = ((FunctionDef) this).body;
        } else if (this instanceof ClassDef) {
            body = ((ClassDef) this).body;
        } else if (this instanceof PyModule) {
            body = ((PyModule) this).body;
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
                    $.same(file, node.file));
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
