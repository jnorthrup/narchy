package nars.derive.op;

import jcog.data.set.ArrayHashSet;
import nars.NAL;
import nars.Op;
import nars.Task;
import nars.derive.model.Derivation;
import nars.derive.model.DerivationFailure;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.control.PREDICATE;
import nars.term.util.conj.ConjSeq;
import nars.term.util.transform.Retemporalize;
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

import java.util.Arrays;
import java.util.Random;
import java.util.function.Function;

import static nars.Op.*;
import static nars.derive.model.DerivationFailure.Success;
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


    static void temporalTask(Term x, OccurrenceSolver time, Taskify t, Derivation d) {



        boolean neg = false;
//        Term xx = x;
//        if (x.op()==NEG && (!d.taskTerm.hasAny(NEG) && !d.beliefTerm.hasAny(NEG))) {
//            //HACK semi-auto-unneg to help occurrify
//            x = x.unneg();
//        }

        Pair<Term, long[]> timing = time.occurrence(x, d);
        if (timing == null) {
            d.nar.emotion.deriveFailTemporal.increment();
            return;
        }

        Term y = timing.getOne();
        if (y.op()!=x.op()) {
            d.nar.emotion.deriveFailTemporal.increment();
            return;
        }

        if (neg)
            y = y.neg();

        long[] occ = timing.getTwo();

        if (!((occ[0] != TIMELESS) && (occ[1] != TIMELESS) &&
                (occ[0] == ETERNAL) == (occ[1] == ETERNAL) &&
                (occ[1] >= occ[0])) || (occ[0] == ETERNAL && !d.occ.validEternal()))
            throw new RuntimeException("bad occurrence result: " + Arrays.toString(occ));


        if (NAL.derive.DERIVATION_FORM_QUESTION_FROM_AMBIGUOUS_BELIEF_OR_GOAL && (d.concPunc == BELIEF || d.concPunc == GOAL)) {
            if (DerivationFailure.failure(y, d.concPunc)) {

                //as a last resort, try forming a question from the remains
                byte qPunc = d.concPunc == BELIEF ? QUESTION : QUEST;
                d.concPunc = qPunc;
                if (DerivationFailure.failure(y, d) == Success) {
                    d.concPunc = qPunc;
                    d.concTruth = null;
                } else {
                    d.nar.emotion.deriveFailTemporal.increment();
                    return; //fail
                }

            } //else: ok
        } else {
            if (DerivationFailure.failure(y, d) != Success) {
                d.nar.emotion.deriveFailTemporal.increment();
                return;
            }
        }

        if (y != null) {
            if (NAL.test.DEBUG_ENSURE_DITHERED_DT)
                assertDithered(y, d.ditherDT);
        }

        t.taskify(y, occ[0], occ[1], d);
    }

    static void eternalTask(Term x, Taskify t, Derivation d) {
        //assert(d.taskStart == ETERNAL && d.taskEnd == ETERNAL);
        //assert((d.beliefStart == ETERNAL && d.beliefEnd == ETERNAL)||(d.beliefStart == TIMELESS && d.beliefEnd == TIMELESS));
        byte punc = d.concPunc;
        if ((punc == BELIEF || punc == GOAL) && x.hasXternal()) { // && !d.taskTerm.hasXternal() && !d.beliefTerm.hasXternal()) {
            //HACK this is for deficiencies in the temporal solver that can be fixed

            x = Retemporalize.retemporalizeXTERNALToDTERNAL.apply(x);

            if (!DerivationFailure.failure(x, d.concPunc)) {
                d.nar.emotion.deriveFailTemporal.increment();
                Taskify.spam(d, NAL.derive.TTL_COST_DERIVE_TASK_FAIL);
                return;
            }
        }

        t.taskify(x, ETERNAL, ETERNAL, d);
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


    private static long[] rangeCombine(Derivation d, OccIntersect mode) {
        long beliefStart = d.beliefStart;
        if (d.concSingle || (d._belief == null || d.beliefStart == ETERNAL))
            return new long[] { d.taskStart, d.taskEnd };
        else if (d.taskStart == ETERNAL)
            return new long[] { d.beliefStart, d.beliefEnd };
        else {
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
                default:
                    throw new UnsupportedOperationException();
            }

            //minimum range = intersection
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
        for (byte i = 0; i < ss; i++) {
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
        return decomposeEvents;
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

    private Occurrify set(Term pattern, boolean decomposeEvents, OccurrenceSolver time) {

        clear();
        //clearSolutions(); //<- is this safe?  accumulates timegraph over multiple derived tasks within before being cleared on new premise

        long taskStart = d.taskStart,
                taskEnd = d.taskEnd,
                beliefStart = !d.concSingle ? d.beliefStart : TIMELESS,
                beliefEnd = !d.concSingle ? d.beliefEnd : TIMELESS;

        this.decomposeEvents = decomposeEvents;

        if (taskStart == ETERNAL && (beliefStart != TIMELESS && beliefStart != ETERNAL)) {
            //taskStart = taskEnd = TIMELESS;
            //taskStart = taskEnd = d.time();

//            if (time.beliefProjection()==BeliefProjection.Task) {
//                //eternal task projected to virtual present moment
//                long now = d.time();
//                int dur = d.dur();
//                taskStart = now - dur / 2;
//                taskEnd = now + dur / 2;
//            } else {
                //eternal task projected to belief's time
                taskStart = beliefStart;
                taskEnd = beliefEnd;
//            }

        } else if (beliefStart == ETERNAL && taskStart!=ETERNAL) {
            beliefStart = taskStart;
            beliefEnd = taskEnd;
        }
            /*else if (beliefStart!=TIMELESS && taskStart != ETERNAL) {
            //conditions for considering vs ignoring belief occurrence:
            boolean ignoreBeliefOcc = false;
            if (beliefStart == ETERNAL)
                ignoreBeliefOcc = true;
            else if (d.concTruth!=null && time.beliefProjection()==BeliefProjection.Task)
                ignoreBeliefOcc = true;

            if (ignoreBeliefOcc)
                beliefStart = beliefEnd = TIMELESS;
        }*/

        final Term taskTerm = d.retransform(d.taskTerm);
        Term beliefTerm;
        if (d.beliefTerm.equals(d.taskTerm))
            beliefTerm = taskTerm;
        else
            beliefTerm = d.retransform(d.beliefTerm);

        Event taskEvent = (taskStart != TIMELESS) ?
                know(taskTerm, taskStart, taskEnd) :
                know(taskTerm);

        if (beliefTerm.op().eventable) {
            boolean equalBT = beliefTerm.equals(taskTerm);
            Event beliefEvent = (beliefStart != TIMELESS) ?
                    know(beliefTerm, beliefStart, beliefEnd) :
                    ((!equalBT) ? know(beliefTerm) : taskEvent) /* same target, reuse the same event */;
        }

        autoneg = (taskTerm.hasAny(NEG) || beliefTerm.hasAny(NEG) || pattern.hasAny(NEG));

        //compact(); //TODO compaction removes self-loops which is bad, not sure if it does anything else either

        return this;
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
        this.patternVolumeMax = d.termVolMax;

        this.nodesMax = d.termVolMax * 2 + pattern.volume();  //should be plenty of event nodes

        solve(pattern,  /* take everything */ this::eachSolution);

        return solutions;
    }

    @Override
    protected int occToDT(long x) {
        return Tense.dither(super.occToDT(x), d.ditherDT);
    }

    private Term solveDT(Term pattern, ArrayHashSet<Event> solutions) {
        Term p;
        Event e = selectSolution(false, solutions);
        if (e == null)
            return pattern;
        else
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
            public Pair<Term, long[]> occurrence(Term x, Derivation d) {
                return solveOccDT(x, d.occ.set(x, true, this));
            }

            @Override
            long[] occurrence(Derivation d) {
                return rangeCombine(d, OccIntersect.Task);
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
                return rangeCombine(d, OccIntersect.Belief);
            }

            @Override
            public BeliefProjection beliefProjection() {
                return BeliefProjection.Belief;
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
                return rangeCombine(d, OccIntersect.Task);
            }

            @Override
            public BeliefProjection beliefProjection() {
                return BeliefProjection.Belief;
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
            public Pair<Term, long[]> occurrence(Term x, Derivation d) {


                Term tt = d.taskTerm.negIf(d.taskTruth.isNegative());
                Term bb = d.beliefTerm.negIf(d._belief.isNegative());

                if (!d.retransform.isEmpty()) {
                    //HACK re-apply variable introduction
                    tt = tt.replace(d.retransform);
                    bb = bb.replace(d.retransform);
                }

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

                    int ditherDT = d.ditherDT;
                    if (tTime == earlyStart)
                        y = ConjSeq.sequence(tt, 0, bb, Tense.dither(bTime - tTime, ditherDT), terms);
                    else
                        y = ConjSeq.sequence(bb, 0, tt, Tense.dither(tTime - bTime, ditherDT), terms);

                    //dont dither occ[] here, since it will be done in Taskify
                    long range = Math.max(Math.min((1 + d.taskEnd - d.taskStart), (1 + d.beliefEnd - d.beliefStart)) - 1, 0);
                    occ = new long[]{earlyStart, earlyStart + range};
                }

                return pair(y, occ);
            }

            @Override
            long[] occurrence(Derivation d) {
                return rangeCombine(d, OccIntersect.Earliest);
            }

            @Override
            public BeliefProjection beliefProjection() {
                return BeliefProjection.Belief;
            }

        },

        Task() {
            @Override
            public Pair<Term, long[]> occurrence(Term x, Derivation d) {
                return solveDT(d, x, false);
            }

            @Override
            long[] occurrence(Derivation d) {
                long[] o = new long[2];
                if (d.occ.validEternal()) {
                    o[0] = o[1] = ETERNAL;
                } else if (d.taskStart != ETERNAL) {
                    o[0] = d.taskStart;
                    o[1] = d.taskEnd;
                } else {
                    o[0] = d.beliefStart;
                    o[1] = d.beliefEnd;
                }
                return o;
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

        Pair<Term, long[]> solveOccDT(Term x, Occurrify o) {

            Event e = o.selectSolution(true, o.solutions(x));
            if (e == null) {
                return pair(x, occurrence(o.d));
            } else {
                long es = e.start();
                return pair(e.id, es == TIMELESS ?
                                occurrence(o.d) :
                                new long[]{es, e.end()});
            }
        }


        @Nullable
        protected Pair<Term, long[]> solveLocal(Derivation d, Term x) {
            return solveDT(d, x, true);
        }

        /**
         * gets the optional premise pre-filter for this consequence.
         */
        @Nullable
        public PREDICATE<Derivation> filter() {
            return null;
        }

        @Nullable Pair<Term, long[]> solveDT(Derivation d, Term x, boolean decomposeEvents) {
            long[] occ = occurrence(d);
            //assert (occ != null);
            return occ == null ? null : pair(
                    x.hasXternal() ? d.occ.solveDT(x, d.occ.set(x, decomposeEvents, this).solutions(x)) : x,
                    occ);
        }


        public BeliefProjection beliefProjection() {
            return BeliefProjection.Task;
        }
    }

    private enum OccIntersect {
        Task, Belief, Earliest
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
                return d.beliefTruthBelief;
            }
        },

        /**
         * belief truth projected to task's occurrence time
         */
        Task {
            @Override
            public Truth apply(Derivation d) {
                return d.beliefTruthTask;
            }
        },

//        /** belief
//        Union {
//
//        },
    }
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
