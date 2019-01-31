//package nars.agent.util;
//
//import com.google.common.collect.Iterables;
//import jcog.data.graph.AdjGraph;
//import jcog.data.list.FasterList;
//import jcog.math.FloatRange;
//import nars.$;
//import nars.NAR;
//import nars.Op;
//import nars.Task;
//import nars.agent.NAgent;
//import nars.concept.Concept;
//import nars.control.DurService;
//import nars.control.channel.CauseChannel;
//import nars.task.ITask;
//import nars.task.NALTask;
//import nars.task.signal.SignalTask;
//import nars.target.Term;
//import nars.target.Termed;
//import nars.time.Tense;
//import nars.truth.Truth;
//import nars.truth.TruthAccumulator;
//import nars.truth.func.NALTruth;
//import nars.truth.func.TruthFunc;
//import nars.util.graph.TermGraph;
//import org.apache.commons.lang3.ArrayUtils;
//import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
//import org.jetbrains.annotations.Nullable;
//
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import static nars.Op.*;
//import static nars.time.Tense.DTERNAL;
//import static nars.time.Tense.TIMELESS;
//import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;
//
//
///**
// * causal implication booster / compiler
// * TODO make Causable
// */
//public class Implier extends DurService {
//
//
//    private final Iterable<Term> seeds;
//    private final CauseChannel<ITask> in;
//
//
//    AdjGraph<Term, Term> impl = null;
//
//
//    private final float[] relativeTargetDurs;
//
//    /** truth cache */
//
//    /**
//     * truth cache
//     */
//
//
//    final Map<LongObjectPair<Term>, TruthAccumulator> beliefTruth = new HashMap();
//    final Map<LongObjectPair<Term>, TruthAccumulator> goalTruth = new HashMap();
//
//    final static TruthFunc ded = NALTruth.get($.the("Deduction"));
//    final static TruthFunc ind = NALTruth.get($.the("Induction"));
//
//    private final FloatRange strength = new FloatRange(0.5f, 0f, 1f);
//    private long then, now;
//    private int dur;
//
//
//
//    public Implier(NAR n, float[] relativeTargetDur, Term... seeds) {
//        this(n, List.of(seeds), relativeTargetDur);
//        assert (seeds.length > 0);
//    }
//
//
//
//
//
//
//    public Implier(float everyDurs, NAgent a, float... relativeTargetDurs) {
//        this(everyDurs, a.nar(),
//                Iterables.concat(
//                        Iterables.transform(
//                                a.actions(), Termed::target
//                        ),
//                        Iterables.transform(
//                                a.happy, Termed::target
//                        )
//                ),
//                relativeTargetDurs
//        );
//    }
//
//    public Implier(NAR n, Iterable<Term> seeds, float... relativeTargetDurs) {
//        this(1, n, seeds, relativeTargetDurs);
//    }
//
//    public Implier(float everyDurs, NAR n, Iterable<Term> seeds, float... relativeTargetDurs) {
//        super(n, everyDurs);
//
//        assert (relativeTargetDurs.length > 0);
//
//        Arrays.sort(relativeTargetDurs);
//        this.relativeTargetDurs = relativeTargetDurs;
//        this.seeds = seeds;
//        this.in = n.newChannel(this);
//
//
//
//
//
//
//    }
//
//    @Override
//    protected void run(NAR nar, long dt) {
//
//        search(nar);
//
//        int implCount = impl.edgeCount();
//        if (implCount == 0)
//            return;
//
//
//
//
//        dur = nar.dur();
//        now = nar.time();
//
//        then = TIMELESS;
//        beliefTruth.clear();
//        goalTruth.clear();
//
//        int dtDither = nar.dtDither();
//
//        for (float relativeTargetDur : relativeTargetDurs) {
//
//
//            long nextThen = Tense.dither(now + Math.round(relativeTargetDur * dur), dtDither);
//            if (then == nextThen)
//                continue;
//
//            then = nextThen;
//
//
//            impl.each((subj, pred, implTerm) -> {
//
//                Concept implConcept = nar.concept(implTerm);
//                if (implConcept!=null)
//                    implConcept.beliefs().forEachTask(this::imply);
//
//
//            });
//
//        }
//
//        if (!beliefTruth.isEmpty() || !goalTruth.isEmpty()) {
//            commit();
//        }
//
//    }
//
//    private void imply(Task impl) {
//
//        if (ArrayUtils.indexOf(impl.cause(), in.id)!=-1)
//            return;
//
//        int implDT = impl.dt();
//        if (implDT == DTERNAL)
//            implDT = 0;
//
//        Term implTerm = impl.target();
//        Term S = implTerm.sub(0);
//        boolean Sneg = false;
//        if (S.op()==NEG) {
//            Sneg = true;
//            S = S.unneg();
//        }
//
//        Term P = implTerm.sub(1);
//
//        /*
//        C = condition
//        S = subj
//        P = pred
//        */
//
//        {
//        /*
//            S, (S ==> P) |- P (Belief:Deduction)
//            P_belief(C,S) = ded(S_belief, impl_belief)
//         */
//
//            long when = then;
//            Truth implTruth = impl.truth(when, dur);
//            if (implTruth!=null) {
//
//                Truth S_belief = nar.beliefTruth(S, when);
//                if (S_belief != null) {
//                    if (implTruth.isPositive()) {
//
//                        Truth P_belief_pos = ded.apply(S_belief.negIf(Sneg), implTruth, nar, Float.MIN_NORMAL);
//                        if (P_belief_pos != null)
//                            believe(P, then + implDT, P_belief_pos);
//                    } else {
//
//                        Truth P_belief_neg = ded.apply(S_belief.negIf(Sneg), implTruth.neg(), nar, Float.MIN_NORMAL);
//                        if (P_belief_neg != null)
//                            believe(P, then + implDT, P_belief_neg.neg());
//                    }
//                }
//            }
//        }
//
//        {
//        /*
//            S, (S ==> P) |- P (Goal:Induction)
//            P_goal(C,S) = ind(S_goal, impl_belief)
//         */
//
//            long when = then;
//            Truth implTruth = impl.truth(when, dur);
//            if (implTruth!=null) {
//
//                Truth S_goal = nar.goalTruth(S, when);
//                if (S_goal != null) {
//                    if (implTruth.isPositive()) {
//
//
//                        Truth P_goal_pos = ind.apply(S_goal.negIf(Sneg), implTruth, nar, Float.MIN_NORMAL);
//                        if (P_goal_pos != null)
//                            goal(P, then + implDT, P_goal_pos);
//                    } else {
//
//                        Truth P_goal_neg = ind.apply(S_goal.negIf(Sneg), implTruth.neg(), nar, Float.MIN_NORMAL);
//                        if (P_goal_neg != null)
//                            goal(P, then + implDT, P_goal_neg.neg());
//                    }
//                }
//            }
//        }
//        {
//        /*
//            P, (S ==> P) |- S (Goal:Deduction)
//            S_goal(C,S) = ded(P_goal, impl_belief)
//         */
//
//            long when = then;
//            Truth implTruth = impl.truth(when, dur);
//            if (implTruth!=null) {
//
//                Truth P_goal = nar.goalTruth(P, when + implDT);
//                if (P_goal != null) {
//                    if (implTruth.isPositive()) {
//
//
//                        Truth S_goal_pos = ded.apply(P_goal, implTruth, nar, Float.MIN_NORMAL);
//                        if (S_goal_pos != null)
//                            goal(P, then, S_goal_pos);
//                    } else {
//
//                        Truth S_goal_neg = ded.apply(P_goal.neg(), implTruth.neg(), nar, Float.MIN_NORMAL);
//                        if (S_goal_neg != null)
//                            goal(P, then, S_goal_neg);
//                    }
//                }
//            }
//        }
//
//
//
//
//
//
//
//
//    }
//
//    private void believe(Term p, long at, Truth belief) {
//        beliefTruth.computeIfAbsent(pair(at, p), (k) -> new TruthAccumulator()).addAt(belief);
//    }
//    private void goal(Term p, long at, Truth goal) {
//        goalTruth.computeIfAbsent(pair(at, p), (k) -> new TruthAccumulator()).addAt(goal);
//    }
//
//    private void commit() {
//
//        List<Task> gen = new FasterList();
//
//        taskify(beliefTruth, BELIEF, gen);
//        taskify(goalTruth, GOAL, gen);
//
//        if (!gen.isEmpty())
//            in.input(gen);
//
//    }
//
//    private void taskify(Map<LongObjectPair<Term>, TruthAccumulator> truths, byte punc, List<Task> gen) {
//        float freqRes = nar.freqResolution.floatValue();
//        float confRes = nar.confResolution.floatValue();
//
//        float strength = this.strength.floatValue();
//        float confMin = nar.confMin.floatValue();
//
//        truths.forEach((tw, a) -> {
//            Term t = tw.getTwo();
//            long w = tw.getOne();
//            long wEnd = w + dur;
//            @Nullable Truth uu = a.commitSum().dither(freqRes, confRes, confMin, strength);
//            long stamp = nar.evidence()[0];
//            NALTask y;
//            if (uu != null && uu.conf() >= confMin) {
//                y = new SignalTask(t, punc, uu, now, w, wEnd, stamp);
//            } else {
//                y = new SignalTask(t, punc == GOAL ? QUEST : QUESTION, null, now, w, wEnd, stamp);
//            }
//            y.pri(nar.priDefault(y.punc));
//
//
//
//
//
//
//            gen.addAt(y);
//        });
//    }
//
//
//    protected void search(NAR nar) {
//        if (impl == null || impl.edgeCount() > 256) {
//            impl = new AdjGraph(true);
//        }
//
//        TermGraph.Statements.update(impl, seeds, nar,
//                (t) -> !t.hasAny(Op.VAR_QUERY.bit | Op.VAR_INDEP.bit),
//                (t) -> t.op() == IMPL
//        );
//    }
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//}
