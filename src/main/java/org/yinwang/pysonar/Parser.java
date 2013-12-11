package org.yinwang.pysonar;


import org.yinwang.pysonar.ast.Node;

public abstract class Parser {
    abstract public Node parseFile(String filename);
    abstract public void close();
}
