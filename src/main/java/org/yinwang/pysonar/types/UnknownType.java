package org.yinwang.pysonar.types;

public class UnknownType extends Type {

    public UnknownType() { }


    @Override
    public boolean equals(Object other) {
        return (other instanceof UnknownType);
    }


    @Override
    public int hashCode() {
        return "UnknownType".hashCode();
    }


    @Override
    protected String printType(CyclicTypeRecorder ctr) {
        return "?";
    }
    
}
