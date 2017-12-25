package nars.concept.dynamic;

import jcog.TODO;
import jcog.Util;
import jcog.list.FasterList;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.concept.NodeConcept;
import nars.concept.TaskConcept;
import nars.table.BeliefTable;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

import static nars.Op.*;
import static nars.time.Tense.*;

/**
 * Created by me on 12/4/16.
 */
abstract public class DynamicTruthModel {


    @Nullable
    public DynTruth eval(final Term superterm, boolean beliefOrGoal, long start, long end, boolean stamp, NAR n) {

        final int DT = superterm.dt();
        assert(DT!=XTERNAL);

        Term[] inputs = components(superterm);
        assert (inputs.length > 0) : this + " yielded no dynamic components for superterm " + superterm;

        DynTruth d = new DynTruth(stamp ? new FasterList(inputs.length) : null);
        d.freq = d.conf = 1f;

        final float confMin = 0; //n.confMin.floatValue();

        int dur = n.dur();
        boolean evi = d.e != null;

        Term[] outputs = null;
        int odt = 0;
        for (int i = 0; i < inputs.length; i++) {
            Term it = inputs[i];


            long subStart, subEnd;


                if (start == ETERNAL || superterm.op() != CONJ || DT==DTERNAL) {
                    subStart = start;
                    subEnd = end;
                } else {
                    int dt = superterm.subTimeSafe(it, odt);
                    if (dt==DTERNAL) {
                        superterm.subTimeSafe(it, odt);
                        throw new RuntimeException(it + " not found in superterm: " + superterm + " -> " + Arrays.toString(inputs));
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
                    n.concept(it);
                    //n.conceptualize(it); //force creation of concept, which if dynamic, could provide data for use here

            if (!(subConcept instanceof TaskConcept)) {
                if (Param.DEBUG) {
                    if (!(subConcept == null || !(subConcept instanceof NodeConcept)))
                        throw new RuntimeException(it + " does not reference a TaskConcept: " + subConcept);
                }
                return null;
            }


            Task bt;
            Term ot;
            Truth nt;
            BeliefTable table = (BeliefTable) ((TaskConcept) subConcept).table(beliefOrGoal ? BELIEF : GOAL);
            if (evi) {
                //task

                bt = table.match(subStart, subEnd, it, n);
                if (bt == null)
                    return null;
                nt = bt.truth(subStart, subEnd, dur, 0f); //project to target time if task isnt at it
                if (nt == null)
                    return null;

                ot = bt.term().negIf(negated);
//                if (ot.hasXternal())
//                    throw new RuntimeException("xternal");
            } else {
                //truth only
                bt = null;
                nt = table.truth(subStart, subEnd, n);
                if (nt == null)
                    return null;
                ot = null;
            }

            nt = nt.negIf(negated);

            if (!add(i, d, nt, confMin))
                return null;

            if (evi) {
                d.add(bt, nt);

                if (!inputs[i].equals(ot)) {
                    //template has changed
                    if (outputs == null)
                        outputs = inputs.clone();
                    outputs[i] = ot;
                }
            }
        }

        if (evi) {
            assert(!d.e.isEmpty());
            if (outputs != null) {
                Term reconstructed = superterm.op().the(DT, outputs);
                if (reconstructed instanceof Bool) {
                    if (reconstructed == True ) {
                        throw new TODO("absolute true result"); //dont interfere with callee's calculation
                    } else {
                        return null;
                    }
                }
                d.term = reconstructed;
            } else
                d.term = superterm;
        }

//        //if (template instanceof Compound) {
        return commit(d);
    }


    /**
     * override for postprocessing
     */
    protected DynTruth commit(DynTruth d) {
        return d;
    }

    abstract public Term[] components(Term superterm);

    //protected abstract DynTruth eval(Compound template, long when, boolean stamp, NAR n);

    protected abstract boolean add(int subterm, DynTruth d, Truth t, float confMin);


    /**
     * conf is multiplied, freq is OR'd
     */
    public static class Union extends DynamicTruthModel.Intersection {

        public Union( Term... comp) {
            super(comp);
            assert (comp.length > 1);
        }

        @Override
        protected float f(float freq) {
            return 1f - freq;
        }

        @Override
        protected DynTruth commit(DynTruth d) {
            d.freq = 1f - d.freq;
            return d;
        }
    }

    public static class Intersection extends DynamicTruthModel {

        /** for use with conjunctions whose subterms may change as its DT changes */
        public final static Intersection conj = new Intersection(null);
        
        private final Term[] comp;

        public Intersection( Term[] comp) {
            this.comp = comp;
        }

        @Override
        protected final boolean add(int subterm, DynTruth d, Truth truth, float confMin) {

            float f0 = d.freq;
            d.freq *= f(truth.freq());

            //HACK for subterms beyond 2, if the frequency has not changed, do not decrease confidence
            if (subterm < 2 || !Util.equals(f0, d.freq, Param.TRUTH_EPSILON)) {
                d.conf *= truth.conf();
                if (d.conf < confMin)
                    return false;
            }


            return true;

        }

        protected float f(float freq) {
            return freq;
        }


        
        @Override
        public Term[] components(Term superterm) {
            return comp!=null ? comp : superterm.subterms().arrayShared();
        }

    }

    public static class Difference extends DynamicTruthModel {
        private final Term[] components;

        public Difference(Term[] xy) {
            super();

//            assert (!(xy[0] instanceof Bool) && !(xy[1] instanceof Bool));
//            assert (!(xy[0] instanceof Variable) && !(xy[1] instanceof Variable)) :
//                    xy[0] + " or " + xy[1] + " is a variable";

            this.components = xy;
        }

        public Difference(Term x, Term y) {
            this(new Term[]{x, y});
        }

        
        @Override
        public Term[] components(Term superterm) {
            return components;
        }

        @Override
        protected boolean add(int subterm, DynTruth d, Truth t, float confMin) {
            float c = t.conf();
            if (subterm == 0) {
                if (c < confMin)
                    return false;
                d.conf = c;
                d.freq = t.freq();
            } else {

                if ((d.conf *= c) < confMin)
                    return false;

                d.freq *= (1f - t.freq());

//                if (t.conf() * tx.conf() < confMin) //early termination check
//                    return false;
            }

            return true;
        }
    }

    public static class Identity extends DynamicTruthModel {

        
        private final Term[] components;

        public Identity(Compound proxy, Compound base) {
            this.components = new Term[]{base};
        }

        
        @Override
        public Term[] components(Term superterm) {
            return components;
        }

        @Override
        protected boolean add(int subterm,  DynTruth d,  Truth t, float confMin) {
            float c = t.conf();
            if (c >= confMin) {
                d.conf = c;
                d.freq = t.freq();
                return true;
            }
            return false;
        }
    }
}