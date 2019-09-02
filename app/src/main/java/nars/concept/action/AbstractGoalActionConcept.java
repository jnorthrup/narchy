package nars.concept.action;

import nars.NAL;
import nars.NAR;
import nars.Task;
import nars.agent.Game;
import nars.concept.action.curiosity.Curiosity;
import nars.concept.action.curiosity.CuriosityTask;
import nars.control.channel.CauseChannel;
import nars.control.op.Remember;
import nars.table.BeliefTable;
import nars.table.BeliefTables;
import nars.table.dynamic.SensorBeliefTables;
import nars.table.dynamic.SeriesBeliefTable;
import nars.table.eternal.EternalDefaultTable;
import nars.table.temporal.RTreeBeliefTable;
import nars.task.util.Answer;
import nars.task.util.series.RingBufferTaskSeries;
import nars.task.util.signal.SignalTask;
import nars.term.Term;
import nars.time.Tense;
import nars.time.When;
import nars.truth.Truth;
import nars.truth.proj.TruthProjection;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

import static nars.Op.GOAL;
import static nars.concept.sensor.Signal.truthDithered;
import static nars.time.Tense.TIMELESS;
import static nars.truth.func.TruthFunctions.w2cSafeDouble;


/**
 * ActionConcept which is driven by Goals that are interpreted into feedback Beliefs
 */
public class AbstractGoalActionConcept extends GameAction {


    @Nullable private Curiosity curiosity = null;


    /** disables revision merge so that revisions, not being CuriosityTask and thus intercepted, cant directly
     *  contaminate the normal derived goal table
     *  and compete (when curiosity confidence is stronger) with authentic derived
     *  goals which we are trying to learn to form and remember. */
    private final SeriesBeliefTable curiosityTable;

    /**
     * current estimate
     */
    protected @Nullable Truth actionTruth, beliefTruth;

    /**
     * instantaneous truth (implemented as avg truth integrated over a finite present-moment answer interval)
     */
    protected @Nullable Truth actionDex;

    /** current action coherence value */
    private double actionCoh;

    /**
     * latches the last non-null actionDex, used for echo/sustain
     * TODO make this more complex like a replay buffer in DQN
     */
    public @Nullable Truth lastNonNullActionDex;

    final short[] causeArray;

    protected AbstractGoalActionConcept(Term term, NAR n) {
        this(term, new RTreeBeliefTable(), n);
    }

    protected AbstractGoalActionConcept(Term term, BeliefTable mutableGoals, NAR n) {
        super(term,
                new SensorBeliefTables(term, true)
                        .minSurprise(NAL.signal.SENSOR_SURPRISE_MIN_DEFAULT_MOTOR) //action concept pay constant attention attention even to boring feedback
                ,new BeliefTables(),
                n);

        causeArray = new short[] { n.newCause(term).id };

        /** make sure to add curiosity table first in the list, as a filter */
        BeliefTables GOALS = ((BeliefTables) goals());
        GOALS.add(curiosityTable =
                new CuriosityBeliefTable(term));
        GOALS.add(mutableGoals);
    }

    public void goalDefault(Truth t, NAR n) {
        EternalDefaultTable.add(this, t, GOAL, n);
    }

    protected CauseChannel<Task> channel(NAR n) {
        return n.newChannel(this);
    }

    public AbstractGoalActionConcept updateCuriosity(Curiosity curiosity) {
        this.curiosity = curiosity;
        return this;
    }




    @Override
    public double dexterity() {
        Truth t = actionDex;
        return t != null ? w2cSafeDouble(t.evi()) : 0;
    }

    @Override
    public double coherency() {
        return actionCoh;
    }
    //    /** in cycles; controls https://en.wikipedia.org/wiki/Legato vs. https://en.wikipedia.org/wiki/Staccato */
//    float actionWindowDexDurs =
//            //0;
//            //0.5f;
//            1;
//
//    float actionWindowCuriDurs =
//            //0;
//            //0.5f;
//            1f;


    static final Predicate<Task> withoutCuriosity =
            t -> !(t instanceof CuriosityTask);// && !t.isEternal();  /* filter curiosity tasks? */

//    public final org.eclipse.collections.api.tuple.Pair<Truth, long[]> truth(boolean beliefsOrGoals, int componentsMax, long prev, long now, NAR n) {
//        return truth(beliefsOrGoals, componentsMax, prev, now, n.dur(), n);
//    }

    @Nullable public TruthProjection truth(boolean beliefsOrGoals, int componentsMax, When<NAR> g, int shift) {
        BeliefTable t = (beliefsOrGoals ? beliefs() : goals());

        if (t.isEmpty())
            return null;

        int limit = componentsMax, tries = (int)Math.ceil(limit * NAL.ANSWER_TRYING);

        float dur = g.dur;
        long s = g.start+shift, e = g.end+shift;
        Answer a = Answer.taskStrength(true, limit, s, e, term,
                withoutCuriosity
                //null
                , g.x).dur(dur);

        a.clear(tries).time(s, e);

        t.match(a);

        return a.truthProjection(true);
    }


    @Override
    public void update( Game g) {

        int limitBelief = NAL.ANSWER_BELIEF_MATCH_CAPACITY;
        int limitGoal = NAL.ANSWER_ACTION_ANSWER_CAPACITY;

        int perceptShift = (int)((g.when.end - g.when.start) * NAL.ACTION_DESIRE_SHIFT_DUR); //half dur

        this.beliefTruth = truth(truth(true, limitBelief, g.when, perceptShift));

        updateCuriosity(g.curiosity);

        this.actionTruth = actionTruth(limitGoal, g, perceptShift);


    }

    private  @Nullable Truth truth(TruthProjection t) {
        return t!=null ? t.truth(NAL.truth.EVI_MIN, false, false, null) : null;
    }

    private Truth actionTruth(int limit, Game g, int shift) {

        TruthProjection gt = truth(false, limit, g.when, shift);

        Truth nextActionDex = truth(gt);
        actionDex = nextActionDex;
        actionCoh = nextActionDex != null ? gt.coherency() : 0;
        if (nextActionDex != null)
            lastNonNullActionDex = actionDex;

        Truth actionCuri = curiosity.curiosity(this);

        long s = g.when.start, e = g.when.end;
        float dur = g.when.dur;
        NAR n = g.nar;

        Curiosity.CuriosityInjection curiosityInject;
        if (actionCuri != null) {
            curiosityInject = Curiosity.CuriosityInjection.Override;

//            float confMin = n.confMin.floatValue();
//            if (actionCuri.conf() < confMin) {
//                //boost sub-threshold confidence to minimum
//                actionCuri = $.t(actionCuri.freq(), confMin);
//            }

            //pre-load curiosity for the future
            if (curiosity.goal.getOpaque()) {
                long lastCuriosity = curiosityTable.end();
                long curiStart = lastCuriosity != TIMELESS ? Math.max(s, lastCuriosity + 1) : s;
                long curiEnd = Math.round(curiStart + dur * NAL.CURIOSITY_TASK_RANGE_DURS); //(1 + (curiosity.Math.max(curiStart, e);

                long[] se = Tense.dither(new long[] { curiStart, curiEnd }, n);
                curiStart = se[0];
                curiEnd = se[1];

                g.what().accept(
                        curiosity(actionCuri /*goal*/, curiStart, curiEnd, n)
                );
            }


        } else {
            curiosityInject = curiosity.injection.get();

            //use existing curiosity
            Answer a = Answer.
				taskStrength(true, 2, s, e, term, null, n)
                    .dur(dur)
                    .match(curiosityTable);
            actionCuri = a.truth();
        }

        if (actionDex != null || actionCuri != null) {
            actionTruth = curiosityInject.inject(actionDex, actionCuri);
        } else {
            actionTruth = null;
        }

        return actionTruth;
    }



    @Nullable SignalTask curiosity(Truth goal, long pStart, long pEnd, NAR n) {
        long[] evi = n.evidence();

        SignalTask t = new CuriosityTask(term, goal, n.time(), pStart, pEnd, evi);
        t.priMax(attn.pri());
        t.cause(causeArray);
        return t;
    }

    protected void feedback(@Nullable Truth f, short[] cause, Game g) {

        if (f!=null)
            f = truthDithered(f.freq(), resolution().floatValue(), g);

        ((SensorBeliefTables) beliefs()).input(f, attn::pri, cause, g.what(), g.when,true);
    }



    public Truth actionTruth() {
        return actionTruth;
    }


    private static class CuriosityBeliefTable extends SeriesBeliefTable {
        CuriosityBeliefTable(Term term) {
            super(term, false, new RingBufferTaskSeries<>(NAL.CURIOSITY_CAPACITY));
        }

        @Override
        @Deprecated public void remember(Remember r) {
            if (r.input instanceof CuriosityTask) {
                add(r.input);
                r.remember(r.input);
            }
        }
    }
}
