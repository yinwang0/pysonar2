package org.yinwang.pysonar.ast;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class FunctionDef extends Node {

    public Name name;
    public List<Node> args;
    public List<Node> defaults;
    public Name vararg;  // *args
    public Name kwarg;   // **kwarg
    public List<Node> afterRest = null;   // after rest arg of Ruby
    public Node body;
    public boolean called = false;
    public boolean isLamba = false;
    public boolean isAsync = false;

    public FunctionDef(Name name, List<Node> args, Node body, List<Node> defaults,
        Name vararg, Name kwarg, String file, boolean isAsync, int start, int end) {
        super(NodeType.FUNCTIONDEF, file, start, end);
        if (name != null) {
            this.name = name;
        } else {
            isLamba = true;
            String fn = genLambdaName();
            this.name = new Name(fn, file, start, start + "lambda".length());
            addChildren(this.name);
        }

        this.args = args;
        this.body = body;
        this.defaults = defaults;
        this.vararg = vararg;
        this.kwarg = kwarg;
        this.isAsync = isAsync;
        addChildren(name);
        addChildren(args);
        addChildren(defaults);
        addChildren(vararg, kwarg, this.body);
    }

    public String getArgumentExpr() {
        StringBuilder argExpr = new StringBuilder();
        argExpr.append("(");
        boolean first = true;

        for (Node n : args) {
            if (!first) {
                argExpr.append(", ");
            }
            first = false;
            argExpr.append(n.toDisplay());
        }

        if (vararg != null) {
            if (!first) {
                argExpr.append(", ");
            }
            first = false;
            argExpr.append("*" + vararg.toDisplay());
        }

        if (kwarg != null) {
            if (!first) {
                argExpr.append(", ");
            }
            argExpr.append("**" + kwarg.toDisplay());
        }

        argExpr.append(")");
        return argExpr.toString();
    }

    private static int lambdaCounter = 0;

    @NotNull
    public static String genLambdaName() {
        lambdaCounter = lambdaCounter + 1;
        return "lambda%" + lambdaCounter;
    }

    @NotNull
    @Override
    public String toString() {
        return "(func:" + start + ":" + name + ")";
    }

}
