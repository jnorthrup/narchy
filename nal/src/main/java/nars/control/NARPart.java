package nars.control;

import jcog.Log;
import jcog.Skill;
import jcog.WTF;
import jcog.data.map.ConcurrentFastIteratingHashSet;
import jcog.event.Off;
import jcog.event.OffOn;
import jcog.event.RunThese;
import jcog.service.Part;
import jcog.service.Parts;
import nars.$;
import nars.NAR;
import nars.term.Term;
import nars.term.Termed;
import nars.time.event.WhenInternal;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 *
 */
abstract public class NARPart extends Part<NAR> implements Termed, OffOn {

    private static final Logger logger = Log.logger(NARPart.class);
    private static final NARPart[] EmptyArray = new NARPart[0];

    public final Term id;

    /**
     * attached resources held until deletion (including while off)
     */
    @Deprecated
    private final RunThese whenDeleted = new RunThese();
    private final ConcurrentFastIteratingHashSet<NARPart> local = new ConcurrentFastIteratingHashSet<>(NARPart.EmptyArray);
    public NAR nar;

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
     * optional event occurrence information.  null if not applicable.
     */
    @Nullable
    public WhenInternal event() {
        return null;
    }

    //    /** register a future deactivation 'off' of an instance which has been switched 'on'. */
    @Deprecated
    protected final void whenDeleted(Off... x) {
        for (Off xx : x)
            whenDeleted.add(xx);
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


    public final void close() {
        delete();
    }

    /**
     * resume
     */
    public final void on() {
        nar.start(this);
    }

    public boolean delete() {

        logger.info("delete {}", term());


        local.removeIf((p)->{p.delete(); return true; });
        whenDeleted.close();

        NAR n = this.nar;
        if (n !=null) {
            n.stop(this);
        } else {
            //experimental hard stop
            try {
                stopping(null);
            } catch (Throwable t) {
                logger.error("stop {} {}", term(), t);
            }
        }

        this.nar = null;
        return true;
    }


    protected final void startIn(NARPart container) {
        logger.info("start {} -> {} {}", container.term(), term(), getClass().getName());
        this.nar = container.nar;
        synchronized (this) {
            _state(Parts.ServiceState.OffToOn);
            starting(nar);
            startLocal();
            _state(Parts.ServiceState.On);
        }
    }

    protected final void stopIn(NARPart container) {
        logger.info(" stop {} -> {} {}", container.term(), term(), getClass().getName());

        synchronized (this) {
            _state(Parts.ServiceState.OnToOff);
            stopLocal();
            stopping(container.nar);
            _state(Parts.ServiceState.Off);
        }
        this.nar = null;

    }

    @Override
    protected final void start(NAR nar) {

        NAR prevNar = this.nar;
        if (!(prevNar == null || prevNar == nar))
            throw new WTF("NAR mismatch");

        logger.info("start {}", term());

        starting(this.nar = nar);

        startLocal();
    }

    private void startLocal() {
        this.local.forEach(c->c.startIn(this));
    }

    private void stopLocal() {
        this.local.forEach(c->c.stopIn(this));
    }

    @Override
    protected final void stop(NAR nar) {
        logger.info("stop {}", term());

        stopLocal();

        stopping(nar);

        this.nar = null;
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


    public final void addAll(NARPart... local) {
        for (NARPart d : local)
            add(d);
    }

    protected final void add(Off component) {
        if (component instanceof NARPart)
            add((NARPart)component);
        else
            whenDeleted.add(component);
    }


    public final void removeAll(NARPart... dd) {
        for (NARPart d : dd)
            remove(d);
    }

    public final void add(NARPart local) {
        if (!isOff())
            throw new UnsupportedOperationException(this + " is not in OFF state");

        if (this.local.add(local)) {
            //..
        } else
            throw new UnsupportedOperationException("duplicate local");
    }


    public final boolean remove(NARPart local) {
        if (this.local.remove(local)) {
            local.stop(nar);
            return true;
        } else
            throw new UnsupportedOperationException("unknown local: " + local + " in " + this);          //return false;
    }

    @Override
    public final String toString() {
        return id.toString();
    }


    /**
     * pause, returns a one-use resume ticket
     */
    public final Runnable pause() {
        NAR nn = nar;
        if (nn == null)
            throw new RuntimeException("not running");

        nn.stop(this);

        return ()->nn.start(this);
        //return new SelfDestructAfterRunningOnlyOnce(nn);
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
