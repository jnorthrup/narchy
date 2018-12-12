package nars.agent;

import jcog.Util;
import jcog.math.FloatSupplier;
import nars.$;
import nars.NAR;
import nars.concept.Concept;
import nars.concept.sensor.Signal;
import nars.control.channel.CauseChannel;
import nars.table.BeliefTables;
import nars.table.dynamic.SeriesBeliefTable;
import nars.table.eternal.EternalTable;
import nars.task.ITask;
import nars.term.Termed;
import nars.time.Tense;
import nars.truth.PreciseTruth;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatFloatToObjectFunction;

import java.util.Objects;
import java.util.stream.Stream;

import static nars.Op.BELIEF;

public abstract class Reward implements Termed, Iterable<Signal> {

    //public final FloatRange motivation = new FloatRange(1f, 0, 1f);

    protected final NAgent agent;
    private final FloatSupplier rewardFunc;

    protected transient volatile float reward = Float.NaN;

    protected final CauseChannel<ITask> in;
    transient private float pri;

    public Reward(NAgent a, FloatSupplier r) {
    //TODO
    //public Reward(NAgent a, FloatSupplier r, float confFactor) {
        this.agent = a;
        this.rewardFunc = r;

        in = a.nar().newChannel(this);

    }

    public final NAR nar() { return agent.nar(); }

    public void pri(float pri) {
        this.pri = pri;
    }

    public final void update(long prev, long now, long next) {
        reward = rewardFunc.asFloat();
        in.input(updateReward(prev, now, next, pri).filter(Objects::nonNull));
    }

    abstract protected Stream<SeriesBeliefTable.SeriesRemember> updateReward(long prev, long now, long next, float pri);

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
