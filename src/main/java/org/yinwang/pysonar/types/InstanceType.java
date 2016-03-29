package org.yinwang.pysonar.types;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.State;
import org.yinwang.pysonar.TypeStack;
import org.yinwang.pysonar.ast.Call;
import org.yinwang.pysonar.visitor.TypeInferencer;

import java.util.List;


public class InstanceType extends Type {

    public Type classType;


    public InstanceType(@NotNull Type c) {
        table.setStateType(State.StateType.INSTANCE);
        table.addSuper(c.table);
        table.setPath(c.table.path);
        classType = c;
    }


    public InstanceType(@NotNull Type c, Call call, List<Type> args, TypeInferencer inferencer) {
        this(c);
        Type initFunc = table.lookupAttrType("__init__");

        if (initFunc != null && initFunc instanceof FunType && ((FunType) initFunc).func != null) {
            ((FunType) initFunc).setSelfType(this);
            inferencer.apply((FunType) initFunc, args, null, null, null, call);
            ((FunType) initFunc).setSelfType(null);
        }
    }


    @Override
    public boolean typeEquals(Object other, TypeStack typeStack) {
        if (other instanceof InstanceType) {
            return classType.equals(((InstanceType) other).classType);
        }
        return false;
    }


    @Override
    public int hashCode() {
        return classType.hashCode();
    }


    @Override
    protected String printType(CyclicTypeRecorder ctr) {
        return ((ClassType) classType).name;
    }
}
