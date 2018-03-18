package nars.concept.dynamic;

import jcog.Util;
import jcog.math.LongInterval;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.table.BeliefTable;
import nars.task.util.TaskRegion;
import nars.term.Term;
import nars.term.compound.util.Conj;
import nars.truth.PreciseTruth;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

import static nars.Op.*;
import static nars.time.Tense.*;
import static nars.truth.TruthFunctions.c2wSafe;

/**
 * Created by me on 12/4/16.
 */
abstract public class DynamicTruthModel implements BiFunction<DynTruth,NAR,Truth> {


    @Nullable
    public DynTruth eval(final Term superterm, boolean beliefOrGoal, long start, long end, NAR n) {

        final int _DT = superterm.dt();

        assert (_DT != XTERNAL);
        assert(superterm.op()!=NEG);

        Term[] inputs = components(superterm);
        for (Term t : inputs) {
            if (!t.unneg().op().conceptualizable) //a component for which truth can not be determined anyway: variable, INT etc
                return null;
        }
        assert (inputs.length > 0) : this + " yielded no dynamic components for superterm " + superterm;

        DynTruth d = new DynTruth(inputs.length);

        int odt = 0;

        for (int i = 0; i < inputs.length; i++) {
            Term it = inputs[i];


            long subStart, subEnd;


            if (start == ETERNAL || superterm.op() != CONJ || _DT == DTERNAL) {
                subStart = start;
                subEnd = end;
            } else {
                int dt = superterm.subTimeSafe(it, odt);
                if (dt == DTERNAL) {
                    superterm.subTimeSafe(it, odt);
                    if (Param.DEBUG)
                        throw new RuntimeException(it + " not found in superterm: " + superterm + " -> " + Arrays.toString(inputs));
                    else
                        return null;
                }
                //assert (dt != DTERNAL): it + " not found in superterm: " + superterm;
                subStart = start + dt;
                subEnd = end + dt;
                odt += dt;
            }


            boolean negated = it.op() == Op.NEG;
            if (negated)
                it = it.unneg();

            Concept subConcept =
                    //n.concept(it);
                    n.conceptualize(it); //force creation of concept, which if dynamic, could provide data for use here

            @Nullable Task bt;
            if (!(subConcept instanceof TaskConcept)) {
//                if (Param.DEBUG) {
//                    if (!(subConcept == null || !(subConcept instanceof NodeConcept)))
//                        throw new RuntimeException(it + " does not reference a TaskConcept: " + subConcept);
//                }
                return null;
            } else {

                BeliefTable table = (BeliefTable) subConcept.table(beliefOrGoal ? BELIEF : GOAL);

                bt = table.match(subStart, subEnd, it, n, d::filterOverlap);
                if (bt != null) {
                    bt = Task.project(bt, subStart, subEnd, n, negated);
                } else {
                    bt = null; //missing truth, but yet still may be able to conclude a result
                }

            }

            if (!add(bt, d))
                return null;
        }

        return d;
    }

    protected boolean add(@Nullable Task bt, DynTruth d) {
        return d.add(bt);
    }



    abstract public Term[] components(Term superterm);


    /**
     * used to reconstruct a dynamic term from some or all components
     */
    abstract public Term construct(Term superterm, List<TaskRegion> c);


    /**
     * conf is multiplied, freq is OR'd
     */
    public static final class Union extends DynamicTruthModel.Intersection {

        public Union(Term... comp) {
            super(comp);
            assert (comp.length > 1);
        }

        @Override
        protected float f(float freq) {
            return 1f - freq;
        }

    }

    public static class Intersection extends DynamicTruthModel {

        /**
         * for use with conjunctions whose subterms may change as its DT changes
         */
        public final static Intersection conj = new Intersection(null);

        private final Term[] comp;

        public Intersection(Term[] comp) {
            this.comp = comp;
        }

        @Override
        @NotNull
        public Term construct(Term superterm, List<TaskRegion> components) {

            int n = components.size();
            if (n == 1) {
                return components.get(0).task().term();
            }

            boolean conj = superterm.op() == CONJ;
            if (conj) {
                Conj c = new Conj();
                //long estVolume = ((FasterList<TaskRegion>)components).sumOfInt(xt -> ((Task)xt).term().volume());
                //TODO heuristic for range sampling parameters
                for (TaskRegion t : components) {
                    if (!c.add(((Task)t).term(), t.start(), t.end(), 2, 1))
                        return Null; //TODO maybe try with less aggressive sampling, if sampling was used
                }
                return c.term();
            } else {
                //SECT's

                Term[] ct = Util.map(0, n, c -> components.get(c).task().term(), Term[]::new);
                if (n == 2) {
                    return inhConstruct2(superterm, SECTe.bit | SECTi.bit, ct);
                } else {
                    return inhConstructN(superterm, SECTe.bit | SECTi.bit, ct);
                }
            }
        }


        @Override
        public Truth apply(DynTruth l, NAR nar) {

            //compute the lazy computed truth, if possible
            int n = l.size();
            int avail = 0;
            for (int i = 0; i < n; i++) {
                LongInterval li = l.get(i);
                if (li!=null) {
                    if ((((Task) li)).truth() == null)
                        l.set(i, null);
                    else
                        avail++;
                }
            }
            if (avail == 0)
                return null;

            int[] order = new int[n];
            for (int i = 0; i < n; i++) {
                order[i] = i;
            }

            //sort by lowest expectation
            jcog.data.array.Arrays.sort(order, (i) -> l.get(i) != null ? (1f - f(l.get(i).expectation())) : Float.NEGATIVE_INFINITY);

            float confMin = nar.confMin.floatValue();
            float freqRes = nar.freqResolution.floatValue();

            float f = 1f, c = 1f;
            int considered = 0;
            for (int i = 0; i < n; i++) {
                TaskRegion ii = l.get(order[i]);
                if (ii == null)
                    continue;
                float cx = ii.confMin();
                c *= cx;
                if (c < confMin)
                    return null;
                float fx = ii.freqMean();
                f *= f(fx);
                considered++;
                if (f < freqRes) {
                    f = 0;
                    //short-circuit
                    if (i < n - 1) {
                        //delete the tasks (evidence) from the dyntruth which are not involved
                        for (int j = i + 1; j < n; j++) {
                            int oj = order[j];
                            l.set(oj, null);
                        }
                    }
                    break;
                }
            }
            if (considered == 0)
                return null;

            if (considered != n) {
                if (f >= freqRes)
                    return null; //missing components without having reached blackhole asymptote f=0
                else {
                    l.removeNulls(); //remove nulls and continue with only the components necessary
                }
            }

            return new PreciseTruth(f(f), c);
        }

        protected float f(float freq) {
            return freq;
        }


        @Override
        public Term[] components(Term superterm) {
            return comp != null ? comp : superterm.subterms().arrayShared();
        }

    }

    public static class Difference extends DynamicTruthModel {
        private final Term[] components;

        public Difference(Term[] xy) {

//            assert (!(xy[0] instanceof Bool) && !(xy[1] instanceof Bool));
//            assert (!(xy[0] instanceof Variable) && !(xy[1] instanceof Variable)) :
//                    xy[0] + " or " + xy[1] + " is a variable";

            this.components = xy;
        }

        public Difference(Term x, Term y) {
            this(new Term[]{x, y});
        }

        @Override
        protected boolean add(@Nullable Task bt, DynTruth d) {
            if (bt == null)
                return false;

            return super.add(bt, d);
        }

        @Override
        public Term construct(Term superterm, List<TaskRegion> components) {

            Term a = components.get(0).task().term();
            Term b = components.get(1).task().term();

            if (superterm.op() == INH) {
                return inhConstruct2(superterm, DIFFe.bit | DIFFi.bit, a, b);
            } else if (superterm.op() == DIFFe) {
                //raw difference
                return Op.DIFFe.the(DTERNAL, a, b);
            }

            throw new RuntimeException();
        }

        @Override
        public Term[] components(Term superterm) {
            return components;
        }

        @Override
        public Truth apply(DynTruth d, NAR n) {
            assert (d.size() == 2);
            TaskRegion a = d.get(0);
//            if (((Task)a).truth()==null)
//                return null;
            TaskRegion b = d.get(1);
//            if (((Task)b).truth()==null)
//                return null;
            float conf = a.confMin() * b.confMin();
            float freq = a.freqMean() * (1f - b.freqMean());
            return Truth.theDithered(freq, c2wSafe(conf), n);
        }
    }

    private static Term inhConstruct2(Term superterm, int bits, Term... components) {
        {
            Term subj = superterm.sub(0);
            Op so = subj.op();
            if (so.isAny(bits))
                return INH.the(so.the(components[0].sub(0), components[1].sub(0)), superterm.sub(1));
        }

        {
            Term pred = superterm.sub(1);
            Op po = pred.op();
            if (po.isAny(bits))
                return INH.the(superterm.sub(0), po.the(components[0].sub(1), components[1].sub(1)));
        }

        return null;
    }

    private static Term inhConstructN(Term superterm, int bits, Term[] components) {
        {
            Term subj = superterm.sub(0);
            Op so = subj.op();
            if (so.isAny(bits))
                return INH.the(
                        so.the(Util.map(x -> x.sub(0), new Term[components.length], components)),
                        superterm.sub(1));
        }

        {
            Term pred = superterm.sub(1);
            Op po = pred.op();
            if (po.isAny(bits))
                return INH.the(
                        superterm.sub(0),
                        po.the(Util.map(x -> x.sub(1), new Term[components.length], components))
                );
        }

        return null;
    }

}