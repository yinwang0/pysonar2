package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.*;
import org.yinwang.pysonar.types.ClassType;
import org.yinwang.pysonar.types.DictType;
import org.yinwang.pysonar.types.TupleType;
import org.yinwang.pysonar.types.Type;

import java.util.ArrayList;
import java.util.List;


public class ClassDef extends Node {

    @NotNull
    public Name name;
    public List<Node> bases;
    public Block body;


    public ClassDef(@NotNull Name name, List<Node> bases, Block body, int start, int end) {
        super(start, end);
        this.name = name;
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
        Builtins builtins = Analyzer.self.builtins;
        addSpecialAttribute(classType.getTable(), "__bases__", new TupleType(baseTypes));
        addSpecialAttribute(classType.getTable(), "__name__", builtins.BaseStr);
        addSpecialAttribute(classType.getTable(), "__dict__", new DictType(builtins.BaseStr, Analyzer.self.builtins.unknown));
        addSpecialAttribute(classType.getTable(), "__module__", builtins.BaseStr);
        addSpecialAttribute(classType.getTable(), "__doc__", builtins.BaseStr);

        // Bind ClassType to name here before resolving the body because the
        // methods need this type as self.
        Binder.bind(s, name, classType, Binding.Kind.CLASS);
        transformExpr(body, classType.getTable());
        return Analyzer.self.builtins.Cont;
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
        return "<ClassDef:" + name.id + ":" + start + ">";
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
