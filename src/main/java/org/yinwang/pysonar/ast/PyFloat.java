package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

public class PyFloat extends Node {

    public double value;

    public PyFloat(String s, String file, int start, int end, int line, int col) {
        super(NodeType.PYFLOAT, file, start, end, line, col);
        s = s.replaceAll("_", "");
        if (s.equals("inf")) {
            this.value = Double.POSITIVE_INFINITY;
        } else if (s.equals("-inf")) {
            this.value = Double.NEGATIVE_INFINITY;
        } else {
            this.value = Double.parseDouble(s);
        }
    }

    @NotNull
    @Override
    public String toString() {
        return "(float:" + value + ")";
    }

}
