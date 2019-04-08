package nars.control.channel;

import jcog.data.iterator.ArrayIterator;
import jcog.pri.Prioritizable;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

/** recipient of instances: in collections, iterators, streams, or individually.
 * */
@FunctionalInterface public interface ConsumerX<X extends Prioritizable> extends Consumer<X> {

    default void acceptAll(Iterable<? extends X> xx) {
        if (xx instanceof Collection)
            acceptAll((Collection)xx);
        else
            acceptAll(xx.iterator());
    }

    default void acceptAll(Collection<? extends X> xx) {
        switch (xx.size()) {
            case 0: return;
            case 1: {
                if (xx instanceof List) {
                    accept(((List<X>) xx).get(0));
                    return;
                }
                break;
            }
        }
        acceptAll(xx.iterator());
    }

    default void acceptAll(Iterator<? extends X> xx) {
        xx.forEachRemaining(this::accept);
    }

    default void acceptAll(Stream<? extends X> x) {
        x.forEach(this::accept);
    }

    default void acceptAll(List<? extends X> x) {
        if (x.size() == 1) {
            accept(x.get(0));
        } else {
            acceptAll((Collection)x);
        }
    }

    default void acceptAll(X[] xx) {
        switch (xx.length) {
            case 0:  break;
            case 1:  accept(xx[0]); break;
            default: acceptAll(ArrayIterator.iterator(xx)); break;
        }
    }

    /** override for multithreading hints */
    default int concurrency() {
        return 1;
    }

}
