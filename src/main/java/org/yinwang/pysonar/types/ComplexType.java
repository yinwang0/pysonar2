package org.yinwang.pysonar.types;

public class ComplexType extends Type {

    @Override
    public boolean typeEquals(Object other) {
        return other instanceof ComplexType;
    }


    @Override
    protected String printType(CyclicTypeRecorder ctr) {
        return "float";
    }

}
