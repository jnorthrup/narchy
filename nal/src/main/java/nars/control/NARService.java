package nars.control;

import jcog.Services;
import jcog.event.Ons;
import nars.$;
import nars.NAR;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NARService extends Services.AbstractService<NAR> implements Termed {

    static final Logger logger = LoggerFactory.getLogger(NARService.class);

    public final Term id;
    protected Ons ons;

    @Deprecated protected NARService(NAR nar) {
        this(nar, null);
    }

    protected NARService(@Nullable NAR nar, @Nullable Term id) {

        this.id = id!=null ? id :
            $.p($.quote(getClass().getName()), $.the(System.identityHashCode(this)) );

        if (nar!=null)
            nar.on(this);
    }

    @Override
    protected void start(NAR nar) {
        synchronized (this) {
            logger.info("start {}", id);
            ons = new Ons(nar.eventClear.on(n -> clear())) {
                @Override
                public void off() {
                    super.off();
                    stop(nar);
                }
            };
        }
    }

    public void clear() {
        //default: nothing
    }


    @Override
    protected final void stop(NAR nar) {
        synchronized (this) {
            logger.info("stop {}", id);
            stopping(nar);
            nar.services.remove(this.id);
        }
    }

    protected void stopping(NAR nar) {

    }

    public void off() {
        synchronized (this) {
            if (ons != null) {
                ons.off();
                ons = null;
            }
        }
    }

    @Override
    public final Term term() {
        return id;
    }

}
