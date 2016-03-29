package org.yinwang.pysonar;

public class TypeStack {

    private Pair head;
    private TypeStack tail;
    public static final TypeStack EMPTY = new TypeStack(null, null);

    public TypeStack(Pair head, TypeStack tail) {
        this.head = head;
        this.tail = tail;
    }

    public TypeStack push(Object first, Object second) {
        return new TypeStack(new Pair(first, second), this);
    }

    public boolean contains(Object first, Object second) {
        if (this == EMPTY) {
            return false;
        } else if (head.equals(first, second)) {
            return true;
        } else {
            return tail.contains(first, second);
        }
    }
}
