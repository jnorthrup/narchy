package nars.concept.scalar;

import com.google.common.collect.Iterables;
import jcog.math.FloatSupplier;
import jcog.util.AtomicFloat;
import nars.$;
import nars.NAR;
import nars.control.CauseChannel;
import nars.control.NARService;
import nars.task.ITask;
import nars.term.Term;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatFloatToObjectFunction;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

import static nars.Op.BELIEF;

/** base class for a multi-concept representation of a real scalar value input */
abstract public class DemultiplexedScalar extends NARService implements Iterable<Scalar>, Consumer<NAR>, FloatSupplier {

    public final AtomicFloat value = new AtomicFloat();

    public final CauseChannel<ITask> in;

    public final FloatSupplier input;

    private final FloatFloatToObjectFunction<Truth> truther;

    public final Term term;

    @Override
    public final float asFloat() {
        return value.floatValue();
    }

    protected DemultiplexedScalar(FloatSupplier input, @Nullable Term id, NAR nar) {
        super(id);

        this.term = id;

        this.input = input;
        this.in = nar.newCauseChannel(id);
        this.truther = (prev,next) -> $.t(next, nar.confDefault(BELIEF));
    }

    public DemultiplexedScalar resolution(float r) {
        forEach(s -> s.resolution(r));
        return this;
    }


    @Override
    public void accept(NAR n) {
        update(n.time(), n.dur(), n);
    }

    public void update(long now, int dur, NAR n) {

        value.set(input.asFloat());

        in.input(Iterables.transform(this, x -> x.update(truther, now, dur, n)));
    }

}
