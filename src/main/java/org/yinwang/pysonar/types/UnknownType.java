package org.yinwang.pysonar.types;

import org.jetbrains.annotations.NotNull;

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
    protected void printType(CyclicTypeRecorder ctr, @NotNull StringBuilder sb) {
        sb.append("?");
    }
    
}
