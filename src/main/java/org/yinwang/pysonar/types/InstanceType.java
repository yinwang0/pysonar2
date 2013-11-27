package org.yinwang.pysonar.types;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.ast.Call;

import java.util.List;


public class InstanceType extends Type {

    private Type classType;


    public InstanceType(@NotNull Type c) {
        this.getTable().setScopeType(Scope.ScopeType.INSTANCE);
        this.getTable().addSuper(c.getTable());
        this.getTable().setPath(c.getTable().getPath());
        classType = c;
    }


    public InstanceType(@NotNull Type c, Call call, List<Type> args) {
        this(c);
        Type initFunc = this.getTable().lookupAttrType("__init__");
        if (initFunc != null && initFunc.isFuncType() && initFunc.asFuncType().getFunc() != null) {
            initFunc.asFuncType().setSelfType(this);
            Call.apply(initFunc.asFuncType(), args, null, null, null, call);
            initFunc.asFuncType().setSelfType(null);
        }
    }


    public Type getClassType() {
        return classType;
    }


    @Override
    public boolean equals(Object other) {
        return this == other;
    }


    @Override
    protected String printType(CyclicTypeRecorder ctr) {
        return getClassType().asClassType().getName();
    }

}
