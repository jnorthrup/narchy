package nars.exe;

import com.google.common.base.Joiner;
import jcog.Util;
import jcog.bag.impl.ArrayBag;
import jcog.data.map.ConcurrentFastIteratingHashMap;
import jcog.pri.Pri;
import jcog.pri.op.PriMerge;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/** maps: What resources to Who consumers
 * realtime quantifiable service allocator
 * */
public class Valve<What, Who> {

    final static int MAX_WORKERS = 64;


    /** allocation state, constraint solution */
    final ConcurrentFastIteratingHashMap<What,Distributor<Who,What>> alloc = new ConcurrentFastIteratingHashMap<>(new Distributor[] { });

    public Valve() {

    }

    /** an allocated share of some resource */
    static class Share<Who,What> extends Pri {
        final What what;
        public Who who;

        /** ownership amount supplied to the client, in absolute fraction of 100%.
         *  determined by the Focus not client. this is stored in the super class's 'pri' field. */
        //float supply = 0;

        /** mutable demand, adjustable by client in [0..1.0].  its meaning is subjective and may be
         * relative to its previous values. */
        float need = 0f;

        public Share(Who who, What what) {
            this.who = who;
            this.what = what;
        }

        public void need(float newDemand) {
            this.need = Util.unitize(newDemand);
        }
        public float need() {
            return this.need;
        }

        @Override public String toString() {
            return "(" + what + "+" + pri() + "|" + need + "~" + who + ")";
        }
    }

    static public class Distributor<Who, What> extends ArrayBag<Who,Share<Who,What>> {

        public final What what;

        public Distributor(What what) {
            super(PriMerge.max /* unused */, MAX_WORKERS);
            this.what = what;
        }

        /** prepare for next cycle */
        @Override public Distributor commit() {
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
    public Valve<What, Who> can(Distributor<Who,What>... dd) {
        for (Distributor<Who,What> d : dd)
            alloc.put(d.what, d);
        return this;
    }

    @Nullable protected Share<Who,What> demand(Customer<What,Who> w, What r) {
        Distributor<Who,What> b = alloc.get(r);
        if (b == null)
            return null; //resource type unsupported
        @Nullable Share<Who,What> s = b.get(w.id);
        if (s == null) {
            s = new Share<Who,What>(w.id, r);
            s = b.put(s);
        }
        return s;
    }



    /** holds the mutable state of an instrumented, resource-consuming
     *  re-iterative process.
     *
     *  each client gets an instance of this upon registration.
     *
     *  it contains (actually, extends) a list of active shares that has been allocated.
     */
    public static class Customer<What, Who> extends ConcurrentHashMap<What,Share<Who,What>> {

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
        public Share<Who,What> need(What resource, float demand) {
            Share<Who,What> s = computeIfAbsent(resource, r -> valve.demand(this, r));
            if (s!=null) {
                s.need(demand);
            }
            return s;
        }

        /** called by the customer to end its interaction */
        public void stop() {
            synchronized (this) {
                values().removeIf(s -> {
                    Distributor<Who,What> b = valve.alloc.get(s.what);
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
                alloc.forEachValue(Distributor::commit);
            } finally {
                commiting.set(false);
            }
        }
    }

    public Customer<What, Who> start(Who id) {
        //TODO check if customer with same id is registered. could be catastrophic if duplicates have access
        return new Customer<>(id, this);
    }

    public String summary() {
        return alloc.toString();
    }

    public static void main(String[] args) {
        Valve<String,String> v = new Valve<>();
        v.can(
            new Distributor<>("time"),
            new Distributor<>("memory"),
            new Distributor<>("profiling")
        );
        
        Customer a = v.start("A");
        a.need("time", 0.25f);
        a.need("memory", 0.25f);
        a.need("profiling", 1);

        Customer b = v.start("B");
        b.need("time", 0.75f);
        b.need("memory", 0.1f);

        Customer c = v.start("idle");
        c.need("time", 0.01f);

        v.commit();
        String s = v.summary();
        System.out.println(s);
    }

}
