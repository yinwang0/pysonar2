package org.yinwang.pysonar;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.pysonar.ast.Attribute;
import org.yinwang.pysonar.ast.Name;
import org.yinwang.pysonar.ast.Node;
import org.yinwang.pysonar.ast.Str;


/**
 * Encapsulates information about a binding reference.
 */
public class Ref {

    private static final int ATTRIBUTE = 0x1;
    private static final int CALL = 0x2;    // function/method call
    private static final int NEW = 0x4;     // instantiation
    private static final int STRING = 0x8;  // source node is a String

    private int start;
    @Nullable
    private String file;
    @NotNull
    private String name;
    private int flags;


    public Ref(@NotNull Node node) {
        file = node.getFile();
        start = node.start;

        if (node instanceof Name) {
            Name n = ((Name) node);
            name = n.getId();
            if (n.isCall()) {
                markAsCall();
            }
        } else if (node instanceof Str) {
            markAsString();
            name = ((Str) node).getStr();
        } else {
            throw new IllegalArgumentException("I don't know what " + node + " is.");
        }

        Node parent = node.getParent();
        if ((parent instanceof Attribute)
                && node == ((Attribute) parent).attr)
        {
            markAsAttribute();
        }
    }


    public Ref(@NotNull String path, int offset, @NotNull String text) {
        file = path;
        start = offset;
        name = text;
    }


    /**
     * Returns the file containing the reference.
     */
    @Nullable
    public String getFile() {
        return file;
    }


    /**
     * Returns the text of the reference.
     */
    @NotNull
    public String getName() {
        return name;
    }


    public int start() {
        return start;
    }


    public int end() {
        return start + length();
    }


    /**
     * Returns the length of the reference text.
     */
    public int length() {
        return isString() ? name.length() + 2 : name.length();
    }


    /**
     * Returns {@code true} if this reference was unquoted name.
     */
    public boolean isName() {
        return !isString();
    }


    /**
     * Returns {@code true} if this reference was an attribute
     * of some other node.
     */
    public boolean isAttribute() {
        return (flags & ATTRIBUTE) != 0;
    }


    public void markAsAttribute() {
        flags |= ATTRIBUTE;
    }


    /**
     * Returns {@code true} if this reference was a quoted name.
     * If so, the {@link #start} and {@link #length} include the positions
     * of the opening and closing quotes, but {@link #isName} returns the
     * text within the quotes.
     */
    public boolean isString() {
        return (flags & STRING) != 0;
    }


    public void markAsString() {
        flags |= STRING;
    }


    /**
     * Returns {@code true} if this reference is a function or method call.
     */
    public boolean isCall() {
        return (flags & CALL) != 0;
    }


    /**
     * Returns {@code true} if this reference is a class instantiation.
     */
    public void markAsCall() {
        flags |= CALL;
        flags &= ~NEW;
    }


    public boolean isNew() {
        return (flags & NEW) != 0;
    }


    public void markAsNew() {
        flags |= NEW;
        flags &= ~CALL;
    }


    public boolean isRef() {
        return !(isCall() || isNew());
    }


    @NotNull
    @Override
    public String toString() {
        return "<Ref:" + file + ":" + name + ":" + start + ">";
    }


    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Ref)) {
            return false;
        } else {
            Ref ref = (Ref) obj;
            return (start == ref.start &&
                    (file == null && ref.file == null) ||
                    (file != null && ref.file != null && file.equals(ref.file)));
        }
    }


    @Override
    public int hashCode() {
        return ("" + file + start).hashCode();
    }
}
