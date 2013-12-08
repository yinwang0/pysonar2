package org.yinwang.pysonar.types;


import org.yinwang.pysonar.Analyzer;


public class NumType extends Type {
    public String typename;
    private double upper = Double.MAX_VALUE;
    private double lower = Double.MIN_VALUE;


    public NumType(String typename) {
        this.typename = typename;
    }


    public NumType(String typename, double value) {
        this.typename = typename;
        this.lower = this.upper = value;
    }


    public NumType(String typename, double lower, double upper) {
        this.typename = typename;
        this.lower = lower;
        this.upper = upper;
    }


    public NumType(NumType other) {
        this.typename = other.typename;
        this.lower = other.lower;
        this.upper = other.upper;
    }


    public static String mixName(String name1, String name2) {
        if (name1.equals("float") || name2.equals("float")) {
            return "float";
        } else {
            return "int";
        }
    }


    public static NumType add(NumType a, NumType b) {
        String typename = mixName(a.typename, b.typename);
        double lower = a.lower + b.lower;
        double upper = a.upper + b.upper;
        return new NumType(typename, lower, upper);
    }


    public static NumType sub(NumType a, NumType b) {
        String typename = mixName(a.typename, b.typename);
        double lower = a.lower - b.upper;
        double upper = a.upper - b.lower;
        return new NumType(typename, lower, upper);
    }


    public NumType negate() {
        return new NumType(typename, -upper, -lower);
    }


    public static NumType mul(NumType a, NumType b) {
        String typename = mixName(a.typename, b.typename);
        double lower = a.lower * b.lower;
        double upper = a.upper * b.upper;
        return new NumType(typename, lower, upper);
    }


    public static NumType div(NumType a, NumType b) {
        String typename = mixName(a.typename, b.typename);
        double lower = a.lower / b.upper;
        double upper = a.upper / b.lower;
        return new NumType(typename, lower, upper);
    }


    public boolean lt(NumType other) {
        return isFeasible() && this.upper < other.lower;
    }


    public boolean lt(double other) {
        return isFeasible() && this.upper < other;
    }


    public boolean gt(NumType other) {
        return isFeasible() && this.lower > other.upper;
    }


    public boolean gt(double other) {
        return isFeasible() && this.lower > other;
    }


    public boolean eq(NumType other) {
        return isActualValue() && other.isActualValue() && this.lower == other.lower;
    }


    public boolean isZero() {
        return isActualValue() && lower == 0;
    }


    public boolean isUpperBounded() {
        return upper != Double.MAX_VALUE;
    }


    public void setUpperUnbounded(boolean upperUnbounded) {
        this.upper = Double.MAX_VALUE;
    }


    public boolean isLowerBounded() {
        return lower != Double.MIN_VALUE;
    }


    public void setLowerUnbounded(boolean lowerUnbounded) {
        this.lower = Double.MIN_VALUE;
    }


    public boolean isActualValue() {
        return lower == upper;
    }


    public boolean isFeasible() {
        return lower <= upper;
    }


    public double getUpper() {
        return upper;
    }


    public void setUpper(double upper) {
        this.upper = upper;
    }


    public double getLower() {
        return lower;
    }


    public void setLower(double lower) {
        this.lower = lower;
    }


    public void setActual(double actual) {
        this.lower = this.upper = actual;
    }


//    @Override
//    public boolean equals(Object other) {
//        return other instanceof NumType;
//    }


    @Override
    protected String printType(CyclicTypeRecorder ctr) {
        StringBuilder sb = new StringBuilder(typename);

        if (Analyzer.self.debug) {
            if (lower == upper) {
                sb.append("(" + lower + ")");
            } else if (isLowerBounded() || isUpperBounded()) {
                sb.append("[");
                if (isLowerBounded()) {
                    sb.append(lower);
                } else {
                    sb.append("?");
                }
                sb.append("..");
                if (isUpperBounded()) {
                    sb.append(upper);
                } else {
                    sb.append("?");
                }
                sb.append("]");
            }
        }

        return sb.toString();
    }

}
