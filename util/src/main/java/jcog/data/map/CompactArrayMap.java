package jcog.data.map;

import jcog.TODO;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Function;


/** compact thread-safe atomic Map implemented as an array of key,value pairs
 *  items are, by default, unsorted so access is sequential O(n) so this is
 *  limited to small size.
 *
 *  TODO make a key-sorted impl for faster access
 * */
public class CompactArrayMap<K, V> {

    private static final AtomicReferenceFieldUpdater<CompactArrayMap, Object[]> ITEMS = AtomicReferenceFieldUpdater.newUpdater(CompactArrayMap.class, Object[].class, "items");
    private static final VarHandle AA = MethodHandles.arrayElementVarHandle(Object[].class);

    @SuppressWarnings("VolatileArrayField")
    volatile Object[] items = null;

    public CompactArrayMap() {
    }

    public CompactArrayMap(K initialKey, V initialValue) {
        this(new Object[]{initialKey, initialValue});
    }

    public CompactArrayMap(@Nullable Object[] initial) {
        assert(initial == null || initial.length%2==0);
        this.items = initial;
    }

    public static boolean containsValue(Object aValue) {
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
        Object[] o = ITEMS.get(this);
        return o != null ? o.length / 2 : 0;
    }

    /**
     * returns previous value, or null if none - like Map.put
     * interpets value==null as removal
     */
    public V put(Object key, V value) {
        Object[] returned = new Object[1];
        ITEMS.accumulateAndGet(this, new Object[]{key, value}, new BinaryOperator<Object[]>() {
            @Override
            public Object[] apply(Object[] a, Object[] kv) {
                if (a == null) {
                    return kv;
                } else {
                    int s = a.length;

                    Object k = kv[0], v = kv[1];
                    int found = -1;
                    for (int i = 0; i < s; i += 2) {
                        if (keyEquals(k, a[i])) {
                            found = i + 1;
                            break;
                        }
                    }

                    if (found != -1) {
                        returned[0] = a[found];
                        if (v != null) {
                            a[found] = v;
                            return a;
                        } else {
                            if (a.length == 2) {
                                return null; //map emptied
                            } else {
                                Object[] b = Arrays.copyOf(a, a.length - 2);
                                if (found - 1 < a.length - 2) {
                                    //TODO test
                                    System.arraycopy(a, found + 1, b, found - 1, a.length - (found - 1) - 2);
                                }
                                return b;
                            }
                        }
                    } else {
                        if (v != null) {
                            Object[] b = Arrays.copyOf(a, s + 2);
                            returned[0] = -1;
                            b[s++] = k;
                            b[s] = v;
                            return b;
                        } else {
                            //tried to remove key which isnt presented; no effect
                            return a;
                        }
                    }

                }
            }
        });
        return (V) returned[0];
    }

    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        V e = get(key);
        if (e != null)
            return e;

        V v = mappingFunction.apply(key);
        put(key, v);
        return v;
    }

    public V remove(Object key) {
        return put(key, null);
    }

    /**
     * override for alternate equality test
     */
    protected static boolean keyEquals(Object a, Object b) {
        return a.equals(b);
    }

    public void clear() {
        ITEMS.set(this, null);
    }

    public Object[] clearPut(K key, V value) {
        return ITEMS.getAndSet(this, new Object[]{key, value});
    }

    public void forEach(BiConsumer<K, V> each) {
        whileEach(new BiPredicate<K, V>() {
            @Override
            public boolean test(K k, V v) {
                each.accept(k, v);
                return true;
            }
        });
    }

    public boolean whileEach(BiPredicate<K, V> each) {
        Object[] ii = ITEMS.get(this);
        for (int i = 0, iiLength = ii.length; i < iiLength; ) {
            K k = (K) ii[i++];
            V v = (V) ii[i++];
            if (!each.test(k,v))
                return false;
        }
        return true;
    }

//    public void clearExcept(K key) {
//
//        V exist = get(key);
//        clear();
//        if (exist != null)
//            put(key, exist);
//
//    }

}
