package nars.control;

import jcog.Services;
import jcog.event.Ons;
import nars.$;
import nars.NAR;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.Nullable;

public class NARService extends Services.AbstractService<NAR> implements Termed {


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
    protected synchronized void start(NAR nar) {
        ons = new Ons(nar.eventClear.on(n -> clear())) {
            @Override public void off() {
                super.off();
                stop(nar);
            }
        };
    }

    public void clear() {
        //default: nothing
    }


    @Override
    protected final synchronized void stop(NAR nar) {
        stopping(nar);
        nar.remove(this.id);
    }

    protected void stopping(NAR nar) {

    }

    public synchronized void off() {
        if (ons!=null) {
            ons.off();
            ons = null;
        }
    }

    @Override
    public final Term term() {
        return id;
    }

}
