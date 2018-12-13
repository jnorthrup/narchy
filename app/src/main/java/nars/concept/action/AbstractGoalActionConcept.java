package nars.concept.action;

import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.action.curiosity.Curiosity;
import nars.concept.action.curiosity.CuriosityGoalTable;
import nars.concept.action.curiosity.CuriosityTask;
import nars.control.channel.CauseChannel;
import nars.control.proto.Remember;
import nars.table.BeliefTable;
import nars.table.BeliefTables;
import nars.table.dynamic.SensorBeliefTables;
import nars.table.dynamic.SeriesBeliefTable;
import nars.table.temporal.RTreeBeliefTable;
import nars.task.ITask;
import nars.task.signal.SignalTask;
import nars.task.util.Answer;
import nars.term.Term;
import nars.truth.Truth;
import nars.truth.polation.TruthPolation;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

import static nars.time.Tense.TIMELESS;


/**
 * ActionConcept which is driven by Goals that are interpreted into feedback Beliefs
 */
public class AbstractGoalActionConcept extends ActionConcept {

//    private static final Logger logger = LoggerFactory.getLogger(AbstractGoalActionConcept.class);

    @Nullable private Curiosity curiosity = null;

    private final CuriosityGoalTable curiosityTable;

    /** current calculated goalTask */
    protected volatile @Nullable Truth actionTruth;

    /** truth calculated (in attempt to) excluding curiosity */
    protected volatile @Nullable Truth actionDex;

    /** latches the last non-null actionDex, used for echo/sustain */
    public volatile @Nullable Truth lastActionDex;

    protected final CauseChannel<ITask> in;

    public AbstractGoalActionConcept(Term term,  NAR n) {
        this(term,
                new RTreeBeliefTable(),
                n);
    }

    protected AbstractGoalActionConcept(Term term, BeliefTable goals, NAR n) {
        super(term, new SensorBeliefTables(term, true, n.conceptBuilder),
                new BeliefTables(goals),
                n);

        ((BeliefTables)goals()).tables.add(curiosityTable = new CuriosityGoalTable(term, 64));

        in = newChannel(n);
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
        return t!=null ? t.conf() : 0;
    }


    /** in cycles; controls https://en.wikipedia.org/wiki/Legato vs. https://en.wikipedia.org/wiki/Staccato */
    int actionSustain =
            //0;
            -1;

//    public AbstractGoalActionConcept actionDur(int actionDur) {
//        this.actionSustain = actionDur;
//        return this;
//    }


    static final Predicate<Task> withoutCuriosity = t -> !(t instanceof CuriosityTask) && !t.isEternal();  /* filter curiosity tasks? */

    @Override
    public void update(long prev, long now, long next, NAR n) {



        //TODO mine truthpolation .stamp()'s and .cause()'s for clues




        int dur = n.dur();
        //long s = prev, e = now;
        //long s = now, e = next;
        long s = now - dur/2, e = next - dur/2;
        //long s = prev, e = next;
        //long agentDur = (now - prev);
        //long s = now - agentDur/2, e = now + agentDur/2;
        //long s = now - dur/2, e = now + dur/2;

        int actionDur = this.actionSustain;
        if (actionDur < 0) {
            actionDur =
                    dur;
                    //Tense.occToDT(agentDur);
        }


        int limit = Answer.TASK_LIMIT_DEFAULT * 2;

        BeliefTable table = goals();

//        long recent =
//                //now - dur*2;
//                prev;

        Predicate<Task> fil =
                withoutCuriosity;
                //Answer.filter(withoutCuriosity, (t) -> t.endsAfter(recent)); //prevent stronger past from overriding weaker future

        try(Answer a = Answer.
                relevance(true, limit, s, e, term, fil, n)) {


            TruthPolation organic = a.match(table).truthpolation(actionDur);
            if (organic != null) {
                actionDex = organic.filtered().truth();
                if (actionDex!=null) {
                    lastActionDex = actionDex;
                }
            } else {
                actionDex = null;
            }
        }


        try (Answer a = Answer.
                relevance(true, limit, s, e, term, null, n).match(table)) {
            TruthPolation raw = a.truthpolation(actionDur);
            if (raw != null) {
                actionTruth = raw.filtered().truth();
            } else
                actionTruth = null;
        }

        //if this happens, for whatever reason..
        if (actionTruth == null && actionDex!=null)
            actionTruth = actionDex;



        Truth curi = curiosity.curiosity(this);
        if (curi!=null) {

            Truth curiDithered = curi.ditherFreq(resolution().floatValue()).dithered(n);
            if (curiDithered != null) {

                curi = curiDithered;

                //pre-load curiosity for the future
                if (curiosity.goal.getOpaque()) {
                    long lastCuriosity = curiosityTable.series.end();
                    long curiStart = lastCuriosity != TIMELESS ? Math.max(s, lastCuriosity + 1) : s;
                    long curiEnd = Math.max(curiStart, e);
                    in.input(
                            curiosity(curi /*goal*/, curiStart, curiEnd, n)
                    );
                }

                actionTruth = curiosity.injection.get().inject(actionTruth, curi);

            } /*else {
                logger.info("curiosity too weak for NAR: {} confMin > {} ( from: {} )",
                        n.confMin.floatValue(), curiDithered, curi);
            }*/
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

    long[] eviShared = null;
    @Nullable SignalTask curiosity(Truth goal, long pStart, long pEnd, NAR n) {
        long[] evi;
        if (Param.ALLOW_REVISION_OVERLAP_IF_DISJOINT_TIME) {
            if (eviShared == null)
                eviShared = n.evidence();
            evi = eviShared;
        } else {
            evi = n.evidence();
        }

        SignalTask curiosity = new CuriosityTask(term, goal, n, pStart, pEnd, evi);
        attn.take(curiosity, 0.5f*attn.supply.priElseZero());
        return curiosity;
    }

    @Deprecated @Nullable public SeriesBeliefTable.SeriesRemember feedback(@Nullable Truth f, long now, long next, float dur, float pct, NAR nar) {

        SeriesBeliefTable.SeriesRemember r = ((SensorBeliefTables) beliefs()).add(f, now, next, attn.supply.priElseZero(), this, dur, nar);
        if (r!=null) {
            attn.taken(r.input.priElseZero());
        }
        return r;
    }


    public Truth actionTruth() {
        return actionTruth;
    }


}
