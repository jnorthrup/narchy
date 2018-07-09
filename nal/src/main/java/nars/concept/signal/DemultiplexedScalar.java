package nars.concept.signal;

import jcog.Util;
import jcog.math.FloatSupplier;
import jcog.util.AtomicFloat;
import nars.$;
import nars.NAR;
import nars.control.NARService;
import nars.control.channel.CauseChannel;
import nars.task.ITask;
import nars.term.Term;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatFloatToObjectFunction;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.stream.StreamSupport;

import static nars.Op.BELIEF;

/** base class for a multi-concept representation of a real scalar value input */
abstract public class DemultiplexedScalar extends NARService implements Iterable<Signal>, Consumer<NAR>, FloatSupplier {

    public final AtomicFloat value = new AtomicFloat();

    public final CauseChannel<ITask> in;

    public final FloatSupplier input;

    private final FloatFloatToObjectFunction<Truth> truther;

    public final Term term;

    private long last;

    @Override
    public float asFloat() {
        return value.floatValue();
    }


    protected DemultiplexedScalar(@Nullable FloatSupplier input, @Nullable Term id, NAR nar) {
        this(input, id, nar, (prev,next) -> next==next ? $.t(Util.unitize(next), nar.confDefault(BELIEF)) : null);
    }

    protected DemultiplexedScalar(@Nullable FloatSupplier input, @Nullable Term id, NAR nar, FloatFloatToObjectFunction<Truth> truther) {
        super(id);

        this.term = id;

        this.last = nar.time();
        this.input = input; 
        this.in = nar.newChannel(id);
        this.truther = truther;
    }

    public DemultiplexedScalar resolution(float r) {
        forEach(s -> s.resolution(r));
        return this;
    }


    @Override
    public void accept(NAR n) {
        synchronized (this) {
            long now = n.time();

            
            update(last, now, Math.max((int) (now - last), 1), n);

            this.last = now;
        }
    }

    public void update(long start, long end, int dur, NAR n) {

        if (input!=null)
            value.set(input.asFloat());

        in.input(StreamSupport.stream(this.spliterator(), false)
                .map(x -> x.update(start, end, truther, dur, n)));
    }

    public void pri(FloatSupplier p) {
        forEach(x -> x.pri(p));
    }
}
