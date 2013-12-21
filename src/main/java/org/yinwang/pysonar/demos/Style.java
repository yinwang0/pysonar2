package org.yinwang.pysonar.demos;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;


/**
 * Represents a simple style run for purposes of source highlighting.
 */
public class Style implements Comparable<Style> {

    public enum Type {
        KEYWORD,
        COMMENT,
        STRING,
        DOC_STRING,
        IDENTIFIER,
        BUILTIN,
        NUMBER,
        CONSTANT,       // ALL_CAPS identifier
        FUNCTION,       // function name
        PARAMETER,      // function parameter
        LOCAL,          // local variable
        DECORATOR,      // function decorator
        CLASS,          // class name
        ATTRIBUTE,      // object attribute
        LINK,           // hyperlink
        ANCHOR,         // name anchor
        DELIMITER,
        TYPE_NAME,      // reference to a type (e.g. function or class name)

        ERROR,
        WARNING,
        INFO
    }


    public Type type;
    private int start;     // file offset
    private int end;     // style run length

    public String message;  // optional hover text
    @Nullable
    public String url;      // internal or external link
    @Nullable
    public String id;       // for hover highlight
    public List<String> highlight;   // for hover highlight


    public Style(Type type, int start, int end) {
        this.type = type;
        this.start = start;
        this.end = end;
    }


    public int start() {
        return start;
    }


    public int end() {
        return end;
    }


    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Style)) {
            return false;
        }
        Style other = (Style) o;
        return other.type == this.type
                && other.start == this.start
                && other.end == this.end
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


    public int compareTo(@NotNull Style other) {
        if (this.equals(other)) {
            return 0;
        } else if (this.start < other.start) {
            return -1;
        } else if (this.start > other.start) {
            return 1;
        } else {
            return this.hashCode() - other.hashCode();
        }
    }


    @NotNull
    @Override
    public String toString() {
        return "[" + type + " start=" + start + " end=" + end + "]";
    }
}
