package nars.concept.action;

import nars.$;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.agent.NAgent;
import nars.concept.action.curiosity.Curiosity;
import nars.concept.action.curiosity.CuriosityTask;
import nars.control.channel.CauseChannel;
import nars.control.op.Remember;
import nars.table.BeliefTable;
import nars.table.BeliefTables;
import nars.table.dynamic.SensorBeliefTables;
import nars.table.dynamic.SeriesBeliefTable;
import nars.table.temporal.RTreeBeliefTable;
import nars.task.ITask;
import nars.task.signal.SignalTask;
import nars.task.util.Answer;
import nars.task.util.series.RingBufferTaskSeries;
import nars.term.Term;
import nars.truth.Truth;
import org.eclipse.collections.api.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

import static nars.time.Tense.ETERNAL;
import static nars.time.Tense.TIMELESS;
import static org.eclipse.collections.impl.tuple.Tuples.pair;


/**
 * ActionConcept which is driven by Goals that are interpreted into feedback Beliefs
 */
public class AbstractGoalActionConcept extends ActionConcept {


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

    /**
     * latches the last non-null actionDex, used for echo/sustain
     * TODO make this more complex like a replay buffer in DQN
     */
    public @Nullable Truth curiDex;

    final short cause;

    public AbstractGoalActionConcept(Term term, NAR n) {
        this(term,
                new RTreeBeliefTable(),
                n);
    }

    protected AbstractGoalActionConcept(Term term, BeliefTable mutableGoals, NAR n) {
        super(term, new SensorBeliefTables(term, true),
                new BeliefTables(),
                n);

        cause = n.newCause(term).id;

        /** make sure to add curiosity table first in the list, as a filter */
        BeliefTables GOALS = ((BeliefTables) goals());
        GOALS.add(curiosityTable =
                new CuriosityBeliefTable(term));
        GOALS.add(mutableGoals);



    }

    protected CauseChannel<ITask> channel(NAR n) {
        return n.newChannel(this);
    }

    public AbstractGoalActionConcept curiosity(Curiosity curiosity) {
        this.curiosity = curiosity;
        return this;
    }




    @Override
    public double dexterity() {
        Truth t = actionDex;
        return t != null ? t.evi() : 0;
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


    static final Predicate<Task> withoutCuriosity = t -> !(t instanceof CuriosityTask) && !t.isEternal();  /* filter curiosity tasks? */

//    public final org.eclipse.collections.api.tuple.Pair<Truth, long[]> truth(boolean beliefsOrGoals, int componentsMax, long prev, long now, NAR n) {
//        return truth(beliefsOrGoals, componentsMax, prev, now, n.dur(), n);
//    }

    public org.eclipse.collections.api.tuple.Pair<Truth, long[]> truth(boolean beliefsOrGoals, int componentsMax, long prev, long now, int agentDur, int narDur, NAR n) {
        Truth next = null;
        List<BeliefTable> tables = ((BeliefTables) (beliefsOrGoals ? beliefs() : goals()));

        long ss = TIMELESS, ee = TIMELESS;

        if (!tables.isEmpty()) {
            int dither = n.dtDither.intValue();

            int organicDur =
                    //Tense.occToDT(e-s);
                    narDur;

            int limit = componentsMax, tries = limit*2;
            Answer a = Answer.relevant(true, limit, ETERNAL, ETERNAL, term, withoutCuriosity, n).dur(organicDur);
            for (int iter = 0; iter < 2; iter++) {

                long s, e;

                switch (iter) {
                    case 0:
                        //duration-precision window
                        s = now - narDur / 2;
                        e = now + narDur / 2;
                        //s = now - Math.max(narDur, agentDur);
                        //e = now;
                        //e = now;
                        break;
                    case 1:
                    default:
                        s = now - narDur;
                        e = now + narDur;
                        //s = now - Math.max(narDur, agentDur)*2;
                        //e = now;
                        //e = now;
                        break;
//                    default:
//                        //frame-precision window
//                        int frameDur = Tense.occToDT(now - prev);
//                        int dn = 3;
//                        //s = (long) (now - Math.max(narDur*dn/2f, frameDur/2));
//                        //e = (long) (now + Math.max(narDur*dn/2f, frameDur/2));
//                        s = now - Math.max(narDur * dn, frameDur);
//                        e = now; //now + Math.max(dur * 2, 0);
//                        break;

                }

                //shift forward to include some immediate future desire as part of present moment desire
                int shift =
                        0;
                        //dither/2;
                        //Math.max(dither/2, narDur/2);

                s += shift;
                e += shift;

                a.clear(tries).time(s, e);

                for (BeliefTable table : tables) {
                    if (table!=curiosityTable) {
                        a.ttl = tries;
                        a.match(table);
                    }
                }

                //TODO my truthpolation .stamp()'s and .cause()'s for clues

                if ((next = Truth.stronger(a.truth(), next))!=next) {
                    ss = s;
                    ee = e;
                }

                //HACK
//                if (next!=null)
//                    break; //early finish on first non-null
            }
        }

        if (ss == TIMELESS) {
            //default
            ss = prev;
            ee = now;
        }
        return pair(next, new long[]{ss, ee});
    }

    @Override
    @Deprecated public final void update(long last, long now, NAR nar) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void update(long prev, long now, NAgent a) {

        NAR n = a.nar();
        int narDur = n.dur();
        int agentDur = a.frameTrigger.dur();

        int limit = Answer.BELIEF_MATCH_CAPACITY * 2;

        if (prev == TIMELESS)
            prev = now - n.dur(); //HACK

        Pair<Truth, long[]> bt = truth(true, limit, prev, now, agentDur, narDur, n);
        this.beliefTruth = bt != null ? bt.getOne() : null;

        this.actionTruth = actionTruth(limit, prev, now, agentDur, narDur, n);

    }

    private Truth actionTruth(int limit, long prev, long now, int agentDur, int narDur, NAR n) {

        int curiDur = narDur;

        Truth actionTruth;
        Pair<Truth, long[]> gt = truth(false, limit, prev, now, agentDur, narDur, n);

        Truth nextActionDex = gt == null ? null : gt.getOne();
        actionDex = nextActionDex;
        if (nextActionDex != null)
            curiDex = actionDex;


        long[] se = gt.getTwo();
        long s = se[0], e = se[1];

        Truth actionCuri = curiosity.curiosity(this);

        Curiosity.CuriosityInjection curiosityInject = null;
        if (actionCuri != null) {
            curiosityInject = Curiosity.CuriosityInjection.Override;

            float confMin = n.confMin.floatValue();
            if (actionCuri.conf() < confMin) {
                //boost sub-threshold confidence to minimum
                actionCuri = $.t(actionCuri.freq(), confMin);
            }

            Truth curiDithered = actionCuri.dither(
                    Math.max(n.freqResolution.floatValue(), resolution().floatValue()),
                    n.confResolution.floatValue()
            );
            if (curiDithered != null) {

                actionCuri = curiDithered;

                //pre-load curiosity for the future
                if (curiosity.goal.getOpaque()) {
                    long lastCuriosity = curiosityTable.series.end();
                    long curiStart = lastCuriosity != TIMELESS ? Math.max(s, lastCuriosity + 1) : s;
                    int dither = n.dtDither();
                    long curiEnd = curiStart + Math.max(dither, Math.round((now - prev) * Param.CURIOSITY_TASK_RANGE_DURS * n.random().nextFloat())); //(1 + (curiosity.Math.max(curiStart, e);

                    //curiStart = Tense.dither(curiStart, dither);
                    //curiEnd = Tense.dither(curiEnd, dither);

                    n.input(
                            curiosity(actionCuri /*goal*/, curiStart, curiEnd, n)
                    );
                }


            }
        } else {
            curiosityInject = curiosity.injection.get();

            //use existing curiosity
            Answer a = Answer.
                    relevant(true, 2, s, e, term, null, n)
                    .dur(curiDur)
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
        curiosity.cause(new short[]{cause});
        return curiosity;
    }

    protected void feedback(@Nullable Truth f, long last, long now, short cause, NAR nar) {
        ((SensorBeliefTables) beliefs()).add(f, last, now, attn::pri, cause, nar);
    }


    public Truth actionTruth() {
        return actionTruth;
    }


    private static class CuriosityBeliefTable extends SeriesBeliefTable {
        public CuriosityBeliefTable(Term term) {
            super(term, false, new RingBufferTaskSeries<>(Param.CURIOSITY_CAPACITY));
        }

        @Override
        public void remember(Remember r) {
            if (r.input instanceof CuriosityTask) {
                add(r.input);
                r.remember(r.input);
            }
        }
    }
}
