package nars.control;

import jcog.Util;
import jcog.WTF;
import jcog.event.Off;
import jcog.event.Offs;
import jcog.service.Service;
import nars.$;
import nars.NAR;
import nars.term.Term;
import nars.term.Termed;
import nars.time.event.InternalEvent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 *
 * a runtime component of a NAR.
 *
 * it is given a generic name such as 'Part' to imply generality in
 * whatever may be
 *          dynamically attached
 *          interfaced with
 *          integrated to
 * any reasoner instance (whether temporarily or permanently).
 *
 *
 * they are dynamically registerable and de-registerable.
 * multiple instances of the same type of Part may be present unless explicitly restricted singleton.
 *
 * this is meant to be flexible and dynamic, to support runtime metaprogramming, telemetry, performance metrics
 *   accessible to both external control and internal reasoner decision processes.
 *
 * TODO methods of accessing parts by different features:
 *      --function
 *      --class
 *      --name
 *      --etc
 *
 *
 * */
public class Part extends Service<NAR> implements Termed {

    private static final Logger logger = Util.logger(Part.class);

    public final Term id;
    private final Term instanceID;

    /** TODO encapsulate */
    private final Offs ons = new Offs();


    protected NAR nar;

    protected Part() {
        this((NAR)null);
    }

    protected Part(NAR nar) {
        this(null, nar);
    }

    protected Part(Term id, NAR nar) {
        this(id);

        if (nar != null)
            (this.nar = nar).add(this);
    }

    protected Part(@Nullable Term id) {
        Term instanceID = $.identity(this);
        this.id = id != null ? id : instanceID;
        this.instanceID = id == null ? instanceID : $.p(id,instanceID);
    }

    /** optional event occurrence information.  null if not applicable. */
    @Nullable public InternalEvent event() {
        return null;
    }

    /** register a future deactivation 'off' of an instance which has been switched 'on'. */
    protected final void on(Off... x) {
        for (Off xx : x)
            ons.add(xx);
    }


    public final void off() {
        NAR n = nar;
        if (n != null) {
            n.part.remove(id);
        }
    }

    @Override
    protected final void start(NAR nar) {

        logger.debug("start {}", id());

        if (!(this.nar == null || this.nar == nar))
            throw new WTF("NAR mismatch");

        this.nar = nar;

        starting(nar);
    }



    @Override
    protected final void stop(NAR nar) {

        this.ons.off();

        stopping(nar);

        logger.debug("stop {}", id());
    }


    protected void starting(NAR nar) {

    }

    protected void stopping(NAR nar) {

    }

    public final Term id() {
        //return id;
        return instanceID;
    }

    @Override
    public Term term() {
        return id;
    }

    //    public Term instanceID() {
//        return instanceID;
//    }
//
//    @Override
//    public final Term term() {
//        return id;
//    }

}
