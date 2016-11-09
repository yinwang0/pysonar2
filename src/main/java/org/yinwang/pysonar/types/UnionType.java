package org.yinwang.pysonar.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class UnionType extends Type {

    public Set<Type> types;


    public UnionType() {
        this.types = new HashSet<>();
    }


    public UnionType(@NotNull Type... initialTypes) {
        this();
        for (Type nt : initialTypes) {
            addType(nt);
        }
    }


    public boolean isEmpty() {
        return types.isEmpty();
    }


    /**
     * Returns true if t1 == t2 or t1 is a union type that contains t2.
     */
    static public boolean contains(Type t1, Type t2) {
        if (t1 instanceof UnionType) {
            return ((UnionType) t1).contains(t2);
        } else {
            return t1.equals(t2);
        }
    }


    static public Type remove(Type t1, Type t2) {
        if (t1 instanceof UnionType) {
            Set<Type> types = new HashSet<>(((UnionType) t1).types);
            types.remove(t2);
            return UnionType.newUnion(types);
        } else if (t1 != Types.CONT && t1 == t2) {
            return Types.UNKNOWN;
        } else {
            return t1;
        }
    }


    @NotNull
    static public Type newUnion(@NotNull Collection<Type> types) {
        Type t = Types.UNKNOWN;
        for (Type nt : types) {
            t = union(t, nt);
        }
        return t;
    }


    public void setTypes(Set<Type> types) {
        this.types = types;
    }


    public void addType(@NotNull Type t) {
        if (t instanceof UnionType) {
            types.addAll(((UnionType) t).types);
        } else {
            types.add(t);
        }
    }


    public boolean contains(Type t) {
        return types.contains(t);
    }


    // take a union of two types
    // with preference: other > None > Cont > unknown
    @NotNull
    public static Type union(@NotNull Type u, @NotNull Type v) {
        if (u.equals(v)) {
            return u;
        } else if (u != Types.UNKNOWN && v == Types.UNKNOWN) {
            return u;
        } else if (v != Types.UNKNOWN && u == Types.UNKNOWN) {
            return v;
        } else if (u != Types.NoneInstance && v == Types.NoneInstance) {
            return u;
        } else if (v != Types.NoneInstance && u == Types.NoneInstance) {
            return v;
        } else if (u instanceof TupleType && v instanceof TupleType &&
                   ((TupleType) u).size() == ((TupleType) v).size()) {
            return union((TupleType) u, (TupleType) v);
        } else {
            return new UnionType(u, v);
        }
    }

    @NotNull
    public static Type union(@NotNull TupleType u, @NotNull TupleType v) {
        List<Type> types = new ArrayList<Type>();
        for (int i = 0; i < u.size(); i++) {
            types.add(union(u.get(i), v.get(i)));
        }
        return new TupleType(types);
    }

    public static Type union(Collection<Type> types) {
        Type result = Types.UNKNOWN;
        for (Type type: types) {
            result = UnionType.union(result, type);
        }
        return result;
    }

    public static Type union(Type... types) {
        return union(types);
    }

    @Nullable
    public Type firstUseful() {
        for (Type type : types) {
            if (!type.isUnknownType() && type != Types.NoneInstance) {
                return type;
            }
        }
        return null;
    }


    @Override
    public boolean typeEquals(Object other) {
        if (typeStack.contains(this, other)) {
            return true;
        } else if (other instanceof UnionType) {
            Set<Type> types1 = types;
            Set<Type> types2 = ((UnionType) other).types;
            if (types1.size() != types2.size()) {
                return false;
            } else {
                for (Type t : types2) {
                    if (!types1.contains(t)) {
                        return false;
                    }
                }
                for (Type t : types1) {
                    if (!types2.contains(t)) {
                        return false;
                    }
                }
                return true;
            }
        } else {
            return false;
        }
    }


    @Override
    public int hashCode() {
        return "UnionType".hashCode();
    }


    @Override
    protected String printType(@NotNull CyclicTypeRecorder ctr) {
        StringBuilder sb = new StringBuilder();

        Integer num = ctr.visit(this);
        if (num != null) {
            sb.append("#").append(num);
        } else {
            int newNum = ctr.push(this);
            List<String> typeStrings = types.stream().map(x->x.printType(ctr)).collect(Collectors.toList());
            Collections.sort(typeStrings);
            sb.append("{");
            sb.append(String.join(" | ", typeStrings));

            if (ctr.isUsed(this)) {
                sb.append("=#").append(newNum).append(":");
            }

            sb.append("}");
            ctr.pop(this);
        }

        return sb.toString();
    }

}
