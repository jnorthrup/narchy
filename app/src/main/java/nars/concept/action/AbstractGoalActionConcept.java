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
import nars.truth.polation.Projection;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

import static nars.time.Tense.TIMELESS;
import static nars.truth.func.TruthFunctions.w2cSafe;


/**
 * ActionConcept which is driven by Goals that are interpreted into feedback Beliefs
 */
public class AbstractGoalActionConcept extends ActionConcept {

//    private static final Logger logger = LoggerFactory.getLogger(AbstractGoalActionConcept.class);

    @Nullable
    private Curiosity curiosity = null;

    private final CuriosityGoalTable curiosityTable;

    /**
     * current calculated goalTask
     */
    protected @Nullable Truth actionTruth;

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

    @Override
    public void sense(long prev, long now, NAR n) {

        int narDur = n.dur();

        int curiDur = narDur;

        int organicDur =
                //Tense.occToDT(e-s);
                narDur;

        int limit = Answer.BELIEF_MATCH_CAPACITY * 2;

//        long recent =
//                //now - dur*2;
//                prev;

        Predicate<Task> fil =
                withoutCuriosity;
        //Answer.filter(withoutCuriosity, (t) -> t.endsAfter(recent)); //prevent stronger past from overriding weaker future

        int dither = n.dtDither.intValue();
        long s = Long.MAX_VALUE, e = Long.MIN_VALUE;
        Truth nextActionDex = null;
        FasterList<BeliefTable> goalTables = ((BeliefTables) goals()).tables;
        if (!goalTables.isEmpty()) {
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



                try (Answer a = Answer.relevance(true, limit, s, e, term, fil, n).dur(organicDur)) {

                    for (BeliefTable b : goalTables) {
                        if (!(b instanceof CuriosityGoalTable)) {
                            a.ttl = limit;
                            a.match(b);
                        }
                    }

                    //TODO my truthpolation .stamp()'s and .cause()'s for clues

                    Projection organic = a.truthpolation(false); //Math.round(actionWindowDexDurs *dur));
                    if (organic != null) {
                        @Nullable Truth maybeNextActionDex = organic.filtered().truth();
                        if (nextActionDex == null)
                            nextActionDex = maybeNextActionDex;
                        else
                            nextActionDex = Truth.stronger(maybeNextActionDex, nextActionDex);

                    }
                }

                //optional:
//            if (nextActionDex!=null)
//                break; //take the first (not the strongest)
            }

            actionDex = nextActionDex;
            if (nextActionDex != null) {
                curiDex = actionDex;
            }
        }


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
                    long curiEnd = Math.max(curiStart, e);

                    curiStart = Tense.dither(curiStart, n);
                    curiEnd = Tense.dither(curiEnd, n);

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
                Projection curi = a.truthpolation(false); //Math.round(actionWindowCuriDurs * dur));
                if (curi != null) {
                    actionCuri = curi.filtered().truth();
                } else
                    actionCuri = null;
            }
        }

        if (actionDex != null || actionCuri != null) {

            actionTruth = curiosityInject.inject(actionDex, actionCuri);
        } else {
            actionTruth = null;
        }

        //System.out.println(actionTruth + " " + actionDex);

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
