package org.yinwang.pysonar.types;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.State;
import org.yinwang.pysonar.ast.Node;
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

    public InstanceType(@NotNull Type c, List<Type> args, TypeInferencer inferencer, Node call)
    {
        this(c);

        // call constructor
        Type initFunc = table.lookupAttrType("__init__");
        if (initFunc != null &&
            initFunc instanceof FunType &&
            ((FunType) initFunc).func != null)
        {
            inferencer.apply((FunType) initFunc, this, args, null, null, null, call);
        }

        if (classType instanceof ClassType)
        {
            ((ClassType) classType).setInstance(this);
        }
    }


    @Override
    public boolean typeEquals(Object other) {
        if (other instanceof InstanceType) {
            return classType.typeEquals(((InstanceType) other).classType);
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
