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
public abstract class NARPart extends Parts<NAR> implements Termed, OffOn, SubPart<NAR> {

    public final Term id;

    protected NARPart() {
        this((NAR) null);
    }

    @Deprecated protected NARPart(NAR nar) {
        this((Term) null);

        if (nar != null)
            nar.add(this);
    }

    protected NARPart(@Nullable Term id) {
        this.id = id != null ? id : $.identity(this);
    }

    public static Term id(@Nullable Term id, NARPart x) {
        if (id == null)
            return ((Termed) x).term();
        else
            return NARPart.singleton() ? (x.id) : $.p(id, $.identity(x));
    }
    /**
     * resume
     */
    public final void on() {
        nar.add(this);
    }
    public final void close() {
        delete();
    }

    public boolean delete() {

        logger.info("delete {}", term());


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
                logger.warn("stop {} {}", term(), t);
            }
        }

        this.nar = null;
        return true;
    }

    /**
     * optional event occurrence information.  null if not applicable.
     */
    public @Nullable WhenInternal event() {
        return null;
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
        logger.info("start {} -> {} {}", container, term(), getClass().getName());

        //synchronized (this) {
            this.nar = nar;

            _state(Thing.ServiceState.OffToOn);


            starting(this.nar);

            startLocal(nar);
            _state(Thing.ServiceState.On);
        //}
    }

    public void stopIn(NAR nar, Part<NAR> container) {

        logger.info(" stop {} -> {} {}", container, term(), getClass().getName());

        //synchronized(this) {
            _state(Thing.ServiceState.OnToOff);
            stopLocal(nar);

            stopping(nar);

            _state(Thing.ServiceState.Off);
        //}
    }

    /** MAKE SURE NOT TO CALL THIS DIRECTLY; IT WILL BE INVOKED.  LIKELY YOU WANT: n.start(x) NOT x.start(a) */
    @Override protected final void start(NAR nar) {

        NAR prevNar = this.nar;
        if (!(prevNar == null || prevNar == nar))
            throw new WTF("NAR mismatch");

        logger.info("start {}", term());

        starting(this.nar = nar);

        startLocal(nar);
    }



    @Override
    protected final void stop(NAR nar) {
        logger.info("stop {}", term());

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
    public static boolean singleton() {
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
    @Deprecated public final Runnable pause() {
        NAR n = this.nar;
        if (n != null) {
            if (n.stop(this)) {
                logger.info("pause {}", this);
                return () -> {
                    NAR nn = this.nar;
                    if (nn == null) {
                        //deleted or unstarted
                    } else {
                        if (nn.add(this)) {
                            logger.info("resume {}", this);
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
