package nars.concept.action;

import nars.NAR;
import nars.Param;
import nars.Task;
import nars.attention.AttBranch;
import nars.concept.action.curiosity.Curiosity;
import nars.concept.action.curiosity.CuriosityGoalTable;
import nars.concept.action.curiosity.CuriosityTask;
import nars.control.channel.ConsumerX;
import nars.control.op.Remember;
import nars.table.BeliefTable;
import nars.table.BeliefTables;
import nars.table.dynamic.SensorBeliefTables;
import nars.table.dynamic.SeriesBeliefTable;
import nars.table.eternal.EternalTable;
import nars.table.temporal.RTreeBeliefTable;
import nars.table.temporal.TemporalBeliefTable;
import nars.task.ITask;
import nars.task.signal.SignalTask;
import nars.task.util.Answer;
import nars.term.Term;
import nars.truth.Truth;
import nars.truth.polation.TruthPolation;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

import static nars.time.Tense.TIMELESS;
import static nars.truth.func.TruthFunctions.w2cSafe;


/**
 * ActionConcept which is driven by Goals that are interpreted into feedback Beliefs
 */
public class AbstractGoalActionConcept extends ActionConcept {
    private final AttBranch attnCuri;

//    private static final Logger logger = LoggerFactory.getLogger(AbstractGoalActionConcept.class);

    @Nullable private Curiosity curiosity = null;

    private final CuriosityGoalTable curiosityTable;

    /** current calculated goalTask */
    protected @Nullable Truth actionTruth;

    /** truth calculated (in attempt to) excluding curiosity */
    protected @Nullable Truth actionDex;

    /** latches the last non-null actionDex, used for echo/sustain */
    public @Nullable Truth lastActionDex;

    protected final ConsumerX<ITask> in;
    private static final boolean shareCuriosityEvi = true;

    public AbstractGoalActionConcept(Term term,  NAR n) {
        this(term,
                new RTreeBeliefTable(),
                n);
    }

    protected AbstractGoalActionConcept(Term term, BeliefTable goals, NAR n) {
        super(term, new SensorBeliefTables(term, true, n.conceptBuilder),
                new BeliefTables(goals),
                n);

        ((BeliefTables)goals()).tables.add(curiosityTable = new CuriosityGoalTable(term, Param.CURIOSITY_CAPACITY));

        in = newChannel(n);

        this.attnCuri = new AttBranch(term, List.of(term));
        attnCuri.parent(attn);
        attnCuri.boost.set(0.5f);
    }

    protected ConsumerX<ITask> newChannel(NAR n) {
        return n.newChannel(this);
    }

    public AbstractGoalActionConcept curiosity(Curiosity curiosity) {
        this.curiosity = curiosity;
        return this;
    }

    @Override
    public float dexterity() {
        Truth t = this.actionDex;
        return t!=null ? w2cSafe(t.evi()) : 0;
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

        //long agentDur = now - prev;
//        long dur = agentDur;
                //narDur; //Math.min(narDur, agentDur);
        //long s = prev, e = now;
        //long s = Math.min(prev + agentDur/2, now-agentDur/2), e = now + agentDur/2;
        long s = prev, e = now + narDur/2;
        //long s = now - dur/2, e = now + dur/2;

        //long s = prev, e = now;
        //long s = now - dur, e = now;
        //long s = now - dur, e = now + dur;
        //long s = now, e = next;
        //long s = now - dur/2, e = next - dur/2;
        //long s = now - dur, e = next - dur;
        //long s = prev, e = next;
        //long agentDur = (now - prev);
        //long s = now - agentDur/2, e = now + agentDur/2;
        //long s = now - dur/2, e = now + dur/2;




        int limit = Answer.BELIEF_MATCH_CAPACITY * 2;

//        long recent =
//                //now - dur*2;
//                prev;

        Predicate<Task> fil =
                withoutCuriosity;
                //Answer.filter(withoutCuriosity, (t) -> t.endsAfter(recent)); //prevent stronger past from overriding weaker future

        try(Answer a = Answer.relevance(true, limit, s, e, term, fil, n)) {


            @Nullable TemporalBeliefTable temporalTable = ((BeliefTables) goals()).tableFirst(TemporalBeliefTable.class);
            if (temporalTable!=null) {
                a.triesRemain = limit;  a.match(temporalTable);
            }


            @Nullable EternalTable eternalTable = ((BeliefTables) goals()).tableFirst(EternalTable.class);
            if (eternalTable!=null) {
                a.triesRemain = limit; a.match(eternalTable);
            }

            TruthPolation organic = a.truthpolation(narDur); //Math.round(actionWindowDexDurs *dur));

            //TODO mine truthpolation .stamp()'s and .cause()'s for clues

            if (organic != null) {
                actionDex = organic.filtered().truth();
                if (actionDex!=null) {
                    lastActionDex = actionDex;
                }
            } else {
                actionDex = null;
            }
        }


        Truth actionCuri = curiosity.curiosity(this);

        Curiosity.CuriosityInjection curiosityInject = null;
        if (actionCuri!=null) {
            curiosityInject = Curiosity.CuriosityInjection.Override;

            Truth curiDithered = actionCuri.dither(
                    Math.max(n.freqResolution.floatValue(),resolution().floatValue()),
                    n.confResolution.floatValue()
            );
            if (curiDithered != null) {

                actionCuri = curiDithered;

                //pre-load curiosity for the future
                if (curiosity.goal.getOpaque()) {
                    long lastCuriosity = curiosityTable.series.end();
                    long curiStart = lastCuriosity != TIMELESS ? Math.max(s, lastCuriosity + 1) : s;
                    long curiEnd = Math.max(curiStart, e);
                    in.input(
                            curiosity(actionCuri /*goal*/, curiStart, curiEnd, n)
                    );
                }


            }
        } else {
            curiosityInject = curiosity.injection.get();

            //use existing curiosity
            @Nullable CuriosityGoalTable curiTable = ((BeliefTables) goals()).tableFirst(CuriosityGoalTable.class);
            try (Answer a = Answer.
                    relevance(true, 1, s, e, term, null, n).match(curiTable)) {
                TruthPolation curi = a.truthpolation(narDur); //Math.round(actionWindowCuriDurs * dur));
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

        SignalTask curiosity = new CuriosityTask(term, goal, n, pStart, pEnd, evi);
        attnCuri.ensure(curiosity, attn.elementPri());
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

    @Nullable protected SeriesBeliefTable.SeriesRemember feedback(@Nullable Truth f, long last, long now, NAR nar) {

        SeriesBeliefTable.SeriesRemember r = ((SensorBeliefTables) beliefs()).add(f, last, now,
                this, nar);

        if (r!=null)
            attn.ensure(r.input, attn.elementPri());

        return r;
    }


    public Truth actionTruth() {
        return actionTruth;
    }


}
