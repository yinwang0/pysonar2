package org.yinwang.pysonar;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.pysonar.ast.*;


/**
 * Encapsulates information about a binding definition site.
 */
public class Def
{

    // Being frugal with fields here is good for memory usage.
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
    @Nullable
    private String name;
    @NotNull
    private Node node;


    public Def(@NotNull Node node, @NotNull Binding binding)
    {
        this.node = node;
        this.binding = binding;

        if (node instanceof Url)
        {
            String url = ((Url) node).getURL();
            if (url.startsWith("file://"))
            {
                fileOrUrl = url.substring("file://".length());
            }
            else
            {
                fileOrUrl = url;
            }
        }
        else
        {
            fileOrUrl = node.getFile();
            if (node instanceof Name)
            {
                name = node.asName().getId();
            }
        }

        initLocationInfo(node);
    }


    private void initLocationInfo(Node node)
    {
        start = node.start;
        end = node.end;

        Node parent = node.getParent();
        if ((parent instanceof FunctionDef && ((FunctionDef) parent).name == node) ||
                (parent instanceof ClassDef && ((ClassDef) parent).name == node))
        {
            bodyStart = parent.start;
            bodyEnd = parent.end;
            Str docstring = parent.docstring();
            if (docstring != null)
            {
                this.docstring = docstring.getStr();
                this.docstringStart = docstring.start;
                this.docstringEnd = docstring.end;
            }
        }
        else if (node instanceof Module)
        {
            name = ((Module) node).name;
            start = 0;
            end = 0;
            bodyStart = node.start;
            bodyEnd = node.end;

            Str docstring = node.docstring();
            if (docstring != null)
            {
                this.docstring = docstring.getStr();
                this.docstringStart = docstring.start;
                this.docstringEnd = docstring.end;
            }
        }
        else
        {
            bodyStart = node.start;
            bodyEnd = node.end;
        }

//        Util.msg("start: " + start + ", end: " + end + ", bodystart: " + bodyStart + ", bodyend: " + bodyEnd);

    }


    /**
     * Returns the name of the node.  Only applies if the definition coincides
     * with a {@link org.yinwang.pysonar.ast.Name} node.
     *
     * @return the name, or null
     */
    @Nullable
    public String getName()
    {
        return name;
    }


    /**
     * Returns the file if this node is from a source file, else {@code null}.
     */
    @Nullable
    public String getFile()
    {
        return isURL() ? null : fileOrUrl;
    }


    /**
     * Returns the URL if this node is from a URL, else {@code null}.
     */
    @Nullable
    public String getURL()
    {
        return isURL() ? fileOrUrl : null;
    }


    /**
     * Returns the file if from a source file, else the URL.
     */
    @Nullable
    public String getFileOrUrl()
    {
        return fileOrUrl;
    }


    /**
     * Returns {@code true} if this node is from a URL.
     */
    public boolean isURL()
    {
        return fileOrUrl != null && fileOrUrl.startsWith("http://");
    }


    public boolean isModule()
    {
        return binding.getKind() == Binding.Kind.MODULE;
    }


    public int getStart()
    {
        return start;
    }


    public int getEnd()
    {
        return end;
    }


    public int getLength()
    {
        return end - start;
    }


    public int getBodyStart()
    {
        return bodyStart;
    }


    public int getBodyEnd()
    {
        return bodyEnd;
    }


    public boolean hasName()
    {
        return name != null;
    }


    void setBinding(Binding b)
    {
        binding = b;
    }


    @NotNull
    public Binding getBinding()
    {
        return binding;
    }


    @NotNull
    public Node getNode()
    {
        return node;
    }


    public void setNode(Node node)
    {
        this.node = node;
    }


    @NotNull
    @Override
    public String toString()
    {
        return "<Def:" + (name == null ? "" : name) +
                ":" + _.baseFileName(fileOrUrl) + ":" + start + ">";
    }


    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof Def))
        {
            return false;
        }
        else
        {
            Def def = (Def) obj;
            return (start == def.start
                    && end == def.end
                    && ((fileOrUrl == null && def.fileOrUrl == null)
                    || (fileOrUrl != null && def.fileOrUrl != null &&
                    fileOrUrl.equals(def.fileOrUrl))));
        }
    }


    @Override
    public int hashCode()
    {
        return ("" + fileOrUrl + start).hashCode();
    }

}
