package org.tdf.common.util;

import java.util.*;


/**
 * wrap byte array hash set as set
 */
public class ByteArraySet implements Set<byte[]> {
    Set<HexBytes> delegate;

    public ByteArraySet() {
        this(new HashSet<HexBytes>());
    }

    public ByteArraySet(Collection<? extends byte[]> all) {
        this();
        addAll(all);
    }

    ByteArraySet(Set<HexBytes> delegate) {
        this.delegate = delegate;
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return delegate.contains(HexBytes.fromBytes((byte[]) o));
    }

    @Override
    public Iterator<byte[]> iterator() {
        return new Iterator<byte[]>() {

            Iterator<HexBytes> it = delegate.iterator();

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public byte[] next() {
                return it.next().getBytes();
            }

            @Override
            public void remove() {
                it.remove();
            }
        };
    }

    @Override
    public Object[] toArray() {
        byte[][] ret = new byte[size()][];

        HexBytes[] arr = delegate.toArray(new HexBytes[size()]);
        for (int i = 0; i < arr.length; i++) {
            ret[i] = arr[i].getBytes();
        }
        return ret;
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return (T[]) toArray();
    }

    @Override
    public boolean add(byte[] bytes) {
        return delegate.add(HexBytes.fromBytes(bytes));
    }

    @Override
    public boolean remove(Object o) {
        return delegate.remove(HexBytes.fromBytes((byte[]) o));
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object e : c)
            if (!contains(e))
                return false;
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends byte[]> c) {
        boolean ret = false;
        for (byte[] bytes : c) {
            ret |= add(bytes);
        }
        return ret;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        Objects.requireNonNull(c);
        boolean modified = false;
        Iterator<byte[]> it = iterator();
        while (it.hasNext()) {
            if (!c.contains(it.next())) {
                it.remove();
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean changed = false;
        for (Object el : c) {
            changed |= remove(el);
        }
        return changed;
    }

    @Override
    public void clear() {
        delegate.clear();
    }
}
