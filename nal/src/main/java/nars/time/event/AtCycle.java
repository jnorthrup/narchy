package nars.time.event;

import nars.$;
import nars.NAR;
import nars.control.NARService;
import nars.term.Term;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class AtCycle extends NAREvent {

    public AtCycle(Consumer<NAR> x) {
        super(x);
    }
    public AtCycle(Term id, Consumer<NAR> x) {
        super(id, x);
    }

    abstract protected static class AtCycleService extends NARService implements Consumer<NAR> {
        static final Logger logger = LoggerFactory.getLogger(AtCycle.class);

//    protected final AtomicBoolean busy = new AtomicBoolean(false);

        protected AtCycleService(NAR nar) {
            super(nar);
        }

        @Override
        protected void starting(NAR nar) {
            on(nar.onCycle(this));
        }

        @Override
        public void accept(NAR nar) {
//        if (busy.compareAndSet(false, true)) {
            try {
                run(nar);
            } catch (Exception e) {
                logger.error("{} {}", this, e);
            }
//            } finally {
//                busy.set(false);
//            }
//        }
        }

        abstract protected void run(NAR timeAware);

    }
}