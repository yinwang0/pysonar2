package org.yinwang.pysonar.types;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.TypeStack;

public class SymbolType extends Type {

    public String name;


    public SymbolType(@NotNull String name) {
        this.name = name;
    }


    @Override
    public boolean typeEquals(Object other, TypeStack typeStack) {
        if (other instanceof SymbolType) {
            return this.name.equals(((SymbolType) other).name);
        } else {
            return false;
        }
    }


    @Override
    protected String printType(CyclicTypeRecorder ctr) {
        return ":" + name;
    }
}
