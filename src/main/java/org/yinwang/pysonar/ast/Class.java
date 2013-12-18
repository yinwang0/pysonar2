package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.pysonar.*;
import org.yinwang.pysonar.types.ClassType;
import org.yinwang.pysonar.types.DictType;
import org.yinwang.pysonar.types.TupleType;
import org.yinwang.pysonar.types.Type;

import java.util.ArrayList;
import java.util.List;


public class Class extends Node {

    @Nullable
    public Name name;
    public List<Node> bases;
    public Node body;


    public Class(@Nullable Name name, List<Node> bases, Node body, int start, int end) {
        super(start, end);
        if (name != null) {
            this.name = name;
        } else {
            this.name = new Name(genClassName(), start, start + 1);
            addChildren(this.name);
        }
        this.bases = bases;
        this.body = body;
        addChildren(name, this.body);
        addChildren(bases);
    }


    @Override
    public boolean isClassDef() {
        return true;
    }


    @NotNull
    public Name getName() {
        return name;
    }


    private static int classCounter = 0;

    @NotNull
    public static String genClassName() {
        classCounter = classCounter + 1;
        return "class%" + classCounter;
    }


    @NotNull
    @Override
    public Type transform(@NotNull State s) {
        ClassType classType = new ClassType(getName().id, s);
        List<Type> baseTypes = new ArrayList<>();
        for (Node base : bases) {
            Type baseType = transformExpr(base, s);
            if (baseType.isClassType()) {
                classType.addSuper(baseType);
            } else if (baseType.isUnionType()) {
                for (Type b : baseType.asUnionType().getTypes()) {
                    classType.addSuper(b);
                    break;
                }
            } else {
                Analyzer.self.putProblem(base, base + " is not a class");
            }
            baseTypes.add(baseType);
        }

        // XXX: Not sure if we should add "bases", "name" and "dict" here. They
        // must be added _somewhere_ but I'm just not sure if it should be HERE.
        addSpecialAttribute(classType.getTable(), "__bases__", new TupleType(baseTypes));
        addSpecialAttribute(classType.getTable(), "__name__", Type.STR);
        addSpecialAttribute(classType.getTable(), "__dict__",
                new DictType(Type.STR, Type.UNKNOWN));
        addSpecialAttribute(classType.getTable(), "__module__", Type.STR);
        addSpecialAttribute(classType.getTable(), "__doc__", Type.STR);

        // Bind ClassType to name here before resolving the body because the
        // methods need this type as self.
        Binder.bind(s, name, classType, Binding.Kind.CLASS);
        if (body != null) {
            transformExpr(body, classType.getTable());
        }
        return Type.CONT;
    }


    private void addSpecialAttribute(@NotNull State s, String name, Type proptype) {
        Binding b = new Binding(name, Builtins.newTutUrl("classes.html"), proptype, Binding.Kind.ATTRIBUTE);
        s.update(name, b);
        b.markSynthetic();
        b.markStatic();
        b.markReadOnly();

    }


    @NotNull
    @Override
    public String toString() {
        return "(class:" + name.id + ":" + start + ")";
    }


    @Override
    public void visit(@NotNull NodeVisitor v) {
        if (v.visit(this)) {
            visitNode(name, v);
            visitNodes(bases, v);
            visitNode(body, v);
        }
    }
}
