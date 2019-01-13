package nars.control.channel;

import jcog.pri.Prioritizable;
import nars.control.Cause;

/**
 * metered and mixable extension of Cause base class
 */
abstract public class CauseChannel<X extends Prioritizable> extends ConsumerX<X> {

    public final Cause cause;
    public final short id;

    public CauseChannel(Cause cause) {
        this.cause = cause;
        this.id = cause.id;
    }

    @Override
    public String toString() {
        return cause.name + "<-" + super.toString();
    }

    public BufferedCauseChannel<X> buffered() {
        return buffered(8*1024);
    }

    public BufferedCauseChannel<X> buffered(int capacity) {
        return new BufferedCauseChannel(this, capacity);
    }

//    public ThreadBufferedCauseChannel<X> threadBuffered() {
//        return new ThreadBufferedCauseChannel<>(this);
//    }

    public float value() {
        return cause.value();
    }
}
