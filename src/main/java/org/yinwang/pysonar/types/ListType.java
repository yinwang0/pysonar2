package org.yinwang.pysonar.types;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Indexer;

import java.util.ArrayList;
import java.util.List;

public class ListType extends Type {

    private Type eltType;
    @NotNull
    public List<Type> positional = new ArrayList<Type>();
    @NotNull
    public List<Object> values = new ArrayList<Object>();

    public ListType() {
        this(Indexer.idx.builtins.unknown);
    }

    public ListType(Type elt0) {
        eltType = elt0;
        getTable().addSuper(Indexer.idx.builtins.BaseList.getTable());
        getTable().setPath(Indexer.idx.builtins.BaseList.getTable().getPath());
    }

    public void setElementType(Type eltType) {
      this.eltType = eltType;
    }

    /**
     * Returns the type of the elements.  You should wrap the result
     * with {@link UnknownType#follow} to get to the actual type.
     */
    public Type getElementType() {
      return eltType;
    }

    public void add(@NotNull Type another) {
        eltType = UnionType.union(eltType, another);
        positional.add(another);
    }

    public void addValue(Object v) {
        values.add(v);
    }

    public Type get(int i) {
        return positional.get(i);
    }

    @NotNull
    public TupleType toTupleType(int n) {
        TupleType ret = new TupleType();
        for (int i = 0; i < n; i++) {
            ret.add(eltType);
        }
        return ret;
    }


    @NotNull
    public TupleType toTupleType() {
        return new TupleType(positional);
    }


    @Override
    public boolean equals(Object other) {
        if (typeStack.contains(this, other)) {
            return true;
        } else if (other instanceof ListType) {
            ListType co = (ListType)other;
            typeStack.push(this, other);
            boolean ret = co.getElementType().equals(getElementType());
            typeStack.pop(this, other);
            return ret;
        } else {
            return false;
        }
    }


    @Override
    public int hashCode() {
        return "ListType".hashCode();
    }


    @Override
    protected void printType(@NotNull CyclicTypeRecorder ctr, @NotNull StringBuilder sb) {
        Integer num = ctr.visit(this);
        if (num != null) {
            sb.append("#").append(num);
        } else {
            ctr.push(this);
            sb.append("[");
            getElementType().printType(ctr, sb);
            sb.append("]");
            ctr.pop(this);
        }
    }

}
