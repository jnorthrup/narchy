package nars.agent.util;

import jcog.TODO;
import jcog.Texts;
import jcog.data.graph.MapNodeGraph;
import jcog.data.graph.NodeGraph;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.concept.dynamic.DynTruth;
import nars.table.BeliefTable;
import nars.term.Term;
import nars.term.Termed;
import nars.term.compound.util.ByteAnonMap;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import static nars.Op.*;
import static nars.time.Tense.*;
import static nars.truth.TruthFunctions.w2cSafe;

/** implication compiler */
public class Impiler {

    /** current eternal (steady) state */
    public static Impiled of(boolean beliefOrGoal, Iterable<? extends Termed> include, NAR n) {
        return of(beliefOrGoal, ETERNAL, ETERNAL, include, n);
    }

    public static Impiled of(boolean beliefOrGoal, long start, long end, Iterable<? extends Termed> include, NAR n) {
        Impiled i = new Impiled(beliefOrGoal, start, end);
        include.forEach(x -> {
            Concept c = n.conceptualizeDynamic(x);
            if (c instanceof TaskConcept)
                i.add((TaskConcept) c, n);
        });
        return i;
    }

    private static class Element {
        public final int id;

        public Element(int id) {
            this.id = id;
        }

        @Override
        public int hashCode() {
            return id;
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj || ((Transition)obj).id == id;
        }
    }

    static class Condition extends Element {


        private final Term term;
        public float freq;
        public float evi;
        //TODO public float pri;

        Condition(int id, Term term) {
            super(id);

            this.term = term;

            truth(null);
        }


        public void truth(@Nullable Truth truth) {
            if (truth == null) {
                this.freq = 0.5f; //Float.NaN;
                this.evi = 0;
            } else {
                this.freq = truth.freq();
                this.evi = truth.evi();
            }
        }

        @Override
        public String toString() {
            return term +
                    (evi > 0 ? ("%" + freq + ";" + w2cSafe(evi) + "%") : "");
        }
    }

    static class Transition extends Element {

        public final short from, to;
        public final boolean polarity;
        public int dt;
        public float conf;


        public Transition(NodeGraph.MutableNode<Condition,Transition> from, NodeGraph.MutableNode<Condition,Transition> to, boolean polarity) {
            this((short)from.id.id, (short)to.id.id, polarity);
        }

        /** 15-bit from, to max */
        Transition(short from, short to, boolean polarity) {
            /* hash = from: 31..16, to: 16..1, polarity: 0*/
            super((from << 16) | (to << 1) | (polarity ? 1 : 0));
            this.polarity = polarity;
            this.from = from;
            this.to = to;

            this.dt = DTERNAL;
        }

        @Override
        public String toString() {
            return (polarity ? "T" : "F") + //from + "," + to + ")" +
                    "(" + (dt==DTERNAL ? "" : (dt + ","))  + "%" + (Texts.n2(conf)) + "%)" ;
        }
    }

    /** result of impiler - compiled program model */
    public static class Impiled extends MapNodeGraph<Condition,Transition> {
        /** condition vocabulary canonicalizer - each condition is assigned a local integer value to be referenced in the graph structure */
        final ByteAnonMap id = new ByteAnonMap(16);

        /** the time for which conditions are calculated.  may be ETERNAL */
        private final long whenStart, whenEnd;
        private final boolean beliefOrGoal;

        public Impiled(boolean beliefOrGoal, long whenStart, long whenEnd) {
            this.beliefOrGoal = beliefOrGoal;
            this.whenStart = whenStart;
            this.whenEnd = whenEnd;
            assert(this.whenStart !=XTERNAL && this.whenEnd!=XTERNAL);
        }

        /** add a condition to be included in the model as a state node */
        public void add(TaskConcept c, NAR n) {



            BeliefTable table = c.tableAnswering(beliefOrGoal ? BELIEF : GOAL);
            if (c.op()!=IMPL) {
                MutableNode<Condition, Transition> node = cond(c.term);

                //just estimate the truth, whether it is calculable or not, create the node anyway
                node.id.truth(table.truth(whenStart, whenEnd, n));
            } else {

                //TODO match an answer separately for pos and negative.  the two paths will vary in confidence but also vary in dt.
                // ie. for one dt range, it could lead positive while another dt range leads negative.

                Task it = table.answer(whenStart, whenEnd, c.term, null, n);
                int idt = it.term().dt();
                if (it!=null) {
                    //TODO for now dont consider the implication/etc.. as its own event although NARS does
                    //  just represent it as transitions
                    //TODO dont track evidence
                    Term i = c.term;
                    Term subj = i.sub(0), pred = i.sub(1);
                    MutableNode<Condition, Transition> s = cond(subj);

                    //split the evidence between the positive and negative paths
                    float f = it.freq();
                    float conf = it.conf();
                    //float e = it.evi();
                    float cp = (conf * f);
                    float cn = (conf * (1-f));
                    if (cp >= Param.TRUTH_MIN_EVI) {
                        MutableNode<Condition, Transition> pp = cond(pred);
                        Transition tp = new Transition(s, pp, true);
                        tp.conf = cp;
                        tp.dt = idt;
                        addEdge(s, tp, pp);
                    }
                    if (cn >= Param.TRUTH_MIN_EVI) {
                        MutableNode<Condition, Transition> pn = cond(pred.neg());
                        Transition tn = new Transition(s, pn, true);
                        tn.conf = cn;
                        tn.dt = idt;
                        addEdge(s, tn, pn);
                    }

                }

            }

        }

        public MutableNode<Condition, Transition> cond(Term t) {
            short cid = id.intern(t);

            //TODO use Map.compute-like method
            MutableNode<Condition, Transition> n = (MutableNode<Condition, Transition>) node(cid);

            if (n == null) {
                Condition cc = new Condition(cid, t);
                n = addNode(cc);
            }
            return n;
        }
    }

    /**
     * a sampling process; mutable
     * input conditions applied to an Impiled network and
     * sampled forward and/or backward through the network,
     * calculating condition: evidence, aggregate truth frequency, confusion, etc. */
    public static class Impiling {
        final Map<Term,Improjection> truth = new HashMap();
        long now;

        public Impiling(long now, Iterable<Term> track) {
            this.now = now;
            track.forEach(t -> {
                truth.put(t, new Improjection());
            });
            next(0); //cause initial update
        }

        public void next(long dt) {
            this.now += dt;
            throw new TODO();
        }
    }

    /** truth state projected (mutable) */
    public static class Improjection  {
        final DynTruth truth;
        Improjection() {
            truth = new DynTruth(1);
        }

        //TODO other statistics
    }

}
