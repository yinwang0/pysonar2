package org.yinwang.pysonar.types;


import org.yinwang.pysonar.Analyzer;


public class ComplexType extends Type {
    public double real;
    public double imag;


    public ComplexType() {
    }


    public ComplexType(double value) {
        this.imag = this.real = value;
    }


    public ComplexType(double imag, double real) {
        this.imag = imag;
        this.real = real;
    }


    public ComplexType(ComplexType other) {
        this.imag = other.imag;
        this.real = other.real;
    }


    public static ComplexType add(ComplexType a, ComplexType b) {
        return new ComplexType(a.imag + b.imag, a.real + b.real);
    }


    public static ComplexType sub(ComplexType a, ComplexType b) {
        return new ComplexType(a.imag - b.real, a.real - b.imag);
    }


    public ComplexType negate() {
        return new ComplexType(-real, -imag);
    }


    public static ComplexType mul(ComplexType a, ComplexType b) {
        double real = a.real * b.real - a.imag * b.imag;
        double imag = a.real * b.imag + a.imag * b.real;
        return new ComplexType(real, imag);
    }


    public boolean eq(ComplexType other) {
        return this.real == other.real && this.imag == other.imag;
    }


    public boolean isZero() {
        return real == 0 && imag == 0;
    }


    public void setReal(double real) {
        this.real = real;
    }


    public void setImag(double imag) {
        this.imag = imag;
    }


    @Override
    protected String printType(CyclicTypeRecorder ctr) {
        StringBuilder sb = new StringBuilder("complex");

        if (Analyzer.self.hasOption("debug")) {
            sb.append("(" + real + " + " + imag + "j)");
        }

        return sb.toString();
    }

}
