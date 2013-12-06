package org.yinwang.pysonar.types;


public class NumType extends Type {
    private boolean upperUnbounded = true;
    private boolean lowerUnbounded = true;
    private boolean actualValue = false;
    private double upper;
    private double lower;
    private double actual;
    public String typename;


    public NumType(String typename) {
        this.typename = typename;
    }


    public NumType(String typename, double value) {
        this.typename = typename;
        this.actual = value;
        this.actualValue = true;
    }


    public NumType(String typename, double lower, double upper) {
        this.typename = typename;
        this.upperUnbounded = false;
        this.lowerUnbounded = false;
        this.lower = lower;
        this.upper = upper;
    }


    @Override
    protected String printType(CyclicTypeRecorder ctr) {
        StringBuilder sb = new StringBuilder(typename);

        if (actualValue) {
            sb.append("(" + actual + ")");
        } else if (!(lowerUnbounded && upperUnbounded)) {
            sb.append("[");
            if (lowerUnbounded) {
                sb.append("?");
            } else {
                sb.append(lower);
            }
            sb.append("..");
            if (upperUnbounded) {
                sb.append("?");
            } else {
                sb.append(upper);
            }
            sb.append("]");
        }

        return sb.toString();
    }


    public boolean isUpperUnbounded() {
        return upperUnbounded;
    }


    public void setUpperUnbounded(boolean upperUnbounded) {
        this.upperUnbounded = upperUnbounded;
    }


    public boolean isLowerUnbounded() {
        return lowerUnbounded;
    }


    public void setLowerUnbounded(boolean lowerUnbounded) {
        this.lowerUnbounded = lowerUnbounded;
    }


    public boolean isActualValue() {
        return actualValue;
    }


    public void setActualValue(boolean actualValue) {
        this.actualValue = actualValue;
    }


    public double getUpper() {
        return upper;
    }


    public void setUpper(double upper) {
        this.upper = upper;
        this.upperUnbounded = false;
    }


    public double getLower() {
        return lower;
    }


    public void setLower(double lower) {
        this.lower = lower;
        this.lowerUnbounded = false;
    }


    public double getActual() {
        return actual;
    }


    public void setActual(double actual) {
        this.actual = actual;
    }
}
