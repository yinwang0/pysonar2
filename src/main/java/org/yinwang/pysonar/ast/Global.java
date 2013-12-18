package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Analyzer;
import org.yinwang.pysonar.State;
import org.yinwang.pysonar.types.Type;

import java.util.List;


public class Global extends Node {

    private List<Name> names;


    public Global(List<Name> names, int start, int end) {
        super(start, end);
        this.names = names;
        addChildren(names);
    }


    @NotNull
    @Override
    public Type transform(State s) {
        // Do nothing here because global names are processed by NBlock
        return Type.CONT;
    }


    public List<Name> getNames() {
        return names;
    }


    @NotNull
    @Override
    public String toString() {
        return "<Global:" + names + ">";
    }

}
