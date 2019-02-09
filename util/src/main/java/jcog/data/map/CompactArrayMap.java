package jcog.data.map;

import jcog.TODO;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Function;


/** TODO more extensive use of the Atomic field updater */
public class CompactArrayMap<K, V> {

    private final static AtomicReferenceFieldUpdater<CompactArrayMap, Object[]> ITEMS = AtomicReferenceFieldUpdater.newUpdater(CompactArrayMap.class, Object[].class, "items");
    private static final VarHandle AA = MethodHandles.arrayElementVarHandle(Object[].class);

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
        Object[] a = ITEMS.get(this);
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
        Object[] i = ITEMS.get(this);
        return i.length / 2;
    }

    /**
     * returns previous value, or null if none - like Map.put
     */
    public V put(K key, V value) {
        Object[] existing = new Object[1];
        ITEMS.accumulateAndGet(this, new Object[]{key, value}, (a, incoming) -> {
            if (a == null) {
                return incoming;
            } else {
                int s = a.length;
                Object k = incoming[0];
                for (int i = 0; i < s; i += 2) {
                    if (keyEquals(k, AA.get(a, i, a[i]))) {
                        existing[0] = AA.getAndSet(a, i + 1, incoming[1]);
                        return a;
                    }
                }
                Object[] b = Arrays.copyOf(a, s + 2);
                existing[0] = -1;
                b[s++] = k;
                b[s] = incoming[1];
                return b;
            }
        });
        return (V) existing[0];
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
        ITEMS.lazySet(this, null);
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
