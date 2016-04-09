package org.yinwang.pysonar.hash;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;


public class MyHashSet<E>
        extends AbstractSet<E>
        implements Set<E>
{
    private transient MyHashMap<E, Object> map;
    private static final Object PRESENT = new Object();


    public MyHashSet(HashFunction hashFunction, EqualFunction equalFunction) {
        map = new MyHashMap<>(hashFunction, equalFunction);
    }


    public MyHashSet(Collection<? extends E> c, HashFunction hashFunction, EqualFunction equalFunction) {
        map = new MyHashMap<>(Math.max((int) (c.size() / .75f) + 1, 16), hashFunction, equalFunction);
        addAll(c);
    }


    public MyHashSet(int initialCapacity, float loadFactor, HashFunction hashFunction, EqualFunction equalFunction) {
        map = new MyHashMap<>(initialCapacity, loadFactor, hashFunction, equalFunction);
    }


    public MyHashSet(int initialCapacity, HashFunction hashFunction, EqualFunction equalFunction) {
        map = new MyHashMap<>(initialCapacity, hashFunction, equalFunction);
    }


    public MyHashSet() {
        map = new MyHashMap<>(new GenericHashFunction(), new GenericEqualFunction());
    }


    public Iterator<E> iterator() {
        return map.keySet().iterator();
    }


    public int size() {
        return map.size();
    }


    public boolean isEmpty() {
        return map.isEmpty();
    }


    public boolean contains(Object o) {
        return map.containsKey(o);
    }


    public boolean add(E e) {
        return map.put(e, PRESENT) == null;
    }


    public boolean remove(Object o) {
        return map.remove(o) == PRESENT;
    }


    public void clear() {
        map.clear();
    }
}
