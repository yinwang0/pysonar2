package org.yinwang.pysonar.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.pysonar.Analyzer;
import org.yinwang.pysonar.State;
import org.yinwang.pysonar.TypeStack;
import org.yinwang.pysonar._;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public abstract class Type {

    @Nullable
    public State table;
    public boolean mutated = false;

    public String file = null;

    public State trueState;
    public State falseState;


    @NotNull
    protected static TypeStack typeStack = new TypeStack();


    public Type() {
    }


    public void setTable(@NotNull State table) {
        this.table = table;
    }


    @NotNull
    public State getTable() {
        if (table == null) {
            table = new State(null, State.StateType.SCOPE);
        }
        return table;
    }


    public String getFile() {
        return file;
    }


    public void setFile(String file) {
        this.file = file;
    }


    public boolean isMutated() {
        return mutated;
    }


    public void setMutated(boolean mutated) {
        this.mutated = mutated;
    }


    /**
     * Returns {@code true} if this Python type is implemented in native code
     * (i.e., C, Java, C# or some other host language.)
     */
    public boolean isNative() {
        return Analyzer.self.builtins.isNative(this);
    }


    public boolean isBool() {
        return this instanceof BoolType;
    }


    public boolean isUndecidedBool() {
        return isBool() && asBool().getValue() == BoolType.Value.Undecided;
    }


    public BoolType asBool() {
        return (BoolType) this;
    }


    public boolean isClassType() {
        return this instanceof ClassType;
    }


    public boolean isDictType() {
        return this instanceof DictType;
    }


    public boolean isFuncType() {
        return this instanceof FunType;
    }


    public boolean isInstanceType() {
        return this instanceof InstanceType;
    }


    public boolean isListType() {
        return this instanceof ListType;
    }


    public boolean isModuleType() {
        return this instanceof ModuleType;
    }


    public boolean isNumType() {
        return this instanceof NumType;
    }


    public boolean isStrType() {
        return this == Analyzer.self.builtins.BaseStr;
    }


    public boolean isTupleType() {
        return this instanceof TupleType;
    }


    public boolean isUnionType() {
        return this instanceof UnionType;
    }


    public boolean isUnknownType() {
        return this == Analyzer.self.builtins.unknown;
    }


    @NotNull
    public ClassType asClassType() {
        return (ClassType) this;
    }


    @NotNull
    public DictType asDictType() {
        return (DictType) this;
    }


    @NotNull
    public NumType asNumType() {
        return (NumType) this;
    }


    @NotNull
    public FunType asFuncType() {
        return (FunType) this;
    }


    @NotNull
    public InstanceType asInstanceType() {
        return (InstanceType) this;
    }


    @NotNull
    public ListType asListType() {
        return (ListType) this;
    }


    @NotNull
    public ModuleType asModuleType() {
        if (this.isUnionType()) {
            for (Type t : this.asUnionType().getTypes()) {
                if (t.isModuleType()) {
                    return t.asModuleType();
                }
            }
            _.die("Not containing a ModuleType");
            // can't get here, just to make the @NotNull annotation happy
            return new ModuleType(null, null, null);
        } else if (this.isModuleType()) {
            return (ModuleType) this;
        } else {
            _.die("Not a ModuleType");
            // can't get here, just to make the @NotNull annotation happy
            return new ModuleType(null, null, null);
        }
    }


    @NotNull
    public TupleType asTupleType() {
        return (TupleType) this;
    }


    @NotNull
    public UnionType asUnionType() {
        return (UnionType) this;
    }


    public boolean isTrue() {
        if (this == Analyzer.self.builtins.True) {
            return true;
        }
        if (this == Analyzer.self.builtins.False || this.isUndecidedBool()) {
            return false;
        }
        if (this.isNumType() && (this.asNumType().lt(0) || this.asNumType().gt(0))) {
            return true;
        }
        if (this.isNumType() && this.asNumType().isZero()) {
            return false;
        }
        if (this != Analyzer.self.builtins.None) {
            return true;
        }
        return false;
    }


    public boolean isFalse() {
        if (this == Analyzer.self.builtins.False) {
            return true;
        }
        if (this == Analyzer.self.builtins.True || this.isUndecidedBool()) {
            return false;
        }
        if (this.isNumType() && this.asNumType().isZero()) {
            return true;
        }
        if (this == Analyzer.self.builtins.None) {
            return true;
        }
        return false;
    }


    public State getTrueState() {
        return trueState;
    }


    public void setTrueState(State trueState) {
        this.trueState = trueState;
    }


    public State getFalseState() {
        return falseState;
    }


    public void setFalseState(State falseState) {
        this.falseState = falseState;
    }


    /**
     * Internal class to support printing in the presence of type-graph cycles.
     */
    protected class CyclicTypeRecorder {
        int count = 0;
        @NotNull
        private Map<Type, Integer> elements = new HashMap<>();
        @NotNull
        private Set<Type> used = new HashSet<>();


        public Integer push(Type t) {
            count += 1;
            elements.put(t, count);
            return count;
        }


        public void pop(Type t) {
            elements.remove(t);
            used.remove(t);
        }


        public Integer visit(Type t) {
            Integer i = elements.get(t);
            if (i != null) {
                used.add(t);
            }
            return i;
        }


        public boolean isUsed(Type t) {
            return used.contains(t);
        }
    }


    protected abstract String printType(CyclicTypeRecorder ctr);


    @NotNull
    @Override
    public String toString() {
        return printType(new CyclicTypeRecorder());
    }

}
