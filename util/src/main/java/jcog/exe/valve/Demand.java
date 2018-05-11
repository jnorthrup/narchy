package jcog.exe.valve;

import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;

/**
 *  a Cause supplied with that which is shared by the Sharing
 *
 *
 *  represents a stakeholder, holding mutable state of an instrumented, resource-consuming
 *  re-iterative process.
 *
 *  each client gets an instance of this upon registration.
 *
 *  it contains (actually, extends) a list of active shares that has been allocated.
 */
public class Demand<Who, What> extends ConcurrentHashMap<What,Share<Who,What>> {

    public final Who id;
    public final int hash;
    private final Sharing<What, Who> context;

    Demand(Who id, Sharing<What, Who> context) {
        super();
        this.id = id;
        this.hash = id.hashCode();
        this.context = context;
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
        return (S) computeIfAbsent(s.what, r -> context.need(s));
    }

    @Nullable
    public Share<Who,What> need(What resource, float demand) {
        Share<Who,What> s = computeIfAbsent(resource, r -> context.need(new Share<>(this.id, r)));
        if (s!=null) {
            s.need(demand);
        }
        return s;
    }

    /** called by the customer to end its interaction */
    public void stop() {
        synchronized (this) {
            values().removeIf(s -> {
                Mix<Who,What,Share<Who,What>> b = context.alloc.get(s.what);
                if (b != null)
                    b.remove(s.who);
                return true;
            });
        }
    }
}
