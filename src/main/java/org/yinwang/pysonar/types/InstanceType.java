package org.yinwang.pysonar.types;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Indexer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.ast.Call;

import java.util.List;


public class InstanceType extends Type {

    private Type classType;


    public InstanceType() {
        classType = Indexer.idx.builtins.unknown;
    }


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


    /*
     * Instances are equal only if: 
     * 1) They are instantiated from the same class.
     * 2) They have the same attributes added to their table, 
     *     or one of them is the canonical instance for the class.
     */
    @Override
    public boolean equals(Object other) {
        if (other instanceof InstanceType) {
            InstanceType type2 = (InstanceType) other;
            return type2.getClassType().equals(getClassType());
//            if (type2.getClassType().equals(getClassType())) {
//                return (type2.getTable().keySet().containsAll(getTable().keySet()) &&
//                        getTable().keySet().containsAll(type2.getTable().keySet()));
//            } else {
//                return false;
//            }
        } else {
            return this == other;
        }
    }


    @Override
    public int hashCode() {
        return "InstanceType".hashCode();
    }


    @Override
    protected String printType(CyclicTypeRecorder ctr) {
        return getClassType().asClassType().getName();
    }

}
