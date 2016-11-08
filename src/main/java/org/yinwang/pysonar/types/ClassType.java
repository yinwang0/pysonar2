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

    public InstanceType getCanon() {
        return new InstanceType(this);
    }

    public InstanceType getCanon(Node call, List<Type> args, TypeInferencer inferencer) {
        return new InstanceType(this, call, args == null ? new ArrayList<>() : args, inferencer);
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
