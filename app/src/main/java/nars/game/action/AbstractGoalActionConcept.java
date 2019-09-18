package nars.game.action;

import nars.NAL;
import nars.NAR;
import nars.attention.What;
import nars.game.Game;
import nars.table.BeliefTable;
import nars.table.BeliefTables;
import nars.table.dynamic.SensorBeliefTables;
import nars.table.eternal.EternalDefaultTable;
import nars.table.temporal.RTreeBeliefTable;
import nars.task.util.Answer;
import nars.term.Term;
import nars.time.When;
import nars.truth.Truth;
import nars.truth.proj.TruthProjection;
import org.jetbrains.annotations.Nullable;

import static nars.Op.GOAL;
import static nars.truth.func.TruthFunctions.w2cSafeDouble;


/**
 * ActionConcept which is driven by Goals that are interpreted into feedback Beliefs
 */
public abstract class AbstractGoalActionConcept extends ActionSignal {

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

    @Nullable private Truth nextFeedback = null;

    protected AbstractGoalActionConcept(Term term, NAR n) {
        this(term, new RTreeBeliefTable(), n);
    }

    protected AbstractGoalActionConcept(Term term, BeliefTable mutableGoals, NAR n) {
        super(term,
                new SensorBeliefTables(term, true)
                        .minSurprise(NAL.signal.SENSOR_SURPRISE_MIN_DEFAULT_MOTOR) //action concept pay constant attention attention even to boring feedback
                ,new BeliefTables(),
                n);

        /** make sure to add curiosity table first in the list, as a filter */
        BeliefTables GOALS = ((BeliefTables) goals());
        GOALS.add(mutableGoals);
        //GOALS.add(curiosityTable = new CuriosityBeliefTable(term));
    }

    public void goalDefault(Truth t, NAR n) {
        EternalDefaultTable.add(this, t, GOAL, n);
    }

//    public AbstractGoalActionConcept updateCuriosity(Curiosity curiosity) {
//        this.curiosity = curiosity;
//        return this;
//    }

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


//    static final Predicate<Task> withoutCuriosity =
//            t -> !(t instanceof CuriosityTask);// && !t.isEternal();  /* filter curiosity tasks? */

//    public final org.eclipse.collections.api.tuple.Pair<Truth, long[]> truth(boolean beliefsOrGoals, int componentsMax, long prev, long now, NAR n) {
//        return truth(beliefsOrGoals, componentsMax, prev, now, n.dur(), n);
//    }

    @Nullable public TruthProjection truth(boolean beliefsOrGoals, int componentsMax, When<What> g, int shift) {
        BeliefTable t = (beliefsOrGoals ? beliefs() : goals());

        if (t.isEmpty())
            return null;

        int limit = componentsMax, tries = (int)Math.ceil(limit * NAL.ANSWER_TRYING);

        float dur = g.dur;
        long s = g.start+shift, e = g.end+shift;
        Answer a = Answer.taskStrength(true, limit, s, e, term, null, g.x.nar).dur(dur);

        a.clear(tries).time(s, e);

        t.match(a);

        return a.truthProjection();
    }


    @Override
    public void update( Game g) {

        int limitBelief = NAL.ANSWER_BELIEF_MATCH_CAPACITY;
        int limitGoal = NAL.ANSWER_ACTION_ANSWER_CAPACITY;

        When<What> w = g.nowWhat;

        int perceptShift = (int) ((w.end - w.start) * NAL.ACTION_DESIRE_SHIFT_DUR); //half dur

        this.nextFeedback = updateAction(
            this.beliefTruth = truth(truth(true, limitBelief, w, perceptShift), w, perceptShift),
            this.actionTruth = actionTruth(limitGoal, w, perceptShift),
            g);

        input(nextFeedback, pri(), cause, w);
    }

    /** returns feedback truth value */
    @Nullable abstract protected Truth updateAction(@Nullable Truth beliefTruth, @Nullable Truth actionTruth, Game g);

    private  @Nullable Truth truth(@Nullable TruthProjection t, When when, int shift) {
        return t!=null ?
            t.dur(when.dur).truth(when.start + shift, when.end + shift, NAL.truth.EVI_MIN,
            false, false, null) :
            null;
    }

    private Truth actionTruth(int limit, When<What> w, int shift) {
        float dur = w.dur;

        TruthProjection gt = truth(false, limit, w, shift);
        if (gt!=null) {
            gt.dur(dur);

            Truth nextActionDex = truth(gt, w, shift);
            actionDex = nextActionDex;
            actionCoh = nextActionDex != null ? gt.coherency() : 0;

        } else {
            actionDex = null;
            actionCoh = 0;
        }


//        Truth actionCuri = curiosity.curiosity(this);
//
//        long s = g.when.start, e = g.when.end;
//        NAR n = g.nar;
//
//        Curiosity.CuriosityInjection curiosityInject;
//        if (actionCuri != null) {
//            curiosityInject = Curiosity.CuriosityInjection.Override;
//
////            float confMin = n.confMin.floatValue();
////            if (actionCuri.conf() < confMin) {
////                //boost sub-threshold confidence to minimum
////                actionCuri = $.t(actionCuri.freq(), confMin);
////            }
//
//            //pre-load curiosity for the future
//            if (curiosity.goal.getOpaque()) {
//                long curiStart = s;
//                long curiEnd = Math.round(curiStart + dur * (n.random().nextFloat()*NAL.CURIOSITY_TASK_RANGE_DURS)); //(1 + (curiosity.Math.max(curiStart, e);
//
//                long[] se = Tense.dither(new long[] { curiStart, curiEnd }, n);
//                curiStart = se[0];
//                curiEnd = se[1];
//
//                g.what().accept(
//                        curiosity(actionCuri /*goal*/, curiStart, curiEnd, n)
//                );
//            }
//
//
//        } else {
//            curiosityInject = curiosity.injection.get();
//
//            //use existing curiosity
//            Answer a = Answer.
//				taskStrength(true, 1, s, e, term, null, n)
//                    .dur(dur)
//                    .match(curiosityTable);
//            actionCuri = a.truth();
//        }
//
//        if (actionDex != null || actionCuri != null) {
//            actionTruth = curiosityInject.inject(actionDex, actionCuri);
//        } else {
//            actionTruth = null;
//        }

        actionTruth = actionDex;

        return actionTruth;
    }



//    protected void feedback(@Nullable Truth f, short[] cause, Game g) {
//        if (f!=null)
//            f = truthDithered(f.freq(), resolution().floatValue(), g);
//        ((SensorBeliefTables) beliefs()).input(f, attn.pri(), cause, g.what(), g.when,true);
//    }



    public Truth actionTruth() {
        return actionTruth;
    }

}