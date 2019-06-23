package nars.derive.op;

import jcog.data.set.ArrayHashSet;
import jcog.math.LongInterval;
import nars.NAL;
import nars.Op;
import nars.Task;
import nars.derive.model.Derivation;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.util.Image;
import nars.time.Tense;
import nars.time.TimeGraph;
import nars.truth.Truth;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.ObjectIntPair;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectIntHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.function.Function;
import java.util.function.Predicate;

import static nars.Op.*;
import static nars.time.Tense.*;
import static org.eclipse.collections.impl.tuple.Tuples.pair;


/**
 * solves a derivation's occurrence time.
 * <p>
 * unknowns to solve otherwise the result is impossible:
 * - derived task start time
 * - derived task end time
 * - dt intervals for any XTERNAL appearing in the input target
 * knowns:
 * - for each task and optional belief in the derived premise:
 * - start/end time of the task
 * - start/end time of any contained events
 * - possible relations between events referred to in the conclusion that
 * appear in the premise.  this may be partial due to variable introduction
 * and other reductions. an attempt can be made to back-solve the result.
 * if that fails, a heuristic could decide the match. in the worst case,
 * the derivation will not be temporalizable and this method returns null.
 */
public class Occurrify extends TimeGraph {

    public static final OccurrenceSolver mergeDefault =
            OccurrenceSolver.Default;
    public static final ImmutableMap<Term, OccurrenceSolver> merge;

    static {
        MutableMap<Term, OccurrenceSolver> tm = new UnifiedMap<>(8);
        for (OccurrenceSolver m : OccurrenceSolver.values()) {
            tm.put(Atomic.the(m.name()), m);
        }
        merge = tm.toImmutable();
    }

    private final Derivation d;
    int absolutePoints = 5;
    int novelPoints = 1;
    int noXternalPoints = 3;
    int sameAsPatternRootPoints = 1;
    private transient boolean decomposeEvents;
    private transient int patternVolumeMin, patternVolumeMax;


//    /**
//     * re-used
//     */
//    private final transient UnifiedSet<Term>
//            nextNeg = new UnifiedSet<>(8, 0.99f),
//            nextPos = new UnifiedSet(8, 0.99f);
    private int ttl = 0;
    private Term pattern;


    public Occurrify(Derivation d) {
        this.d = d;
    }






    /**
     * whether a term is 'temporal' and its derivations need analyzed by the temporal solver:
     * if there is any temporal terms with non-DTERNAL dt()
     */
    public static boolean temporal(Term x) {
        if (x instanceof Compound && x.hasAny(Op.Temporal)) {
            if (x.op().temporal) {
                int dt = x.dt();
                return (dt != DTERNAL && dt != XTERNAL);
            }
            return x.subterms().OR(Occurrify::temporal);
        }
        return false;
    }


    private static long[] rangeCombine(Derivation d, OccMerge mode) {
        long beliefStart = d.beliefStart;
        if (d.concSingle || (d._belief == null || d.beliefStart == ETERNAL))
            return new long[] { d.taskStart, d.taskEnd };
        else if (d.taskStart == ETERNAL) {
            assert(d.beliefStart!=TIMELESS);
            return new long[]{d.beliefStart, d.beliefEnd};
        } else {
            long taskStart = d.taskStart;


            long start;
            switch (mode) {
                case Earliest:
                    start = Math.min(taskStart, beliefStart);
                    break;
                case Task:
                    start = taskStart;
                    break;
                case Belief:
                    start = beliefStart;
                    break;
                case Union:
                    return LongInterval.union(taskStart, d.taskEnd, beliefStart, d.beliefEnd).toArray();
                case Intersect: {
                    long[] i = d.taskBelief_TimeIntersection;
//                    if (i[0] == TIMELESS)
//                        throw new WTF("intersection filter failure");

//                    if (d.concPunc == BELIEF || d.concPunc == GOAL) {
//                        long iRange = LongInterval.intersectLength(taskStart, d.taskEnd, beliefStart, d.beliefEnd);
//                        long uRange = i[1] - i[0];
//                        double pct = (1 + iRange) / (1.0 + uRange);
//
//                    }
                    return i;
                }
                case UnionDilute: {
                    long[] u = LongInterval.union(taskStart, d.taskEnd, beliefStart, d.beliefEnd).toArray();
                    if (d.concPunc == BELIEF || d.concPunc == GOAL) {
                        long iRange = LongInterval.intersectLength(taskStart, d.taskEnd, beliefStart, d.beliefEnd);
                        long uRange = u[1] - u[0];
                        double pct = (1 + iRange) / (1.0 + uRange);
                        if (!d.concTruthEviMul((float) pct, false))
                            return null;
                    }
                    return u;
                }
                default:
                    throw new UnsupportedOperationException();
            }

            //minimum range
            return new long[]{
                    start,
                    start + Math.min(d.taskEnd - taskStart, d.beliefEnd - beliefStart)
            };
        }
    }

    private static long[] occ(Task t) {
        return new long[]{t.start(), t.end()};
    }

    @Nullable
    private Event selectSolution(boolean occ, ArrayHashSet<Event> solutions) {
        int ss = solutions.size();
        if (ss == 0)
            return null;
        if (ss == 1)
            return solutions.get(0);
        if (ss == 2) {
            //quick test
            if ((solutions.get(0) instanceof Absolute) && (solutions.get(1) instanceof Relative))
                return solutions.get(0);
        }

        ObjectIntHashMap<Event> e = new ObjectIntHashMap(ss);
        int maxPoints = 0;
        for (int i = 0; i < ss; i++) {
            Event s = solutions.get(i);
            Term st = s.id;
            int points = 0;
            if (occ && s instanceof Absolute) {
                points += absolutePoints;
                if (node(s)==null)
                    points += novelPoints;
            }
            if (st.equalsRoot(pattern)) {
                points += sameAsPatternRootPoints;
            }
            if (!st.hasXternal()) {
                points += noXternalPoints;
            }

            if (points >= maxPoints) {
                e.put(s, points);
                maxPoints = points;
            }
        }

        int finalMaxPoints = maxPoints;
        MutableList<ObjectIntPair<Event>> l = e.keyValuesView().select(x -> x.getTwo() == finalMaxPoints).toList();
        int ls = l.size();
        return l.get(ls > 1 ? random().nextInt(ls) : 0).getOne();


    }

    @Override
    public long eventOcc(long when) {
        if (NAL.derive.TIMEGRAPH_DITHER_EVENTS_INTERNALLY)
            return Tense.dither(when, d.ditherDT);
        else
            return when;
    }

    @Override
    protected Random random() {
        return d.random;
    }

    @Override
    protected boolean decomposeAddedEvent(Event event) {
        return decomposeEvents && super.decomposeAddedEvent(event);
    }

    @Override
    public void clear() {
        super.clear();
        clearSolutions();
    }

    /**
     * doesnt clear the graph
     */
    public void clearSolutions() {
        solutions.clear();
    }

    private Occurrify know(Term pattern, boolean taskOccurr, boolean beliefOccurr, boolean decomposeEvents, OccurrenceSolver time) {


        long taskStart = taskOccurr ? d.taskStart : TIMELESS,
                taskEnd = taskOccurr ? d.taskEnd : TIMELESS,
                beliefStart =
                        beliefOccurr && (!d.concSingle || (d.concPunc==QUESTION || d.concPunc==QUEST)) ? d.beliefStart : TIMELESS,
                        //d.beliefStart,
                beliefEnd =
                        beliefOccurr && (!d.concSingle || (d.concPunc==QUESTION || d.concPunc==QUEST)) ? d.beliefEnd : TIMELESS;
                        //d.beliefEnd;

        this.decomposeEvents = decomposeEvents;


        if (taskStart == ETERNAL && (beliefStart != TIMELESS && beliefStart != ETERNAL)) {

//            if (time.beliefProjection()==BeliefProjection.Task) {
//                //taskStart = taskEnd = TIMELESS;
//
////                //eternal task projected to virtual present moment
//                long now = d.time();
//                int dur = Math.min(d.nar.dtDither(), Tense.occToDT(beliefEnd - beliefStart));
//                taskStart = now - dur / 2;
//                taskEnd = now + dur / 2;
//            } else {
                //eternal task projected to belief's time
                taskStart = beliefStart;
                taskEnd = beliefEnd;
//            }

        } else if ((beliefStart == ETERNAL && taskStart!=ETERNAL)) { //|| time.beliefProjection()==BeliefProjection.Task) {
            beliefStart = beliefEnd = TIMELESS;
        }
//        if ((d.concPunc==GOAL || d.concPunc==QUEST) && (d.taskPunc == GOAL || d.taskPunc == QUEST) && taskStart!=ETERNAL && (beliefStart!=ETERNAL && beliefStart!=TIMELESS)) {
//            //ignore belief occurrence in deriving goal/quest from goal/quest
//            beliefStart = beliefEnd = TIMELESS;
//        }



        final Term taskTerm = d.retransform(d.taskTerm);
        Term beliefTerm = d.beliefTerm.equals(d.taskTerm) ? taskTerm : d.retransform(d.beliefTerm);

        Event taskEvent = (taskStart != TIMELESS) ?
                know(taskTerm, taskStart, taskEnd) :
                know(taskTerm);

        imageNormalize(pattern);
        imageNormalize(taskEvent);

        if (beliefTerm.op().eventable) {
            boolean equalBT = beliefTerm.equals(taskTerm);
            Event beliefEvent = (beliefStart != TIMELESS) ?
                    know(beliefTerm, beliefStart, beliefEnd) :
                    ((!equalBT) ? know(beliefTerm) : taskEvent) /* same target, reuse the same event */;
            if (!equalBT)
                imageNormalize(beliefEvent);
        }

        autoneg = (taskTerm.hasAny(NEG) || beliefTerm.hasAny(NEG) || pattern.hasAny(NEG));


        //compact(); //TODO compaction removes self-loops which is bad, not sure if it does anything else either

        return this;
    }

    private void imageNormalize(Event p) {
        Term ip = Image.imageNormalize(p.id);
        if (!p.id.equals(ip))
            link(p, 0, shadow(ip));
    }
    private void imageNormalize(Term p) {
        Term ip = Image.imageNormalize(p);
        if (!p.equals(ip))
            link(shadow(p), 0, shadow(ip));
    }

    /**
     * called after solution.add
     */
    private boolean eachSolution(Event solution) {
        return (ttl-- > 0);
    }

    @Override
    protected boolean validPotentialSolution(Term y) {
        if (super.validPotentialSolution(y)) {
            int v = y.volume();
            return
                    v <= patternVolumeMax &&
                            v >= patternVolumeMin;
        } else
            return false;
    }

    private ArrayHashSet<Event> solutions(Term pattern) {

        ttl = NAL.derive.TIMEGRAPH_ITERATIONS;
        this.pattern = pattern;
        this.patternVolumeMin =
                (int) Math.floor(NAL.derive.TIMEGRAPH_IGNORE_DEGENERATE_SOLUTIONS_FACTOR * pattern.volume());
        this.patternVolumeMax = d.termVolMax + 1; //+1 for possible negation/unnegation

        this.nodesMax = d.termVolMax * 2 + pattern.volume();  //should be plenty of event nodes

        solve(pattern,  /* take everything */ this::eachSolution);

        return solutions;
    }

    @Override
    protected int occToDT(long x) {
        return Tense.dither(super.occToDT(x), d.ditherDT);
    }

    private Term solveDT(Term x, Derivation d, boolean decomposeEvents,OccurrenceSolver occ) {
        Term p;
        Event e = selectSolution(false, d.occ.know(x,  true,true,decomposeEvents,occ).solutions(x));
        if (e == null) {
            if ((e == null || (e.id.hasXternal())) && (d.taskTerm.hasAny(NEG) || d.beliefTerm.hasAny(NEG) || x.hasAny(NEG)) ) {

                //HACK for deficiencies in TimeGraph, try again solving for the negative
                Event e2 = selectSolution(false, solutions((e == null ? x : e.id /* some XTERNAL's may have been solved */).neg()));
                if (e2!=null)
                    return e2.id.neg();
            }

            return pattern; //last resort
        } else
            return e.id;
    }


    /**
     * eternal check: conditions under which an eternal result might be valid
     */
    boolean validEternal() {
        return d.taskStart == ETERNAL && (d.concSingle || d.beliefStart == ETERNAL);
    }

    public enum OccurrenceSolver {

        /**
         * TaskRange is a specialization of Task timing that applies a conj's range to the result, effectively a union across its dt span
         */
        TaskRange() {
            @Override
            public Pair<Term, long[]> occurrence(Term x, Derivation d) {
                return solveDT(d, x, true);
            }

            @Override
            long[] occurrence(Derivation d) {
                assert (d.taskTerm.op() == CONJ);

                if (d.taskStart != ETERNAL) {
                    int r = d.taskTerm.eventRange();
                    long[] o = new long[]{d.taskStart, r + d.taskEnd};


                    if (r > 0 && d.concTruth != null) {
                        //HACK decrease evidence by proportion of time expanded
                        float ratio = (float) (((double) (1 + d.taskEnd - d.taskStart)) / (1 + o[1] - o[0]));
                        if (!d.concTruthEviMul(ratio, false))
                            return null;
                    }

                    return o;
                } else {
                    return new long[]{ETERNAL, ETERNAL};
                }
            }

        },


        Default() {
            @Override
            @Nullable public Pair<Term, long[]> occurrence(Term x, Derivation d) {
                return solve(x, d, true, true);
            }

            @Override
            long[] occurrence(Derivation d) {
                return rangeCombine(d, OccMerge.Task);
            }
        },

        /** composition of non-events to a single outcome event.  a simplified version of Default */
        Compose() {
            final BeliefProjection PROJ = BeliefProjection.Task;

            final OccMerge combine = NAL.OCCURRIFY_COMPOSE_UNION_DILUTE ? OccMerge.UnionDilute : OccMerge.Task;

            @Override
            @Nullable public Pair<Term, long[]> occurrence(Term x, Derivation d) {
                if (x.hasXternal()) {
                    return solveDT(d, x, true);
                } else {
                    long[] o = occurrence(d);
                    return o != null ? pair(x, o) : null;
                }
            }

            @Override
            long[] occurrence(Derivation d) {
                return rangeCombine(d,
                        combine

                );
            }


            @Override
            public final BeliefProjection beliefProjection() {
                return PROJ;
            }
        },

        /**
         * for unprojected truth rules;
         * result should be left-aligned (relative) to the task's start time
         * used by temporal induction rules
         */
        TaskRelative() {
            @Override
            public Pair<Term, long[]> occurrence(Term x, Derivation d) {
                return solveDT(d, x, false);
            }

            @Override
            long[] occurrence(Derivation d) {
                return rangeCombine(d, OccMerge.Task);
            }

            @Override
            public BeliefProjection beliefProjection() {
                return BeliefProjection.Belief;
            }
            @Override
            public @Nullable Predicate<Derivation> filter() {
                return differentTermsOrTimes;
            }

        },
        /**
         * for unprojected truth rules;
         * result should be left-aligned (relative) to the belief's start time
         */
        BeliefRelative() {
            @Override
            public Pair<Term, long[]> occurrence(Term x, Derivation d) {
                return solveDT(d, x, false);
            }

            @Override
            long[] occurrence(Derivation d) {
                return rangeCombine(d, OccMerge.Belief);
            }

            @Override
            public BeliefProjection beliefProjection() {
                return BeliefProjection.Belief;
            }
            @Override
            public @Nullable Predicate<Derivation> filter() {
                return differentTermsOrTimes;
            }

        },
        /** belief modulates the truth but the occurrence time to be solved should center around the task */
        TaskEvent() {

            @Override
            @Nullable public Pair<Term, long[]> occurrence(Term x, Derivation d) {
                return solve(x, d, true, false);
            }

            @Override
            long[] occurrence(Derivation d) {
                return rangeCombine(d, OccMerge.Task);
            }
        },
        /** task modulates the truth but the occurrence time to be solved should center around the belief */
        BeliefEvent() {

            @Override
            @Nullable public Pair<Term, long[]> occurrence(Term x, Derivation d) {
                return solve(x, d, false, true);
            }

            @Override
            long[] occurrence(Derivation d) {
                return rangeCombine(d, OccMerge.Belief);
            }
        },


        /**
         * specificially for conjunction induction
         * <p>
         * unprojected span, for sequence induction
         * events are not decomposed as this could confuse the solver unnecessarily.  automatic autoneg?
         */
        Sequence() {

            @Override
            public @Nullable Predicate<Derivation> filter() {
                return differentTermsOrTimes;
            }

            @Override
            public Pair<Term, long[]> occurrence(Term x, Derivation d) {


                Term tt = Image.imageNormalize(d.taskTerm);
                Term bb = Image.imageNormalize(d.beliefTerm);

                if (!d.retransform.isEmpty()) {
                    //HACK re-apply variable introduction
                    tt = tt.replace(d.retransform);
                    if (tt instanceof Bool) return null;
                    bb = bb.replace(d.retransform);
                    if (bb instanceof Bool) return null;
                }

                tt = tt.negIf(d.taskTruth.isNegative());
                bb = bb.negIf(d.beliefTruth_at_Task.isNegative());

                long tTime = d.taskStart, bTime = d.beliefStart;

                Term y;
                long[] occ;
                if (tTime == ETERNAL && bTime == ETERNAL) {
                    y = CONJ.the(DTERNAL, tt, bb);
                    occ = new long[]{ETERNAL, ETERNAL};
                } else if (tTime == ETERNAL) {
                    y = CONJ.the(DTERNAL, tt, bb);
                    occ = new long[]{bTime, d.beliefEnd};
                } else if (bTime == ETERNAL) {
                    y = CONJ.the(DTERNAL, tt, bb);
                    occ = new long[]{tTime, d.taskEnd};
                } else {

                    long earlyStart = Math.min(tTime, bTime);

                    Term a, b;
                    long  dt;
                    if (tTime == earlyStart) {
                        a = tt; b = bb; dt = bTime - tTime;
                    } else {
                        a = bb; b = tt; dt = tTime - bTime;
                    }

                    if (NAL.derive.TIMEGRAPH_DITHER_EVENTS_INTERNALLY)
                        dt = Tense.dither(dt, d.ditherDT);

                    y = terms.conjMerge(a, Tense.occToDT(dt), b);

                    //dont dither occ[] here, since it will be done in Taskify
                    long range = Math.max(Math.min((1 + d.taskEnd - tTime), (1 + d.beliefEnd - bTime)) - 1, 0);
                    occ = new long[]{earlyStart, earlyStart + range};
                }

                return pair(y, occ);
            }

            @Override
            long[] occurrence(Derivation d) {
                throw new UnsupportedOperationException();
            }

            @Override
            public BeliefProjection beliefProjection() {
                return BeliefProjection.Belief;
            }

        },

        Task() {
            @Override
            public Pair<Term, long[]> occurrence(Term x, Derivation d) {
                return solveDT(d, x, true);
            }

            @Override
            long[] occurrence(Derivation d) {
                return rangeCombine(d, OccMerge.Task);

//                long[] o = new long[2];
//                /*if (d.occ.validEternal()) {
//                    o[0] = o[1] = ETERNAL;
//                } else */if (d.taskStart != ETERNAL) {
//                    o[0] = d.taskStart;
//                    o[1] = d.taskEnd;
//                } else {
//                    o[0] = d.beliefStart;
//                    o[1] = d.beliefEnd;
//                }
//                return o;
            }

        },

        ;

        private final Term term;

        OccurrenceSolver() {
            this.term = Atomic.the(name());
        }

        /**
         * fallback
         */
        abstract long[] occurrence(Derivation d);

        abstract public Pair<Term, long[]> occurrence(Term x, Derivation d);



        /**
         * gets the optional premise pre-filter for this consequence.
         */
        @Nullable
        public Predicate<Derivation> filter() {
            return null;
        }

        @Nullable Pair<Term, long[]> solveDT(Derivation d, Term x, boolean decomposeEvents) {
            long[] occ = occurrence(d);
            if (occ == null)
                return null;

            if (x.hasXternal()) {
                d.occ.clear();

                if (occ != null && occ[0] != TIMELESS && occ[0] != ETERNAL)
                    d.occ.know(x, occ[0], occ[1]);

                return pair(d.occ.solveDT(x, d, decomposeEvents, this), occ);
            } else {
                return pair(x, occ);
            }

        }

        protected @Nullable Pair<Term, long[]> solve(Term x, Derivation d, boolean taskOccurr, boolean beliefOccurr) {
            if (nonTemporal(x) && nonTemporal(d.taskTerm) && nonTemporal(d.beliefTerm))
                return pair(x, occurrence(d));

            if (!taskOccurr && beliefOccurr && (d.concSingle || d._belief == null || d.beliefStart == ETERNAL || d.beliefStart == TIMELESS))
                taskOccurr = true; //allow task occurrence
            if (!beliefOccurr && taskOccurr && (d.taskStart == ETERNAL) && (d.beliefStart != ETERNAL && d.beliefStart != TIMELESS))
                beliefOccurr = true; //allow belief occurrence

            d.occ.clear();
            Occurrify o = d.occ.know(x, taskOccurr, beliefOccurr, true, this);
            Event e = o.selectSolution(true, o.solutions(x));

            if (e == null && (d.taskTerm.hasAny(NEG) || d.beliefTerm.hasAny(NEG) || x.hasAny(NEG)) ) {
                //HACK for deficiencies in TimeGraph, try again solving for the negative
//                Occurrify o2 = d.occ.know(x.neg(), taskOccurr, beliefOccurr, true, this);
                Event e2 = o.selectSolution(true, o.solutions(x.neg()));
                if (e2!=null) {
                    e = e2.neg();
                }
            }

            if (e == null) {
//                if (d.concPunc==QUESTION || d.concPunc==QUEST)
//                    return pair(x, occurrence(d)); //fail-safe
//                else
//                    return NAL.OCCURRIFY_STRICT ? null : pair(x, occurrence(d)); //fail-safe

                if (NAL.OCCURRIFY_STRICT)
                    return null;

                return pair(x, occurrence(d)); //fail-safe

            } else {
                long es = e.start();
                return pair(e.id,
                        es == TIMELESS ?
                                occurrence(d) :
                                new long[]{es, e.end()});
            }
        }

        public BeliefProjection beliefProjection() {
            return BeliefProjection.Task;
        }
    }

    /** ignores temporal subterms ofof --> and <-> */
    static boolean nonTemporal(Term x) {
        return !x.hasAny(Op.Temporal) ||
                (!x.hasXternal() && x.recurseTerms(term->term.hasAny(Op.Temporal), (Term term,Compound suuper)->{
                    if (term.op().temporal) {
                        if (suuper == null || (suuper.op() != INH && suuper.op() != SIM))
                            return false;
                    }
                    return true;
                }, null));
    }

    private enum OccMerge {
        Task, Belief, Earliest,
        Union, UnionDilute, Intersect
        //TODO Mid?
    }


    /**
     * TODO do derivation truth calculation in implemented method of this enum
     */
    public enum BeliefProjection implements Function<Derivation, Truth> {

        /**
         * belief truth evident at its own occurrence time
         */
        Belief {
            @Override
            public Truth apply(Derivation d) {
                return d.beliefTruth_at_Belief;
            }
        },

        /**
         * belief truth projected to task's occurrence time
         */
        Task {
            @Override
            public Truth apply(Derivation d) {
                return d.beliefTruth_at_Task;
            }
        },

//        /** belief
//        Union {
//
//        },
    }

    private static final Predicate<Derivation> intersection = d ->
        d.taskBelief_TimeIntersection[0] != TIMELESS;

    private static final Predicate<Derivation> differentTermsOrTimes = d ->
        !d.taskTerm.equals(d.beliefTerm) || Tense.simultaneous(d.taskStart, d.beliefStart, d.ditherDT);

}
















































































































//    @Deprecated static boolean temporal(Truthify truth, Derivation d) {
//        if (!d.temporal)
//            return false;
//
////        //HACK reset to the input
////        d.taskStart = d._task.start();
////        d.taskEnd = d._task.end();
////        if (d._belief != null && !d.concSingle) {
////            d.beliefStart = d._belief.start();
////            d.beliefEnd = d._belief.end();
////
////            boolean taskEternal = d.taskStart == ETERNAL;
////            if (truth.beliefProjection == BeliefProjection.Belief || taskEternal) {
////
////                //unchanged: d.beliefStart = d._belief.start();  d.beliefEnd = d._belief.end();
////
////            } else if (truth.beliefProjection == BeliefProjection.Task) {
////
////                boolean bothNonEternal = d.taskStart != ETERNAL && d._belief.start() != ETERNAL;
////                if (bothNonEternal && d.taskTerm.op().temporal && !d.beliefTerm.op().temporal) {
////
////                    //mask task's occurrence, focusing on belief's occ
////                    d.beliefStart = d.taskStart = d._belief.start();
////                    d.taskEnd = d.taskStart + (d._task.range() - 1);
////                    d.beliefEnd = d._belief.end();
////
////                } else {
////
////                    if (d._belief.isEternal()) {
////                        d.beliefStart = d.beliefEnd = ETERNAL; //keep eternal
////                    } else {
////                        //the temporal belief has been shifted to task in the truth computation
////                        long range = (taskEternal || d._belief.start() == ETERNAL) ? 0 : d._belief.range() - 1;
////                        d.beliefStart = d.taskStart;
////                        d.beliefEnd = d.beliefStart + range;
////                    }
////                }
////            } else {
////
////                throw new UnsupportedOperationException();
////            }
////
////        } else {
////            d.beliefStart = d.beliefEnd = TIMELESS;
////        }
//        return d.temporalTerms || (d.taskStart != ETERNAL) || (d.beliefStart != ETERNAL && d.beliefStart != TIMELESS);
//    }
