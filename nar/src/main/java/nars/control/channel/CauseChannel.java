package nars.control.channel;

import jcog.pri.Prioritizable;
import jcog.util.ConsumerX;
import nars.control.Cause;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/**
 * metered and mixable extension of Cause base class
 */
public abstract class CauseChannel<X extends Prioritizable>  {

    public final Cause why;
    public final short id;

    protected CauseChannel(Cause why) {
        this.why = why;
        this.id = why.id;
    }

    @Override
    public String toString() {
        return why.name + "<-" + super.toString();
    }

    public float pri() {
        return why.pri();
    }

    public final void accept(X x, ConsumerX<? super X> target) {
        preAccept(x);
        target.accept(x);
    }

    public final void acceptAll(Stream<? extends X> xx, ConsumerX<? super X> target) {
        target.acceptAll(xx.peek(this::preAccept));
    }

    public final void acceptAll(Iterable<? extends X> xx, ConsumerX<? super X> target) {
        for (X x : xx)
            preAccept(x);
        target.acceptAll(xx);
    }
    public final void acceptAll(Collection<? extends X> xx, ConsumerX<? super X> target) {
        switch (xx.size()) {
            case 0: return;
            case 1: {
                if (xx instanceof List) {
                    accept(((List<X>) xx).get(0), target);
                } else {
                    accept(xx.iterator().next(), target);
                }
                break;
            }
            default: {
                acceptAll((Iterable)xx, target);
                break;
            }
        }
    }

    public final void acceptAll(X[] xx, ConsumerX<? super X> target) {
        switch (xx.length) {
            case 0: return;
            case 1: accept(xx[0], target); break;
            default: {
                for (X x : xx)
                    preAccept(x);
                target.acceptAll(xx);
                break;
            }
        }
    }


    //TODO other input() methods (Stream, Iterable, etc)

    protected abstract void preAccept(X x);


}

//    public BufferedCauseChannel<X> buffered() {
//        return buffered(8*1024);
//    }

//    public BufferedCauseChannel<X> buffered(int capacity) {
//        return new BufferedCauseChannel(this, capacity);
//    }

//    public ThreadBufferedCauseChannel<X> threadBuffered() {
//        return new ThreadBufferedCauseChannel<>(this);
//    }
