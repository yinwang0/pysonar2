package org.yinwang.pysonar.ast;

/**
 * dummy node for locating purposes only
 * rarely used
 */
public class Dummy extends Node {

    public Dummy(String file, int start, int end) {
        super(NodeType.DUMMY, file, start, end);
    }

}
