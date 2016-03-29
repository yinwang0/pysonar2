package org.yinwang.pysonar.hash;


public class GenericHashFunction extends HashFunction {

    @Override
    public int hash(Object o) {
        return o.hashCode();
    }
}
