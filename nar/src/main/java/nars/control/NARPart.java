package nars.control;

import jcog.WTF;
import jcog.event.OffOn;
import jcog.thing.Part;
import jcog.thing.Parts;
import jcog.thing.SubPart;
import jcog.thing.Thing;
import nars.$;
import nars.NAR;
import nars.term.Term;
import nars.term.Termed;
import nars.time.event.WhenInternal;
import org.jetbrains.annotations.Nullable;

/**
 *
 */
abstract public class NARPart extends Parts<NAR> implements Termed, OffOn, SubPart<NAR> {

    private static final NARPart[] EmptyArray = new NARPart[0];

    public final Term id;


    protected NARPart() {
        this((NAR) null);
    }

    protected NARPart(NAR nar) {
        this((Term) null);

        if (nar != null)
            nar.start(this);
    }

    protected NARPart(@Nullable Term id) {
        this.id = id != null ? id : $.identity(this);
    }

    public static Term id(@Nullable Term id, NARPart x) {
        if (id == null)
            return ((Termed) x).term();
        else
            return x.singleton() ? (x.id) : $.p(id, $.identity(x));
    }
    /**
     * resume
     */
    public final void on() {
        nar.start(this);
    }
    public final void close() {
        delete();
    }

    public boolean delete() {

        logger.atFine().log("delete {}", term());


        local.removeIf((p) -> {
            ((NARPart)p).delete();
            return true;
        });
        whenDeleted.close();

        NAR n = this.nar;
        if (n != null) {
            n.stop(this);
        } else {
            //experimental hard stop
            try {
                stopping(null);
            } catch (Throwable t) {
                logger.atSevere().withCause(t).log("stop {}", term());
            }
        }

        this.nar = null;
        return true;
    }

    /**
     * optional event occurrence information.  null if not applicable.
     */
    @Nullable
    public WhenInternal event() {
        return null;
    }


    @Override
    public final boolean equals(Object obj) {
        return this == obj;
    }

    //    @Deprecated protected final void whenDelete(Off... x) {
//        for (Off xx : x)
//            whenDeleted.add(xx);
//    }


//    public final void pause() {
//        NAR n = nar;
//        if (n != null) {
//            boolean ok = n.stop(id);
//            assert(ok);
//        } else {
//            throw new NullPointerException();
//        }
//    }


    public void startIn(NAR nar, Part<NAR> container) {
        logger.atFine().log("start {} -> {} {}", container, term(), getClass().getName());

        this.nar = nar;

        _state(Thing.ServiceState.OffToOn);


        starting(this.nar);

        startLocal(nar);
        _state(Thing.ServiceState.On);
    }

    public void stopIn(NAR nar, Part<NAR> container) {

        logger.atFine().log(" stop {} -> {} {}", container, term(), getClass().getName());

        synchronized(this) {
            _state(Thing.ServiceState.OnToOff);
            stopLocal(nar);

            stopping(nar);

            _state(Thing.ServiceState.Off);
        }
    }

    @Override
    protected final void start(NAR nar) {

        NAR prevNar = this.nar;
        if (!(prevNar == null || prevNar == nar))
            throw new WTF("NAR mismatch");

        logger.atFine().log("start {}", term());

        starting(this.nar = nar);

        startLocal(nar);
    }



    @Override
    protected final void stop(NAR nar) {
        logger.atFine().log("stop {}", term());

        stopLocal(nar);

        stopping(nar);
    }


    protected void starting(NAR nar) {

    }

    protected void stopping(NAR nar) {

    }



    /**
     * if false, allows multiple threads to execute this instance
     * otherwise it is like being synchronized
     * TODO generalize to one of N execution contexts:
     * --singleton
     * --threadsafe
     * --thread local
     * --threadgroup local
     * --remote?
     * --etc
     */
    /*abstract*/
    public boolean singleton() {
        return false;
    }


    @Override
    public Term term() {
        return id;
    }


    @Override
    public final String toString() {
        return id.toString();
    }


    /**
     * pause, returns a one-use resume ticket
     */
    public final Runnable pause() {
        NAR n = this.nar;
        if (n != null) {
            if (n.stop(this)) {
                logger.atFine().log("pause", this);
                return () -> {
                    assert(!isOn());
                    NAR nn = this.nar;
                    if (nn == null) {
                        //deleted or unstarted
                    } else {
                        if (nn.start(this)) {
                            logger.atFine().log("resume", this);
                        }
                    }
                };
            }
        }
        //return new SelfDestructAfterRunningOnlyOnce(nn);
        return () -> {
        };
    }


//    private final class SelfDestructAfterRunningOnlyOnce extends WeakReference<NAR> implements Runnable {
//
//        public SelfDestructAfterRunningOnlyOnce(NAR nar) {
//            super(nar);
//        }
//
//        @Override
//        public void run() {
//            NAR n = get();
//            if (n != null) {
//                clear();
//                n.start(NARPart.this);
//            }
//        }
//    }
}
