package nars.game.sensor;

import jcog.math.FloatSupplier;
import nars.NAR;
import nars.Task;
import nars.attention.PriAmp;
import nars.attention.PriBranch;
import nars.attention.What;
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
public abstract class VectorSensor extends AbstractSensor implements Iterable<ComponentSignal> {


    public final CauseChannel<Task> in;


    /** used and shared by all components */
    public final PriAmp pri;

    protected VectorSensor(Term rootID, NAR n) {
        super(rootID, n);

        this.pri = new PriBranch(this.id, this);
        this.in = n.newChannel(id != null ? id : this);
    }

    /**
     * best to override
     */
    public abstract int size();// {return Iterables.size(this);}

//    /** surPRIse */
//    public double surprise() {
//        double s = 0;
//        for (Signal c : this)
//            s += ((SensorBeliefTables)c.beliefs()).surprise();
//        return s;
//    }

    @Override
    public void accept(Game g) {


        float res = Math.max(g.nar.freqResolution.floatValue(), this.res.floatValue());

        FloatToObjectFunction<Truth> truther = Signal.truther(res, g.confDefaultBelief, g);

        When<What> wLoop = g.nowLoop;

        //pre-commit
        int active = 0;
        Term why = in.why.why;
        for (ComponentSignal s : this) {
            //if (quality >= 1 || rng.nextFloat() < quality )
            if (s.input(truther.valueOf(s.value(g)), why, wLoop))
                active++;
        }
        if (active > 0) {
            //post-commit phase
            float aPri = pri.pri() / active;
            for (ComponentSignal s : this)
                s.commit(aPri, wLoop);
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


    @Override
    public final Iterable<? extends Termed> components() {
        return this;
    }
}
