package nars.control;

import jcog.Log;
import jcog.WTF;
import jcog.data.map.ConcurrentFastIteratingHashSet;
import jcog.event.Off;
import jcog.event.OffOn;
import jcog.event.RunThese;
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
    private final ConcurrentFastIteratingHashSet<NARPart> children = new ConcurrentFastIteratingHashSet<>(NARPart.EmptyArray);
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

        logger.info("delete {}", this);

        NAR n = this.nar;
        if (n !=null)
            stopping(this.nar);

        //assert (isOff()); // && nar == null);

        //assert(nar.remove(this);

        children.forEach(NARPart::delete);
        children.clear();

        //boolean ok = nar.remove(id); assert(ok);

        whenDeleted.close();
        this.nar = null;
        return true;
    }

    @Override
    protected final void start(NAR nar) {

        NAR prevNar = this.nar;

        if (!(this.nar == null || this.nar == nar))
            throw new WTF("NAR mismatch");

        logger.info("start {}", this);

        this.nar = nar;

        starting(nar);

        this.children.forEachWith(nar, (x,n)->{
            n.start(x);
        });
    }


    @Override
    protected final void stop(NAR nar) {
        logger.info("stop {}", this);

        try {
            this.children.forEachWith(nar, (x,n)->n.stop(x));

            stopping(nar);

        } finally {

            this.nar = null;

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
    /*abstract*/
    public boolean singleton() {
        return false;
    }


    @Override
    public Term term() {
        return id;
    }

    /**
     * TODO make sure this is atomic with some kind of on/off counter
     */
    public final void addAll(NARPart... dd) {
        for (NARPart dependency : dd)
            add(dependency);
    }

    protected final void add(Off offable) {
        whenDeleted.add(offable);
    }

    public void add(NARPart dependency) {
        if (!isOff())
            throw new UnsupportedOperationException(this + " is not in OFF state");

        if (children.add(dependency)) {
            //..
        }
    }

    public final void removeAll(NARPart... dd) {
        for (NARPart dependency : dd)
            remove(dependency);
    }

    public void remove(NARPart dependency) {
        if (children.remove(dependency)) {
            nar.stop(dependency);
        }
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
