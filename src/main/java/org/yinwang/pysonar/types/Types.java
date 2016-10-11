package org.yinwang.pysonar.types;

public class Types {
    public static InstanceType UNKNOWN = new InstanceType(new ClassType("?", null, null));
    public static InstanceType CONT = new InstanceType(new ClassType("None", null, null));
    public static InstanceType NONE = new InstanceType(new ClassType("None", null, null));
    public static StrType STR = new StrType(null);
    public static IntType INT = new IntType();
    public static FloatType FLOAT = new FloatType();
    public static ComplexType COMPLEX = new ComplexType();
    public static BoolType BOOL = new BoolType(BoolType.Value.Undecided);
}
