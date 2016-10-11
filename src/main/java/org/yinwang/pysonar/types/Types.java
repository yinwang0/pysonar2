package org.yinwang.pysonar.types;

public class Types {
    public static ClassType ObjectClass = new ClassType("object", null, null);
    public static Type ObjectInstance = ObjectClass.getCanon();

    public static ClassType TypeClass = new ClassType("type", null, null);
    public static Type TypeInstance = TypeClass.getCanon();

    public static ClassType BoolClass = new ClassType("bool", null, ObjectClass);
    public static Type BoolInstance = BoolClass.getCanon();

    public static ClassType IntClass = new ClassType("int", null, ObjectClass);
    public static Type IntInstance = IntClass.getCanon();

    public static ClassType LongClass = new ClassType("long", null, ObjectClass);
    public static Type LongInstance = LongClass.getCanon();

    public static ClassType StrClass = new ClassType("str", null, ObjectClass);
    public static Type StrInstance = StrClass.getCanon();

    public static ClassType FloatClass = new ClassType("float", null, ObjectClass);
    public static Type FloatInstance = FloatClass.getCanon();

    public static ClassType ComplexClass = new ClassType("complex", null, ObjectClass);
    public static Type ComplexInstance = ComplexClass.getCanon();

    public static ClassType NoneClass = new ClassType("None", null, ObjectClass);
    public static Type NoneInstance = NoneClass.getCanon();

    // Synthetic types used only for inference purposes
    // They don't exist in Python
    public static Type UNKNOWN = new InstanceType(new ClassType("?", null, ObjectClass));
    public static Type CONT = new InstanceType(new ClassType("None", null, null));
}
