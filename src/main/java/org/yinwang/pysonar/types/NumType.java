package org.yinwang.pysonar.types;


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


    public boolean lt(NumType other) {
        return isFeasible() && this.upper < other.lower;
    }


    public boolean gt(NumType other) {
        return isFeasible() && this.lower > other.upper;
    }


    public boolean eq(NumType other) {
        return isActualValue() && other.isActualValue() && this.lower == other.lower;
    }


    @Override
    protected String printType(CyclicTypeRecorder ctr) {
        StringBuilder sb = new StringBuilder(typename);

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

        return sb.toString();
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
}
