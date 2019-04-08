package nars.control.channel;

import jcog.pri.Prioritizable;
import nars.control.Why;

import java.util.Collection;
import java.util.List;

/**
 * metered and mixable extension of Cause base class
 */
abstract public class CauseChannel<X extends Prioritizable>  {

    public final Why why;
    public final short id;

    public CauseChannel(Why why) {
        this.why = why;
        this.id = why.id;
    }

    @Override
    public String toString() {
        return why.name + "<-" + super.toString();
    }

    public float value() {
        return why.value();
    }

    public final void accept(X x, ConsumerX<? super X> target) {
        preAccept(x);
        target.accept(x);
    }

    public final void acceptAll(Iterable<? extends X> xx, ConsumerX<? super X> target) {
        xx.forEach(this::preAccept);
        target.acceptAll(xx);
    }
    public final void acceptAll(Collection<? extends X> xx, ConsumerX<? super X> target) {
        switch (xx.size()) {
            case 0: return;
            case 1: {
                if (xx instanceof List) {
                    accept(((List<X>) xx).get(0), target);
                    return;
                } else {
                    accept((X)(xx.iterator().next()), target);
                }
                break;
            }
        }

        xx.forEach(this::preAccept);
        target.acceptAll(xx);
    }

    public final void acceptAll(X[] xx, ConsumerX<? super X> target) {
        for (X x : xx)
            preAccept(x);
        target.acceptAll(xx);
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
