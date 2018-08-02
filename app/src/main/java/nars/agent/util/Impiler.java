package nars.agent.util;

import com.google.common.collect.Iterables;
import jcog.TODO;
import jcog.data.graph.FromTo;
import jcog.data.graph.Node;
import jcog.data.graph.NodeGraph;
import jcog.data.graph.search.Search;
import jcog.data.list.FasterList;
import jcog.sort.SortedArray;
import jcog.util.HashCachedPair;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.bag.leak.TaskLeak;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.control.channel.CauseChannel;
import nars.table.BeliefTable;
import nars.task.ITask;
import nars.task.NALTask;
import nars.term.Term;
import nars.term.Termed;
import nars.term.compound.util.Conj;
import nars.time.Tense;
import nars.truth.PreciseTruth;
import nars.truth.Truth;
import nars.truth.dynamic.DynTruth;
import nars.truth.func.NALTruth;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.tuple.primitive.BooleanObjectPair;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static nars.Op.*;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.ETERNAL;
import static nars.time.Tense.XTERNAL;

/**
 * implication compiler
 */
public class Impiler {

    /**
     * concept metadata field key storing impiler node instances
     */
    static final String IMPILER_NODE = ImpilerTracker.class.getSimpleName();

    public static class ImpilerSolver extends TaskLeak {

        private final CauseChannel<ITask> in;

        public ImpilerSolver(int capacity, float ratePerDuration, NAR n) {
            super(capacity, ratePerDuration, n);
            in = n.newChannel(this);
        }

        @Override
        protected boolean preFilter(Task next) {
            return (next.isGoal() || next.isQuestionOrQuest()) && !next.term().hasVars() && !next.hasXternal();
        }

        @Override
        protected float leak(Task next) {
            TaskConcept c = (TaskConcept) nar.conceptualizeDynamic(next.term());
            if (c != null) {
                ImplNode i = c.meta(IMPILER_NODE);
                if (i != null) {
                    solve(next, c, i);
                    return 1;
                }
            }
            return 0;
        }

        protected void solve(Task t, TaskConcept c, ImplNode i) {
            //System.out.println(t + "\t" + i);
            ImplSource s = new ImplSource(nar, t, c).scan(i.out);
            if (!s.result.isEmpty()) {
                s.result.forEach(z -> System.err.println(this + "\t:" + z));
                in.input(s.result);
            }

            //new ImplTarget(c).scan(i.out);
        }

        @Override
        public float value() {
            return in.value();
        }
    }

    /**
     * searches forward, discovering shortcuts in the downstream impl graph
     * TODO configurable trigger, dont just extend TaskLeak
     */
    public static class ImpilerDeduction extends TaskLeak {

        public ImpilerDeduction(int capacity, float ratePerDuration, NAR n) {
            super(capacity, ratePerDuration, n);
        }

        @Override
        protected boolean preFilter(Task next) {
            return true;
        }

        @Override
        protected float leak(Task next) {
            Term t = next.term();
            return deduce(t);

        }

        public float deduce(Term root) {
            if (root.op()==IMPL)
                root = root.sub(0); //subj

            Concept c = nar.conceptualizeDynamic(root.unneg());
            if (c == null)
                return 0;

            ImplNode m = c.meta(IMPILER_NODE);
            if (m != null) {
                if (!m.out.isEmpty()) {
                    //TODO handle negations correctly
                    Term R = root;
                    new Search<Term, ImplEdge>() {

                        final static int recursionMin = 1;
                        final static int recursionMax = 3;
                        final float minConf = nar.confMin.floatValue();

                        /** fails if: excessive volume, insufficient conf, time range, or overlap */
                        protected boolean truth(List<BooleanObjectPair<FromTo<Node<Term, ImplEdge>, ImplEdge>>> path) {

                            int n = path.size();
                            //DynTruth d = new DynTruth(n); //<- for tracking evidence?

                            Conj e = new Conj();
                            long when = 0;

                            Truth t = null;
                            for (int s = 0; s < n; s++) {
                                ImplEdge ss = path.get(s).getTwo().id();
                                PreciseTruth tt = $.t(ss.freq, ss.conf);
                                if (t == null) {
                                    t = tt;
                                } else {
                                    t = NALTruth.Deduction.apply(t, tt, nar, minConf);
                                    if (t == null)
                                        return false;
                                }
                            }

                            for (int s = 0; s < n; s++) {
                                ImplEdge ss = path.get(s).getTwo().id();
                                switch (ss.dt) {
                                    case DTERNAL: {
                                        if (s == 0) {
                                            //e.add(DTERNAL, ss.getOne()); //root? if so include in the IMPL but not here
                                        } else {
                                            Term f = e.term();
                                            e = new Conj();
                                            e.add(DTERNAL, f); //add existing accumulated sequence DTERNALly
                                        }
                                        e.add(DTERNAL, ss.getTwo());
                                        when = 0; //reset to zero
                                        break;
                                    }
                                    case XTERNAL:
                                        throw new UnsupportedOperationException();
                                    default: {
                                        if (s == 0) {
                                            //e.add(0, ss.getOne());  //root? if so include in the IMPL but not here
                                        }
                                        when += ss.dt;
                                        e.add(when, ss.getTwo());
                                        break;
                                    }
                                }
                            }

                            Term ee = IMPL.the(R, +1 /*DTERNAL*/ /* ? */, e.term());
                            if (ee.op().taskable) {
                                //HACK TODO FIX
                                Task z = new NALTask(ee, BELIEF, t, nar.time(), ETERNAL, ETERNAL, nar.evidence());
                                z.pri(nar);
                                nar.input(z);
                                System.out.println(z);
                                return true;
                            }

                            return false;
                        }


                        @Override
                        protected boolean next(BooleanObjectPair<FromTo<Node<Term, ImplEdge>, ImplEdge>> move, Node<Term, ImplEdge> next) {
//                            int d = path.size();
//                            if (d >= recursionMin && d <= recursionMax) {
//                                truth(path);
//                            }
                            return true; //continue
                        }

                        @Override
                        protected Node<Term, ImplEdge> next(FromTo<Node<Term, ImplEdge>, ImplEdge> edge, Node<Term, ImplEdge> at) {
                            if (path.size() > recursionMax)
                                return null; //end this probe

                            Term et = edge.id().getTwo();
                            Concept c = nar.conceptualizeDynamic(et);
                            if (c!=null) {
                                ImplNode m = c.meta(IMPILER_NODE);
                                if (m != null) {
                                    if (!m.out.isEmpty()) {
                                        int d = path.size();
                                        if (d >= recursionMin && d <= recursionMax) {
                                            if (log.visit(m)) {
                                                truth(path);
                                                return null;
                                            }
                                        }
                                        return m;
                                    }
                                }
                            }
                            return null;
                        }
                    }.dfs(List.of(m));
                }
            }

            return 0;
        }


        @Override
        public float value() {
            return 0;
        }
    }

    /**
     * builds the implication graph in concept metadata fields
     * TODO configurable trigger, dont just extend TaskLeak
     */
    public static class ImpilerTracker extends TaskLeak {

        final static boolean beliefOrGoal = true;
//        final static long whenStart = ETERNAL, whenEnd = ETERNAL;

        public ImpilerTracker(int capacity, float ratePerDuration, NAR n) {
            super(capacity, ratePerDuration, n);
        }

        @Override
        protected boolean preFilter(Task next) {
            return next.op() == IMPL && next.punc() == BELIEF && !next.hasVars();
        }

        @Override
        protected float leak(Task next) {
            TaskConcept c = (TaskConcept) nar.conceptualizeDynamic(next.term());
            if (c == null)
                return 0;

            Term i = next.term(); //from the task not the concept
            BeliefTable table = c.tableAnswering(beliefOrGoal ? BELIEF : GOAL);
            Task it = table.sample(ETERNAL, ETERNAL, null, nar);
            //Task it = table.answer(whenStart, whenEnd, i, null, nar);
            if (it != null) {
                int idt = it.term().dt();
                if (it != null) {
                    //TODO for now dont consider the implication/etc.. as its own event although NARS does
                    //  just represent it as transitions
                    //TODO dont track evidence
                    Term subj = i.sub(0), pred = i.sub(1);

                    //split the evidence between the positive and negative paths
//                    float f = it.freq();
//                    float conf = it.conf();
//                    //float e = it.evi();
//                    float cp = (conf * f);
//                    float cn = (conf * (1 - f));

                    Concept sc = nar.conceptualizeDynamic(subj.unneg());
                    Concept pc = nar.conceptualizeDynamic(pred/*.unneg()*/);
                    if (sc != null && pc != null) {
                        ImplEdge e = new ImplEdge(subj, pred);
                        e.dt = idt;
                        e.freq = it.freq();
                        e.conf = it.conf();
                        if (sc != null) {
                            ImplNode sn = node(sc);
                            sn.addSource(e);
                        }
                        if (pc != null) {
                            ImplNode pn = node(pc);
                            pn.addTarget(e);
                        }
                    }

                }
            }
            return 1;
        }

        private ImplNode node(Concept sc) {
            return sc.meta(IMPILER_NODE, (s) -> new ImplNode(sc.term()));
        }

        @Override
        public float value() {
            return 0;
        }
    }

    static class ImplNode extends NodeGraph.AbstractNode<Term, ImplEdge> implements FloatFunction<ImplEdge> {

        final static int CAP = 8; //TODO parameterize

        final ImplBag
                out = new ImplBag(CAP),
        //TODO separate outPos and outNeg?
        //outNeg = new ImplBag(CAP),
        in = new ImplBag(CAP);

        public ImplNode(Term id) {
            super(id);
        }

        public final void addSource(ImplEdge e) {
            add(out, e);
        }

        public final void addTarget(ImplEdge e) {
            add(in, e);
        }

        private boolean add(ImplBag which, ImplEdge e) {
            synchronized (which) {
                return which.add(e, this) != -1;
            }
        }

        @Override
        public float floatValueOf(ImplEdge f) {
            return f.conf; //TODO factor in dt?
        }

        @Override
        public String toString() {
            return in + " " + out;
        }

        @Override
        public Iterable<FromTo<Node<Term, ImplEdge>, ImplEdge>> edges(boolean in, boolean out) {
            if (out && !in) return edges(this.out);
            else if (!out && in) return edges(this.in);
            else {
                boolean ie = this.in.isEmpty();
                boolean oe = this.out.isEmpty();
                if (ie && oe) return List.of();
                if (ie) return edges(this.out);
                if (oe) return edges(this.in);
                return Iterables.concat(edges(this.out), edges(this.in));
            }
        }

        private Iterable<FromTo<Node<Term, ImplEdge>, ImplEdge>> edges(ImplBag xx) {
            return Iterables.transform(xx, (ImplEdge x) -> Node.edge(this, x, this /* HACK */));
        }
    }

    static class ImplBag extends SortedArray<ImplEdge> {

        public ImplBag(int capacity) {
            super();
            resize(capacity);
        }

        @Override
        protected boolean grows() {
            return false;
        }

        @Override
        protected ImplEdge[] newArray(int s) {
            return new ImplEdge[s];
        }
    }

    static class ImplEdge extends HashCachedPair<Term, Term> {

        public float freq, conf;
        public int dt = DTERNAL;

        public ImplEdge(Term newOne, Term newTwo) {
            super(newOne, newTwo);
        }

        @Override
        public String toString() {
            return super.toString() + " " + (conf > 0 ? ("%" + freq + ";" + conf + "%") : "") + (dt == DTERNAL ? "" : (".." + dt));
        }

    }


    abstract static class ImplBeam extends FasterList<ImplEdge> {
        public final static int MAX_DEPTH = 3;
        protected final Concept source;

        /**
         * belief cache
         */
        protected final Map<Term, Truth> belief = new HashMap();

        protected final long start, end;
        protected final NAR nar;

        /**
         * stored separately in case it contains temporal information
         */
        protected final Term sourceTerm;


        public final List<Task> result = new FasterList();

        public ImplBeam(NAR n, Task t, Concept c) {
            this.sourceTerm = t.term();
            this.source = c;
            this.start = t.start();
            this.end = t.end();
            this.nar = n;
        }

        @Nullable
        protected Truth belief(Termed x) {
            return belief.computeIfAbsent(x.term(), (xx) -> nar.beliefTruth(xx, start, end));
        }

    }


    //scan forward, matches probable events
    static class ImplSource extends ImplBeam {


        public ImplSource(NAR n, Task t, Concept c) {
            super(n, t, c);
        }

        public ImplSource scan(ImplBag outgoing) {
            //SIMPLE impl for testing
            scan(sourceTerm, source, outgoing, 0);
            return this;
        }

        private void scan(Term atTerm, Concept at, ImplBag outgoing, int depth) {
            Truth aa = belief(atTerm);
            if (aa != null) {
                //TODO try harder on select to find deeper recursive target. this gives up after the first
                ImplEdge x = select(aa, outgoing);
                if (x != null) {
                    Term next = x.getTwo();

                    add(x);

                    if (depth < MAX_DEPTH) {
                        Concept nextC = nar.conceptualizeDynamic(next);
                        if (nextC != null) {

                            @Nullable ImplNode nextCout = nextC.meta(IMPILER_NODE);
                            ImplBag io = nextCout != null ? nextCout.out : null;
                            if (io != null && !io.isEmpty()) {
                                scan(next, nextC, io, depth + 1);
                                removeLast();
                                return;
                            }
                        }
                    }

                    finish();

                    removeLast();
                }
            }

            //TODO question formation?
        }

        @Nullable ImplEdge select(Truth at, ImplBag outgoing) {

            boolean pos = at.isPositive();
            int s = outgoing.size();

            //find matching polarity
            //TODO compute based on expectation not conf
            for (int i = 0; i < s; i++) {
                ImplEdge ii = outgoing.get(i);
                if (pos == (ii.getOne().op() != NEG)) {
                    return ii;
                }
            }
            return null;

        }

        protected boolean finish() {
            if (size() > 1) {
                //System.out.println(this);

                Term start = sourceTerm;
                Term end = getLast().getTwo();

                float confMin = nar.confMin.floatValue();

                long dt = 0;
                float freq = 1f, conf = 1f;
                for (ImplEdge x : this) {
                    int xdt = x.dt;
                    if (xdt != DTERNAL)
                        dt += x.dt;
                    conf *= x.conf;
                    if (conf < confMin)
                        return false;
                    freq *= Math.max(1f - x.freq, x.freq); //these can be consiered virtually positive since we already selected based on current truth
                }

                int idt = Tense.occToDT(dt);

                Term tt = IMPL.the(start, idt, end);

                Task y = Task.tryTask(tt, BELIEF, $.t(freq, conf), (ttt, tr) ->
                        new NALTask(
                                ttt, BELIEF, tr, nar.time(), this.start, this.end,
                                nar.evidence() //TODO correct evidence tracking
                        )
                );
                if (y != null)
                    result.add(y.priSet(nar.priDefault(BELIEF)));

                return true;
            }

            //TODO form answer
            return false;
        }
    }

//    //scan reverse, from pred to impl
//    static class ImplTarget extends ImplBeam {
//
//
//        public ImplTarget(Concept c) {
//            super(c);
//        }
//
//        public void scan(ImplBag incoming) {
//            //SIMPLE impl for testing
//            scan(source, incoming, 0);
//        }
//
//        private void scan(Concept at, ImplBag incoming, int depth) {
//            @Nullable ImplEdge x = incoming.first();
//            add(x);
//        }
//
//    }


//    /** current eternal (steady) state */
//    public static Impiled of(boolean beliefOrGoal, Iterable<? extends Termed> include, NAR n) {
//        return of(beliefOrGoal, ETERNAL, ETERNAL, include, n);
//    }
//
//    public static Impiled of(boolean beliefOrGoal, long start, long end, Iterable<? extends Termed> include, NAR n) {
//        Impiled i = new Impiled(beliefOrGoal, start, end);
//        include.forEach(x -> {
//            Concept c = n.conceptualizeDynamic(x);
//            if (c instanceof TaskConcept)
//                i.add((TaskConcept) c, n);
//        });
//        return i;
//    }
//
//    private static class Element {
//        public final int id;
//
//        public Element(int id) {
//            this.id = id;
//        }
//
//        @Override
//        public int hashCode() {
//            return id;
//        }
//
//        @Override
//        public boolean equals(Object obj) {
//            return this == obj || (/*(obj instanceof Element) &&*/ ((Element)obj).id == id);
//        }
//    }
//
//    static class Condition extends Element {
//
//        public float freq;
//        public float evi;
//        //TODO public float pri;
//
//        Condition(Term term) {
//
//
//            truth(null);
//        }
//
//
//        public void truth(@Nullable Truth truth) {
//            if (truth == null) {
//                this.freq = 0.5f; //Float.NaN;
//                this.evi = 0;
//            } else {
//                this.freq = truth.freq();
//                this.evi = truth.evi();
//            }
//        }
//
//        @Override
//        public String toString() {
//            return term +
//                    (evi > 0 ? ("%" + freq + ";" + w2cSafe(evi) + "%") : "");
//        }
//    }
//
//    static class Transition  {
//
//        public final short from, to;
//        public final boolean polarity;
//        public int dt;
//        public float conf;
//
//
//        public Transition(NodeGraph.MutableNode<Condition,Transition> from, NodeGraph.MutableNode<Condition,Transition> to, boolean polarity) {
//            this((short)from.id.id, (short)to.id.id, polarity);
//        }
//
//        /** 15-bit from, to max */
//        Transition(short from, short to, boolean polarity) {
//            /* hash = from: 31..16, to: 16..1, polarity: 0*/
//            super((from << 16) | (to << 1) | (polarity ? 1 : 0));
//            this.polarity = polarity;
//            this.from = from;
//            this.to = to;
//
//            this.dt = DTERNAL;
//        }
//
//        @Override
//        public String toString() {
//            return (polarity ? "T" : "F") + //from + "," + to + ")" +
//                    "(" + (dt==DTERNAL ? "" : (dt + ","))  + "%" + (Texts.n2(conf)) + "%)" ;
//        }
//    }
//
//    /** result of impiler - compiled program model */
//    public static class Impiled extends MapNodeGraph<Condition,Transition> {
//        /** condition vocabulary canonicalizer - each condition is assigned a local integer value to be referenced in the graph structure */
//        final ByteAnonMap id = new ByteAnonMap(16);
//
//        /** the time for which conditions are calculated.  may be ETERNAL */
//        private final long whenStart, whenEnd;
//        private final boolean beliefOrGoal;
//
//        public Impiled(boolean beliefOrGoal, long whenStart, long whenEnd) {
//            this.beliefOrGoal = beliefOrGoal;
//            this.whenStart = whenStart;
//            this.whenEnd = whenEnd;
//            assert(this.whenStart !=XTERNAL && this.whenEnd!=XTERNAL);
//        }
//
//        /** add a condition to be included in the model as a state node */
//        public void add(TaskConcept c, NAR n) {
//
//
//
//            BeliefTable table = c.tableAnswering(beliefOrGoal ? BELIEF : GOAL);
//            if (c.op()!=IMPL) {
//                MutableNode<Condition, Transition> node = cond(c.term);
//
//                //just estimate the truth, whether it is calculable or not, create the node anyway
//                node.id.truth(table.truth(whenStart, whenEnd, n));
//            } else {
//
//
//                if (c.hasAny(VAR_INDEP) || c.hasAny(VAR_DEP))
//                    return; //no way to guarantee that the variables on either side of the transition are semantically equivalent in the aggregate
//
//                //TODO match an answer separately for pos and negative.  the two paths will vary in confidence but also vary in dt.
//                // ie. for one dt range, it could lead positive while another dt range leads negative.
//
//                interpretImpl(c, n, table);
//            }
//
//        }
//
//        public void interpretImpl(TaskConcept c, NAR n, BeliefTable table) {
//            Task it = table.answer(whenStart, whenEnd, c.term, null, n);
//            if (it!=null) {
//                int idt = it.term().dt();
//                if (it != null) {
//                    //TODO for now dont consider the implication/etc.. as its own event although NARS does
//                    //  just represent it as transitions
//                    //TODO dont track evidence
//                    Term i = c.term;
//                    Term subj = i.sub(0), pred = i.sub(1);
//
//                    //split the evidence between the positive and negative paths
//                    float f = it.freq();
//                    float conf = it.conf();
//                    //float e = it.evi();
//                    float cp = (conf * f);
//                    float cn = (conf * (1 - f));
//
//                    MutableNode<Condition, Transition> s = cond(subj);
//
//                    if (cp >= Param.TRUTH_MIN_EVI) {
//                        MutableNode<Condition, Transition> pp = cond(pred);
//                        Transition tp = new Transition(s, pp, true);
//                        tp.conf = cp;
//                        tp.dt = idt;
//                        addEdge(s, tp, pp);
//                    }
//                    if (cn >= Param.TRUTH_MIN_EVI) {
//                        MutableNode<Condition, Transition> pn = cond(pred.neg());
//                        Transition tn = new Transition(s, pn, true);
//                        tn.conf = cn;
//                        tn.dt = idt;
//                        addEdge(s, tn, pn);
//                    }
//
//                }
//            }
//        }
//
//        public MutableNode<Condition, Transition> cond(Term t) {
//            short cid = id.intern(t);
//
//            //TODO use Map.compute-like method
//            MutableNode<Condition, Transition> n = (MutableNode<Condition, Transition>) node(cid);
//
//            if (n == null) {
//                Condition cc = new Condition(cid, t);
//                n = addNode(cc);
//            }
//            return n;
//        }
//    }

    /**
     * a sampling process; mutable
     * input conditions applied to an Impiled network and
     * sampled forward and/or backward through the network,
     * calculating condition: evidence, aggregate truth frequency, confusion, etc.
     */
    public static class Impiling {
        final Map<Term, Improjection> truth = new HashMap();
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

    /**
     * truth state projected (mutable)
     */
    public static class Improjection {
        final DynTruth truth;

        Improjection() {
            truth = new DynTruth(1);
        }

        //TODO other statistics
    }

}
