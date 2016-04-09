package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

public class PyComplex extends Node {

    public double real;
    public double imag;

    public PyComplex(double real, double imag, String file, int start, int end) {
        super(NodeType.PYCOMPLEX, file, start, end);
        this.real = real;
        this.imag = imag;
    }

    @NotNull
    @Override
    public String toString() {
        return "(" + real + "+" + imag + "j)";
    }

}
