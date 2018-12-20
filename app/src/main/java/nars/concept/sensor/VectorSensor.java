package nars.concept.sensor;

import com.google.common.collect.Iterables;
import nars.$;
import nars.NAR;
import nars.attention.AttVectorNode;
import nars.concept.NodeConcept;
import nars.control.channel.CauseChannel;
import nars.table.dynamic.SeriesBeliefTable;
import nars.task.ITask;
import nars.term.Termed;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatFloatToObjectFunction;

import static nars.Op.BELIEF;

/**
 * base class for a group of concepts representing a sensor 'vector'
 */
abstract public class VectorSensor extends AbstractSensor implements Iterable<Signal> {


    public final CauseChannel<ITask> in;

    public final AttVectorNode attn;


    @Override
    public Iterable<Termed> components() {
        return Iterables.transform(this, NodeConcept::term);
    }

//    protected VectorSensor(@Nullable FloatSupplier input, @Nullable Term id, NAR nar) {
//        this(input, id, (prev, next) -> next==next ? $.t(Util.unitize(next), nar.confDefault(BELIEF)) : null, nar);
//    }

    protected VectorSensor(NAR nar) {
        super(nar);

        attn = new AttVectorNode(this, this);

        this.in = newChannel(nar);
    }

    protected CauseChannel<ITask> newChannel(NAR nar) {
        return nar.newChannel(id != null ? id : this);
    }

    /**
     * best to override
     */
    public int size() {
        return Iterables.size(this);
    }


    @Override
    public void update(long last, long now, long next, NAR nar) {

        float confDefault = nar.confDefault(BELIEF);
        float min = nar.confMin.floatValue();
        FloatFloatToObjectFunction<Truth> truther = (p, n) -> {
            float c = confDefault;// * Math.abs(n - 0.5f) * 2f;
            return c > min ? $.t(n, c) : null;
        };

        for (Signal s : this) {

            SeriesBeliefTable.SeriesRemember r = s.update(last, now, truther, this.nar);

            if (r != null) {
                attn.ensure(r.input, attn.elementPri(this.nar));
                in.input(r);
            }
        }
    }

}
