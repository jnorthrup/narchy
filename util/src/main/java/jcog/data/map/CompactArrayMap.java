package jcog.data.map;

import jcog.TODO;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Function;


/** TODO more extensive use of the Atomic field updater */
public class CompactArrayMap<K, V> {

    private final static AtomicReferenceFieldUpdater<CompactArrayMap, Object[]> ITEMS = AtomicReferenceFieldUpdater.newUpdater(CompactArrayMap.class, Object[].class, "items");
    volatile Object[] items = null;

    public CompactArrayMap() {
    }

    public CompactArrayMap(K initialKey, V initialValue) {
        this(new Object[]{initialKey, initialValue});
    }

    public CompactArrayMap(Object[] initial) {
        this.items = initial;
    }

    public boolean containsValue(Object aValue) {
        throw new TODO();
    }

    public boolean containsKey(Object key) {
        return get(key) != null;
    }

    public V get(Object key) {
        Object[] a = items;
        if (a != null) {
            int s = a.length;
            for (int i = 0; i < s; ) {
                Object k = a[i];
                if (k != null) {
                    if (keyEquals(k, key))
                        return (V) a[i + 1];
                }
                i += 2;
            }
        }
        return null;
    }

    public int size() {
        Object[] i = this.items;
        return i.length / 2;
    }

    /**
     * returns previous value, or null if none - like Map.put
     */
    public V put(K key, V value) {
        synchronized (this) {
            Object[] a = items;
            if (a == null) {
                this.items = new Object[]{key, value};
            } else {
                int s = a.length;
                for (int i = 0; i < s; ) {
                    if (keyEquals(a[i], key)) {
                        Object e = a[i + 1];
                        a[i + 1] = value;
                        return (V) e;
                    }
                    i += 2;
                }
                a = Arrays.copyOf(a, s + 2);
                a[s++] = key;
                a[s] = value;
                this.items = a;
            }
        }
        return null;
    }

    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        V e = get(key);
        if (e != null)
            return e;

        V v = mappingFunction.apply(key);
        put(key, v);
        return v;
    }


    public boolean remove(Object object) {
        throw new UnsupportedOperationException("use removeKey");
    }

    public V removeKey(Object key) {
        throw new TODO();


    }

    /**
     * override for alternate equality test
     */
    boolean keyEquals(Object a, Object b) {
        return a.equals(b);
    }


    public void clear() {
        items = null;
    }

    public void clearExcept(K key) {

        V exist = get(key);
        clear();
        if (exist != null)
            put(key, exist);

    }

    public Object[] clearPut(K key, V value) {
        return ITEMS.getAndSet(this, new Object[]{key, value});
    }
}
