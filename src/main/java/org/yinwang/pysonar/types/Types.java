package org.yinwang.pysonar.types;

public class Types {
    public static ClassType ObjectClass = new ClassType("object", null, null);
    public static Type ObjectInstance = ObjectClass.getInstance();

    public static ClassType TypeClass = new ClassType("type", null, null);
    public static Type TypeInstance = TypeClass.getInstance();

    public static ClassType BoolClass = new ClassType("bool", null, ObjectClass);
    public static Type BoolInstance = BoolClass.getInstance();

    public static ClassType IntClass = new ClassType("int", null, ObjectClass);
    public static Type IntInstance = IntClass.getInstance();

    public static ClassType LongClass = new ClassType("long", null, ObjectClass);
    public static Type LongInstance = LongClass.getInstance();

    public static ClassType StrClass = new ClassType("str", null, ObjectClass);
    public static Type StrInstance = StrClass.getInstance();

    public static ClassType FloatClass = new ClassType("float", null, ObjectClass);
    public static Type FloatInstance = FloatClass.getInstance();

    public static ClassType ComplexClass = new ClassType("complex", null, ObjectClass);
    public static Type ComplexInstance = ComplexClass.getInstance();

    public static ClassType NoneClass = new ClassType("None", null, ObjectClass);
    public static Type NoneInstance = NoneClass.getInstance();

    // Synthetic types used only for inference purposes
    // They don't exist in Python
    public static Type UNKNOWN = new InstanceType(new ClassType("?", null, ObjectClass));
    public static Type CONT = new InstanceType(new ClassType("None", null, null));

    public static ClassType BaseDict = new ClassType("dict", null, ObjectClass);
}
