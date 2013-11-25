package org.yinwang.pysonar;

import org.jetbrains.annotations.NotNull;
import org.yinwang.pysonar.ast.Node;
import org.yinwang.pysonar.types.Type;

import java.util.HashSet;
import java.util.Set;


public class CallStack
{

    //    private Map<Node, Set<Type>> stack = new HashMap<Node, Set<Type>>();
    @NotNull
    private Set<Node> stack = new HashSet<Node>();


    public void push(Node call, Type type)
    {
//        Set<Type> inner = stack.get(call);
//        if (inner != null) {
//            inner.add(type);
//        } else {
//            inner = new HashSet<Type>();
//            inner.add(type);
//            stack.put(call, inner);
//        }
        stack.add(call);
    }


    public void pop(Node call, Type type)
    {
//        Set<Type> inner = stack.get(call);
//        if (inner != null) {
//            inner.remove(type);
//            if (inner.isEmpty()) {
//                stack.remove(call);
//            }
//        }
        stack.remove(call);
    }


    public boolean contains(Node call, Type type)
    {
//        Set<Type> inner = stack.get(call);
//        if (inner != null) {
//            return inner.contains(type);
//        } else {
//            return false;
//        }
        return stack.contains(call);
    }
}
