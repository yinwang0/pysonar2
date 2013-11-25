package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.pysonar.Indexer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.Type;
import org.yinwang.pysonar.types.UnionType;

import java.util.ArrayList;
import java.util.List;


public abstract class Node implements java.io.Serializable
{

    public int start = -1;
    public int end = -1;

    @Nullable
    protected Node parent = null;


    public Node()
    {
    }


    public Node(int start, int end)
    {
        this.start = start;
        this.end = end;
    }


    public void setParent(Node parent)
    {
        this.parent = parent;
    }


    @Nullable
    public Node getParent()
    {
        return parent;
    }


    @NotNull
    public Node getAstRoot()
    {
        if (parent == null)
        {
            return this;
        }
        return parent.getAstRoot();
    }


    public int length()
    {
        return end - start;
    }


    /**
     * Returns {@code true} if this is a name-binding node.
     * Includes functions/lambdas, function/lambda params, classes,
     * assignments, imports, and implicit assignment via for statements
     * and except clauses.
     *
     * @see "http://www.python.org/dev/peps/pep-0227"
     */
    public boolean bindsName()
    {
        return false;
    }


    /**
     * @return the path to the code that generated this AST
     */
    @Nullable
    public String getFile()
    {
        return parent != null ? parent.getFile() : null;
    }


    public void addChildren(@Nullable Node... nodes)
    {
        if (nodes != null)
        {
            for (Node n : nodes)
            {
                if (n != null)
                {
                    n.setParent(this);
                }
            }
        }
    }


    public void addChildren(@Nullable List<? extends Node> nodes)
    {
        if (nodes != null)
        {
            for (Node n : nodes)
            {
                if (n != null)
                {
                    n.setParent(this);
                }
            }
        }
    }


    @Nullable
    public Str docstring()
    {
        Node body = null;
        if (this instanceof FunctionDef)
        {
            body = ((FunctionDef) this).body;
        }
        else if (this instanceof ClassDef)
        {
            body = ((ClassDef) this).body;
        }
        else if (this instanceof Module)
        {
            body = ((Module) this).body;
        }

        if (body instanceof Block && ((Block) body).seq.size() >= 1)
        {
            Node firstExpr = ((Block) body).seq.get(0);
            if (firstExpr instanceof Expr)
            {
                Node docstrNode = ((Expr) firstExpr).value;
                if (docstrNode != null && docstrNode instanceof Str)
                {
                    return (Str) docstrNode;
                }
            }
        }
        return null;
    }


    @NotNull
    public static Type resolveExpr(@NotNull Node n, Scope s)
    {
        return n.resolve(s);
    }


    @NotNull
    abstract public Type resolve(Scope s);


    public boolean isCall()
    {
        return this instanceof Call;
    }


    public boolean isModule()
    {
        return this instanceof Module;
    }


    public boolean isClassDef()
    {
        return false;
    }


    public boolean isFunctionDef()
    {
        return false;
    }


    public boolean isLambda()
    {
        return false;
    }


    public boolean isName()
    {
        return this instanceof Name;
    }


    public boolean isGlobal()
    {
        return this instanceof Global;
    }


    @NotNull
    public Call asCall()
    {
        return (Call) this;
    }


    @NotNull
    public Module asModule()
    {
        return (Module) this;
    }


    @NotNull
    public ClassDef asClassDef()
    {
        return (ClassDef) this;
    }


    @NotNull
    public FunctionDef asFunctionDef()
    {
        return (FunctionDef) this;
    }


    @NotNull
    public Lambda asLambda()
    {
        return (Lambda) this;
    }


    @NotNull
    public Name asName()
    {
        return (Name) this;
    }


    @NotNull
    public Global asGlobal()
    {
        return (Global) this;
    }


    protected void addWarning(String msg)
    {
        Indexer.idx.putProblem(this, msg);
    }


    protected void addError(String msg)
    {
        Indexer.idx.putProblem(this, msg);
    }


    /**
     * Utility method to resolve every node in {@code nodes} and
     * return the union of their types.  If {@code nodes} is empty or
     * {@code null}, returns a new {@link org.yinwang.pysonar.types.UnknownType}.
     */
    @Nullable
    protected Type resolveListAsUnion(@Nullable List<? extends Node> nodes, Scope s)
    {
        if (nodes == null || nodes.isEmpty())
        {
            return Indexer.idx.builtins.unknown;
        }

        Type result = null;
        for (Node node : nodes)
        {
            Type nodeType = resolveExpr(node, s);
            if (result == null)
            {
                result = nodeType;
            }
            else
            {
                result = UnionType.union(result, nodeType);
            }
        }
        return result;
    }


    /**
     * Resolves each element of a node list in the passed scope.
     * Node list may be empty or {@code null}.
     */
    static protected void resolveList(@Nullable List<? extends Node> nodes, Scope s)
    {
        if (nodes != null)
        {
            for (Node n : nodes)
            {
                resolveExpr(n, s);
            }
        }
    }


    @Nullable
    static protected List<Type> resolveAndConstructList(@Nullable List<? extends Node> nodes, Scope s)
    {
        if (nodes == null)
        {
            return null;
        }
        else
        {
            List<Type> typeList = new ArrayList<>();
            for (Node n : nodes)
            {
                typeList.add(resolveExpr(n, s));
            }
            return typeList;
        }
    }


    public String toDisplay()
    {
        return "";
    }


    public abstract void visit(NodeVisitor visitor);


    protected void visitNode(@Nullable Node n, NodeVisitor v)
    {
        if (n != null)
        {
            n.visit(v);
        }
    }


    protected void visitNodeList(@Nullable List<? extends Node> nodes, NodeVisitor v)
    {
        if (nodes != null)
        {
            for (Node n : nodes)
            {
                if (n != null)
                {
                    n.visit(v);
                }
            }
        }
    }
}
