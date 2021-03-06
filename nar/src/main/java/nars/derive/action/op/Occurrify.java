package nars.derive.action.op;

import jcog.TODO;
import jcog.data.set.ArrayHashSet;
import jcog.decide.Roulette;
import jcog.math.Longerval;
import nars.NAL;
import nars.Op;
import nars.derive.Derivation;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.atom.IdempotentBool;
import nars.term.util.Image;
import nars.time.Tense;
import nars.time.TimeGraph;
import nars.truth.MutableTruth;
import org.eclipse.collections.api.block.function.primitive.IntToFloatFunction;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Random;
import java.util.function.BiPredicate;
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

    public static final OccurrenceSolver solverDefaultSingle = OccurrenceSolver.Task;
    public static final OccurrenceSolver solverDefaultDouble = OccurrenceSolver.Default;

    public static final ImmutableMap<Term, OccurrenceSolver> solvers;

    static {
        MutableMap<Term, OccurrenceSolver> tm = new UnifiedMap<>(8);
        for (OccurrenceSolver m : OccurrenceSolver.values()) {
            tm.put(Atomic.the(m.name()), m);
        }
        solvers = tm.toImmutable();
    }

    private final Derivation d;
    private static final int absolutePoints = 10;
    private static final int noXternalPoints = 10;
    //    int novelPoints = 1;
//    int sameAsPatternRootPoints = 1;
    private transient boolean decomposeEvents;
    private transient int patternVolumeMin;
    private transient int patternVolumeMax;



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
            return ((Compound)x).subtermsDirect().OR(Occurrify::temporal);
        }
        return false;
    }


    private static long[] rangeCombine(Derivation d, OccMerge mode) {

        long taskEnd = d.taskEnd;
        long taskStart = d.taskStart;
        long beliefStart = d.beliefStart;

        if (/*d.single ||*/ beliefStart == TIMELESS ||  beliefStart == ETERNAL)
            return new long[] { taskStart, taskEnd};
        else {
            long beliefEnd = d.beliefEnd;
            if (taskStart == ETERNAL) {
    //            assert(d.beliefStart!=TIMELESS);
                return new long[]{ beliefStart, beliefEnd};
            } else {

                long start;
                switch (mode) {
                    case Earliest:
                        start = Math.min(taskStart, beliefStart);
                        break;
                    case Task:
    //                    if ((d.punc == QUESTION || d.punc == QUEST) && (d.taskPunc == QUESTION || d.taskPunc == QUEST))
    //                        start = beliefStart; //follow the non-question part of a question progression
    //                    else
                        start = taskStart;
                        break;
                    case Belief:
                        start = beliefStart;
                        break;
                    case Union:
                        return Longerval.unionArray(taskStart, taskEnd, beliefStart, beliefEnd);
                    case Intersect:
                        throw new TODO();
//                        long[] i = d.taskBelief_TimeIntersection;
//    //                    if (i[0] == TIMELESS)
//    //                        throw new WTF("intersection filter failure");
//
//    //                    if (d.concPunc == BELIEF || d.concPunc == GOAL) {
//    //                        long iRange = LongInterval.intersectLength(taskStart, d.taskEnd, beliefStart, d.beliefEnd);
//    //                        long uRange = i[1] - i[0];
//    //                        double pct = (1 + iRange) / (1.0 + uRange);
//    //
//    //                    }
//                        return i;

                    case UnionDilute: {
                        long[] u = Longerval.unionArray(taskStart, taskEnd, beliefStart, beliefEnd);
                        if (!d.isBeliefOrGoal()) {
                            return u; //questions or quests
                        } else {
                            long tRange = 1L + taskEnd - taskStart, bRange = 1L + beliefEnd - beliefStart;
                            double uRange = 1.0 + (double) u[1] - (double) u[0];
                            //long iRange = LongInterval.intersectLength(taskStart, taskEnd, beliefStart, beliefEnd);
                            double pct = (double) Math.max(tRange, bRange) / (uRange);
                            //assert(pct <= 1.0);

                            if (d.doubt(pct))
                                return u; //union accepted, diluted as needed


                            //try intersection
                            if (Longerval.intersectionArray(taskStart, taskEnd, beliefStart, beliefEnd, u)!=null)
                                return u;

                            return null; //fail
                        }

                    }
                    default:
                        throw new UnsupportedOperationException();
                }

                //minimum range
                return new long[]{
                        start,
                        start + Math.min(taskEnd - taskStart, beliefEnd - beliefStart)
                };
            }
        }
    }


    private @Nullable Event selectSolution(boolean occ, ArrayHashSet<Event> s) {
        int ss = s.size();
        if (ss == 0)
            return null;
        Event s0 = s.get(0);
        if (ss == 1)
            return s0;

        float[] score = new float[ss];
        for (int i = 0; i < ss; i++)
            score[i] = score(s.get(i), occ);
        return solutions.get(Roulette.selectRoulette(ss, new IntToFloatFunction() {
            @Override
            public float valueOf(int i) {
                return score[i];
            }
        }, random()));
    }

    private static float score(Event e, boolean occ) {
        Term st = e.id;
        float points = 1.0F;
        if (e instanceof Absolute) {
            points = points + (float) (occ ? (absolutePoints * 2) : absolutePoints);
//            if (node(st)==null)
//                points += novelPoints;
        }
//        if (st.equalsRoot(pattern)) {
//            points += sameAsPatternRootPoints;
//        }
        if (!st.hasXternal()) {
            points += occ ? (float) noXternalPoints : ((float) noXternalPoints *2f);
        }
        return points;
    }

    @Override
    public long eventOcc(long when) {
        return NAL.derive.TIMEGRAPH_DITHER_EVENTS_INTERNALLY ? Tense.dither(when, d.ditherDT) : when;
    }

    @Override
    protected Random random() {
        return d.random();
    }

    @Override
    protected boolean decomposeAddedEvent(Event event) {
        return decomposeEvents && super.decomposeAddedEvent(event);
    }

    @Override
    public void clear() {
        super.clear();
        solutions.clear();
    }

    private Occurrify know(Term pattern, boolean taskOccurr, boolean beliefOccurr, boolean decomposeEvents, OccurrenceSolver time) {


        long taskStart = taskOccurr ? d.taskStart : TIMELESS,
                taskEnd = taskOccurr ? d.taskEnd : TIMELESS,
                beliefStart =
                        beliefOccurr && (!d.single || ((int) d.punc == (int) QUESTION || (int) d.punc == (int) QUEST)) ? d.beliefStart : TIMELESS,
                        //d.beliefStart,
                beliefEnd =
                        beliefOccurr && (!d.single || ((int) d.punc == (int) QUESTION || (int) d.punc == (int) QUEST)) ? d.beliefEnd : TIMELESS;
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


        Term taskTerm = d.retransform(d.taskTerm);
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

        boolean b = false;
        for (Term term : Arrays.asList(taskTerm, beliefTerm, pattern)) {
            if (term.hasAny(NEG)) {
                b = true;
                break;
            }
        }
        autoneg = b;

        //compact(); //TODO compaction removes self-loops which is bad, not sure if it does anything else either

        return this;
    }

    private void imageNormalize(Event p) {
        Term ip = Image.imageNormalize(p.id);
        if (!p.id.equals(ip))
            link(p, 0L, shadow(ip));
    }
    private void imageNormalize(Term p) {
        Term ip = Image.imageNormalize(p);
        if (!p.equals(ip))
            link(shadow(p), 0L, shadow(ip));
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
                    v <= patternVolumeMax && v >= patternVolumeMin;
        } else
            return false;
    }

    private ArrayHashSet<Event> solutions(Term pattern) {

        ttl = NAL.derive.TIMEGRAPH_ITERATIONS;
        this.pattern = pattern;
        this.patternVolumeMin =
                (int) Math.floor((double) (NAL.derive.TIMEGRAPH_IGNORE_DEGENERATE_SOLUTIONS_FACTOR * (float) pattern.volume()));
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
        Event e = selectSolution(false, d.occ.know(x,  true,true, decomposeEvents, occ).solutions(x));
        if (e == null) {
            if (d.taskTerm.hasAny(NEG) || d.beliefTerm.hasAny(NEG) || x.hasAny(NEG)) {

                //HACK for deficiencies in TimeGraph, try again solving for the negative
                /* some XTERNAL's may have been solved */
                /* some XTERNAL's may have been solved */
                Event e2 = selectSolution(false, solutions(x.neg()));
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
        return d.taskStart == ETERNAL && (d.single || d.beliefStart == ETERNAL);
    }

    public enum OccurrenceSolver {



        Default() {
            @Override
            public @Nullable Pair<Term, long[]> occurrence(Term x, Derivation d) {
                return solve(x, d, true, true);
            }

            @Override
            long[] occurrence(Derivation d) {
                return rangeCombine(d, OccMerge.Task);
            }

            @Override
            public BeliefProjection beliefProjection() {
                return BeliefProjection.Task;
            }
        },

        /** composition of non-events to a single outcome event.  a simplified version of Default */
        Compose() {

            final OccMerge combine = NAL.OCCURRIFY_COMPOSE_UNION_DILUTE ? OccMerge.UnionDilute : OccMerge.Task;

            final BeliefProjection PROJ = combine == OccMerge.UnionDilute ?  BeliefProjection.Mean : BeliefProjection.Task;

            @Override
            public @Nullable Pair<Term, long[]> occurrence(Term x, Derivation d) {
                if (x.hasXternal()) {
                    return solveDT(d, x, true);
                } else {
                    long[] o = occurrence(d);
                    return o != null ? pair(x, o) : null;
                }
            }

            @Override
            long[] occurrence(Derivation d) {
                return rangeCombine(d, combine);
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
            public Predicate<Derivation> filter() {
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
            public Predicate<Derivation> filter() {
                return differentTermsOrTimes;
            }

        },
        /** belief modulates the truth but the occurrence time to be solved should center around the task */
        TaskEvent() {

            @Override
            public @Nullable Pair<Term, long[]> occurrence(Term x, Derivation d) {
                return solve(x, d, true, false);
            }

            @Override
            long[] occurrence(Derivation d) {
                return rangeCombine(d, OccMerge.Task);
            }
            @Override
            public BeliefProjection beliefProjection() {
                return BeliefProjection.Task;
            }
        },
        TaskImmediate() {

            @Override
            public @Nullable Pair<Term, long[]> occurrence(Term x, Derivation d) {
                @Nullable Pair<Term, long[]> p = solve(x, d, true, false);
                if (p!=null)
                    filter(d, p.getTwo());
                return p;
            }

            @Override
            long[] occurrence(Derivation d) {
                long[] x = rangeCombine(d, OccMerge.Task);
                filter(d, x);
                return x;
            }

            private void filter(Derivation d, long[] x) {
                if (x!=null && x[0]!=ETERNAL && x[0]!=TIMELESS) {
                    long imm = d.taskStart;
                    if (imm == ETERNAL) imm = d.time;
                    //if (taskStart != ETERNAL) { // && taskStart > x[0]){

                    long delta = imm - x[0];

                    long r = x[1] - x[0];
                    x[0] = imm;
                    x[1] = imm + r;

                    if (delta > 0L)
                        d.doubt(NAL.eviEternalizable(1.0, delta, d.dur));

                }
                if (x == null) {
                    //TODO may be solvable directly if no xternal etc
                }
            }

            @Override
            public BeliefProjection beliefProjection() {
                return BeliefProjection.Task;
            }
        },
        /** task modulates the truth but the occurrence time to be solved should depend on the belief */
        BeliefEvent() {

            @Override
            public @Nullable Pair<Term, long[]> occurrence(Term x, Derivation d) {
                return solve(x, d, false, true);
            }

            @Override
            long[] occurrence(Derivation d) {
                return rangeCombine(d, OccMerge.Task);
            }
            @Override
            public BeliefProjection beliefProjection() {
                return BeliefProjection.Task;
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
            public Predicate<Derivation> filter() {
                return differentTermsOrTimes;
            }

            @Override
            public Pair<Term, long[]> occurrence(Term x, Derivation d) {

                Term tt = d.taskTerm;
                Term bb = d.beliefTerm;

                if (!d.retransform.isEmpty()) {
                    //HACK re-apply variable introduction
                    tt = tt.replace(d.retransform);
                    if (tt instanceof IdempotentBool) return null;
                    bb = bb.replace(d.retransform);
                    if (bb instanceof IdempotentBool) return null;
                }

                tt = tt.negIf(d.taskTruth.isNegative());
                bb = bb.negIf(d.beliefTruth_at_Belief.isNegative());

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

                    dt = Tense.dither(dt, d.ditherDT);

                    y = terms.conjMerge(a, Tense.occToDT(dt), b);

                    //dont dither occ[] here, since it will be done in Taskify
                    long range = Math.max(Math.min((1L + d.taskEnd - tTime), (1L + d.beliefEnd - bTime)) - 1L, 0L);
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
            }
            @Override
            public BeliefProjection beliefProjection() {
                return BeliefProjection.Task;
            }
        },
        Belief() {
            @Override
            public Pair<Term, long[]> occurrence(Term x, Derivation d) {
                return solveDT(d, x, true);
            }

            @Override
            long[] occurrence(Derivation d) {
                return rangeCombine(d, OccMerge.Belief);
            }

            @Override
            public BeliefProjection beliefProjection() {
                return BeliefProjection.Belief;
            }
        },

        ;

        final Term term;

        OccurrenceSolver() {
            this.term = Atomic.the(name());
        }

        /**
         * fallback
         */
        abstract long[] occurrence(Derivation d);

        public abstract Pair<Term, long[]> occurrence(Term x, Derivation d);



        /**
         * gets the optional premise pre-filter for this consequence.
         */
        public @Nullable Predicate<Derivation> filter() {
            return null;
        }

        @Nullable Pair<Term, long[]> solveDT(Derivation d, Term x, boolean decomposeEvents) {
            long[] occ = occurrence(d);
            if (occ == null)
                return null;

            Term y;
            if (!x.hasXternal()) {
                y = x;
            } else {

                boolean neg = false;
                if (preUnneg(x, d)) { x = x.unneg();neg = true; }

                d.occ.clear();

                if (occ[0] != TIMELESS && occ[0] != ETERNAL)
                    d.occ.know(x, occ[0], occ[1]);

                y = d.occ.solveDT(x, d, decomposeEvents, this).negIf(neg);
            }
            return pair(y, occ);

        }

        protected @Nullable Pair<Term, long[]> solve(Term x0, Derivation d, boolean taskOccurr, boolean beliefOccurr) {
            Term x = x0;
            if (nonTemporal(x) && nonTemporal(d.taskTerm) && nonTemporal(d.beliefTerm))
                return pair(x, occurrence(d));

            if (!taskOccurr && beliefOccurr && (d.single || d._belief == null || d.beliefStart == ETERNAL || d.beliefStart == TIMELESS))
                taskOccurr = true; //allow task occurrence
            if (!beliefOccurr && taskOccurr && (d.taskStart == ETERNAL) && (d.beliefStart != ETERNAL && d.beliefStart != TIMELESS))
                beliefOccurr = true; //allow belief occurrence

            boolean neg = false;if (preUnneg(x, d)) { x = x.unneg();neg = true; }

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

                return pair(x0, occurrence(d)); //fail-safe

            } else {
                long es = e.start();
                return pair(e.id.negIf(neg),
                        es == TIMELESS ?
                                occurrence(d) :
                                new long[]{es, e.end()});
            }
        }

        public abstract BeliefProjection beliefProjection();
    }

    /** semi-auto-unneg to help occurrify */
    private static boolean preUnneg(Term x, Derivation d) {
        return x instanceof Neg && (!d.taskTerm.hasAny(NEG) && !d.beliefTerm.hasAny(NEG));
    }

    /** ignores temporal subterms ofof --> and <-> */
    static boolean nonTemporal(Term x) {
        return !x.hasAny(Op.Temporal) ||
                (!x.hasXternal() && x.hasAny(INH.bit | SIM.bit) &&
                x.recurseTerms(new Predicate<Compound>() {
                    @Override
                    public boolean test(Compound term) {
                        return term.hasAny(Op.Temporal);
                    }
                }, new BiPredicate<Term, Compound>() {
                    @Override
                    public boolean test(Term term, Compound suuper) {
                        if (term.op().temporal) {
                            if (suuper == null)
                                return false;
                            Op suo = suuper.op();
                            return suo == INH || suo == SIM;
                        }
                        return true;
                    }
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
    public enum BeliefProjection implements Function<Derivation, MutableTruth> {

        /**
         * belief truth evident at its own occurrence time
         */
        Belief {
            @Override
            public MutableTruth apply(Derivation d) {
                return d.beliefTruth_at_Belief;
            }
        },

        /**
         * belief truth projected to task's occurrence time
         */
        Task {
            @Override
            public MutableTruth apply(Derivation d) {
                return d.beliefTruth_at_Task;
            }
        },
        /** avg of Task and Belief */
        Mean {
            @Override
            public MutableTruth apply(Derivation d) {
                return d.beliefTruth_mean_TaskBelief;
            }
        },
//        /** belief
//        Union {
//
//        },
    }

//    private static final Predicate<Derivation> intersection = d ->
//        d.taskBelief_TimeIntersection[0] != TIMELESS;

    private static final Predicate<Derivation> differentTermsOrTimes = new Predicate<Derivation>() {
        @Override
        public boolean test(Derivation d) {
            return !d.taskTerm.equals(d.beliefTerm) || !Tense.simultaneous(d.taskStart, d.beliefStart, (float) d.ditherDT);
        }
    };

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
