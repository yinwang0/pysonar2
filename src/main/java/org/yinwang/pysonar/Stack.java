package org.yinwang.pysonar;

import java.util.ArrayList;
import java.util.List;

public class Stack<T>
{
    private List<T> content = new ArrayList<>();

    public void push(T item)
    {
        content.add(item);
    }

    public T top()
    {
        return content.get(content.size() - 1);
    }

    public T pop()
    {
        if (!content.isEmpty())
        {
            return content.remove(content.size() - 1);
        } else {
            return null;
        }
    }
}
