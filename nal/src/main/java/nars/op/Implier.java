package nars.op;

import com.google.common.collect.Iterables;
import jcog.data.graph.AdjGraph;
import jcog.math.FloatParam;
import jcog.pri.Prioritized;
import nars.$;
import nars.NAR;
import nars.NAgent;
import nars.Task;
import nars.concept.ActionConcept;
import nars.control.CauseChannel;
import nars.control.DurService;
import nars.task.ITask;
import nars.task.NALTask;
import nars.term.Term;
import nars.term.var.Variable;
import nars.truth.Truth;
import nars.truth.TruthAccumulator;
import nars.truth.func.GoalFunction;
import nars.truth.func.TruthOperator;
import nars.util.graph.TermGraph;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static nars.Op.*;
import static nars.time.Tense.DTERNAL;
import static nars.truth.TruthFunctions.w2c;
import static nars.truth.TruthFunctions.w2cSafe;


/**
 * causal implication booster / compiler
 * TODO make Causable
 */
public class Implier extends DurService {

    private final TermGraph.ImplGraph tg;
    private final Iterable<Term> seeds;
    private final CauseChannel<ITask> in;

    float min = Prioritized.EPSILON; //even though it's for truth
    final Map<Term, TruthAccumulator> goalTruth = new HashMap();

    AdjGraph<Term, Term> impl;

    private final float[] relativeTargetDurs;

    /**
     * truth cache
     */
    private final HashMap<Term, Truth> desire = new HashMap();
    /**
     * truth cache
     */
    private final HashMap<Term, Task> belief = new HashMap();


    final static TruthOperator ded = GoalFunction.get($.the("DeciDeduction"));
    final static TruthOperator ind = GoalFunction.get($.the("DeciInduction"));
    private long then;
    private final FloatParam strength = new FloatParam(0.5f, 0f, 1f);
    private long now;

    public Implier(NAR n, float[] relativeTargetDur, Term... seeds) {
        this(n, List.of(seeds), relativeTargetDur);
        assert(seeds.length > 0);
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

        this.relativeTargetDurs = relativeTargetDurs;
        this.seeds = seeds;
        this.in = n.newCauseChannel(this);
        this.tg = new TermGraph.ImplGraph() {
            @Override
            protected boolean acceptTerm(Term p) {
                return !(p instanceof Variable);// && !p.isTemporal();
            }
        };
    }

    @Override
    protected void run(NAR nar, long dt) {


        if (impl != null && impl.edgeCount() > 256) { //HACK
//            System.err.print("saved impl graph to file");
//            try {
//                impl.writeGML(new PrintStream(new FileOutputStream("/tmp/x.gml")));
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            }
            impl = null; //reset
        }

        impl = tg.snapshot(impl, seeds, nar);
        int implCount = impl.edgeCount();

        if (implCount == 0)
            return;

        float confMin = nar.confMin.floatValue();
        float confSubMin = confMin / implCount;

        int dur = nar.dur();
        now = nar.time();

        for (float relativeTargetDur : relativeTargetDurs) {

            desire.clear();
            belief.clear();
            goalTruth.clear();

            then = now + Math.round(relativeTargetDur * dur);


            //System.out.println(impl);

            impl.each((subj, pred, impl) -> {


                Task SGimpl = belief(impl);
                if (SGimpl == null)
                    return;

                float implConf = w2cSafe(SGimpl.evi());
                if (implConf < confSubMin)
                    return;

                int implDT = SGimpl.dt();
                if (implDT == DTERNAL)
                    implDT = 0;


                //compute desire(S) @ then:
                //      desireDed( G @ then+implDT, belief(S ==>+- G) @ then )
                //G, (S ==>+- G) |- S  (Goal:DeductionRecursivePB)

                Truth Gdesire = desire(pred, this.then + implDT); //the desire at the predicate time
                if (Gdesire == null)
                    return;


                Truth Sg = ded.apply(Gdesire, $.t(SGimpl.freq(), implConf), nar, confSubMin);

                if (Sg != null) {
                    goal(goalTruth, subj, Sg);
                }


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

            });


            //            List<IntHashSet> ws = new GraphMeter().weakly(s);
            //            ws.forEach(x -> {
            //                if (!x.isEmpty()) { //HACK
            //                    System.out.println( x.collect(i -> s.node(i)) );
            //                }
            //            });

            float freqRes = nar.freqResolution.floatValue();
            float confRes = nar.confResolution.floatValue();

            float strength = this.strength.floatValue();

            goalTruth.forEach((t, a) -> {
                @Nullable Truth uu = a.commitSum().dither(freqRes, confRes, confMin,  strength);
                if (uu != null) {
                    float c = uu.conf();
                    NALTask y;
                    long[] stamp = nar.time.nextInputStamp();
                    if (c >= confMin) {
                        y = new NALTask(t, GOAL, uu, now, then-dur/2, then + dur/2 /* + dur */, stamp);
                    } else {
                        y = new NALTask(t, QUEST, null, now, then-dur/2, then + dur/2, stamp);
                    }
                    y.pri(nar.priDefault(y.punc));

                        //                        if (Param.DEBUG)
                        //                            y.log("")
                        in.input(y);
                        //System.out.println("\t" + y);

                }
            });

            //        if (s!=null)
            //            System.out.println(s.toString());
        }

    }

    private Truth desire(Term x, long from) {
        return nar.goalTruth(x, from);
    }

    private Truth desire(Term x) {
        return desire.computeIfAbsent(x, (xx) -> desire(xx, then));
    }

    private Task belief(Term x) {
        return belief.computeIfAbsent(x, (xx) -> nar.belief(xx, then));
    }

    public void goal(Map<Term, TruthAccumulator> goals, Term tt, Truth g) {

        if (tt.op() == NEG) {
            tt = tt.unneg();
            g = g.neg();
        }

        if (!tt.op().conceptualizable)
            return;

        //recursively divide the desire among the conjunction events occurring NOW,
        // emulating (not necessarily exactly) StructuralDeduction's
        if (tt.op() == CONJ) {
            FastList<LongObjectPair<Term>> e = tt.eventList();
            if (e.size() > 1) {
                float eSub = g.evi() / e.size();
                float cSub = w2c(eSub);
                if (cSub >= nar.confMin.floatValue()) {
                    Truth gSub = $.t(g.freq(), cSub);
                    for (LongObjectPair<Term> ee : e) {
                        Term one = ee.getTwo();
                        if (ee.getOne() == 0  && (one.op().conceptualizable))
                            goal(goals, one, gSub);
                    }
                }
                return;
            }
        }

        goals.computeIfAbsent(tt, (ttt) -> new TruthAccumulator()).add(g);
    }
}
