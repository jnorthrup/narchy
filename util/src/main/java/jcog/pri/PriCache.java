package jcog.pri;

import com.google.common.collect.Iterators;
import jcog.TODO;
import jcog.Util;
import jcog.exe.Loop;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import static jcog.pri.PriCache.State.FREE;

/**
 * next-gen bag experiment
 * hybrid strong/weak + prioritized + forgetting bounded cache
 */
public class PriCache<X, Y> extends AbstractMap<X, Y> {


    public enum Hold {
        STRONG, WEAK, SOFT
    }

    public enum State {
        FREE,
        ALERT,
        CRITICAL
    }

    public State state = FREE;

    private final Hold defaultMode = Hold.STRONG;

    abstract public static class TLink<X, Y> implements Supplier<Y> {
        public final X key;
        public final int hash;
        //TODO support hash64 and larger

        /** activity counter */
        long action = 0;

        public long lastActive;


        public TLink(X key, long now) {
            this.key = key;
            this.hash = key.hashCode();
            this.lastActive = now;
        }

        @Override
        public final int hashCode() {
            return hash;
        }

        @Override
        public final boolean equals(Object obj) {
            return this == obj || (key == obj || key.equals(obj));
        }

        public void updateAction(long boost, long now, float forgetRate) {

            //TODO use atomics to avoid synch block
            synchronized (key) {
                long prevLastActive = this.lastActive;
                long dt = now - prevLastActive;

                long nextAction = this.action;
                if (dt > 0) {
                    nextAction = Math.max(0, Math.round(forgetRate * dt));
                }

                lastActive = now;
                this.action = nextAction + boost;
            }

        }

        @Override
        public String toString() {
            return key + "{" +
                    ", action=" + action +
                    ", lastActive=" + lastActive +
                    '}';
        }
    }

    public static class BasicTLink<X, Y> extends TLink<X, Y> {
        private Y value;

        public BasicTLink(X key, long now) {
            super(key, now);
        }

        public TLink<X, Y> setValue(Y newValue) {
            this.value = newValue;
            return this;
        }

        @Override
        public Y get() {
            return value;
        }
    }

    public final long now() {
        return clock.getAsLong();
    }

    TLink<X, Y> link(@Nullable TLink<X, Y> t, X k, Y v, Hold mode) {

        if (v == null) {
            //if (t!=null) t.delete();
            return null; //pass-through for map removal
        }
        if (mode == null)
            mode = mode(k, v);

        switch (mode) {

            case STRONG:
                if (!(t instanceof BasicTLink))
                    t = new BasicTLink(k, now());
                ((BasicTLink) t).setValue(v);
                break;

            case SOFT:
                if (!(t instanceof ProxyTLink))
                    t = new ProxyTLink(k, now());
                ((ProxyTLink) t).setValue(new MySoftReference(k, v, refq));
                break;

            case WEAK:
                if (!(t instanceof ProxyTLink))
                    t = new ProxyTLink(k, now());
                ((ProxyTLink) t).setValue(new MyWeakReference(k, v, refq));
                break;

            default:
                throw new UnsupportedOperationException();
        }
        return t;
    }

    public static class ProxyTLink<X, Y> extends TLink<X, Y> {

        private Supplier<Y> value = () -> null;

        public ProxyTLink(X key, long now) {
            super(key, now);
        }

        public void setValue(Supplier<Y> value) {
            this.value = value;
        }

        @Override
        public Y get() {
            return value.get();
        }
    }

    private static final class MyWeakReference<X, Y> extends WeakReference<Y> implements Supplier<Y> {
        public final X key;

        public MyWeakReference(X key, Y val, ReferenceQueue q) {
            super(val, q);
            this.key = key;
        }
    }

    private static final class MySoftReference<X, Y> extends SoftReference<Y> implements Supplier<Y> {
        public final X key;

        public MySoftReference(X key, Y val, ReferenceQueue q) {
            super(val, q);
            this.key = key;
        }
    }

    final Map<X, TLink<X, Y>> map = new ConcurrentHashMap<>();

    private final LongSupplier clock;

    private final float forgetRate = 1;

    public final ReferenceQueue refq = new ReferenceQueue();
    public final Loop cleaner;

    public PriCache() {
        this(System::currentTimeMillis);
    }

    public PriCache(LongSupplier clock) {

        this.clock = clock;
        this.cleaner = new Cleaner();
    }

    public Y get(Object x) {
        TLink<X, Y> t = getLink(x);
        if (t!=null) {
            Y y = t.get();
            if (y!=null) {
                t.updateAction(1, now(), forgetRate);
                return y;
            }
        }
        return null;
    }

    protected TLink<X, Y> getLink(Object x) {
        return map.get(x);
    }

    public Y computeIfAbsent(X x, Hold mode, Function<X, Y> builder) {
        TLink<X, Y> r = map.computeIfAbsent(x, (xx) -> link(null, xx, builder.apply(xx), mode));
        return r != null ? r.get() : null;
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public Y computeIfAbsent(X key, Function<? super X, ? extends Y> mappingFunction) {
        throw new TODO("TODO");
    }

    @Override
    public Y computeIfPresent(X key, BiFunction<? super X, ? super Y, ? extends Y> remappingFunction) {
        throw new TODO("TODO");
    }

    @Override
    public Y compute(X x, BiFunction<? super X, ? super Y, ? extends Y> remap) {
        TLink<X, Y> r = map.compute(x, (xx, prev) -> link(prev, xx, remap.apply(xx, prev != null ? prev.get(): null), null));
        return r != null ? r.get() : null;
    }

    /** selects the default mode for an entry */
    protected Hold mode(X x, Y v) {
        return defaultMode;
    }

    @Override
    public Y merge(X x, Y value, BiFunction<? super Y, ? super Y, ? extends Y> remap) {
        TLink<X, Y> r = map.compute(x, (xx, prev) -> link(prev, xx, remap.apply(prev != null ? prev.get() : null, value), null));
        return r != null ? r.get() : null;
    }

    public Y put(X x, @Nullable Hold mode, Y y) {
        final Object[] previous = {null};
        TLink<X, Y> cur = map.compute(x, (xx, prev) -> {
            if (prev!=null)
                previous[0] = prev.get();
            return link(prev, xx, y, mode);
        });
        Object p = previous[0];
        if (p != null) {
            Y py = (Y) p;
            onRemove(x, py);
            return py;
        } else {
            return null;
        }
    }

    protected void onRemove(X x, Y y) {

    }

    @Override
    public Y put(X x, Y y) {
        return put(x, null, y);
    }

    @Override
    public Y remove(Object key) {
        TLink<X, Y> r = map.remove(key);
        if (r != null) {
            Y v = r.get();
            onRemove(r.key, v);
            return v;
        } else {
            return null;
        }
    }

    protected void removeGC(Object x) {
        Object key;
        if (x instanceof MyWeakReference) {
            key = ((MyWeakReference) x).key;
        } else if (x instanceof MySoftReference) {
            key = ((MySoftReference) x).key;
        } else {
            throw new UnsupportedOperationException();
        }
        remove(key);
    }

    @Override
    public Set<Entry<X, Y>> entrySet() {
        return new MyEntrySet();
    }

    @Override
    public Set<X> keySet() {
        throw new TODO();
    }

    @Override
    public Collection<Y> values() {
        throw new TODO();
    }

    public Iterator<TLink<X, Y>> linkIterator() {
        return map.values().iterator();
    }

    public Iterator<Y> valueIterator() {
        return Iterators.transform(linkIterator(), Supplier::get);
    }

    private class MyEntrySet extends AbstractSet<Entry<X, Y>> {

        final Set<Entry<X, TLink<X, Y>>> e = map.entrySet();

        @Override
        public Iterator iterator() {
            //TODO this SimpleEntry can be recycled
            return Iterators.transform(e.iterator(), ee -> new SimpleEntry(ee.getKey(), ee.getValue().get()));
        }

        @Override
        public int size() {
            return e.size();
        }
    }


    /** TODO abstract this into a general purpose PriMap-independent resource watcher which triggers the various eviction options as well as controls other controllable parameters as plugins like AgentService */
    protected class Cleaner extends Loop {

        int minPeriod = 1, maxPeriod = 200;


        public Cleaner() {
            super();
            setPeriodMS(maxPeriod);
        }

        @Override
        public boolean next() {


            Object ref;
            for (ref = refq.poll(); ref != null; ref = refq.poll()) {
                removeGC(ref);
            }

            float evictPower = updateMemory(Util.memoryUsed());

            evict(evictPower);

            //TODO adjust cycle time in proportion to eviction power
            setPeriodMS((int)Util.round( Util.lerp(1f - (evictPower*evictPower), (float)minPeriod, maxPeriod),
                    2 /* ms increments */) );

            return true;
        }




    }

    @Override
    public int size() {
        return map.size();
    }

    /**
     * returns the decided strength of the active eviction to run, if any
     */
    protected float updateMemory(float used) {
        if (used > 0.9f) {
            state = State.CRITICAL;
        } else if (used > 0.25f) {
            state = State.ALERT;
        } else {
            state = State.FREE;
        }
        return Util.unitize(used*used*used);
    }

    public void evict(float strength) {

    }
}
