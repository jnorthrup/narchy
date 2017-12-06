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
    protected void start(NAR nar) {
        ons = new Ons();
        ons.add(nar.eventClear.on(n -> clear()));
    }

    public void clear() {
        //default: nothing
    }


    @Override
    protected void stop(NAR nar) {
        stop();
    }

    public synchronized void stop() {
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
