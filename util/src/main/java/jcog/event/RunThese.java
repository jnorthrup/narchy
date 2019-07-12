package jcog.event;

import java.util.Collection;

/**
 * essentially holds a list of registrations but forms an activity context
 * from the dynamics of its event reactivity
 *
 * a thread-safe shutdown hook.  could just as easily be: CopyOnWriteArrayList<Runnable>
 */
public class RunThese extends jcog.data.list.FastCoWList<Runnable> implements Runnable, Off  {

    RunThese(int capacity) {
        super(capacity, Runnable[]::new);
    }

    RunThese() {
        this(1);
    }

    public RunThese(Runnable... r) {
        this(r.length);
        for (Runnable o : r)
            add(o);
    }

    @Deprecated
    public final boolean add(Off o) {
        return add((Runnable)o::close);
    }

    @Deprecated public final void close() {
        run();
    }

    @Override public final void run() {
        super.forEach(Runnable::run);
        super.clear();
//        super.removeIf(o -> {
//            o.run();
//            return true;
//        });
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
    public Runnable remove(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeIf(java.util.function.Predicate<? super Runnable> predicate) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeFirstInstance(Runnable Runnable) {
        throw new UnsupportedOperationException();
    }

}
