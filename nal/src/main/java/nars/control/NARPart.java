package nars.control;

import jcog.Log;
import jcog.WTF;
import jcog.event.Off;
import jcog.event.OffOn;
import jcog.event.Offs;
import jcog.service.Part;
import nars.$;
import nars.NAR;
import nars.term.Term;
import nars.term.Termed;
import nars.time.event.WhenInternal;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 *

 *
 *
 * */
abstract public class NARPart extends Part<NAR> implements Termed, OffOn {

    private static final Logger logger = Log.logger(NARPart.class);

    public final Term id;

    /** TODO encapsulate */
    private final Offs whenOff = new Offs();

    public NAR nar;

    protected NARPart() {
        this((NAR)null);
    }

    protected NARPart(NAR nar) {
        this((Term)null);

        if (nar!=null)
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

    /** optional event occurrence information.  null if not applicable. */
    @Nullable public WhenInternal event() {
        return null;
    }

    /** register a future deactivation 'off' of an instance which has been switched 'on'. */
    @Deprecated protected final void whenOff(Off... x) {
        for (Off xx : x)
            whenOff.add(xx);
    }


//    public final void pause() {
//        NAR n = nar;
//        if (n != null) {
//            boolean ok = n.stop(id);
//            assert(ok);
//        } else {
//            throw new NullPointerException();
//        }
//    }

    /** disables this part but does not remove it */
    public void off() {
        NAR n = nar;
        if (n!=null) {
            boolean ok = n.stop(id);
            //assert (ok);
        }
    }

    /** resume */
    public void on() {
        nar.start(id);
    }

    public boolean delete() {
        boolean ok = nar.remove(id); assert(ok);
        return true;
    }

    @Override
    protected final void start(NAR nar) {

        assert(this.nar == null);

        logger.debug("start {}", this);

        if (!(this.nar == null || this.nar == nar))
            throw new WTF("NAR mismatch");

        this.nar = nar;

        starting(nar);
    }



    @Override
    protected final void stop(NAR nar) {
        try {

            stopping(nar);

        } finally {

            try {

                this.whenOff.off();

            } finally {

                logger.debug("stop {}", this);
                this.nar = null;

            }
        }
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
    /*abstract*/ public boolean singleton() {
        return false;
    }


    @Override
    public Term term() {
        return id;
    }

}
