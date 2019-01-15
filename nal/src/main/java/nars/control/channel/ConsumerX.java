package nars.control.channel;

import jcog.pri.Prioritizable;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

/** recipient of instances: in collections, iterators, streams, or individually */
public abstract class ConsumerX<X extends Prioritizable> implements Consumer<X> {

    /** override for multithreading hints */
    public int concurrency() {
        return 1;
    }

    abstract public void input(X x);

    public void input(Iterable<? extends X> xx) {
        input(xx.iterator());
    }

    public void input(Collection<? extends X> xx) {
        input((Iterable)xx);
    }

    public void input(Iterator<? extends X> xx) {
        xx.forEachRemaining(this::input);
    }

    public void input(Stream<? extends X> x) {
        x.forEach(this::input);
    }

    public void input(List<? extends X> x) {
        if (x.size() == 1) {
            input(x.get(0));
        } else {
            input((Collection)x);
        }
    }

    public void input(Object... xx) {
        for (Object x : xx) {
            if(x!=null)
                input((X) x);
        }
    }

    @Override
    public final void accept(X x) {
        input(x);
    }
}
