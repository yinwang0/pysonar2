package org.yinwang.pysonar;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.ast.Node;
import org.yinwang.pysonar.types.Type;


/**
 * dummy node for locating purposes only
 * rarely used
 */
public class Dummy extends Node {

    public Dummy(String name, String file, int start, int end) {
        this.name = name;
        this.file = file;
        this.start = start;
        this.end = end;
    }


    @NotNull
    @Override
    protected Type transform(State s) {
        return Type.UNKNOWN;
    }

}
