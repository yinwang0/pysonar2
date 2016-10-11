package org.yinwang.pysonar.types;

public class Types {
    public static Type ObjectClass = new ClassType("object", null, null);
    public static Type ObjectInstance = new InstanceType(ObjectClass);

    public static Type BoolClass = new ClassType("bool", null, ObjectClass);
    public static Type BoolInstance = new InstanceType(BoolClass);

    public static Type IntClass = new ClassType("int", null, ObjectClass);
    public static Type IntInstance = new InstanceType(IntClass);

    public static Type StrClass = new ClassType("str", null, ObjectClass);
    public static Type StrInstance = new InstanceType(StrClass);

    public static Type FloatClass = new ClassType("float", null, ObjectClass);
    public static Type FloatInstance = new InstanceType(FloatClass);

    public static Type ComplexClass = new ClassType("complex", null, ObjectClass);
    public static Type ComplexInstance = new InstanceType(ComplexClass);

    public static Type NoneClass = new ClassType("None", null, ObjectClass);
    public static Type NoneInstance = new InstanceType(NoneClass);

    public static Type UNKNOWN = new InstanceType(new ClassType("?", null, ObjectClass));

    // synthetic type used only for control flow purposes
    public static Type CONT = new InstanceType(new ClassType("None", null, null));
}
