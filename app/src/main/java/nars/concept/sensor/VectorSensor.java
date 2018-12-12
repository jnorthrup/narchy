package nars.concept.sensor;

import com.google.common.collect.Iterables;
import nars.$;
import nars.NAR;
import nars.attention.AttnDistributor;
import nars.concept.NodeConcept;
import nars.control.channel.CauseChannel;
import nars.table.dynamic.SeriesBeliefTable;
import nars.task.ITask;
import nars.term.Termed;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatFloatToObjectFunction;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static nars.Op.BELIEF;

/**
 * base class for a group of concepts representing a sensor 'vector'
 */
abstract public class VectorSensor extends AbstractSensor implements Iterable<Signal> {


    public final CauseChannel<ITask> in;

    private AttnDistributor attn;


    @Override
    public Iterable<Termed> components() {
        return Iterables.transform(this, NodeConcept::term);
    }

//    protected VectorSensor(@Nullable FloatSupplier input, @Nullable Term id, NAR nar) {
//        this(input, id, (prev, next) -> next==next ? $.t(Util.unitize(next), nar.confDefault(BELIEF)) : null, nar);
//    }

    protected VectorSensor(NAR nar) {
        super(nar);

        this.in = nar.newChannel(id != null ? id : this);
    }

    /**
     * best to override
     */
    public int size() {
        return Iterables.size(this);
    }


//    @Override
//    protected void update(long prev, long now, Iterable<Signal> signals, CauseChannel<ITask> in, FloatFloatToObjectFunction<Truth> t) {
//        if (attn == null) //HACK
//            attn = new AttnDistributor(this, pri::set, nar);
//
//        super.update(prev, now, signals, in, t);
//    }

    @Override
    public void update(long last, long now, long next, NAR nar) {
        if (attn == null) //HACK
            attn = new AttnDistributor(this, pri::set, nar);

        updateSensor(last, now, nar).forEach(x -> {
            if (x != null) {
                pri(x.input);
                in.input(x);
            }
        });
    }

    public final Stream<SeriesBeliefTable.SeriesRemember> updateSensor(long last, long now, NAR nar) {
        float confDefault = nar.confDefault(BELIEF);
        float min = nar.confMin.floatValue();

        int dur = VectorSensor.this.nar.dur();
        return updateSensor(last, now, (p, n) -> {
            float c = confDefault;// * Math.abs(n - 0.5f) * 2f;
            return c > min ? $.t(n, c) : null;
        }, dur);
    }

    protected final Stream<SeriesBeliefTable.SeriesRemember> updateSensor(long last, long now, FloatFloatToObjectFunction<Truth> truther, int dur) {
        return StreamSupport.stream(spliterator(), false).map(
                s -> s.update(last, now, truther, dur, nar));
    }

}
