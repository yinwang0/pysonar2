package org.yinwang.pysonar.types;

public class Types {
    public static Type UNKNOWN = new InstanceType(new ClassType("?", null, null));
    public static Type CONT = new InstanceType(new ClassType("None", null, null));
    public static Type NONE = new InstanceType(new ClassType("None", null, null));
    public static Type STR = new StrType(null);
    public static Type IntClass = new ClassType("int", null, null);
    public static Type IntInstance = new InstanceType(IntClass);
    public static Type FLOAT = new FloatType();
    public static Type COMPLEX = new ComplexType();
    public static Type BOOL = new BoolType(BoolType.Value.Undecided);
}
