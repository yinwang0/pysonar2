package org.yinwang.pysonar.hash;


public class GenericEqualFunction extends EqualFunction {
    @Override
    public boolean equals(Object x, Object y) {
        return x.equals(y);
    }
}
