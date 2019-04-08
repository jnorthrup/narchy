package jcog.event;

import org.eclipse.collections.api.block.predicate.Predicate;

import java.util.Collection;

/**
 * essentially holds a list of registrations but forms an activity context
 * from the dynamics of its event reactivity
 */
public class Offs extends jcog.data.list.FastCoWList<Off> implements Off {

    Offs(int capacity) {
        super(capacity, Off[]::new);
    }

    Offs() {
        this(1);
    }

    public Offs(Off... r) {
        this(r.length);
        for (Off o : r)
            add(o);
    }

    public final void add(Runnable r) {
        add((Off)(r::run));
    }

    public final void off() {
        super.removeIf(o -> {
            o.off();
            return true;
        });
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Off remove(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeIf(Predicate<? super Off> predicate) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeFirstInstance(Off off) {
        throw new UnsupportedOperationException();
    }

}
