package org.yinwang.pysonar.types;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.State;
import org.yinwang.pysonar.ast.Call;

import java.util.List;


public class InstanceType extends Type {

    private Type classType;


    public InstanceType(@NotNull Type c) {
        this.getTable().setStateType(State.StateType.INSTANCE);
        this.getTable().addSuper(c.getTable());
        this.getTable().setPath(c.getTable().getPath());
        classType = c;
    }


    public InstanceType(@NotNull Type c, Call call, List<Type> args) {
        this(c);
        Type initFunc = getTable().lookupAttrType("__init__");

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
        if (other instanceof InstanceType) {
            InstanceType iother = (InstanceType) other;
            // for now ignore the case where an instance of the same class is modified
            if (classType.equals(iother.classType) &&
                    getTable().keySet().equals(iother.getTable().keySet()))
            {
                return true;
            }
        }
        return false;
    }


    @Override
    public int hashCode() {
        return classType.hashCode();
    }


    @Override
    protected String printType(CyclicTypeRecorder ctr) {
        return getClassType().asClassType().getName();
    }
}
