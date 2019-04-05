package nars.time.part;

import nars.NAR;
import nars.control.Part;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/** per-cycle invoked part */
abstract public class CycPart extends Part implements Consumer<NAR> {

    static final Logger logger = LoggerFactory.getLogger(CycPart.class);

//    protected final AtomicBoolean busy = new AtomicBoolean(false);

    protected CycPart(NAR nar) {
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
