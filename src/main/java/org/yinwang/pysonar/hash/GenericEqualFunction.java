package org.yinwang.pysonar.hash;


public class GenericEqualFunction extends EqualFunction {
    public boolean equals(Object x, Object y) {
        return x.equals(y);
    }
}
