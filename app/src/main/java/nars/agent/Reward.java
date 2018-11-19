package nars.agent;

import jcog.Util;
import jcog.math.FloatSupplier;
import nars.$;
import nars.NAR;
import nars.concept.Concept;
import nars.table.BeliefTables;
import nars.table.eternal.EternalTable;
import nars.term.Termed;
import nars.time.Tense;
import nars.truth.PreciseTruth;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatFloatToObjectFunction;

import static nars.Op.BELIEF;

public abstract class Reward implements Termed, Iterable<Concept> {

    //public final FloatRange motivation = new FloatRange(1f, 0, 1f);

    protected final NAgent agent;
    private final FloatSupplier rewardFunc;

    protected transient volatile float reward = Float.NaN;


    public Reward(NAgent a, FloatSupplier r) {
    //TODO
    //public Reward(NAgent a, FloatSupplier r, float confFactor) {
        this.agent = a;
        this.rewardFunc = r;


    }

    public final NAR nar() { return agent.nar(); }

    public void update(long prev, long now, long next) {
        reward = rewardFunc.asFloat();
    }

    protected FloatFloatToObjectFunction<Truth> truther() {
        return (prev, next) -> (next == next) ?
                $.t(Util.unitize(next), nar().confDefault(BELIEF)) : null;
    }

    public float summary() {
        return reward;
    }

    public void setDefault(PreciseTruth t) {
        for (Concept c : this) {
            //TODO assert that it has no eternal tables already
            ((BeliefTables) c.beliefs()).tables.add(new EternalTable(1));
            nar().believe(c.term(), Tense.Eternal, t.freq(), t.conf());
        }

    }
}
