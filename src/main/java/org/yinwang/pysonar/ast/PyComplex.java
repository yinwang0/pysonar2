package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

public class PyComplex extends Node {

    public double real;
    public double imag;

    public PyComplex(double real, double imag, String file, int start, int end, int line, int col) {
        super(NodeType.PYCOMPLEX, file, start, end, line, col);
        this.real = real;
        this.imag = imag;
    }

    @NotNull
    @Override
    public String toString() {
        return "(" + real + "+" + imag + "j)";
    }

}
