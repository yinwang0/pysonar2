package org.yinwang.pysonar.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.pysonar.State;
import org.yinwang.pysonar.ast.Node;
import org.yinwang.pysonar.visitor.TypeInferencer;

import java.util.ArrayList;
import java.util.List;

public class ClassType extends Type {

    public String name;
    public Type superclass;
    private InstanceType instance;

    public ClassType(@NotNull String name, @Nullable State parent) {
        this.name = name;
        this.setTable(new State(parent, State.StateType.CLASS));
        table.setType(this);
        if (parent != null) {
            table.setPath(parent.extendPath(name));
        } else {
            table.setPath(name);
        }
    }


    public ClassType(@NotNull String name, State parent, @Nullable Type superClass) {
        this(name, parent);
        if (superClass != null) {
            addSuper(superClass);
        }
    }


    public void setName(String name) {
        this.name = name;
    }


    public void addSuper(@NotNull Type superclass) {
        this.superclass = superclass;
        table.addSuper(superclass.table);
    }

    public InstanceType getInstance() {
        if (instance == null) {
            instance = new InstanceType(this);
        }
        return instance;
    }

    public InstanceType getInstance(List<Type> args, TypeInferencer inferencer, Node call) {
        if (instance == null) {
            List<Type> initArgs = args == null ? new ArrayList<>() : args;
            instance = new InstanceType(this, initArgs, inferencer, call);
        }
        return instance;
    }

    public void setInstance(InstanceType instance) {
        this.instance = instance;
    }

    @Override
    public boolean typeEquals(Object other) {
        return this == other;
    }

    @Override
    protected String printType(CyclicTypeRecorder ctr) {
        return "<" + name + ">";
    }
}
