package nars.concept.sensor;

import com.google.common.collect.Iterables;
import jcog.math.FloatSupplier;
import nars.$;
import nars.NAR;
import nars.agent.Game;
import nars.attention.AttnBranch;
import nars.concept.Concept;
import nars.control.channel.CauseChannel;
import nars.task.ITask;
import nars.term.Term;
import nars.term.Termed;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatFloatToObjectFunction;

import static nars.Op.BELIEF;

/**
 * base class for a group of concepts representing a sensor 'vector'
 */
abstract public class VectorSensor extends AbstractSensor implements Iterable<Signal> {


    public final CauseChannel<ITask> in;

    public final AttnBranch attn;


    @Override
    public Iterable<Termed> components() {
        return Iterables.transform(this, Concept::term);
    }

//    protected VectorSensor(@Nullable FloatSupplier input, @Nullable Term id, NAR nar) {
//        this(input, id, (prev, next) -> next==next ? $.t(Util.unitize(next), nar.confDefault(BELIEF)) : null, nar);
//    }


    protected VectorSensor(NAR n) {
        this(null, n);
    }
    protected VectorSensor(Term rootID, NAR n) {
        super(rootID, n);

        attn = new AttnBranch(rootID, this);

        this.in = newChannelIn(n);
    }

    protected CauseChannel<ITask> newChannelIn(NAR nar) {
        return nar.newChannel(id != null ? id : this);
    }

    /**
     * best to override
     */
    public int size() {
        return Iterables.size(this);
    }


    @Override
    public void update(Game g) {

        float confDefault = nar.confDefault(BELIEF);
        float min = nar.confMin.floatValue();
        FloatFloatToObjectFunction<Truth> truther = (p, n) -> {
            float c = confDefault;// * Math.abs(n - 0.5f) * 2f;
            return c > min ? $.t(n, c) : null;
        };

        short cause = in.id;
        FloatSupplier aPri = attn::pri;
        for (Signal s : this) {
            s.update(truther, aPri, cause, g);
        }
    }

}
