package nars.control.channel;

import jcog.pri.Prioritizable;
import nars.control.Why;

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

    public final void acceptAll(Iterable<? extends X> x, ConsumerX<? super X> target) {
        x.forEach(this::preAccept);
        target.acceptAll(x);
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
