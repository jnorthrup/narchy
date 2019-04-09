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

import java.lang.ref.WeakReference;

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

    /**
     * disables this part but does not remove it
     */
    public void close() {
        NAR n = nar;
        if (n != null) {
            boolean ok = n.stop(id);
            //assert (ok);
        }
    }

    /**
     * resume
     */
    public void on() {
        nar.start(id);
    }

    public boolean delete() {
        assert (isOff() && nar == null);

        children.forEach(NARPart::delete);
        children.clear();

        //boolean ok = nar.remove(id); assert(ok);

        whenDeleted.close();
        return true;
    }

    @Override
    protected final void start(NAR nar) {

        if (this.nar != null)
            throw new WTF("already started?");

        logger.debug("start {}", this);

        if (!(this.nar == null || this.nar == nar))
            throw new WTF("NAR mismatch");

        this.nar = nar;

        starting(nar);

        this.children.forEachWith(nar, (x,n)->{
            n.start(x);
        });
    }


    @Override
    protected final void stop(NAR nar) {
        try {
            this.children.forEachWith(nar, (x,n)->n.stop(x));

            stopping(nar);

        } finally {

            logger.debug("stop {}", this);
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
        assert (isOff());
        if (children.add(dependency)) {
            //if (isOnOrStarting())
            //nar.start(dependency);
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

        WeakReference<NAR> wnar = new WeakReference(nn);
        return () -> {
            NAR n = wnar.get();
            if (n != null) {
                n.start(this);
                wnar.clear();
            }
        };
    }


}
