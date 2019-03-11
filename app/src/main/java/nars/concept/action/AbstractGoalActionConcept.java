package nars.concept.action;

import jcog.data.list.FasterList;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.action.curiosity.Curiosity;
import nars.concept.action.curiosity.CuriosityGoalTable;
import nars.concept.action.curiosity.CuriosityTask;
import nars.control.channel.CauseChannel;
import nars.control.op.Remember;
import nars.table.BeliefTable;
import nars.table.BeliefTables;
import nars.table.dynamic.SensorBeliefTables;
import nars.table.temporal.RTreeBeliefTable;
import nars.task.ITask;
import nars.task.signal.SignalTask;
import nars.task.util.Answer;
import nars.term.Term;
import nars.time.Tense;
import nars.truth.Truth;
import org.eclipse.collections.api.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

import static nars.time.Tense.TIMELESS;
import static nars.truth.func.TruthFunctions.w2cSafe;
import static org.eclipse.collections.impl.tuple.Tuples.pair;


/**
 * ActionConcept which is driven by Goals that are interpreted into feedback Beliefs
 */
public class AbstractGoalActionConcept extends ActionConcept {

    private static final long CURIOSITY_TASK_RANGE_DURS = 2;

//    private static final Logger logger = LoggerFactory.getLogger(AbstractGoalActionConcept.class);

    @Nullable
    private Curiosity curiosity = null;

    private final CuriosityGoalTable curiosityTable;

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

    private static final boolean shareCuriosityEvi = true;

    public AbstractGoalActionConcept(Term term, NAR n) {
        this(term,
                new RTreeBeliefTable(),
                n);
    }

    protected AbstractGoalActionConcept(Term term, BeliefTable goals, NAR n) {
        super(term, new SensorBeliefTables(term, true),
                new BeliefTables(goals),
                n);

        cause = n.newCause(term).id;

        ((BeliefTables) goals()).tables.add(curiosityTable = new CuriosityGoalTable(term, Param.CURIOSITY_CAPACITY));


    }

    protected CauseChannel<ITask> newChannel(NAR n) {
        return n.newChannel(this);
    }

    public AbstractGoalActionConcept curiosity(Curiosity curiosity) {
        this.curiosity = curiosity;
        return this;
    }

    @Override
    public float dexterity() {
        Truth t = this.actionDex;
        return t != null ? w2cSafe(t.evi()) : 0;
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

    public final org.eclipse.collections.api.tuple.Pair<Truth, long[]> truth(boolean beliefsOrGoals, int componentsMax, long prev, long now, NAR n) {
        return truth(beliefsOrGoals, componentsMax, prev, now, n.dur(), n);
    }

    public org.eclipse.collections.api.tuple.Pair<Truth, long[]> truth(boolean beliefsOrGoals, int componentsMax, long prev, long now, int narDur, NAR n) {
        long s = Long.MAX_VALUE, e = Long.MIN_VALUE;
        Truth next = null;
        FasterList<BeliefTable> tables = ((BeliefTables) (beliefsOrGoals ? beliefs() : goals())).tables;
        if (!tables.isEmpty()) {
            int dither = n.dtDither.intValue();

            int organicDur =
                    //Tense.occToDT(e-s);
                    narDur;

            for (int iter = 0; iter < 3; iter++) {


                switch (iter) {
                    case 0:
                        //duration-precision window
                        //s = now - dur / 2;
                        //e = now + dur / 2;
                        s = now - narDur;
                        e = now;
                        break;
                    case 1:
//                    s = now - dur;
//                    e = now + dur;
                        s = now - narDur * 2;
                        e = now; //now + dur;
                        break;
                    default:
                        //frame-precision window
                        long frameDur = now - prev;
                        int dn = 3;
//                    s = now - Math.max(dur*dn/2f, frameDur/2);
//                    e = now + Math.max(dur*dn/2f, frameDur/2);
                        s = now - Math.max(narDur * dn, frameDur);
                        e = now; //now + Math.max(dur * 2, 0);
                        break;

                }

                //shift forward
                s += dither / 2;
                e += dither / 2;

                int limit = componentsMax;

                Predicate<Task> fil =
                        withoutCuriosity;

                try (Answer a = Answer.relevance(true, limit, s, e, term, fil, n).dur(organicDur)) {

                    for (BeliefTable b : tables) {
                        if (!(b instanceof CuriosityGoalTable)) {
                            a.ttl = limit;
                            a.match(b);
                        }
                    }

                    //TODO my truthpolation .stamp()'s and .cause()'s for clues

                    Truth organic = a.truth();
                    if (organic != null) {
                        @Nullable Truth maybeNextActionDex = organic;
                        if (next == null)
                            next = maybeNextActionDex;
                        else
                            next = Truth.stronger(maybeNextActionDex, next);

                    }
                }

                //optional:
//            if (nextActionDex!=null)
//                break; //take the first (not the strongest)
            }


        }

        return pair(next, new long[]{ s, e});
    }

    @Override
    public void act(long prev, long now, NAR n) {

        int narDur = n.dur();
        
        int limit = Answer.BELIEF_MATCH_CAPACITY * 2;

        Pair<Truth, long[]> bt = truth(true, limit, prev, now, narDur, n);
        this.beliefTruth = bt != null ? bt.getOne() : null;

        this.actionTruth = actionTruth(limit, prev, now, narDur, n);

    }

    private Truth actionTruth(int limit, long prev, long now, int narDur, NAR n) {

        int curiDur = narDur;


        Truth actionTruth;
        Pair<Truth, long[]> t = truth(false, limit, prev, now, narDur, n);

        Truth nextActionDex = t == null ? null : t.getOne();
        actionDex = nextActionDex;
        if (nextActionDex != null)
            curiDex = actionDex;


        long[] se = t.getTwo();
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
                    long curiEnd = curiStart + Math.max(dither, Math.round((now-prev)*CURIOSITY_TASK_RANGE_DURS * n.random().nextFloat())); //(1 + (curiosity.Math.max(curiStart, e);

                    curiStart = Tense.dither(curiStart, dither);
                    curiEnd = Tense.dither(curiEnd, dither);

                    n.input(
                            curiosity(actionCuri /*goal*/, curiStart, curiEnd, n)
                    );
                }


            }
        } else {
            curiosityInject = curiosity.injection.get();

            //use existing curiosity
            @Nullable CuriosityGoalTable curiTable = ((BeliefTables) goals()).tableFirst(CuriosityGoalTable.class);
            try (Answer a = Answer.
                    relevance(true, 2, s, e, term, null, n).match(curiTable).dur(curiDur)) {
                actionCuri = a.truth();
            }
        }

        if (actionDex != null || actionCuri != null) {
            actionTruth = curiosityInject.inject(actionDex, actionCuri);
        } else {
            actionTruth = null;
        }

        return actionTruth;
    }


    @Override
    public void add(Remember r, NAR n) {

        if (r.input instanceof CuriosityTask) {
            //intercept curiosity goals for the curiosity table
            curiosityTable.add(r, n);
        } else {
            super.add(r, n);
        }

    }

    private long[] eviShared = null;

    @Nullable SignalTask curiosity(Truth goal, long pStart, long pEnd, NAR n) {
        long[] evi = evi(n);

        SignalTask curiosity = new CuriosityTask(term, goal, n.time(), pStart, pEnd, evi);
        curiosity.priMax(attn.elementPri());
        curiosity.cause(new short[]{cause});
        return curiosity;
    }

    private long[] evi(NAR n) {
        return shareCuriosityEvi ? eviShared(n) : n.evidence();
    }

    private long[] eviShared(NAR n) {
        long[] evi;
        if (eviShared == null)
            eviShared = n.evidence();
        evi = eviShared;
        return evi;
    }

    protected void feedback(@Nullable Truth f, long last, long now, short cause, NAR nar) {
        ((SensorBeliefTables) beliefs()).add(f, last, now, attn::elementPri, cause, nar);
    }


    public Truth actionTruth() {
        return actionTruth;
    }


}
