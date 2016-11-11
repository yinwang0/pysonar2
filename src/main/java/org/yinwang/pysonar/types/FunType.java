package org.yinwang.pysonar.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.pysonar.Analyzer;
import org.yinwang.pysonar.State;
import org.yinwang.pysonar.ast.FunctionDef;
import org.yinwang.pysonar.hash.MyHashMap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class FunType extends Type {

    private static final int MAX_ARROWS = 10;

    @NotNull
    public Map<Type, Type> arrows = new MyHashMap<>();
    public FunctionDef func;
    @Nullable
    public ClassType cls = null;
    public State env;
    public List<Type> defaultTypes;       // types for default parameters (evaluated at def time)


    public FunType() {
    }


    public FunType(FunctionDef func, State env) {
        this.func = func;
        this.env = env;
    }


    public FunType(Type from, Type to) {
        addMapping(from, to);
        table.addSuper(Analyzer.self.builtins.BaseFunction.table);
        table.setPath(Analyzer.self.builtins.BaseFunction.table.path);
    }


    public void addMapping(Type from, Type to) {
        if (arrows.size() < MAX_ARROWS) {
            arrows.put(from, to);
        }
    }

    public void removeMapping(Type from)
    {
        arrows.remove(from);
    }

    @Nullable
    public Type getMapping(@NotNull Type from) {
        return arrows.get(from);
    }

    public boolean oversized() {
        return arrows.size() >= MAX_ARROWS;
    }

    public Type getReturnType() {
        if (!arrows.isEmpty()) {
            return arrows.values().iterator().next();
        } else {
            return Types.UNKNOWN;
        }
    }


    public void setCls(ClassType cls) {
        this.cls = cls;
    }


    public void setDefaultTypes(List<Type> defaultTypes) {
        this.defaultTypes = defaultTypes;
    }


    @Override
    public boolean typeEquals(Object other) {
        if (other instanceof FunType) {
            FunType fo = (FunType) other;
            return fo.table.path.equals(table.path) || this == other;
        } else {
            return false;
        }
    }


    @Override
    public int hashCode() {
        return "FunType".hashCode();
    }


    private boolean subsumed(Type type1, Type type2) {
        return subsumedInner(type1, type2);
    }


    private boolean subsumedInner(Type type1, Type type2) {
        if (typeStack.contains(type1, type2)) {
            return true;
        }

        if (type1.isUnknownType() || type1 == Types.NoneInstance || type1.equals(type2)) {
            return true;
        }

        if (type1 instanceof TupleType && type2 instanceof TupleType) {
            List<Type> elems1 = ((TupleType) type1).eltTypes;
            List<Type> elems2 = ((TupleType) type2).eltTypes;

            if (elems1.size() == elems2.size()) {
                for (int i = 0; i < elems1.size(); i++) {
                    if (!subsumedInner(elems1.get(i), elems2.get(i))) {
                        return false;
                    }
                }
            }

            return true;
        }

        if (type1 instanceof ListType && type2 instanceof ListType) {
            return subsumedInner(((ListType) type1).toTupleType(), ((ListType) type2).toTupleType());
        }

        return false;
    }


    private Map<Type, Type> compressArrows(Map<Type, Type> arrows) {
        Map<Type, Type> ret = new HashMap<>();

        for (Map.Entry<Type, Type> e1 : arrows.entrySet()) {
            boolean subsumed = false;

            for (Map.Entry<Type, Type> e2 : arrows.entrySet()) {
                if (e1 != e2 && subsumed(e1.getKey(), e2.getKey())) {
                    subsumed = true;
                    break;
                }
            }

            if (!subsumed) {
                ret.put(e1.getKey(), e1.getValue());
            }
        }

        return ret;
    }


    // If the self type is set, use the self type in the display
    // This is for display purpose only, it may not be logically
    //   correct wrt some pathological programs
    private TupleType simplifySelf(TupleType from) {
        TupleType simplified = new TupleType();
        if (from.eltTypes.size() > 0) {
            if (cls != null) {
                simplified.add(cls.getInstance());
            } else {
                simplified.add(from.get(0));
            }
        }

        for (int i = 1; i < from.eltTypes.size(); i++) {
            simplified.add(from.get(i));
        }
        return simplified;
    }


    @Override
    protected String printType(@NotNull CyclicTypeRecorder ctr) {

        if (arrows.isEmpty()) {
            return "? -> ?";
        }

        StringBuilder sb = new StringBuilder();

        Integer num = ctr.visit(this);
        if (num != null) {
            sb.append("#").append(num);
        } else {
            int newNum = ctr.push(this);

            int i = 0;
            Set<String> seen = new HashSet<>();

            for (Map.Entry<Type, Type> e : arrows.entrySet()) {
                Type from = e.getKey();
                String as = from.printType(ctr) + " -> " + e.getValue().printType(ctr);

                if (!seen.contains(as)) {
                    if (i != 0) {
                        if (Analyzer.self.multilineFunType) {
                            sb.append("\n/ ");
                        } else {
                            sb.append(" / ");
                        }
                    }

                    sb.append(as);
                    seen.add(as);
                }

                i++;
            }

            if (ctr.isUsed(this)) {
                sb.append("=#").append(newNum).append(": ");
            }
            ctr.pop(this);
        }
        return sb.toString();
    }
}
