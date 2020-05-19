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


    @Override
    public Iterator<E> iterator() {
        return map.keySet().iterator();
    }


    @Override
    public int size() {
        return map.size();
    }


    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }


    @Override
    public boolean contains(Object o) {
        return map.containsKey(o);
    }


    @Override
    public boolean add(E e) {
        return map.put(e, PRESENT) == null;
    }


    @Override
    public boolean remove(Object o) {
        return map.remove(o) == PRESENT;
    }


    @Override
    public void clear() {
        map.clear();
    }
}
