package org.yinwang.pysonar.types;

import org.yinwang.pysonar.TypeStack;

public class FloatType extends Type {

    @Override
    public boolean equals(Object other) {
        return other instanceof FloatType;
    }

    @Override
    public boolean typeEquals(Object other, TypeStack typeStack) {
        return other instanceof FloatType;
    }

    @Override
    protected String printType(CyclicTypeRecorder ctr) {
        return "float";
    }

}
