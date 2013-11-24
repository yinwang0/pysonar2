package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.Binding;
import org.yinwang.pysonar.Builtins;
import org.yinwang.pysonar.Indexer;
import org.yinwang.pysonar.Scope;
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

    @Override
    public boolean bindsName() {
        return true;
    }

    @NotNull
    @Override
    public Type resolve(@NotNull Scope s) {
        ClassType classType = new ClassType(getName().getId(), s);
        List<Type> baseTypes = new ArrayList<>();
        for (Node base : bases) {
            Type baseType = resolveExpr(base, s);
            if (baseType.isClassType()) {
                classType.addSuper(baseType);
            } else if (baseType.isUnionType()) {
                for (Type b : baseType.asUnionType().getTypes()) {
                    classType.addSuper(b);
                    break;
                }
            } else {
                Indexer.idx.putProblem(base, base + " is not a class");
            }
            baseTypes.add(baseType);
        }

        // XXX: Not sure if we should add "bases", "name" and "dict" here. They
        // must be added _somewhere_ but I'm just not sure if it should be HERE.
        Builtins builtins = Indexer.idx.builtins;
        addSpecialAttribute(classType.getTable(), "__bases__", new TupleType(baseTypes));
        addSpecialAttribute(classType.getTable(), "__name__", builtins.BaseStr);
        addSpecialAttribute(classType.getTable(), "__dict__", new DictType(builtins.BaseStr, Indexer.idx.builtins.unknown));
        addSpecialAttribute(classType.getTable(), "__module__", builtins.BaseStr);
        addSpecialAttribute(classType.getTable(), "__doc__", builtins.BaseStr);

        // Bind ClassType to name here before resolving the body because the
        // methods need this type as self.
        NameBinder.bind(s, name, classType, Binding.Kind.CLASS);
        resolveExpr(body, classType.getTable());
        return Indexer.idx.builtins.Cont;
    }

    private void addSpecialAttribute(@NotNull Scope s, String name, Type proptype) {
        Binding b = s.update(name, Builtins.newTutUrl("classes.html"), proptype, Binding.Kind.ATTRIBUTE);
        b.markSynthetic();
        b.markStatic();
        b.markReadOnly();
    }

    @NotNull
    @Override
    public String toString() {
        return "<ClassDef:" + name.getId() + ":" + start + ">";
    }

    @Override
    public void visit(@NotNull NodeVisitor v) {
        if (v.visit(this)) {
            visitNode(name, v);
            visitNodeList(bases, v);
            visitNode(body, v);
        }
    }
}
