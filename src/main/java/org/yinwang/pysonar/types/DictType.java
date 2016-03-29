package org.yinwang.pysonar.types;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Analyzer;
import org.yinwang.pysonar.TypeStack;

public class DictType extends Type {

    public Type keyType;
    public Type valueType;

    public DictType(Type key0, Type val0) {
        keyType = key0;
        valueType = val0;
        table.addSuper(Analyzer.self.builtins.BaseDict.table);
        table.setPath(Analyzer.self.builtins.BaseDict.table.path);
    }

    public void add(@NotNull Type key, @NotNull Type val) {
        keyType = UnionType.union(keyType, key);
        valueType = UnionType.union(valueType, val);
    }

    @NotNull
    public TupleType toTupleType(int n) {
        TupleType ret = new TupleType();
        for (int i = 0; i < n; i++) {
            ret.add(keyType);
        }
        return ret;
    }

    @Override
    public boolean typeEquals(Object other, TypeStack typeStack) {
        if (typeStack.contains(this, other)) {
            return true;
        } else if (other instanceof DictType) {
            TypeStack extended = typeStack.push(this, other);
            DictType co = (DictType) other;
            return co.keyType.typeEquals(keyType, extended) &&
                   co.valueType.typeEquals(valueType, extended);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return "DictType".hashCode();
    }

    @Override
    protected String printType(@NotNull CyclicTypeRecorder ctr) {
//        StringBuilder sb = new StringBuilder();
//
//        Integer num = ctr.visit(this);
//        if (num != null) {
//            sb.append("#").append(num);
//        } else {
//            ctr.push(this);
//            sb.append("{");
//            sb.append(keyType.printType(ctr));
//            sb.append(" : ");
//            sb.append(valueType.printType(ctr));
//            sb.append("}");
//            ctr.pop(this);
//        }
//
//        return sb.toString();
        return "dict";
    }

}
