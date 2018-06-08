package nars.derive.step;

import jcog.data.ArrayHashSet;
import jcog.math.Longerval;
import nars.Param;
import nars.Task;
import nars.derive.Derivation;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.compound.util.Image;
import nars.term.control.AbstractPred;
import nars.term.control.PrediTerm;
import nars.time.Tense;
import nars.time.TimeGraph;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;
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
            TaskTimeMerge.Union;


    public static final ImmutableMap<Term, TaskTimeMerge> merge;

    static {
        MutableMap<Term, TaskTimeMerge> tm = new UnifiedMap<>(8);
        for (TaskTimeMerge m : TaskTimeMerge.values()) {
            tm.put(Atomic.the(m.name()), m);
        }
        merge = tm.toImmutable();
    }


    /**
     * temporary set for filtering duplicates
     */
    final Set<Event> seen = new UnifiedSet(Param.TEMPORAL_SOLVER_ITERATIONS * 2);
    final Set<Term> expanded = new UnifiedSet<>();

    final Derivation d;


    private Task curTask;
    private long curTaskAt = XTERNAL, curBeliefAt = XTERNAL;
    private Termed curBelief;

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

    public void know(Task t, long when) {
        assert (when != TIMELESS);


        Term tt = t.term();

        long range;
        if (when == ETERNAL || t.isEternal() || (range = t.range() - 1) == 0) {
            event(tt, when, true);
        } else {
            event(tt, when, when + range, true);
        }
    }


    public void reset(boolean autoNeg) {
        seen.clear();

        Task task = d.task;
        boolean single = d.single;
        Task belief = !single ? d.belief : null;
        long beliefAt = single ? XTERNAL : d.beliefAt;
        long taskAt = d.taskAt;
        Termed bb = !single ? belief : d.beliefTerm;


        if (!autoNeg && (task.term().hasAny(NEG) || (!single && bb.hasAny(NEG)))) {
            autoNeg = true;
        }

        if (!single) {
            boolean taskEte = task.isEternal();
            boolean beliefEte = belief.isEternal();
            if (taskEte && !beliefEte) {
                taskAt = beliefAt; //use belief time for eternal task
            } else if (beliefEte && !taskEte) {
                beliefAt = taskAt; //use task time for eternal belief
            }
        }

        //determine re-usability:
        boolean reUse =
                autoNeg == this.autoNeg && this.curBeliefAt == beliefAt && this.curTaskAt == taskAt && Objects.equals(d.task, curTask) && Objects.equals(bb, curBelief);

        this.curTask = task; //update to current instance, even if equal
        this.curBelief = bb; //update to current instance, even if equal


        if (!reUse) {
            clear();
            expanded.clear();
            this.autoNeg(autoNeg);
            this.curBeliefAt = beliefAt;
            this.curTaskAt = taskAt;

            if (single) {
                know(task, taskAt);
//                if (!task.term().equals(bb))
//                    know((Term)bb);
            } else {
                know(task, taskAt);

                if (!belief.equals(task) || (taskAt!=beliefAt)) {
                    know(belief, beliefAt);
                }
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
        if (u != null && !(u instanceof Bool) && !u.equals(t)) {
            expanded.add(u);
            link(tt, 0, shadow(u));
        }
        Term v = Image.imageNormalize(t);
        if (!v.equals(t)) {
            expanded.add(v);
            link(tt, 0, shadow(v));
        }

    }


    Pair<Term, long[]> solveThe(Event event) {
        long es = event.start();
        return pair(event.id,
                es == TIMELESS ? new long[]{TIMELESS, TIMELESS} : new long[]{es, event.end()});
    }


    public ArrayHashSet<Event> solutions(Term pattern) {
        ArrayHashSet<Event> solutions = new ArrayHashSet<>(Param.TEMPORAL_SOLVER_ITERATIONS * 2);

        final int[] triesRemain = {Param.TEMPORAL_SOLVER_ITERATIONS};

        Predicate<Event> each = (solution) -> {
            assert (solution != null);
            solutions.add(solution);
            return triesRemain[0]-- > 0;
        };


        solve(pattern, false /* take everything */, seen, each);

        return solutions;

    }

    protected Term solveDT(Term pattern, ArrayHashSet<Event> solutions) {
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

    protected Supplier<Pair<Term, long[]>> solveOccDT(ArrayHashSet<Event> solutions) {

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

    private int filterOnlyNonXternal(ArrayHashSet<Event> solutions) {
        int ss = solutions.size();
        if (ss > 1) {
            int occurrenceSolved = solutions.list.count(t -> t instanceof Absolute);
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
        TaskImmediate() {
            @Override
            public Pair<Term, long[]> solve(Derivation d, Term x) {
                Pair<Term, long[]> p = Task.solve(d, x);
                if (p != null) {
                    if (d.concPunc == GOAL) {
                        immediateIfPast(d, p.getTwo());
                    }
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
//        },

        TaskPlusBeliefDT() {
            @Override
            public Pair<Term, long[]> solve(Derivation d, Term x) {
                return solveShiftBeliefDT(d, solveDT(d, x, d.occ(x)), +1);
            }

            @Override
            long[] occurrence(Derivation d) {
                return taskOccurrenceIfNotEternalElseNow(d);
            }

        },
        TaskMinusBeliefDT() {
            @Override
            public Pair<Term, long[]> solve(Derivation d, Term x) {
                return solveShiftBeliefDT(d, solveDT(d, x, d.occ(x)), -1);
            }


            @Override
            long[] occurrence(Derivation d) {
                return taskOccurrenceIfNotEternalElseNow(d);
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
            public BeliefProjection projection() {
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
            public BeliefProjection projection() {
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
            public BeliefProjection projection() {
                return BeliefProjection.Raw;
            }

        },
        /**
         * result occurs in the intersecting time interval, if exists; otherwise fails
         */
        Intersect() {

            final PrediTerm<Derivation> filter = new AbstractPred<Derivation>(Atomic.the("TimeIntersects")) {
                @Override
                public boolean test(Derivation derivation) {
                    nars.Task b = derivation._belief;
                    return b != null && derivation._task.intersectsTime(b);
                }
            };

            @Override
            public Pair<Term, long[]> solve(Derivation d, Term x) {
                return solveOccDTWithGoalOverride(d, x, d.occ(x));
            }

            @Override
            public PrediTerm<Derivation> filter() {
                return filter;
            }

            @Override
            long[] occurrence(Derivation d) {
                Longerval i = Longerval.intersect(d.task.start(), d.task.end(), d.belief.start(), d.belief.end());
                if (i == null)
                    throw new RuntimeException("should have been filtered");
                return new long[]{i.a, i.b};
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
                return solveOccDTWithGoalOverride(d, x, d.occ(x));
            }


            @Override
            long[] occurrence(Derivation d) {
                Longerval i = Longerval.union(d.task.start(), d.task.end(), d.belief.start(), d.belief.end());
                return i != null ? new long[]{i.a, i.b} : null;
            }
        };

        private final Term term;

        TaskTimeMerge() {
            this.term = Atomic.the(name());
        }

        private static long[] taskOccurrenceIfNotEternalElseNow(Derivation d) {
            long start = d.task.start();
            if (start != ETERNAL || d.belief == null || d.belief.isEternal())
                return new long[]{start, d.task.end()};
            else {

                return d.nar.timeFocus();
            }
        }

        static Pair<Term, long[]> solveShiftBeliefDT(Derivation d, Pair<Term, long[]> p, int sign) {
            if (p == null)
                return null;

            int bdt = d.beliefTerm.dt();
            if (bdt != DTERNAL && bdt != 0 && bdt != XTERNAL) {

                long[] o = p.getTwo();
                long s = o[0];
                if (s != TIMELESS && s != ETERNAL) {
                    bdt *= sign;
                    o[0] += bdt;
                    o[1] += bdt;

                    immediateIfPast(d, o);
                }
            }
            return p;
        }

        private static void immediateIfPast(Derivation d, long[] o) {
            if (o[0] != ETERNAL) {
                long NOW = d.time;
                if (o[0] < NOW) {
                    long delta = o[1] - o[0];
                    o[0] = NOW;
                    o[1] = NOW + delta;
                }
            }
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

        protected Pair<Term, long[]> solveOccDTWithGoalOverride(Derivation d, Term x, Occurrify occ) {

            if (d.concPunc == GOAL) {
                return Task.solve(d, x);
            }

            return solveOccDT(d, x, d.occ(x));
        }

        /**
         * gets the optional premise pre-filter for this consequence.
         */
        @Nullable
        public PrediTerm<Derivation> filter() {
            return null;
        }

        protected Pair<Term, long[]> solveDT(Derivation d, Term x, Occurrify o) {
            ArrayHashSet<Event> solutions = o.solutions(x);
            Term p = o.solveDT(x, solutions);
            if (p == null)
                p = x;

            return pair(p, occurrence(d));
        }

        /**
         * failsafe mode
         */
        public Pair<Term, long[]> solveAuto(Derivation d, Term x) {

            if ((d.concPunc == BELIEF || d.concPunc == GOAL) && x.hasXternal())
                return null;

            Task task = d.task;
            Task belief = d.belief;
            long s, e;
        /*if (task.isQuestOrQuestion() && (!task.isEternal() || belief == null)) {
            
            s = task.start();
            e = task.end();
        } else*/
            boolean taskEvent =

                    !(task.term().op() == CONJ);

            if (task.isEternal()) {
                if (belief == null || belief.isEternal()) {

                    s = e = ETERNAL;
                } else {
                    if (taskEvent) {
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

        public BeliefProjection projection() {
            return BeliefProjection.Task;
        }
    }


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

    }
}
















































































































