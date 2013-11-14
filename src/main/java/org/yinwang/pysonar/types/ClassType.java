package org.yinwang.pysonar.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.pysonar.Scope;

public class ClassType extends Type {

    private String name;
    private InstanceType canon;
    private Type superclass;

    public ClassType() {
        this("<unknown>", null);
    }


    @Override
    public boolean equals(Object other) {
        if (other instanceof ClassType) {
            ClassType co = (ClassType) other;
            return getTable().getPath().equals(co.getTable().getPath());
        } else {
            return this == other;
        }
    }


    public ClassType(@NotNull String name, @Nullable Scope parent) {
        this.name = name;
        this.setTable(new Scope(parent, Scope.ScopeType.CLASS));
        this.getTable().setType(this);
        if (parent != null) {
            this.getTable().setPath(parent.extendPath(name));
        } else {
            this.getTable().setPath(name);
        }
    }

    public ClassType(@NotNull String name, Scope parent, @Nullable ClassType superClass) {
        this(name, parent);
        if (superClass != null) {
            addSuper(superClass);
        }
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    public void addSuper(@NotNull Type superclass) {
        this.superclass = superclass;
        getTable().addSuper(superclass.getTable());
    }
    
    public InstanceType getCanon() {
        if (canon == null) {
            canon = new InstanceType(this, null, null, 0);
        }
        return canon;
    }


    @Override
    public int hashCode() {
        return "ClassType".hashCode();
    }


    // XXX: Type equality for ClassType is now object identity, because classes
    // can have have multiple definition sites so they shouldn't be considered
    // identical even if they have the
    // same path name (qname). NInstance type equality is now rigorously
    // defined.
    
    @Override
    protected String printType(CyclicTypeRecorder ctr) {
        StringBuilder sb = new StringBuilder();
        sb.append("<").append(getName()).append(">");
        return sb.toString();
    }
}
