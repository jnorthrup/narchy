//package jcog.data.map;
//
//import org.eclipse.collections.impl.map.mutable.UnifiedMap;
//import org.jetbrains.annotations.NotNull;
//
//import java.util.function.BiFunction;
//import java.util.function.Function;
//
///** synchronized in a few methods for NARS bag purposes.  conserves memory but at the cost of some CPU usage */
//public final class SynchronizedUnifiedMap<K, V> extends UnifiedMap<K, V> {
//
//    public SynchronizedUnifiedMap(int cap, float loadFactor) {
//        super(cap, loadFactor);
//    }
//
//    @Override
//    public V remove(@NotNull Object key) {
//        synchronized (this) {
//            return super.remove(key);
//        }
//    }
//
//    @Override
//    public V compute(@NotNull K key, @NotNull BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
//        synchronized (this) {
//            return super.compute(key, remappingFunction);
//        }
//    }
//
//    @Override
//    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
//        synchronized (this) {
//            return super.computeIfAbsent(key, mappingFunction);
//        }
//    }
//
//    @Override
//    public V get(Object key) {
//        synchronized (this) {
//            return super.get(key);
//        }
//    }
//}
