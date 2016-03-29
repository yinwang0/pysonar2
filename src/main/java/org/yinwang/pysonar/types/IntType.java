package org.yinwang.pysonar.types;

import org.yinwang.pysonar.TypeStack;

public class IntType extends Type {

    @Override
    public boolean typeEquals(Object other, TypeStack typeStack) {
        return other instanceof IntType;
    }


    @Override
    protected String printType(Type.CyclicTypeRecorder ctr) {
        return "int";
    }

}
