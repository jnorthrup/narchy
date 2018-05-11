package jcog.exe.valve;

import com.google.common.base.Joiner;
import jcog.bag.impl.ArrayBag;
import jcog.data.map.ConcurrentFastIteratingHashMap;
import jcog.pri.op.PriMerge;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/** maps: What resources/services/qualities/sources to Who consumers/targets
 * realtime quantifiable service allocator
 * multi-throttle
 **/
//public enum Valve { ;
public class Valve<What,Who> {

    final static int MAX_CUSTOMERS = 64;


//    /** allocation state, constraint solution */
    final ConcurrentFastIteratingHashMap<What,Mix<Who,What>> alloc = new ConcurrentFastIteratingHashMap<>(new Mix[] { });


//    static class Distribution<What,Who> extends Services<Distribution<What,Who>, What, Distributor<Who,What>> {
//
//        public Distribution() {
//            super();
//        }
//    }

    /** base class; does nothing (token indicator) */
    public static class Mix<Who, What> extends ArrayBag<Who,Share<Who,What>> {

        public final What what;

        protected Mix(What what) {
            super(PriMerge.max /* unused */, MAX_CUSTOMERS);
            this.what = what;
        }

        /** prepare for next cycle */
        @Override public Mix<Who, What> commit() {
            //TODO normalize shares according to some policy
            if (isEmpty())
                return this;

            final float[] sum = {0};
            forEach(s -> {
                sum[0] += s.need;
            });
            float summ = sum[0];
            if (summ < Float.MIN_NORMAL) {
                //no demand at all: set all to default 0.5 supply
                commit(s -> s.priSet(0.5f));
            } else {
                commit(s -> s.priSet(s.need / summ)); //TODO this is not precisely accurate if demand was modified in between the calculation
                //ArrayBag will be sorted after that
            }

            return this;
        }

        @Override
        public String toString() {
            return Joiner.on(",").join(this);
        }

        @Nullable
        @Override
        public Who key(Share<Who,What> rShare) {
            return rShare.who;
        }
    }


    /** CAN/provide/offer something */
    public Valve<What, Who> can(Mix<Who,What>... dd) {
        for (Mix<Who,What> d : dd)
            alloc.put(d.what, d);
        return this;
    }

    @Nullable protected Share<Who,What> need(Share<Who,What> s) {
        Mix<Who,What> b = alloc.get(s.what);
        if (b == null) {
            throw new UnsupportedOperationException("unsupported resource type for: " + s);
            //return null; //resource type unsupported
        } else
            return b.put(s);
    }



    /** holds the mutable state of an instrumented, resource-consuming
     *  re-iterative process.
     *
     *  each client gets an instance of this upon registration.
     *
     *  it contains (actually, extends) a list of active shares that has been allocated.
     */
    public static class Customer<Who, What> extends ConcurrentHashMap<What,Share<Who,What>> {

        public final Who id;
        public final int hash;
        private final Valve<What, Who> valve;

        private Customer(Who id, Valve<What, Who> valve) {
            super();
            this.id = id;
            this.hash = id.hashCode();
            this.valve = valve;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object o) {
            return this == o;
        }

        @Nullable
        public <S extends Share<Who,What>> S need(S s) {
            return (S) computeIfAbsent(s.what, r -> valve.need(s));
        }

        @Nullable
        public Share<Who,What> need(What resource, float demand) {
            Share<Who,What> s = computeIfAbsent(resource, r -> valve.need(new Share<>(this.id, r)));
            if (s!=null) {
                s.need(demand);
            }
            return s;
        }

        /** called by the customer to end its interaction */
        public void stop() {
            synchronized (this) {
                values().removeIf(s -> {
                    Mix<Who,What> b = valve.alloc.get(s.what);
                    if (b != null)
                        b.remove(s.who);
                    return true;
                });
            }
        }
    }

    private final AtomicBoolean commiting = new AtomicBoolean();

    /** called periodically to update allocations */
    protected void commit() {
        if (commiting.compareAndSet(false, true)) {
            try {
                alloc.forEachValue(Mix::commit);
            } finally {
                commiting.set(false);
            }
        }
    }

    public Customer<Who, What> start(Who id) {
        //TODO check if customer with same id is registered. could be catastrophic if duplicates have access
        return new Customer<>(id, this);
    }

    public String summary() {
        return alloc.toString();
    }


}
