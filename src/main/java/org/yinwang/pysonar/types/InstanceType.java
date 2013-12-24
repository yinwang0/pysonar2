package org.yinwang.pysonar.types;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.State;
import org.yinwang.pysonar.ast.Call;

import java.util.List;


public class InstanceType extends Type {

    public Type classType;


    public InstanceType(@NotNull Type c) {
        table.setStateType(State.StateType.INSTANCE);
        table.addSuper(c.table);
        table.setPath(c.table.path);
        classType = c;
    }


    public InstanceType(@NotNull Type c, Call call, List<Type> args) {
        this(c);
        Type initFunc = table.lookupAttrType("__init__");

        if (initFunc != null && initFunc.isFuncType() && initFunc.asFuncType().func != null) {
            initFunc.asFuncType().setSelfType(this);
            Call.apply(initFunc.asFuncType(), args, null, null, null, call);
            initFunc.asFuncType().setSelfType(null);
        }
    }


    @Override
    public boolean equals(Object other) {
        if (other instanceof InstanceType) {
            InstanceType iother = (InstanceType) other;
            // for now ignore the case where an instance of the same class is modified
            if (classType.equals(iother.classType) &&
                    table.keySet().equals(iother.table.keySet()))
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
        return classType.asClassType().name;
    }
}
