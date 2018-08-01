package nars.concept.sensor;

import jcog.Util;
import jcog.math.FloatRange;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.concept.PermanentConcept;
import nars.concept.TaskConcept;
import nars.table.dynamic.DynamicBeliefTable;
import nars.table.eternal.EternalTable;
import nars.task.signal.SignalTask;
import nars.term.Term;
import nars.term.Termed;
import nars.time.Tense;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.LongToFloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * dynamically computed time-dependent function
 */
public class Scalar extends TaskConcept implements Sensor, PermanentConcept {

    private FloatRange pri, res;

    public Scalar(Term c, LongToFloatFunction belief, NAR n) {
        this(c, belief, null, n);
    }

    public Scalar(Term term, @Nullable LongToFloatFunction belief, @Nullable LongToFloatFunction goal, NAR n) {
        super(term,
                belief != null ? new ScalarBeliefTable(term, true, belief, n) : n.conceptBuilder.newTable(term, true),
                goal != null ? new ScalarBeliefTable(term, false, goal, n) : n.conceptBuilder.newTable(term, false),
                n.conceptBuilder);

        pri = FloatRange.unit(goal!=null ? n.goalPriDefault : n.beliefPriDefault );
        res = FloatRange.unit(n.freqResolution);

        if (belief!=null) {
            ((ScalarBeliefTable) beliefs()).setPri(pri);
            ((ScalarBeliefTable) beliefs()).setRes(res);
        }
        if (goal!=null) {
            ((ScalarBeliefTable) goals()).setPri(pri);
            ((ScalarBeliefTable) goals()).setRes(res);
        }
    }

    @Override
    public Iterable<Termed> components() {
        return List.of(this);
    }

    @Override
    public void update(long last, long now, NAR nar) {
        //?
    }

    @Override
    public FloatRange resolution() {
        return res;
    }

    @Override
    public FloatRange pri() {
        return pri;
    }

    /** samples at time-points */
    static class ScalarBeliefTable extends DynamicBeliefTable {

        FloatRange pri, res;

        private final LongToFloatFunction value;
        final long stampSeed;
        final long stampStart;

        protected ScalarBeliefTable(Term term, boolean beliefOrGoal, LongToFloatFunction value, NAR n) {
            super(term, beliefOrGoal, EternalTable.EMPTY,
                    n.conceptBuilder.newTemporalTable(term));
            stampStart = n.time();

            this.stampSeed = term.hashCode();

            this.value = value;
        }

        protected void setPri(FloatRange pri) {
            this.pri = pri;
        }

        protected void setRes(FloatRange res) {
            this.res = res;
        }

        @Override
        protected Task taskDynamic(long start, long end, Term template /* ignored */, NAR nar) {
            long mid = Tense.dither((start+end)/2L, nar);
            Truth t = truthDynamic(mid, mid, template, nar);
            if (t == null)
                return null;
            else {

                long stampDelta = mid - stampStart; //TODO optional dithering
                long stamp = stampDelta ^ stampSeed; //hash
                SignalTask tt = new SignalTask(term, punc(), t, nar.time(), mid, mid, stamp);
                tt.pri(pri.get());
                return tt;
            }
        }

        @Override
        protected @Nullable Truth truthDynamic(long start, long end, Term template /* ignored */, NAR nar) {
            long t = (start + end) / 2L; //time-point
            float f = value.valueOf(t);
            return f == f ? truth(Util.round(f, res.get()), nar) : null;
        }

        /** truther: value -> truth  */
        protected Truth truth(float v, NAR n) {
            return $.t(v, n.confDefault(punc()));
        }

    }
}
