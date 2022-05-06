package org.tdf.common.util;

import lombok.AllArgsConstructor;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * wrap byte array wrapper hash map as byte array map
 */
public class ByteArrayMap<V> implements Map<byte[], V> {
    private final Map<HexBytes, V> delegate;

    public ByteArrayMap() {
        this.delegate = new HashMap<>();
    }

    public ByteArrayMap(Map<byte[], V> map) {
        this();
        putAll(map);
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
    public boolean containsKey(Object key) {
        return delegate.containsKey(HexBytes.fromBytes((byte[]) key));
    }

    @Override
    public boolean containsValue(Object value) {
        return delegate.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return delegate.get(HexBytes.fromBytes((byte[]) key));
    }

    @Override
    public V put(byte[] key, V value) {
        return delegate.put(HexBytes.fromBytes(key), value);
    }

    @Override
    public V remove(Object key) {
        return delegate.remove(HexBytes.fromBytes((byte[]) key));
    }

    @Override
    public void putAll(Map<? extends byte[], ? extends V> m) {
        for (Entry<? extends byte[], ? extends V> entry : m.entrySet()) {
            delegate.put(HexBytes.fromBytes(entry.getKey()), entry.getValue());
        }
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public Set<byte[]> keySet() {
        return new ByteArraySet(new SetAdapter<>(delegate));
    }

    @Override
    public Collection<V> values() {
        return delegate.values();
    }

    @Override
    public Set<Entry<byte[], V>> entrySet() {
        return new MapEntrySet<>(delegate.entrySet());
    }

    @Override
    public boolean equals(Object o) {
        return delegate.equals(o);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @AllArgsConstructor
    private static class BytesEntry<V> implements Map.Entry<byte[], V> {
        private final HexBytes wrapper;

        private V value;

        public BytesEntry(Map.Entry<HexBytes, V> entry) {
            super();
            this.wrapper = entry.getKey();
            this.value = entry.getValue();
        }

        @Override
        public byte[] getKey() {
            return wrapper.getBytes();
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            V old = this.value;
            this.value = value;
            return old;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            return wrapper.equals(((BytesEntry) o).wrapper) && value.equals(((BytesEntry) o).value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(wrapper, value);
        }
    }

    @AllArgsConstructor
    public static class MapEntrySet<V> implements Set<Entry<byte[], V>> {
        private final Set<Entry<HexBytes, V>> delegate;

        private Entry<HexBytes, V> mapToEntry(Object o) {
            return new HashMap.SimpleEntry<>(
                HexBytes.fromBytes(
                    ((Entry<byte[], V>) o).getKey()
                )
                , ((Entry<byte[], V>) o).getValue()
            );
        }

        private Stream<Entry<HexBytes, V>> mapToEntries(Collection<?> c) {
            return c.stream().map(this::mapToEntry);
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
            return delegate.contains(o);
        }

        @Override
        public Iterator<Entry<byte[], V>> iterator() {
            final Iterator<Entry<HexBytes, V>> it = delegate.iterator();
            return new Iterator<Entry<byte[], V>>() {

                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public Entry<byte[], V> next() {
                    Entry<HexBytes, V> next = it.next();
                    return new AbstractMap.SimpleImmutableEntry<>(next.getKey().getBytes(), next.getValue());
                }

                @Override
                public void remove() {
                    it.remove();
                }
            };
        }

        @Override
        public Object[] toArray() {
            return delegate
                .stream()
                .map(BytesEntry::new).toArray();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return (T[]) delegate
                .stream()
                .map(BytesEntry::new).toArray();
        }

        @Override
        public boolean remove(Object o) {
            return delegate.remove(mapToEntry(o));
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            for (Object o : c) {
                Entry<HexBytes, V> entry = mapToEntry(o);
                if (!delegate.contains(entry)) return false;
            }
            return true;
        }

        @Override
        public boolean add(Entry<byte[], V> entry) {
            return
                delegate.add(mapToEntry(entry));
        }

        @Override
        public boolean addAll(Collection<? extends Entry<byte[], V>> c) {
            return delegate.addAll(mapToEntries(c).collect(Collectors.toList()));
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return delegate.retainAll(mapToEntries(c).collect(Collectors.toList()));
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return delegate.removeAll(mapToEntries(c).collect(Collectors.toList()));
        }

        @Override
        public void clear() {
            delegate.clear();
        }
    }
}
