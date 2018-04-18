package nars.op;

import com.google.common.collect.Iterables;
import jcog.data.graph.AdjGraph;
import jcog.list.FasterList;
import jcog.math.FloatRange;
import nars.*;
import nars.concept.Concept;
import nars.control.DurService;
import nars.control.channel.CauseChannel;
import nars.task.ITask;
import nars.task.NALTask;
import nars.task.signal.SignalTask;
import nars.term.Term;
import nars.term.Termed;
import nars.truth.Truth;
import nars.truth.TruthAccumulator;
import nars.truth.func.BeliefFunction;
import nars.truth.func.TruthOperator;
import nars.util.graph.TermGraph;
import nars.util.time.Tense;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static nars.Op.*;
import static nars.util.time.Tense.DTERNAL;
import static nars.util.time.Tense.TIMELESS;
import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;


/**
 * causal implication booster / compiler
 * TODO make Causable
 */
public class Implier extends DurService {


    private final Iterable<Term> seeds;
    private final CauseChannel<ITask> in;


    AdjGraph<Term, Term> impl = null;
    //NodeGraph<Term, Term> impl;

    private final float[] relativeTargetDurs;

    /** truth cache */
    //private final Map<Term, Truth> desire = new HashMap();
    /**
     * truth cache
     */
//    private final Map<Term, Task> belief = new HashMap();

    final Map<LongObjectPair<Term>, TruthAccumulator> beliefTruth = new HashMap();
    final Map<LongObjectPair<Term>, TruthAccumulator> goalTruth = new HashMap();

    final static TruthOperator ded = BeliefFunction.get($.the("Deduction"));
    final static TruthOperator ind = BeliefFunction.get($.the("Induction"));

    private final FloatRange strength = new FloatRange(0.5f, 0f, 1f);
    private long then, now;
    private int dur;

    //final ArrayBag<Task,PLink<Task>> implTasks = new PLinkArrayBag(256, PriMerge.max, new HashMap());

    public Implier(NAR n, float[] relativeTargetDur, Term... seeds) {
        this(n, List.of(seeds), relativeTargetDur);
        assert (seeds.length > 0);
    }


//    public Implier(NAgent a, float... relativeTargetDurs) {
//        this(1, a, relativeTargetDurs);
//    }

    public Implier(float everyDurs, NAgent a, float... relativeTargetDurs) {
        this(everyDurs, a.nar(),
                Iterables.concat(
                        Iterables.transform(
                                a.actions.keySet(), Termed::term
                        ),
                        Iterables.transform(
                                a.happy, Termed::term
                        )
                ),
                relativeTargetDurs
        );
    }

    public Implier(NAR n, Iterable<Term> seeds, float... relativeTargetDurs) {
        this(1, n, seeds, relativeTargetDurs);
    }

    public Implier(float everyDurs, NAR n, Iterable<Term> seeds, float... relativeTargetDurs) {
        super(n, everyDurs);

        assert (relativeTargetDurs.length > 0);

        Arrays.sort(relativeTargetDurs);
        this.relativeTargetDurs = relativeTargetDurs;
        this.seeds = seeds;
        this.in = n.newChannel(this);
//        this.tg = new TermGraph.ImplGraph() {
//            @Override
//            protected boolean acceptTerm(Term p) {
//                return !(p instanceof Variable);// && !p.isTemporal();
//            }
//        };
    }

    @Override
    protected void run(NAR nar, long dt) {

        search(nar);

        int implCount = impl.edgeCount();
        if (implCount == 0)
            return;

        //impl.edges.forEach


        dur = nar.dur();
        now = nar.time();

        then = TIMELESS;
        beliefTruth.clear();
        goalTruth.clear();

        int dtDither = nar.dtDitherCycles();

        for (float relativeTargetDur : relativeTargetDurs) {


            long nextThen = Tense.dither(now + Math.round(relativeTargetDur * dur), dtDither);
            if (then == nextThen)
                continue; //same time as previous iteration, don't repeat

            then = nextThen;


            impl.each((subj, pred, implTerm) -> {

                Concept implConcept = nar.concept(implTerm);
                if (implConcept!=null)
                    implConcept.beliefs().forEachTask(this::imply);


            });

        }

        if (!beliefTruth.isEmpty() || !goalTruth.isEmpty()) {
            commit();
        }

    }

    private void imply(Task impl) {

        if (ArrayUtils.indexOf(impl.cause(), in.id)!=-1)
            return; //avoid cyclical logic by any implications at least partially resulting from this cause

        int implDT = impl.dt();
        if (implDT == DTERNAL)
            implDT = 0;

        Term implTerm = impl.term();
        Term S = implTerm.sub(0);
        boolean Sneg = false;
        if (S.op()==NEG) {
            Sneg = true;
            S = S.unneg();
        }

        Term P = implTerm.sub(1);

        /*
        C = condition
        S = subj
        P = pred
        */

        {
        /*
            S, (S ==> P) |- P (Belief:Deduction)
            P_belief(C,S) = ded(S_belief, impl_belief)
         */

            long when = then;
            Truth implTruth = impl.truth(when, dur);
            if (implTruth!=null) {

                Truth S_belief = nar.beliefTruth(S, when);
                if (S_belief != null) {
                    if (implTruth.isPositive()) {
                        //IMPL+
                        Truth P_belief_pos = ded.apply(S_belief.negIf(Sneg), implTruth, nar, Float.MIN_NORMAL);
                        if (P_belief_pos != null)
                            believe(P, then + implDT, P_belief_pos);
                    } else {
                        //IMPL-
                        Truth P_belief_neg = ded.apply(S_belief.negIf(Sneg), implTruth.neg(), nar, Float.MIN_NORMAL);
                        if (P_belief_neg != null)
                            believe(P, then + implDT, P_belief_neg.neg());
                    }
                }
            }
        }

        {
        /*
            S, (S ==> P) |- P (Goal:Induction)
            P_goal(C,S) = ind(S_goal, impl_belief)
         */

            long when = then;
            Truth implTruth = impl.truth(when, dur);
            if (implTruth!=null) {

                Truth S_goal = nar.goalTruth(S, when);
                if (S_goal != null) {
                    if (implTruth.isPositive()) {

                        //IMPL+
                        Truth P_goal_pos = ind.apply(S_goal.negIf(Sneg), implTruth, nar, Float.MIN_NORMAL);
                        if (P_goal_pos != null)
                            goal(P, then + implDT, P_goal_pos);
                    } else {
                        //IMPL-
                        Truth P_goal_neg = ind.apply(S_goal.negIf(Sneg), implTruth.neg(), nar, Float.MIN_NORMAL);
                        if (P_goal_neg != null)
                            goal(P, then + implDT, P_goal_neg.neg());
                    }
                }
            }
        }
        {
        /*
            P, (S ==> P) |- S (Goal:Deduction)
            S_goal(C,S) = ded(P_goal, impl_belief)
         */

            long when = then;
            Truth implTruth = impl.truth(when, dur);
            if (implTruth!=null) {

                Truth P_goal = nar.goalTruth(P, when + implDT);
                if (P_goal != null) {
                    if (implTruth.isPositive()) {

                        //IMPL+
                        Truth S_goal_pos = ded.apply(P_goal, implTruth, nar, Float.MIN_NORMAL);
                        if (S_goal_pos != null)
                            goal(P, then, S_goal_pos);
                    } else {
                        //IMPL-
                        Truth S_goal_neg = ded.apply(P_goal.neg(), implTruth.neg(), nar, Float.MIN_NORMAL);
                        if (S_goal_neg != null)
                            goal(P, then, S_goal_neg);
                    }
                }
            }
        }


        //TODO
        //   P,   (S ==> P) |-   S (Belief:Abduction)
        //   P, (--S ==> P) |- --S (Belief:Abduction)
        // x P,   (S ==> P) |-   S (Goal:Deduction)  ^^above
        //   P, (--S ==> P) |- --S (Goal:Deduction)

    }

    private void believe(Term p, long at, Truth belief) {
        beliefTruth.computeIfAbsent(pair(at, p), (k) -> new TruthAccumulator()).add(belief);
    }
    private void goal(Term p, long at, Truth goal) {
        goalTruth.computeIfAbsent(pair(at, p), (k) -> new TruthAccumulator()).add(goal);
    }

    private void commit() {

        List<Task> gen = new FasterList();

        taskify(beliefTruth, BELIEF, gen);
        taskify(goalTruth, GOAL, gen);

        if (!gen.isEmpty())
            in.input(gen);

    }

    private void taskify(Map<LongObjectPair<Term>, TruthAccumulator> truths, byte punc, List<Task> gen) {
        float freqRes = nar.freqResolution.floatValue();
        float confRes = nar.confResolution.floatValue();

        float strength = this.strength.floatValue();
        float confMin = nar.confMin.floatValue();

        truths.forEach((tw, a) -> {
            Term t = tw.getTwo();
            long w = tw.getOne();
            long wEnd = w + dur;
            @Nullable Truth uu = a.commitSum().dither(freqRes, confRes, confMin, strength);
            long stamp = nar.time.nextStamp();
            NALTask y;
            if (uu != null && uu.conf() >= confMin) {
                y = new SignalTask(t, punc, uu, now, w, wEnd, stamp);
            } else {
                y = new SignalTask(t, punc == GOAL ? QUEST : QUESTION, null, now, w, wEnd, stamp);
            }
            y.pri(nar.priDefault(y.punc));

            //                        if (Param.DEBUG)
            //                            y.log("")

            //System.out.println("\t" + y);

            gen.add(y);
        });
    }


    protected void search(NAR nar) {
        if (impl == null || impl.edgeCount() > 256) { //HACK
            impl = new AdjGraph(true);
        }

        TermGraph.Statements.update(impl, seeds, nar,
                (t) -> !t.hasAny(Op.VAR_QUERY.bit | Op.VAR_INDEP.bit),
                (t) -> t.op() == IMPL
        );
    }

    //    private Truth desire(Term x) {
//        return desire.computeIfAbsent(x, (xx) -> desire(xx, then));
//    }


//    public void goal(Map<LongObjectPair<Term>, TruthAccumulator> goals, Term tt, Truth g) {
//
//        if (tt.op() == NEG) {
//            tt = tt.unneg();
//            g = g.neg();
//        }
//
//        if (!tt.op().conceptualizable)
//            return;
//
//        //recursively divide the desire among the conjunction events occurring NOW,
//        // emulating (not necessarily exactly) StructuralDeduction's
//        if (tt.op() == CONJ) {
//            FastList<LongObjectPair<Term>> e = tt.eventList();
//            if (e.size() > 1) {
//                float eSub = g.evi() / e.size();
//                float cSub = w2c(eSub);
//                if (cSub >= nar.confMin.floatValue()) {
//                    Truth gSub = $.t(g.freq(), cSub);
//                    for (LongObjectPair<Term> ee : e) {
//                        Term one = ee.getTwo();
//                        if (ee.getOne() == 0  && (one.op().conceptualizable))
//                            goal(goals, one, gSub);
//                    }
//                }
//                return;
//            }
//        }
//
//        goals.computeIfAbsent(tt, (ttt) -> new TruthAccumulator()).add(g);
//    }
}
