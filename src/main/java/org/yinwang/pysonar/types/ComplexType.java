package org.yinwang.pysonar.types;

import org.yinwang.pysonar.TypeStack;

public class ComplexType extends Type {

    @Override
    public boolean typeEquals(Object other, TypeStack typeStack) {
        return other instanceof ComplexType;
    }


    @Override
    protected String printType(CyclicTypeRecorder ctr) {
        return "float";
    }

}
