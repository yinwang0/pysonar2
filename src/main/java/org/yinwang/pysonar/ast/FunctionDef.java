package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.pysonar.Binding;
import org.yinwang.pysonar.Indexer;
import org.yinwang.pysonar.Scope;
import org.yinwang.pysonar.types.FunType;
import org.yinwang.pysonar.types.Type;

import java.util.ArrayList;
import java.util.List;

public class FunctionDef extends Node {

    public Name name;
    public List<Node> args;
    public List<Node> defaults;
    @Nullable
    public List<Type> defaultTypes;
    public Name vararg;  // *args

    public Name kwarg;   // **kwarg
    public Node body;
    private List<Node> decoratorList;
    public boolean called = false;


    public FunctionDef(Name name, List<Node> args, Block body, List<Node> defaults,
                       Name vararg, Name kwarg, int start, int end) {
        super(start, end);
        this.name = name;
        this.args = args;
        this.body = body;
        this.defaults = defaults;
        this.vararg = vararg;
        this.kwarg = kwarg;
        addChildren(name);
        addChildren(args);
        addChildren(defaults);
        addChildren(vararg, kwarg, this.body);
    }

    public void setDecoratorList(List<Node> decoratorList) {
        this.decoratorList = decoratorList;
        addChildren(decoratorList);
    }

    public List<Node> getDecoratorList() {
        if (decoratorList == null) {
            decoratorList = new ArrayList<Node>();
        }
        return decoratorList;
    }

    @Override
    public boolean isFunctionDef() {
        return true;
    }

    @Override
    public boolean bindsName() {
        return true;
    }

    /**
     * Returns the name of the function for indexing/qname purposes.
     * Lambdas will return a generated name.
     */
    @Nullable
    protected String getBindingName(Scope s) {
        return name.getId();
    }

    public List<Node> getArgs() {
        return args;
    }

    public List<Node> getDefaults() {
        return defaults;
    }

    @Nullable
    public List<Type> getDefaultTypes() {
        return defaultTypes;
    }
    
    public Node getBody() {
        return body;
    }

    public Name getName() {
        return name;
    }
    
    /**
     * @return the vararg
     */
    public Name getVararg() {
        return vararg;
    }

    /**
     * @param vararg the vararg to set
     */
    public void setVararg(Name vararg) {
        this.vararg = vararg;
    }

    /**
     * @return the kwarg
     */
    public Name getKwarg() {
        return kwarg;
    }

    /**
     * @param kwarg the kwarg to set
     */
    public void setKwarg(Name kwarg) {
        this.kwarg = kwarg;
    }


    /**
     * A function's environment is not necessarily the enclosing scope. A
     * method's environment is the scope of the most recent scope that is not a
     * class.
     * 
     * Be sure to distinguish the environment and the symbol table. The
     * function's table is only used for the function's attributes like
     * "im_class". Its parent should be the table of the enclosing scope, and
     * its path should be derived from that scope too for locating the names
     * "lexically".
     */
    @NotNull
    @Override
    public Type resolve(@NotNull Scope outer) {
        resolveList(decoratorList, outer);   //XXX: not handling functional transformations yet
        FunType fun = new FunType(this, outer.getForwarding());
        fun.getTable().setParent(outer);
        fun.getTable().setPath(outer.extendPath(getName().getId()));
        fun.setDefaultTypes(resolveAndConstructList(defaults, outer));
        Indexer.idx.addUncalled(fun);
        Binding.Kind funkind;

        if (outer.getScopeType() == Scope.ScopeType.CLASS) {
            if ("__init__".equals(name.getId())) {
                funkind = Binding.Kind.CONSTRUCTOR;
            } else {
                funkind = Binding.Kind.METHOD;
            }
        } else {
            funkind = Binding.Kind.FUNCTION;
        }

        Type outType = outer.getType();
        if (outType != null && outType.isClassType()) {
            fun.setCls(outType.asClassType());
        }

        NameBinder.bind(outer, name, fun, funkind);
        return Indexer.idx.builtins.Cont;
    }

    @NotNull
    @Override
    public String toString() {
        return "<Function:" + start + ":" + name + ">";
    }

    @Override
    public void visit(@NotNull NodeVisitor v) {
        if (v.visit(this)) {
            visitNode(name, v);
            visitNodeList(args, v);
            visitNodeList(defaults, v);
            visitNode(kwarg, v);
            visitNode(vararg, v);
            visitNode(body, v);
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FunctionDef) {
            FunctionDef fo = (FunctionDef)obj;
            return (fo.getFile().equals(getFile()) && fo.start == start);
        } else {
            return false;
        }
    }

}
