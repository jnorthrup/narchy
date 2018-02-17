package nars;

import jcog.event.On;
import nars.control.DurService;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Function;

/** attaches sub-NAR to a super-NAR as an isolated dependent context
 * and basis for task delegation, communication, and other forms
 * of hierarchical interaction */
public class SubNAR extends DurService {

    private final Function<NAR, NAR> subNAR;
    private NAR sub = null;
    private final List<BiConsumer<NAR,NAR>> onDur = new CopyOnWriteArrayList<>();

    public SubNAR(NAR superNAR, Function<NAR,NAR> subNAR) {
        super(superNAR);
        this.subNAR = subNAR;
    }

    @Override
    protected void start(NAR nar) {
        synchronized(this) {
            super.start(nar);
            this.sub = subNAR.apply(nar);
        }
    }

    @Override
    protected void stopping(NAR nar) {
        synchronized(this) {
            this.sub.stop();
            this.sub = null;
        }
    }

    @Override
    protected void run(NAR n, long dt) {
        NAR s = this.sub;
        if (s !=null)
            onDur.forEach(x->x.accept(nar, s));
    }

    public On onDur(BiConsumer<NAR,NAR> interact) {
        onDur.add(interact);
        return new On() {
            @Override public void off() {
                onDur.remove(interact);
            }
        };
    }

    /** TODO unidirectional task stream buffered by a bag
     *      receiver flow control
     *      backpressure management
     *      metrics collection
     *      input/output task transformation
     *
     *  TODO implement InterNAR using this class
     */
    public static class TaskStream {

    }

    /** TODO bidirectional pair of TaskStreams */
    public static class TaskChannel {

    }
}
