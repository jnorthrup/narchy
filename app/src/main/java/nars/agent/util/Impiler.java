package nars.agent.util;

import com.google.common.collect.Iterables;
import jcog.data.graph.AdjGraph;
import jcog.data.graph.Node;
import jcog.data.graph.NodeGraph;
import jcog.data.graph.path.FromTo;
import jcog.data.graph.search.Search;
import jcog.data.set.MetalLongSet;
import jcog.pri.PLink;
import jcog.pri.bag.Bag;
import jcog.pri.bag.impl.PriReferenceArrayBag;
import jcog.pri.op.PriMerge;
import nars.NAL;
import nars.NAR;
import nars.Task;
import nars.attention.What;
import nars.concept.Concept;
import nars.control.channel.CauseChannel;
import nars.op.TaskLeak;
import nars.task.NALTask;
import nars.term.Term;
import nars.term.util.conj.ConjBuilder;
import nars.term.util.conj.ConjTree;
import nars.truth.PreciseTruth;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.truth.func.NALTruth;
import org.eclipse.collections.api.tuple.primitive.BooleanObjectPair;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BooleanSupplier;

import static nars.NAL.STAMP_CAPACITY;
import static nars.Op.BELIEF;
import static nars.Op.IMPL;
import static nars.term.atom.Bool.Null;
import static nars.time.Tense.*;

/**
 * Implication Graph / Compiler
 * a set of plugins that empower and accelerate causal reasoning
 * <p>
 * see: https://github.com/opennars/opennars-archived/blob/1.6.1_Plugins/nars_java/nars/plugin/app/plan
 * see: https://github.com/opennars/opennars-archived/blob/1.6.1_Plugins/nars_java/nars/plugin/app/plan/GraphExecutive.java
 */
public class Impiler {

    /**
     * concept metadata field key storing impiler node instances
     */
    static final String IMPILER_NODE = ImplGrapher.class.getSimpleName();

    public static void init(NAR n) {

//        Impiler.ImpilerSolver s = new Impiler.ImpilerSolver(32, 2, n);
        Impiler.ImpilerDeduction d = new Impiler.ImpilerDeduction( n);

        ImplGrapher t = new ImplGrapher(n) {
            @Override
            public float value() {
                return d.value();
            }
        };
    }

    static ImplNode node(Concept sc, boolean createIfMissing) {
        Term sct = sc.term();
        return createIfMissing ? sc.meta(IMPILER_NODE, s -> new ImplNode(sct)) : sc.meta(IMPILER_NODE);
    }

    private static boolean implFilter(Term next) {
        return next.op() == IMPL && !next.hasVars();
    }

    static public float implValue(Task t) {
        return t.priElseZero();
        //return (float) t.evi();
        //return (1f + (float) t.evi()) * (1 + t.priElseZero());
    }

    /**
     * searches forward, discovering shortcuts in the downstream impl graph
     * TODO configurable trigger, dont just extend TaskLeak
     */
    public static class ImpilerDeduction extends TaskLeak {

        private transient float dur;
        private final CauseChannel<Task> in;

        public ImpilerDeduction( NAR n) {
            super(n);
            in = n.newChannel(this);
        }

        @Override
        public void next(What w, BooleanSupplier kontinue) {
            super.next(w, kontinue);
            dur = w.dur();
        }

        @Override
        protected boolean filter(Term term) {
            if (term.hasVars())
                return false;

            return true;
        }

        @Override
        protected float leak(Task next, What what) {
            return deduce(next, what);
        }

        public float deduce(Task task, What what) {
            Term root = task.term();
            if (root.op() == IMPL)
                root = root.sub(0); //subj

            Concept c = nar.concept(root.unneg());
            if (c == null)
                return 0;

            ImplNode m = node(c, false);
            if (m != null) {
                //TODO handle negations correctly
                List<Node> targets = List.of(m);
                new ImpilerDeductionSearch(root, what,
                    task.isEternal() ? what.time() : task.start())
                        //.dfs(targets);
                        .bfs(targets);
                return 1;
            }


            return 0;
        }

        /**
         * creates graph snapshot
         */
        public AdjGraph<Term, Task> graph() {
            AdjGraph g = new AdjGraph(true);

            nar.concepts().forEach(c -> {
                ImplNode m = c.meta(IMPILER_NODE);
                if (m != null) {
                    g.addNode(c.term());
                    m.edges(false, true).forEach(e -> {
                        g.setEdge(e.from().id(), e.to().id(), e); //TODO check if multiedge
                    });
                }
            });

            return g;
        }


        @Override
        public float value() {
            return in.value();
        }

        private class ImpilerDeductionSearch extends Search<Term, Task> {

            final static int recursionMin = 2;
            final static int recursionMax = 3;
            static final int volPadding = 2;
            private static final int STAMP_LIMIT = Integer.MAX_VALUE;
            final float confMin;
            private final Term source;
            private final What what;
            private final long start;
            MetalLongSet stamp = null;
            ConjBuilder cc = null;

            public ImpilerDeductionSearch(Term source, What what, final long when) {
                this.source = source.unneg();
                this.what = what;
                confMin = nar.confMin.floatValue();
                this.start = when;
            }

            /**
             * out only
             */
            @Override
            protected Iterable<FromTo<Node<Term, Task>, Task>> find(Node<Term, Task> n, List<BooleanObjectPair<FromTo<Node<Term, Task>, Task>>> path) {

                if (path.size() >= recursionMin && !deduce(path))
                    return empty; //boundary

                if (path.size() + 1 > recursionMax)
                    return empty;

                return ((ImplNode) n).edges(false, true);
            }

            @Override
            protected boolean go(List<BooleanObjectPair<FromTo<Node<Term, Task>, Task>>> path, Node<Term, Task> next) {
                return true;
            }

            /**
             * returns whether to continue further
             */
            protected boolean deduce(List<BooleanObjectPair<FromTo<Node<Term, Task>, Task>>> path) {

                int n = path.size();

                Task[] pathTasks = new Task[n];

                final int stampLimit = STAMP_CAPACITY;
                Term nextEvent = source;
                int j = 0;
                int volEstimate = 1 + n/2; //initial cost estimate
                for (BooleanObjectPair<FromTo<Node<Term, Task>, Task>> pe : path) {
                    Task e = pe.getTwo().id();
                    Term ee = e.term();
                    volEstimate += ee.volume();
                    if (volEstimate > volMax - volPadding)
                        return false; //path exceeds volume
                    if (!ee.sub(0).unneg().equals(nextEvent))
                        return false; //path is not continuous
                    nextEvent = ee.sub(1);
                    pathTasks[j++] = e;
                }

                if (stamp != null)
                    stamp.clear();
                Truth tAccum = null;



                //final long now = nar.time();

                long offset =
                        //now
                        start;

                for (Task e : pathTasks) {
                    Truth ttt = e.truth();

                    Term ee = e.term();
                    Truth tt = e.isEternal() ? ttt : project(ttt, e.start(), offset);
                    if (tt == null)
                        return false; //too weak

                    offset += ee.sub(0).eventRange();
                    int edt = ee.dt(); if (edt == DTERNAL) edt = 0;
                    offset += edt;



                    if (tAccum == null) {
                        tAccum = tt;
                    } else {
                        tAccum = NALTruth.Deduction.apply(tAccum, tt, confMin, null);
                        if (tAccum == null)
                            return false;
                    }
                    long[] ss = e.stamp();
                    if (stamp == null) {
                        stamp = new MetalLongSet(path.size() * STAMP_CAPACITY);

                        stamp.addAll(ss);
                    } else {
                        if (STAMP_LIMIT < Integer.MAX_VALUE && stamp.size() + ss.length > STAMP_LIMIT)
                            return false; //stamp exceeds limit

                        for (long es : ss) {
                            if (!stamp.add(es))
                                return false; //overlap TODO just cancel at this point
                        }
                    }
                }

                if (cc == null)
                    cc = new ConjTree();
                else
                    cc.clear();


                Term pred = Null;
                int zDT = 0;
                long range = Long.MAX_VALUE;
                offset = 0;
                for (int i = 0, pathTasksLength = pathTasks.length; i < pathTasksLength; i++) {
                    Task e = pathTasks[i];

                    Term ee = e.term();


                    long es = e.start();
                    if (es != ETERNAL)
                        range = Math.min(range, e.end() - es);

                    pred = ee.sub(1).negIf(e.isNegative());
                    int dt = ee.dt(); if (dt == DTERNAL) dt = 0; //HACK
                    if (dt == XTERNAL)
                        throw new UnsupportedOperationException();


//                        switch (dt) {
//                            case DTERNAL: {
//                                if (i == 0) {
//                                    cc.add(ETERNAL, source);
//                                } else {
//                                    Term f = cc.term();
//                                    if (f == False || f == Null)
//                                        return false;
//                                    cc.clear();
//                                    cc.add(ETERNAL, f); //add existing accumulated sequence DTERNALly
//                                }
//                                if (i != n - 1) {
//                                    if (!cc.add(ETERNAL, Z))
//                                        return false;
//                                }
//                                lastDT = 0;
//                                break;
//                            }
//                            case XTERNAL:
//                            default: {

                    Term zubj = ee.sub(0);

                    zDT = dt;
                    if (i == 0) {
                        cc.add(0, zubj);
                    }
                    offset += zubj.eventRange();
                    offset += dt;
                    if (i != n - 1) {
                        if (!cc.add(offset, pred))
                            return false;
                    }


//                                break;
//                            }
//                        }
                }

                Term ee = IMPL.the(cc.term(), zDT, pred);
                if (ee.volume() > volMax)
                    return false;

                if (range == Long.MAX_VALUE)
                    range = 0; //all eternal

                long finalStart = start;
                long finalEnd = start + range;
                Task z = Task.tryTask(ee, BELIEF, tAccum, (ttt, tr) ->
                        NALTask.the(ttt, BELIEF, tr, nar.time(), finalStart, finalEnd, Stamp.sample(STAMP_CAPACITY, stamp, ThreadLocalRandom.current())));
                if (z != null) {

                    Task.fund(z, pathTasks, true);

                    //System.out.println(z);
                    in.accept(z, what);
                }


                return true; //continue
            }


        }

        private Truth project(Truth t, long start, long offset) {
            long delta = Math.abs(start - offset);
            if (delta == 0)
                return t;
            else
                return PreciseTruth.byEvi(t.freq(), NAL.evi(t.evi(), delta, dur));
        }
    }

    /**
     * builds the implication graph in concept metadata fields
     * TODO configurable trigger, dont just extend TaskLeak
     */
    public static class ImplGrapher extends TaskLeak {


//        final static long whenStart = ETERNAL, whenEnd = ETERNAL;

        public ImplGrapher(NAR n) {
            super( n, BELIEF);
        }

//        @Override
//        protected boolean filter(Task next) {
//            return next.punc() == BELIEF;
//        }

        @Override
        protected boolean filter(Term term) {
            return implFilter(term);
        }

        @Override
        protected float leak(Task t, What what) {

            Term i = t.term(); //from the task not the concept
            //BeliefTable table = c.tableAnswering(beliefOrGoal ? BELIEF : GOAL);
            //table.sample(ETERNAL, ETERNAL, null, nar);
            //Task it = table.answer(whenStart, whenEnd, i, null, nar);
            /*if (it != null) */

            {
                //TODO for now dont consider the implication/etc.. as its own event although NARS does
                //  just represent it as transitions
                //TODO dont track evidence
                Term subj = i.sub(0).concept(), pred = i.sub(1).concept();
                if (!subj.equals(pred)) {


                    //split the evidence between the positive and negative paths
//                    float f = it.freq();
//                    float conf = it.conf();
//                    //float e = it.evi();
//                    float cp = (conf * f);
//                    float cn = (conf * (1 - f));

                    Concept sc =
                            //nar.conceptualize(subj.unneg());
                            nar.conceptualizeDynamic(subj.unneg());
                    if (sc != null) {
                        Concept pc =
                                //nar.conceptualize(pred);
                                nar.conceptualizeDynamic(pred/*.unneg()*/);
                        if (pc != null) {
                            edge(t, sc, pc);
                        }
                    }
                }

            }

            return 1;
        }

        protected void edge(Task t, Concept sc, Concept pc) {
            node(sc, true).add(true, t, pc);
            node(pc, true).add(false, t, sc);
        }


        @Override
        public float value() {
            return 0;
        }
    }

    static class ImplNode extends NodeGraph.AbstractNode<Term, Task> {

        final static int CAP = 8; //TODO parameterize
        final Bag<Task, ImplPLink> tasks = new PriReferenceArrayBag<>(PriMerge.max, CAP);

        public ImplNode(Term id) {
            super(id);
        }

        public final void add(boolean direction, Task e, Concept target) {
            tasks.commit();
            tasks.put(new ImplPLink(e, implValue(e), direction, target));
        }


        @Override
        public String toString() {
            return id + ":" + tasks.toString();
        }

        @Override
        public Iterable<FromTo<Node<Term, Task>, Task>> edges(boolean in, boolean out) {
            assert (in || out);
            return tasks.isEmpty() ? List.of() : Iterables.filter(Iterables.transform(tasks, (t) -> {
                boolean td = t.direction;
                if ((in && out) || (td == out)) {
                    Task tt = t.get();
                    Term other = tt.term().sub(td ? 1 : 0);

                    if (t.target.isDeleted())
                        t.delete(); //targetconcept deleted

                    Node otherNode = node(t.target, false);
                    if (otherNode != null) {
                        return td ? Node.edge(this, tt, otherNode) : Node.edge(otherNode, tt, this);
                    } else {
                        t.delete();
                    }
                }
                return null;
            }), x -> x != null);
        }

        private static final class ImplPLink extends PLink<Task> {

            /**
             * true = out, false = in
             */
            private final boolean direction;

            /**
             * TODO weakref?
             */
            private final Concept target;

            public ImplPLink(Task task, float p, boolean direction, Concept target) {
                super(task, p);
                this.direction = direction;
                this.target = target;
            }
        }
//
//        private Iterable<FromTo<Node<Term, ImplEdge>, ImplEdge>> edges(ImplBag xx) {
//            return Iterables.transform(xx, (ImplEdge x) -> Node.edge(this, x, this /* HACK */));
//        }
    }
}
//    static class ImplBag extends SortedArray<ImplEdge> {
//
//        public ImplBag(int capacity) {
//            super();
//            resize(capacity);
//        }
//
//        @Override
//        protected boolean grows() {
//            return false;
//        }
//
//
//    }

//    static class ImplEdge extends HashCachedPair<Term, Term> {
//
//        public float freq;
//        public double evi;
//        public int dt = DTERNAL;
//        public long[] stamp = ArrayUtil.EMPTY_LONG_ARRAY;
//        public float pri;
//
//        public ImplEdge(Term newOne, Term newTwo) {
//            super(newOne, newTwo);
//        }
//
//        @Override
//        public String toString() {
//            float conf = conf();
//            return super.toString() + ' ' + (conf > 0 ? ("%" + freq + ';' + conf + '%') : "") + (dt == DTERNAL ? "" : (".." + dt));
//        }
//
//        public float conf() {
//            return w2cSafe(evi);
//        }
//    }


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
//            addAt(x);
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
//                i.addAt((TaskConcept) c, n);
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
//        Condition(Term target) {
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
//            return target +
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
//        public void addAt(TaskConcept c, NAR n) {
//
//
//
//            BeliefTable table = c.tableAnswering(beliefOrGoal ? BELIEF : GOAL);
//            if (c.op()!=IMPL) {
//                MutableNode<Condition, Transition> node = cond(c.target);
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
//            Task it = table.answer(whenStart, whenEnd, c.target, null, n);
//            if (it!=null) {
//                int idt = it.target().dt();
//                if (it != null) {
//                    //TODO for now dont consider the implication/etc.. as its own event although NARS does
//                    //  just represent it as transitions
//                    //TODO dont track evidence
//                    Term i = c.target;
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

//    /**
//     * a sampling process; mutable
//     * input conditions applied to an Impiled network and
//     * sampled forward and/or backward through the network,
//     * calculating condition: evidence, aggregate truth frequency, confusion, etc.
//     */
//    public static class Impiling {
//        final Map<Term, Improjection> truth = new HashMap();
//        long now;
//
//        public Impiling(long now, Iterable<Term> track) {
//            this.now = now;
//            track.forEach(t -> {
//                truth.put(t, new Improjection());
//            });
//            next(0); //cause initial update
//        }
//
//        public void next(long dt) {
//            this.now += dt;
//            throw new TODO();
//        }
//    }
//
//    /**
//     * truth state projected (mutable)
//     */
//    public static class Improjection {
//        final TaskList truth;
//
//        Improjection() {
//            truth = new TaskList(1);
//        }
//
//        //TODO other statistics
//    }
//
//}
//    /** TODO make this use the graph Search API */
//    public static class ImpilerSolver extends TaskLeak {
//
//        private final CauseChannel<Task> in;
//
//        public ImpilerSolver(int capacity, float ratePerDuration, NAR n) {
//            super(capacity, n);
//            in = n.newChannel(this);
//        }
//
//
//        @Override
//        protected boolean filter(Task next) {
//            return (next.isGoal() || next.isQuestionOrQuest()) && !next.term().hasVars();
//        }
//
//        @Override
//        protected float leak(Task next, What what) {
//            TaskConcept c = (TaskConcept) nar.conceptualizeDynamic(next.term());
//            if (c != null) {
//                ImplNode i = c.meta(IMPILER_NODE);
//                if (i != null) {
//                    solve(next, c, i, what);
//                    return 1;
//                }
//            }
//            return 0;
//        }
//
//        protected void solve(Task t, TaskConcept c, ImplNode i, What w) {
//            //System.out.println(t + "\t" + i);
//            ImplSource s = new ImplSource(nar, t, c).scan(i.out);
//            if (!s.result.isEmpty()) {
//                s.result.forEach(z -> System.err.println(this + "\t:" + z));
//                this.in.acceptAll(s.result, w);
//            }
//
//            //new ImplTarget(c).scan(i.out);
//        }
//
//        @Override
//        public float value() {
//            return in.value();
//        }
//
//        abstract static class ImplBeam extends FasterList<ImplEdge> {
//            public final static int MAX_DEPTH = 3;
//            public final List<Task> result = new FasterList();
//            protected final Concept source;
//            /**
//             * belief cache
//             */
//            protected final Map<Term, Truth> belief = new HashMap();
//            protected final long start, end;
//            protected final NAR nar;
//            /**
//             * stored separately in case it contains temporal information
//             */
//            protected final Term sourceTerm;
//
//            public ImplBeam(NAR n, Task t, Concept c) {
//                this.sourceTerm = t.term();
//                this.source = c;
//                this.start = t.start();
//                this.end = t.end();
//                this.nar = n;
//            }
//
//            @Nullable
//            protected Truth belief(Termed x) {
//                return belief.computeIfAbsent(x.term(), (xx) -> nar.beliefTruth(xx, start, end));
//            }
//
//        }
//
//        //scan forward, matches probable events
//        static class ImplSource extends ImplBeam {
//
//
//            public ImplSource(NAR n, Task t, Concept c) {
//                super(n, t, c);
//            }
//
//            @Deprecated public ImplSource scan(ImplBag outgoing) {
//                //SIMPLE impl for testing
//                scan(sourceTerm, source, outgoing, 0);
//                return this;
//            }
//
//            private void scan(Term atTerm, Concept at, ImplBag outgoing, int depth) {
//                Truth aa = belief(atTerm);
//                if (aa != null) {
//                    //TODO try harder on select to find deeper recursive target. this gives up after the first
//                    ImplEdge x = select(aa, outgoing);
//                    if (x != null) {
//                        Term next = x.getTwo();
//
//                        add(x);
//
//                        if (depth < MAX_DEPTH) {
//                            Concept nextC = nar.conceptualizeDynamic(next);
//                            if (nextC != null) {
//
//                                @Nullable ImplNode nextCout = nextC.meta(IMPILER_NODE);
//                                ImplBag io = nextCout != null ? nextCout.out : null;
//                                if (io != null && !io.isEmpty()) {
//                                    scan(next, nextC, io, depth + 1);
//                                    removeLast();
//                                    return;
//                                }
//                            }
//                        }
//
//                        finish();
//
//                        removeLast();
//                    }
//                }
//
//                //TODO question formation?
//            }
//
//            @Nullable ImplEdge select(Truth at, ImplBag outgoing) {
//
//                boolean pos = at.isPositive();
//                int s = outgoing.size();
//
//                //find matching polarity
//                //TODO compute based on expectation not conf
//                for (int i = 0; i < s; i++) {
//                    ImplEdge ii = outgoing.get(i);
//                    if (pos == (ii.getOne().op() != NEG)) {
//                        return ii;
//                    }
//                }
//                return null;
//
//            }
//
//            protected boolean finish() {
//                if (size() > 1) {
//                    //System.out.println(this);
//
//                    Term start = sourceTerm;
//                    Term end = getLast().getTwo();
//
//                    float confMin = nar.confMin.floatValue();
//
//                    LongHashSet stamp = null;
//                    long dt = 0;
//                    float freq = 1f, conf = 1f;
//                    final int stampLimit = NAL.STAMP_CAPACITY;
//                    for (ImplEdge e : this) {
//                        int xdt = e.dt;
//                        if (xdt != DTERNAL)
//                            dt += e.dt;
//                        conf *= e.conf();
//                        if (conf < confMin)
//                            return false;
//                        freq *= Math.max(1f - e.freq, e.freq); //these can be consiered virtually positive since we already selected based on current truth
//                        if (stamp == null)
//                            stamp = new LongHashSet(e.stamp);
//                        else {
//                            if (stamp.size() + e.stamp.length > stampLimit)
//                                return false; //stamp exceeds limit
//
//                            for (long s : e.stamp) {
//                                if (!stamp.add(s))
//                                    return false; //overlap
//                            }
//                        }
//
//                    }
//
//                    int idt = Tense.occToDT(dt);
//
//                    Term tt = IMPL.the(start, idt, end);
//
//                    LongHashSet ss = stamp;
//                    Task y = Task.tryTask(tt, BELIEF, $.t(freq, conf), (ttt, tr) -> NALTask.the(ttt, BELIEF, tr, nar.time(), this.start, this.end, ss.toArray())
//                    );
//
//                    if (y != null)
//                        result.add(y.priSet(nar.priDefault(BELIEF)));
//
//                    return true;
//                }
//
//                //TODO form answer
//                return false;
//            }
//        }
//
//    }
