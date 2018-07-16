package nars.derive.step;

import jcog.WTF;
import jcog.data.list.FasterList;
import jcog.data.set.ArrayHashSet;
import jcog.math.Longerval;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.derive.Derivation;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.compound.util.Image;
import nars.term.control.AbstractPred;
import nars.term.control.PREDICATE;
import nars.time.Tense;
import nars.time.TimeGraph;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static nars.Op.*;
import static nars.time.Tense.*;
import static org.eclipse.collections.impl.tuple.Tuples.pair;


/**
 * solves a derivation's occurrence time.
 * <p>
 * unknowns to solve otherwise the result is impossible:
 * - derived task start time
 * - derived task end time
 * - dt intervals for any XTERNAL appearing in the input term
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

    public static final TaskTimeMerge mergeDefault =
            TaskTimeMerge.Intersect;
            //TaskTimeMerge.Union;


    public static final ImmutableMap<Term, TaskTimeMerge> merge;

    static {
        MutableMap<Term, TaskTimeMerge> tm = new UnifiedMap<>(8);
        for (TaskTimeMerge m : TaskTimeMerge.values()) {
            tm.put(Atomic.the(m.name()), m);
        }
        merge = tm.toImmutable();
    }


    /** re-used */
    private final transient UnifiedSet<Term> nextNeg = new UnifiedSet<>(8, 0.99f), nextPos = new UnifiedSet(8, 0.99f);
    private final transient MutableSet<Term> autoNegNext = new UnifiedSet<>(8, 0.99f);

    /**
     * temporary set for filtering duplicates
     */
    private final Set<Event> seen = new UnifiedSet(Param.TEMPORAL_SOLVER_ITERATIONS * 2, 0.99f);
    private final Set<Term> expandedUntransforms = new UnifiedSet<>();
    private final Set<Term> expanded = new UnifiedSet<>();

    private final Derivation d;


    private Task curTask;
    private Termed curBelief;
    private Map<Term, Term> prevUntransform = Map.of();
    private boolean curSingle;


    public Occurrify(Derivation d) {
        this.d = d;
    }

    /**
     * heuristic for deciding a derivation result from among the calculated options
     */
    static protected Event merge(Event a, Event b) {
        Term at = a.id;
        Term bt = b.id;
        if (at.hasXternal() && !bt.hasXternal())
            return b;
        if (bt.hasXternal() && !at.hasXternal())
            return a;

        long bstart = b.start();
        if (bstart != TIMELESS) {
            long astart = a.start();
            if (astart == TIMELESS)
                return b;

            if (bstart != ETERNAL && astart == ETERNAL) {
                return b;
            } else if (astart != ETERNAL && bstart == ETERNAL) {
                return a;
            }
        }

        return null;
    }

    @Override
    protected Term dt(Term x, int dt) {
        int ddt = Tense.dither(dt, d.ditherTime);
        Term y = super.dt(x, ddt);
        if (y instanceof Bool && ddt != dt) {

            y = super.dt(x, dt);
        }
        return y;
    }

    @Override
    protected Random random() {
        return d.random;
    }

    public Event know(Termed t, long start, long end) {
        assert (start != TIMELESS);


        Term tt = t.term();


        if (end==start) {
            return event(tt, start, true);
        } else {
            return event(tt, start, end, true);
        }
    }


    public void reset(Term pattern) {



        seen.clear();

        Task task = d.task;
        Term taskTerm = task.term();
        final boolean single = d.concSingle;
        Task belief = !single ? d.belief : null;
        Termed bb = !single ? belief : d.beliefTerm;
        Term beliefTerm = bb.term();

        long taskStart = d.taskStart, taskEnd = d.task.end();
        long beliefStart = single ? TIMELESS : d.beliefStart, beliefEnd = single ? TIMELESS : d.belief.end();


        autoNegNext.clear();

        if (task.hasAny(NEG) || beliefTerm.hasAny(NEG) || pattern.hasAny(NEG)) {

            assert(nextPos.isEmpty() && nextNeg.isEmpty());

            UnifiedSet<Term> pp = nextPos;
            UnifiedSet<Term> nn = nextNeg;

            BiConsumer<Term, Compound> gather = (sub, sup) -> {
                if (sub.op()==NEG) nn.add(sub.unneg());
                else if (sup==null || sup.op()!=NEG) pp.add(sub); //dont add the inner positive unneg'd term of a negation
            };
            pattern.recurseTerms(gather);
            taskTerm.recurseTerms(gather);
            if (!single)
                beliefTerm.recurseTerms(gather);

            pp.intersectInto(nn, autoNegNext);

            pp.clear();
            nn.clear();
        }

        if (!single) {
            boolean taskEte = task.isEternal();
            boolean beliefEte = belief.isEternal();
            if (taskEte && !beliefEte) {
                taskStart = beliefStart; //use belief time for eternal task
                taskEnd = beliefEnd;
            } else if (beliefEte && !taskEte) {
                beliefStart = taskStart; //use task time for eternal belief
                beliefEnd = taskEnd;
            }
        }


        boolean reUse =
                Objects.equals(autoNeg, autoNegNext) &&
                this.curSingle == single &&
                Objects.equals(d.task, curTask) &&
                Objects.equals(bb, curBelief);

        //determine re-usability:
        Map<Term, Term> nextUntransform = d.untransform;

        if (reUse) {
            if (expandedUntransforms.isEmpty())
                prevUntransform = Map.of(); //if expanded was empty then the previous usage's untransforms had no effect, so pretend like they are empty in case they are empty this time then we can re-use the graph

            reUse &= nextUntransform.equals(prevUntransform);
        }

        this.curTask = task; //update to current instance, even if equal
        this.curBelief = bb; //update to current instance, even if equal


        if (!reUse) {
            clear();
            expanded.clear();
            expandedUntransforms.clear();

            autoNeg.clear();
            autoNeg.addAll(autoNegNext);

            this.curSingle = single;

            if (single) {
                Event s = know(task, taskStart, taskEnd);
//                if (taskTerm.op()==IMPL && taskStart!=ETERNAL) {
//                    /* HACK since impl absolute time are not linked,
//                       link here to reify the implication subj as its own event in the single case
//                       this will in turn link the predicate if it is temporally calculable.
//                     */
//                    Term t0 = taskTerm.sub(0);
//                    know(t0, taskStart, taskEnd);
//                    if (taskStart!=ETERNAL && taskStart!=XTERNAL) {
//                        int tdt = taskTerm.dt();
//                        if (tdt != DTERNAL && tdt != XTERNAL) {
//                            long predStart = taskStart + tdt + t0.dtRange();
//                            know(taskTerm.sub(1), predStart, predStart + (taskEnd - taskStart)); //TODO check this for reverse (neg dt) impl
//                        }
//                    }
//                }
//                if (d.concPunc == QUESTION || d.concPunc == QUEST) {
//                    //if doing this then punctuation must be a caching condition
//                    //use the beliefTerm in question/quest cases because there could be timing information
//                    //but in belief/goal derivations, i consider such temporal info potentially interfering with purely single premise derivation
//                    know(beliefTerm);
//                }
            } else {
                know(task, taskStart, taskEnd);

                if (!belief.equals(task) || (taskStart!=beliefStart)) {
                    know(belief, beliefStart, beliefEnd);
                }
            }
            if (!nextUntransform.isEmpty()) {
                this.prevUntransform = Map.copyOf(nextUntransform);

                nextUntransform.forEach((x, y) -> {
                    if (y.isAny(Op.BOOL.bit |  Op.INT.bit))
                        return;
                    link(shadow(x), 0, shadow(y)); //weak
                });
            } else {
                this.prevUntransform = Map.of();
            }

        }
    }


    @Override
    protected void onNewTerm(Term t) {
        if (!expanded.add(t))
            return;

        super.onNewTerm(t);

        Event tt = shadow(t);

        Term u = d.untransform(t);
        if (u!=t && u != null && !(u instanceof Bool) && !u.equals(t)) {
            expandedUntransforms.add(u);
            expanded.add(u);
            link(tt, 0, shadow(u));
        }
        Term v = Image.imageNormalize(t);
        if (v!=t && !v.equals(t)) {
            expanded.add(v);
            link(tt, 0, shadow(v));
        }

    }


    private static Pair<Term, long[]> solveThe(Event event) {
        long es = event.start();
        return pair(event.id,
                es == TIMELESS ? new long[]{TIMELESS, TIMELESS} : new long[]{es, event.end()});
    }

    private final ArrayHashSet<Event> solutions = new ArrayHashSet<>(Param.TEMPORAL_SOLVER_ITERATIONS * 2);
    private int triesRemain = 0;

    private boolean eachSolution(Event solution) {
        assert (solution != null);
        solutions.add(solution);
        return triesRemain-- > 0;
    }

    private ArrayHashSet<Event> solutions(Term pattern) {
        solutions.clear();
        triesRemain = Param.TEMPORAL_SOLVER_ITERATIONS;

        solve(pattern, false /* take everything */, seen, this::eachSolution);

        return solutions;
    }

    private Term solveDT(Term pattern, ArrayHashSet<Event> solutions) {
        Term p;
        int ss = filterOnlyNonXternal(solutions);
        if (ss == 0)
            p = pattern;
        else if (ss == 1) {
            p = solutions.first().id;
        } else {
            p = solutions.get(random()).id;


        }
        return p;
    }

    private Supplier<Pair<Term, long[]>> solveOccDT(ArrayHashSet<Event> solutions) {

        int ss = solutions.size();
        if (ss == 0) {
            return () -> null;
        }

        ss = filterOnlyNonXternal(solutions);

        switch (ss) {
            case 0:
                return () -> null;
            case 1:
                return () -> solveThe(solutions.first());
            default:
                return () -> solveThe(solutions.get(random()));
        }
    }

    private static int filterOnlyNonXternal(ArrayHashSet<Event> solutions) {
        int ss = solutions.size();
        if (ss > 1) {
            int occurrenceSolved = ((FasterList)solutions.list).count(t -> t instanceof Absolute);
            if (occurrenceSolved > 0 && occurrenceSolved < ss) {
                if (solutions.removeIf(t -> t instanceof Relative))
                    ss = solutions.size();
            }
        }
        return ss;
    }

    /**
     * eternal check: conditions under which an eternal result might be valid
     */
    boolean validEternal() {
        return d.task.isEternal() || (d.belief != null && d.belief.isEternal());
    }


    private static final PREDICATE<Derivation> intersectFilter = new AbstractPred<>(Atomic.the("TimeIntersects")) {
        @Override
        public boolean test(Derivation d) {
            return d.concSingle || d.taskBeliefTimeIntersects;
        }
    };

    public enum TaskTimeMerge {

        Task() {
            @Override
            public Pair<Term, long[]> solve(Derivation d, Term x) {
                return solveDT(d, x, d.occ(x));
            }

            @Override
            long[] occurrence(Derivation d) {
                return new long[]{d.task.start(), d.task.end()};
            }

        },

        /** TaskRange is a specialization of Task timing that applies a conj's range to the result, effectively a union across its dt span */
        TaskRange() {
            @Override
            public Pair<Term, long[]> solve(Derivation d, Term x) {
                return solveDT(d, x, d.occ(x));
            }

            @Override
            long[] occurrence(Derivation d) {
                assert(d.taskTerm.op()==CONJ);

                if (!d.task.isEternal()) {
                    int r = d.taskTerm.dtRange();
                    long[] o = new long[]{d.task.start(), r + d.task.end()};


                    if (r > 0 && d.concTruth!=null) {
                        //decrease evidence by proportion of time expanded
                        float ratio = (float)(((double)d.task.range()) / (1 + o[1] - o[0]));
                        if (!d.concTruthEviMul(ratio))
                            return null;
                    }

                    return o;
                } else {
                    return new long[] { ETERNAL, ETERNAL };
                }
            }

        },

        TaskImmediate() {
            @Override
            public Pair<Term, long[]> solve(Derivation d, Term x) {

                Pair<Term, long[]> p = Task.solve(d, x);
                if (p != null) {
                    if (d.concPunc == GOAL) {
                        if (!immediateIfPast(d, p.getTwo()))
                            return null;
                    }
                }
                return p;
            }

            @Override
            long[] occurrence(Derivation d) {
                return new long[]{d.task.start(), d.task.end()};
            }

        },

        /** happens in current present focus. no projection */
        TaskInstant() {
            @Override
            public Pair<Term, long[]> solve(Derivation d, Term x) {

                Pair<Term, long[]> p = Task.solve(d, x);
                if (p != null) {

                    //immediate future, dont interfere with present
                    long[] when = d.nar.timeFocus(d.nar.time() + d.dur * 2);

                    System.arraycopy(when, 0, p.getTwo(), 0, 2);
                }
                return p;
            }

            @Override
            long[] occurrence(Derivation d) {
                return new long[]{d.task.start(), d.task.end()};
            }

        },

//        Belief() {
//            @Override
//            public Pair<Term, long[]> solve(Derivation d, Term x) {
//                Pair<Term, long[]> p = solveDT(d, x, d.occ(x));
//                if (p != null) {
//                    if (d.concPunc == GOAL) {
//                        immediateIfPast(d, p.getTwo());
//                    }
//                }
//                return p;
//            }
//
//            @Override
//            long[] occurrence(Derivation d) {
//                return new long[]{d.belief.start(), d.belief.end()};
//            }
//
//            @Override
//            public BeliefProjection projection() {
//                return BeliefProjection.Raw;
//            }
//        },

        TaskPlusBeliefDT() {
            @Override
            public Pair<Term, long[]> solve(Derivation d, Term x) {
                return solveShiftBeliefDT(d, solveDT(d, x, d.occ(x)), +1);
            }

            @Override
            long[] occurrence(Derivation d) {
                return new long[]{d.task.start(), d.task.end()};
            }

        },
        TaskMinusBeliefDT() {
            @Override
            public Pair<Term, long[]> solve(Derivation d, Term x) {
                return solveShiftBeliefDT(d, solveDT(d, x, d.occ(x)), -1);
            }


            @Override
            long[] occurrence(Derivation d) {
                return new long[]{d.task.start(), d.task.end()};
            }
        },

        /**
         * for unprojected truth rules;
         * result should be left-aligned (relative) to the task's start time
         * used by temporal induction rules
         */
        TaskRelative() {
            @Override
            public Pair<Term, long[]> solve(Derivation d, Term x) {
                return solveDT(d, x, d.occ(x));
            }

            @Override
            long[] occurrence(Derivation d) {
                Task task = d.task;
                return new long[]{task.start(), task.end()};
            }

            @Override
            public BeliefProjection beliefProjection() {
                return BeliefProjection.Raw;
            }

        },
        /**
         * for unprojected truth rules;
         * result should be left-aligned (relative) to the belief's start time
         */
        BeliefRelative() {
            @Override
            public Pair<Term, long[]> solve(Derivation d, Term x) {
                return solveDT(d, x, d.occ(x));
            }

            @Override
            long[] occurrence(Derivation d) {
                Task belief = d.belief;
                return new long[]{belief.start(), belief.end()};
            }

            @Override
            public BeliefProjection beliefProjection() {
                return BeliefProjection.Raw;
            }

        },

        /**
         * unprojected
         */
        Relative() {
            @Override
            public Pair<Term, long[]> solve(Derivation d, Term x) {
                return solveOccDT(d, x, d.occ(x));
            }

            @Override
            long[] occurrence(Derivation d) {
                return null;
            }

            @Override
            public BeliefProjection beliefProjection() {
                return BeliefProjection.Raw;
            }

        },
        /**
         * result occurs in the intersecting time interval, if exists; otherwise fails
         */
        Intersect() {

            @Override
            public Pair<Term, long[]> solve(Derivation d, Term x) {
                //return solveOccDTWithGoalOverride(d, x);
                return solveOccDT(d, x, d.occ(x));
            }

            @Override
            public PREDICATE<Derivation> filter() {
                return intersectFilter;
            }

            @Override
            long[] occurrence(Derivation d) {
                Longerval i = Longerval.intersect(d.task.start(), d.task.end(), d.belief.start(), d.belief.end());
                if (i == null) {
                    if (Param.DEBUG)
                        throw new WTF("shouldnt happen");
                    return null;
                }
                return new long[]{i.a, i.b};
            }
            @Override
            public BeliefProjection beliefProjection() {
                return BeliefProjection.Raw;
            }
        },

        /**
         * result occurs in the union time interval, and this always exists.
         * the evidence integration applied in the truth calculation should
         * reflect the loss of evidence from any non-intersecting time ranges
         */
        Union() {
            @Override
            public Pair<Term, long[]> solve(Derivation d, Term x) {
                //return solveOccDTWithGoalOverride(d, x);
                return solveOccDT(d, x, d.occ(x));
            }


            @Override
            long[] occurrence(Derivation d) {
                Longerval i = Longerval.union(d.task.start(), d.task.end(), d.belief.start(), d.belief.end());
                return new long[]{i.a, i.b};
            }

        };

        private final Term term;

        TaskTimeMerge() {
            this.term = Atomic.the(name());
        }

//        private long[] taskOccurrenceIfNotEternalElseSolve(Derivation d) {
//            long start = d.task.start();
//            if (start != ETERNAL || d.belief == null || d.belief.isEternal())
//                return new long[]{start, d.task.end()};
//            else {
//                solveOccDT()
//            }
//        }

        static Pair<Term, long[]> solveShiftBeliefDT(Derivation d, Pair<Term, long[]> p, int sign) {
            if (p == null)
                return null;


            int bdt = d.beliefTerm.dt();
            if (bdt != DTERNAL && bdt != XTERNAL) {

                long[] o = p.getTwo();
                long s = o[0];
                if (s != TIMELESS && s != ETERNAL) {
                    bdt *= sign;


                    if (sign == 1) {
                        bdt += d.beliefTerm.sub(0).dtRange(); //impl subj dtRange
                    } else if (sign == -1) {
                        bdt -= d.beliefTerm.sub(0).dtRange(); //impl subj dtRange
                    }

                    o[0] += bdt;
                    o[1] += bdt;

                    if (d.concPunc==GOAL)
                        if (!immediateIfPast(d, o))
                            return null;
                }
            }
            return p;
        }

        private static boolean immediateIfPast(Derivation d, long[] o) {

            if (o[0] == ETERNAL) {
                //convert eternal to present
                //System.arraycopy(d.nar.timeFocus(), 0, o, 0, 2);
            } else  {
                long NOW = d.time;
                if (o[0] < NOW) {
                    if (NOW <= o[1]) {
                        //NOW is contained in the interval
                    } else {
                        //entirely within the past:
                        // shift and project to present, "as-if" past-perfect/subjunctive tense
                        long deltaToStart = Math.abs(NOW - o[0]);
                        long deltaToEnd = Math.abs(NOW - o[1]);
                        long delta = Math.min(deltaToStart, deltaToEnd);
                        if (delta > 0) {
                            //discount for projection
                            float e = (float) Param.evi(d.concTruth.evi(), delta, d.dur);
                            if (!d.concTruthEvi(e)) {
                                return false;
                            }
                        }

                        System.arraycopy(d.nar.timeFocus(), 0, o, 0, 2);

//                        long range = o[1] - o[0];
//                        o[0] = NOW;
//                        o[1] = NOW + range;

                    }
                }
            }
            return true;
        }

        /**
         * fallback
         */
        abstract long[] occurrence(Derivation d);

        public Term term() {
            return term;
        }

        abstract public Pair<Term, long[]> solve(Derivation d, Term x);

        protected Pair<Term, long[]> solveOccDT(Derivation d, Term x, Occurrify o) {
            ArrayHashSet<Event> solutions = o.solutions(x);
            Pair<Term, long[]> p = o.solveOccDT(solutions).get();
            if (p != null && p.getTwo()[0] == TIMELESS) {

                x = p.getOne();
                p = null;
            }
            return p == null ? solveAuto(d, x) : p;
        }

//        protected Pair<Term, long[]> solveOccDTWithGoalOverride(Derivation d, Term x) {
//
//            if (d.concPunc == GOAL) {
//                return Task.solve(d, x);
//            }
//
//            return solveOccDT(d, x, d.occ(x));
//        }

        /**
         * gets the optional premise pre-filter for this consequence.
         */
        @Nullable
        public PREDICATE<Derivation> filter() {
            return null;
        }

        @Nullable protected Pair<Term, long[]> solveDT(Derivation d, Term x, Occurrify o) {
            ArrayHashSet<Event> solutions = o.solutions(x);
            Term p = o.solveDT(x, solutions);
            if (p == null)
                p = x;

            long[] occ = occurrence(d);
            return occ == null ? null : pair(p, occ);
        }

        /**
         * failsafe mode
         */
        public Pair<Term, long[]> solveAuto(Derivation d, Term x) {

            if ((d.concPunc == BELIEF || d.concPunc == GOAL) && x.hasXternal())
                return null;

            Task task = d.task;
            Task belief = d.concSingle ?  null : d.belief;
            long s, e;
        /*if (task.isQuestOrQuestion() && (!task.isEternal() || belief == null)) {
            
            s = task.start();
            e = task.end();
        } else*/
            boolean taskConj =
                    !(task.term().op() == CONJ);

            if (task.isEternal()) {
                if (belief == null || belief.isEternal()) {

                    s = e = ETERNAL;
                } else {
                    if (taskConj) {
                        s = belief.start();
                        e = belief.end();
                    } else {

                        return null;
                    }
                }
            } else {
                if (belief == null) {

                    s = task.start();
                    e = task.end();

                } else if (belief.isEternal()) {
                    if (!task.isEternal()) {

                        s = task.start();
                        e = task.end();
                    } else {
                        s = e = ETERNAL;
                    }


                } else {
                    byte p = d.concPunc;
                    if ((p == BELIEF || p == GOAL)) {
                        boolean taskEvi = !task.isQuestionOrQuest();
                        boolean beliefEvi = !belief.isQuestionOrQuest();
                        if (taskEvi && beliefEvi) {
                            long[] u = occurrence(d);
                            if (u != null) {
                                s = u[0];
                                e = u[1];
                            } else {
                                return null;
                            }
                        } else if (taskEvi) {
                            s = task.start();
                            e = task.end();
                        } else if (beliefEvi) {
                            s = belief.start();
                            e = belief.end();
                        } else {
                            throw new UnsupportedOperationException("evidence from nowhere?");
                        }
                    } else {

                        Longerval u = Longerval.union(task.start(), task.end(), belief.start(), belief.end());
                        s = u.start();
                        e = u.end();
                    }
                }
            }


            return pair(x, new long[]{s, e});

        }

        public BeliefProjection beliefProjection() {
            return BeliefProjection.Task;
        }
    }


    /** TODO do derivation truth calculation in implemented method of this enum */
    public enum BeliefProjection {

        /**
         * belief's truth at the time it occurred
         */
        Raw {

        },

        /**
         * belief projected to task's occurrence time
         */
        Task {

        },

//        /** belief
//        Union {
//
//        },
    }
}
















































































































