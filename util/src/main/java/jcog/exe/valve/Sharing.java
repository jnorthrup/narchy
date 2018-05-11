package jcog.exe.valve;

import jcog.data.map.ConcurrentFastIteratingHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

/** maps: What resources/services/qualities/sources to Who consumers/targets
 * realtime quantifiable service allocator
 * multi-throttle
 * quality of service (QoS)
 * mixer board / equalizer
 **/
//public enum Valve { ;
public class Sharing<What,Who> {

    final static int MAX_CUSTOMERS = 64;


//    /** allocation state, constraint solution */
    final ConcurrentFastIteratingHashMap<What,Mix<Who,What,Share<Who,What>>> alloc = new ConcurrentFastIteratingHashMap<>(new Mix[] { });


//    static class Distribution<What,Who> extends Services<Distribution<What,Who>, What, Distributor<Who,What>> {
//
//        public Distribution() {
//            super();
//        }
//    }


    /** CAN/provide/offer something */
    public Sharing<What, Who> can(Mix<Who,What,Share<Who,What>>... dd) {
        for (Mix<Who,What,Share<Who,What>> d : dd)
            alloc.put(d.what, d);
        return this;
    }

    @Nullable protected Share<Who,What> need(Share<Who,What> s) {
        Mix<Who, What, Share<Who,What>> b = alloc.get(s.what);
        if (b == null) {
            throw new UnsupportedOperationException("unsupported resource type for: " + s);
            //return null; //resource type unsupported
        } else
            return b.put(s);
    }


    private final AtomicBoolean commiting = new AtomicBoolean();

    /** called periodically to trigger updates */
    public void commit() {
        if (commiting.compareAndSet(false, true)) {
            try {
                alloc.forEachValue(Mix::commit);
            } finally {
                commiting.set(false);
            }
        }
    }

    public Demand<Who, What> start(Who id) {
        //TODO check if customer with same id is registered. could be catastrophic if duplicates have access
        return new Demand<>(id, this);
    }

    public String summary() {
        return alloc.toString();
    }

}
