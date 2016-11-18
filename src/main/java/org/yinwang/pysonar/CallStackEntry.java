package org.yinwang.pysonar;

import org.yinwang.pysonar.types.FunType;
import org.yinwang.pysonar.types.Type;

public class CallStackEntry
{
    public FunType fun;
    public Type from;

    public CallStackEntry(FunType fun, Type from)
    {
        this.fun = fun;
        this.from = from;
    }
}
