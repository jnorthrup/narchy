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

public class Sharing<What,Who> {

    final static int MAX_CUSTOMERS = 64;



    final ConcurrentFastIteratingHashMap<What,Mix<Who,What,Share<Who,What>>> alloc = new ConcurrentFastIteratingHashMap<>(new Mix[] { });


    /** CAN/provide/offer something */
    public Sharing<What, Who> can(Mix... dd) {
        for (Mix<Who,What,Share<Who,What>> d : dd)
            alloc.put(d.what, d);
        return this;
    }

    public void off(What what, Who who) {
        Mix<Who, What, Share<Who, What>> ww = this.alloc.get(what);
        if (ww != null) {
            @Nullable Share<Who, What> r = ww.remove(who);
            if (ww.isEmpty()) {
                Mix<Who, What, Share<Who, What>> m = alloc.remove(what);
                assert(m.isEmpty());
            }
        }
    }

    @Nullable protected Share<Who,What> need(Share<Who,What> s) {
        Mix<Who, What, Share<Who,What>> b = alloc.get(s.what);
        if (b == null)
            throw new UnsupportedOperationException("unsupported resource type for: " + s);

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
        
        return new Demand<>(id, this);
    }

    public String summary() {
        return alloc.toString();
    }


}
