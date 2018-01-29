package nars.op;

import com.google.common.collect.Iterables;
import jcog.data.graph.AdjGraph;
import jcog.list.FasterList;
import jcog.math.FloatParam;
import nars.*;
import nars.concept.ActionConcept;
import nars.concept.Concept;
import nars.control.CauseChannel;
import nars.control.DurService;
import nars.task.ITask;
import nars.task.NALTask;
import nars.term.Term;
import nars.truth.Truth;
import nars.truth.TruthAccumulator;
import nars.truth.func.BeliefFunction;
import nars.truth.func.TruthOperator;
import nars.util.graph.TermGraph;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static nars.Op.*;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.TIMELESS;
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
    //final static TruthOperator ind = GoalFunction.get($.the("DeciInduction"));

    private final FloatParam strength = new FloatParam(0.5f, 0f, 1f);
    private long then, now;
    private int dur;

    public Implier(NAR n, float[] relativeTargetDur, Term... seeds) {
        this(n, List.of(seeds), relativeTargetDur);
        assert (seeds.length > 0);
    }


    public Implier(float everyDurs, NAgent a, float... relativeTargetDurs) {
        this(everyDurs, a.nar,
                Iterables.concat(
                        Iterables.transform(a.actions.keySet(), ActionConcept::term),
                        Collections.singleton(a.happy.term)
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
        this.in = n.newCauseChannel(this);
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


        dur = nar.dur();
        now = nar.time();

        then = TIMELESS;
        beliefTruth.clear();
        goalTruth.clear();

        for (float relativeTargetDur : relativeTargetDurs) {


            long nextThen = now + Math.round(relativeTargetDur * dur);
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


        {
        /*
            C = condition
            S = subj
            P = pred
            S, (S ==> P),  notImpl(B) |- P (Belief:Deduction, Goal:DeciInduction)


            P_belief(C,S) = ded(S_belief, impl_belief)
            //TODO if impl is neg
         */

            long when = then;
            Truth implTruth = impl.truth(when, dur, nar);
            if (implTruth!=null) {

                Truth S_belief = nar.beliefTruth(S, when);
                if (S_belief != null) {

                    {
                        //IMPL+
                        Truth P_belief = ded.apply(S_belief.negIf(Sneg), implTruth, nar, Float.MIN_NORMAL);
                        if (P_belief != null)
                            believe(P, then + implDT, P_belief);
                    }
                    {
                        //IMPL-
                        Truth P_belief = ded.apply(S_belief.negIf(!Sneg), implTruth.neg(), nar, Float.MIN_NORMAL);
                        if (P_belief != null)
                            believe(P, then + implDT, P_belief.neg());
                    }
                }
            }
        }

//        Truth Gdesire = nar.goalTruth(pred, this.then + implDT); //the desire at the predicate time
//        if (Gdesire == null)
//            return;


//        Truth Sg = ded.apply(Gdesire, $.t(impl.freq(), implConf), nar, 0);
//
//        if (Sg != null) {
//            goal(goalTruth, subj, Sg);
//        }


        //experimental:
        //            {
        //                //G, (G ==> P) |- P (Goal:InductionRecursivePB)
        //                //G, ((--,G) ==> P) |- P (Goal:InductionRecursivePBN)
        //
        //                //HACK only immediate future otherwise it needs scheduled further
        //                if (implDT >= 0 && implDT <= dur/2) {
        //
        //                    Truth Ps = desire(subj, nowStart, nowEnd); //subj desire now
        //                    if (Ps == null)
        //                        return;
        //
        //                    float implFreq = f;
        //
        //
        //
        //                    if (Ps.isNegative()) {
        //                        subj = subj.neg();
        //                        Ps = Ps.neg();
        //                    }
        //
        //                    //TODO invert g and choose indRec/indRecN
        //                    Truth Pg = indRec.apply(Ps, $.t(implFreq, implConf), nar, confSubMin);
        //                    if (Pg != null) {
        //                        goal(goalTruth, pred, Pg);
        //                    }
        //                }
        //            }


    }

    private void believe(Term p, long at, Truth p_belief) {
        beliefTruth.computeIfAbsent(pair(at, p), (k) -> new TruthAccumulator()).add(p_belief);
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
            @Nullable Truth uu = a.commitSum().dither(freqRes, confRes, confMin, strength);
            long[] stamp = nar.time.nextInputStamp();
            NALTask y;
            if (uu != null && uu.conf() > confMin) {

                y = new NALTask(t, punc, uu, now, w, w, stamp);
            } else {
                y = new NALTask(t, punc == GOAL ? QUEST : QUESTION, null, now, w, w, stamp);
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
