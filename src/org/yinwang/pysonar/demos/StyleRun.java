package org.yinwang.pysonar.demos;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Represents a simple style run for purposes of source highlighting.
 */
public class StyleRun implements Comparable<StyleRun> {

    public enum Type {
        KEYWORD,
        COMMENT,
        STRING,
        DOC_STRING,
        IDENTIFIER,
        BUILTIN,
        NUMBER,
        CONSTANT,  // ALL_CAPS identifier
        FUNCTION,  // function name
        PARAMETER,  // function parameter
        LOCAL,  // local variable
        DECORATOR,  // function decorator
        CLASS,  // class name
        ATTRIBUTE,  // object attribute
        LINK,  // hyperlink
        ANCHOR,  // name anchor
        DELIMITER,
        TYPE_NAME,  // reference to a type (e.g. function or class name)
        // diagnostics
        ERROR,
        WARNING,
        INFO
    }

    public Type type;
    private int offset;  // file offset
    private int length;  // style run length

    public String message;  // optional hover text
    @Nullable
    public String url;  // internal or external link
    @Nullable
    public String id;   // for hover highlight
    public List<String> highlight;   // for hover highlight

    public StyleRun(Type type, int offset, int length) {
        this.type = type;
        this.offset = offset;
        this.length = length;
    }


    public int start() {
        return offset;
    }

    public int end() {
        return offset + length;
    }

    public int length() {
        return length;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof StyleRun)) {
            return false;
        }
        StyleRun other = (StyleRun)o;
        return other.type == this.type
                && other.offset == this.offset
                && other.length == this.length
                && equalFields(other.message, this.message)
                && equalFields(other.url, this.url);
    }

    private boolean equalFields(@Nullable Object o1, @Nullable Object o2) {
        if (o1 == null) {
            return o2 == null;
        } else {
            return o1.equals(o2);
        }
    }

    public int compareTo(@NotNull StyleRun other) {
        if (this.equals(other)) {
            return 0;
        }
        if (this.offset < other.offset) {
            return -1;
        }
        if (other.offset < this.offset) {
            return 1;
        }
        return this.hashCode() - other.hashCode();
    }

    @NotNull
    @Override
    public String toString() {
        return "[" + type + " beg=" + offset + " len=" + length + "]";
    }
}
