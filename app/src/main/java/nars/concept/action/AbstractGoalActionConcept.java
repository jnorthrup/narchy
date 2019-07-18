package nars.concept.action;

import nars.$;
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
import nars.truth.Truth;
import nars.truth.proj.TruthProjection;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

import static nars.Op.GOAL;
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

    @Nullable public TruthProjection truth(boolean beliefsOrGoals, int componentsMax, long now, float dur, NAR n) {
        BeliefTable tables = (beliefsOrGoals ? beliefs() : goals());


        float sensitivityRange = 1;

        //long s = Math.round(now - dur/2 * sensitivityRange), e = Math.round(now + dur/2 * sensitivityRange);
        long s = Math.round(now - dur * sensitivityRange), e = now;

        if (!tables.isEmpty()) {
//            int dither = n.dtDither.intValue();

            int limit = componentsMax, tries = (int)Math.ceil(limit * NAL.ANSWER_TRYING);

            Answer a = Answer.relevance(true, limit, s, e, term,
                    //withoutCuriosity
                    null
                    , n).dur(dur);
//            for (int iter = 0; iter < 1; iter++) {
//
//
//                switch (iter) {
//                    case 0:
//                        //duration-precision window
//                        s = now - gameDur / 2;
//                        e = now + gameDur / 2;
//                        //s = now - Math.max(narDur, agentDur);
//                        //e = now;
//                        //e = now;
//                        break;
//                    case 1:
//                    default:

                        //s = now - Math.max(narDur, agentDur)*2;
                        //e = now;
                        //e = now;
//                        break;
////                    default:
////                        //frame-precision window
////                        int frameDur = Tense.occToDT(now - prev);
////                        int dn = 3;
////                        //s = (long) (now - Math.max(narDur*dn/2f, frameDur/2));
////                        //e = (long) (now + Math.max(narDur*dn/2f, frameDur/2));
////                        s = now - Math.max(narDur * dn, frameDur);
////                        e = now; //now + Math.max(dur * 2, 0);
////                        break;
//
//                }

//                //shift forward to include some immediate future desire as part of present moment desire
//                int shift =
//                        0;
//                        //dither/2;
//                        //Math.max(dither/2, narDur/2);
//
//                s += shift;
//                e += shift;

                a.clear(tries).time(s, e);

                a.ttl = tries;
                tables.match(a);
//                for (BeliefTable table : (BeliefTables)tables) {
//                    if (table!=curiosityTable) {
//                        a.ttl = tries;
//                        a.match(table);
//                    }
//                }


                TruthProjection p = a.truthProjection();
                return p;
//
//                if (atl!=null) {
//
//                    TruthProjection p =
//                            new LinearTruthProjection(a.time.start, a.time.end, dur);
//                            //new FocusingLinearTruthProjection(a.time.start, a.time.end, dur);
//                    p.addAll(atl);
//
//                    //Truth next = nextP.truth(NAL.truth.EVI_MIN, false, true, n);
//                    //if (next!=null) {
//                    return p;
//                    //}
//                }
                //TODO my truthpolation .stamp()'s and .cause()'s for clues

//                if ((next = Truth.stronger(a.truth(), next))!=next) {
//                    ss = s;
//                    ee = e;
//                }

                //HACK
//                if (next!=null)
//                    break; //early finish on first non-null
            }
//        }

        return null;
    }


    @Override
    public void update( Game g) {
        long now = g.now;

        updateCuriosity(g.curiosity);

        NAR n = g.nar();
        float gameDur =
                //0;
                g.dur();
                //g.durPhysical();

        int limitBelief = Answer.BELIEF_MATCH_CAPACITY; //high sensitivity
        int limitGoal = limitBelief * 2;

        this.beliefTruth = truth(truth(true, limitBelief, now, gameDur, n));

        this.actionTruth = actionTruth(limitGoal, now, gameDur, g);

    }

    private  @Nullable Truth truth(TruthProjection t) {
        return t!=null ? t.truth(NAL.truth.EVI_MIN, false, false, null) : null;
    }

    private Truth actionTruth(int limit, long now, float gameDur, Game g) {


        NAR n = g.nar;
        Truth actionTruth;
        TruthProjection gt = truth(false, limit, now, gameDur, n);

        Truth nextActionDex = truth(gt);
        actionDex = nextActionDex;
        actionCoh = nextActionDex != null ? gt.coherency() : 0;
        if (nextActionDex != null)
            lastNonNullActionDex = actionDex;


        long s = Math.round(now - gameDur), e = now;

        Truth actionCuri = curiosity.curiosity(this);

        Curiosity.CuriosityInjection curiosityInject = null;
        if (actionCuri != null) {
            curiosityInject = Curiosity.CuriosityInjection.Override;

            float confMin = n.confMin.floatValue();
            if (actionCuri.conf() < confMin) {
                //boost sub-threshold confidence to minimum
                actionCuri = $.t(actionCuri.freq(), confMin);
            }

            Truth curiDithered = g.dither(actionCuri, this);
            if (curiDithered != null) {

                actionCuri = curiDithered;

                //pre-load curiosity for the future
                if (curiosity.goal.getOpaque()) {
                    long lastCuriosity = curiosityTable.end();
                    long curiStart = lastCuriosity != TIMELESS ? Math.max(s, lastCuriosity + 1) : s;
                    long curiEnd = Math.round(curiStart + gameDur * NAL.CURIOSITY_TASK_RANGE_DURS); //(1 + (curiosity.Math.max(curiStart, e);

                    int dither = n.dtDither();
                    curiStart = Tense.dither(curiStart, dither);
                    curiEnd = Tense.dither(curiEnd, dither);

                    g.what().accept(
                            curiosity(actionCuri /*goal*/, curiStart, curiEnd, n)
                    );
                }


            }
        } else {
            curiosityInject = curiosity.injection.get();

            //use existing curiosity
            Answer a = Answer.
                    relevance(true, 2, s, e, term, null, n)
                    .dur(gameDur)
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

        SignalTask curiosity = new CuriosityTask(term, goal, n.time(), pStart, pEnd, evi);
        curiosity.priMax(attn.pri());
        curiosity.cause(causeArray);
        return curiosity;
    }

    protected void feedback(@Nullable Truth f, short[] cause, Game g) {

        f = g.dither(f, this);

        ((SensorBeliefTables) beliefs()).input(f, g.now, attn::pri, cause, g.durPhysical(), g.what(), true);
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
