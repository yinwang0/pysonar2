package org.yinwang.pysonar;

import java.util.Objects;

public class Pair {
    public Object first;
    public Object second;

    public Pair(Object first, Object second) {
        this.first = first;
        this.second = second;
    }

    public boolean equals(Object first, Object second) {
        return this.first == first && this.second == second ||
               this.first == second && this.second == first;
    }
}
