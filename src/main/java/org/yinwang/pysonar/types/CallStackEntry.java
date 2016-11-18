package org.yinwang.pysonar.types;

public class CallStackEntry
{
    public Type fun;
    public Type from;

    public CallStackEntry(Type fun, Type from)
    {
        this.fun = fun;
        this.from = from;
    }
}
