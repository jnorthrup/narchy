package nars.game.sensor;

import com.google.common.collect.Iterables;
import jcog.math.FloatSupplier;
import nars.NAR;
import nars.Task;
import nars.attention.AttnBranch;
import nars.attention.What;
import nars.concept.Concept;
import nars.control.channel.CauseChannel;
import nars.game.Game;
import nars.term.Term;
import nars.term.Termed;
import nars.time.When;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;

/**
 * base class for a group of concepts representing a sensor 'vector'
 */
abstract public class VectorSensor extends AbstractSensor implements Iterable<ComponentSignal> {


    public final CauseChannel<Task> in;
    protected final short[] cause;

    /** used and shared by all components */
    public final AttnBranch pri;


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

        this.pri = new AttnBranch(this.id, this);
        this.in = n.newChannel(id != null ? id : this);
        this.cause = new short[] { in.id };
    }

    /**
     * best to override
     */
    abstract public int size();// {return Iterables.size(this);}

//    /** surPRIse */
//    public double surprise() {
//        double s = 0;
//        for (Signal c : this)
//            s += ((SensorBeliefTables)c.beliefs()).surprise();
//        return s;
//    }

    @Override
    public void update(Game g) {


        float res = this.res.floatValue();

        FloatToObjectFunction<Truth> truther = Signal.truther(res, g.confDefaultBelief, g);

        When<What> w = g.nowWhat;

        //pre-commit
        int active = 0;
        for (ComponentSignal s : this) {
            //if (quality >= 1 || rng.nextFloat() < quality )
            if (s.input(truther.valueOf(s.value(g)), cause, w))
                active++;
        }
        if (active > 0) {
            //post-commit phase
            float aPri = pri.pri() / active;
            for (ComponentSignal s : this)
                s.commit(aPri, w);
        }
    }

    protected ComponentSignal newComponent(Term id, FloatSupplier f) {
        return new LambdaComponentSignal(id, f, this);
    }

    private static final class LambdaComponentSignal extends ComponentSignal {

        private final FloatSupplier f;

        public LambdaComponentSignal(Term id, FloatSupplier f, VectorSensor v) {
            super(id, v);
            this.f = f;
        }



        @Override
        protected float value(Game g) {
            return f.asFloat();
        }
    }
}
