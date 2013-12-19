package org.yinwang.pysonar;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.pysonar.ast.Name;
import org.yinwang.pysonar.ast.Node;
import org.yinwang.pysonar.ast.Str;


public class Ref implements Comparable<Object> {

    @NotNull
    private String name;
    @Nullable
    private String file;
    private int start;


    public Ref(@NotNull Node node) {
        file = node.getFile();
        if (file != null && file.startsWith(Analyzer.self.projectDir)) {
            file = file.substring(Analyzer.self.projectDir.length() + 1);
        }

        start = node.start;

        if (node instanceof Name) {
            Name n = ((Name) node);
            name = n.id;
        } else if (node instanceof Str) {
            name = ((Str) node).value;
        } else {
            throw new IllegalArgumentException("Only accept Name and Str, but got:" + node);
        }
    }


    public Ref(@NotNull String name, String file, int start) {
        this.name = name;
        this.file = file;
        this.start = start;
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


    public int length() {
        return name.length();
    }


    @NotNull
    @Override
    public String toString() {
        return "(ref:" + file + ":" + name + ":" + start + ")";
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
    public int compareTo(@NotNull Object o) {
        if (o instanceof Ref) {
            return start - ((Ref) o).start;
        } else {
            return -1;
        }
    }


    @Override
    public int hashCode() {
        return ("" + file + start).hashCode();
    }
}
