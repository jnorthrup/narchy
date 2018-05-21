package nars.derive.step;

import jcog.data.ArrayHashSet;
import jcog.list.FasterList;
import jcog.math.Longerval;
import nars.Param;
import nars.Task;
import nars.derive.Derivation;
import nars.term.Term;
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
            //TaskTimeMerge.Intersect;

    public static final ImmutableMap<Term, TaskTimeMerge> merge;
    static {
        MutableMap<Term,TaskTimeMerge> tm = new UnifiedMap<>(8);
        for (TaskTimeMerge m : TaskTimeMerge.values()){
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

        return null; //save both as alternates
    }

    @Override
    protected Term dt(Term x, int dt) {
        int ddt = Tense.dither(dt, d.ditherTime);
        Term y = super.dt(x, ddt);
        if (y instanceof Bool && ddt != dt) {
            //the dithered dt has destroyed it, so try the non-dithered (more precise) dt
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
        //assert (when != TIMELESS);

        Term tt = t.term();

        long range;
        if (when == ETERNAL || t.isEternal() || (range = t.range() - 1) == 0) {
            event(tt, when, true);
        } else {
            event(tt, when, when + range, true);
        }
    }

//    private int knowIfSameTruth(Task t, Term tt, Truth tr, long w, LongHashSet sampled) {
//        if (sampled.add(w)) {
//            if (t.isQuestionOrQuest() || tr.equalsIn(t.truth(w, d.dur), d.nar)) {
//                event(tt, w, true);
//                return 1;
//            }
//        }
//        return 0;
//    }


//    public void know(Task t) {
//        Iterable<Event> ee = know(t, t.term());
//        //both positive and negative possibilities
////        if (autoNegEvents && tt.op() != CONJ) {
////            for (Event e : ee)
////                link(know(tt.neg()), 0, e);
////        }
//    }
//
//    private Iterable<Event> know(Task task, Term term) {
//        long start = task.start();
//        long end = task.end();
//        if (end != start && (end - start) >= d.dur) {
//            //add each endpoint separately
//            return List.of(
//                    event(term, start, true),
//                    event(term, end, true));
//        } else {
//            return Collections.singleton(event(term, start == ETERNAL ? ETERNAL : (start + end) / 2 /* midpoint */, true));
//        }
//    }

    public Occurrify reset() {
        clear();
        expanded.clear();
        seen.clear();
        autoNeg(false);
        return this;
    }


    public Occurrify reset(boolean autoNeg) {
        reset();

        Task task = d.task;
        boolean single = d.single;
        Task belief = !single ? d.belief : null;

        Term bb = !single ? belief.term() : d.beliefTerm;

        //disable autoneg if no negations appear in the premise
        if (!autoNeg && (task.term().hasAny(NEG) || bb.hasAny(NEG))) {
            autoNeg = true;
        }

        this.autoNeg(autoNeg);

        if (single) {
            know(task, d.taskAt);
        } else {

            boolean taskEte = task.isEternal();
            boolean beliefEte = belief.isEternal();
            if (taskEte && !beliefEte) {
                //if belief is non-eternal against an eternal goal, pretend the goal occurrs now to provide an overriding time for the belief
                know(task, d.beliefAt);
            } else {
                know(task, d.taskAt);
            }

            if (!d.belief.equals(d.task)) {
                if (beliefEte && !taskEte) {
                    know(belief, d.taskAt);
                } else {
                    know(belief, d.beliefAt);
                }
            }
        }

        return this;

    }



    @Override
    protected void onNewTerm(Term t) {
        if (!expanded.add(t))
            return; //already linked

        super.onNewTerm(t);

        Event tt = shadow(t);

        Term u = d.untransform(t);
        if (u != null && !(u instanceof Bool) && !u.equals(t)) {
            expanded.add(u); //HACK prevent infinite loop
            link(tt, 0, shadow(u));
        }
        Term v = Image.imageNormalize(t);
        if (!v.equals(t)) {
            expanded.add(v); //HACK prevent infinite loop
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


        //if (!join.occOverride() || pattern.hasXternal())
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
            //TODO
            //find the most common non-XTERNAL containing solution term result
        }
        return p;
    }

    protected Supplier<Pair<Term, long[]>> solveOccDT(ArrayHashSet<Event> solutions) {

        int ss = solutions.size();
        if (ss == 0) {
            return ()->null;
        }

        //can only prefer occurrence range if the events all have the same term
//        if (ss > 1) { //prefer occurence range
//            int occurrenceSolvedRange = ((FasterList<Event>) solutions.list).count(t -> t instanceof AbsoluteRange);
//            if (occurrenceSolvedRange > 0 && occurrenceSolvedRange < ss) {
//                if (solutions.removeIf(t -> !(t instanceof AbsoluteRange))) //filter non-ranged
//                    ss = solutions.size();
//            }
//        }

        ss = filterOnlyNonXternal(solutions);

        switch (ss) {
            case 0:
                return () -> null;
            case 1:
                return () -> solveThe(solutions.first());
            default:

                //return solveRandomOne(solutions);
                //FasterList<Event> solutionsCopy = new FasterList(solutions.list);

//                Function<long[], Term> tt = solveMerged(solutions, d.dur);
//                if (tt == null) {
                return () -> solveThe(solutions.get(random())); //choose one at random
//                } else {
//                    long[] when = new long[]{TIMELESS, TIMELESS};
//                    Term ttt = tt.apply(when);
//                    if (ttt == null || when[0] == TIMELESS)
//                        return null; //should not happen
//                    return () -> {
//                        d.concOcc[0] = when[0];
//                        d.concOcc[1] = when[1];
//                        return ttt;
//                    };
//                }
        }
    }

    private int filterOnlyNonXternal(ArrayHashSet<Event> solutions) {
        int ss = solutions.size();
        if (ss > 1) { //prefer occurrence point
            int occurrenceSolved = ((FasterList<Event>) solutions.list).count(t -> t instanceof Absolute);
            if (occurrenceSolved > 0 && occurrenceSolved < ss) {
                if (solutions.removeIf(t -> t instanceof Relative)) //filter timeless
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
                Pair<Term, long[]> p = solveDT(d, x, d.occ(x));
                if (p!=null) {
                    if (d.concPunc==GOAL) {
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
        Belief() {
            @Override
            public Pair<Term, long[]> solve(Derivation d, Term x) {
                Pair<Term, long[]> p = solveDT(d, x, d.occ(x));
                if (p!=null) {
                    if (d.concPunc==GOAL) {
                        immediateIfPast(d, p.getTwo());
                    }
                }
                return p;
            }

            @Override
            long[] occurrence(Derivation d) {
                return new long[]{d.belief.start(), d.belief.end()};
            }

        },

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

            @Override public BeliefProjection projection() {
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

            @Override public BeliefProjection projection() {
                return BeliefProjection.Raw;
            }

        },

        /** unprojected */
        Relative() {
            @Override
            public Pair<Term, long[]> solve(Derivation d, Term x) {
                return solveOccDT(d, x, d.occ(x));
            }

            @Override
            long[] occurrence(Derivation d) {
                return null;
            }

            @Override public BeliefProjection projection() {
                return BeliefProjection.Raw;
            }

        },
        /**
         * result occurs in the intersecting time interval, if exists; otherwise fails
         */
        Intersect() {

            final PrediTerm<Derivation> filter = new AbstractPred<>(Atomic.the("TimeIntersects")) {
                @Override
                public boolean test(Derivation derivation) {
                    nars.Task b = derivation._belief;
                    return b!=null && derivation._task.intersectsTime(b);
                }
            };

            @Override public Pair<Term, long[]> solve(Derivation d, Term x) {
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
            @Override public Pair<Term, long[]> solve(Derivation d, Term x) {
                return solveOccDTWithGoalOverride(d, x, d.occ(x));
            }



            @Override
            long[] occurrence(Derivation d) {
                Longerval i = Longerval.union(d.task.start(), d.task.end(), d.belief.start(), d.belief.end());
                return i != null ? new long[]{i.a, i.b} : null;
            }
        };

        private static long[] taskOccurrenceIfNotEternalElseNow(Derivation d) {
            long start = d.task.start();
            if (start != ETERNAL || d.belief == null || d.belief.isEternal())
                return new long[]{start, d.task.end()};
            else {
                //task is eternal and belief is non-eternal. so to prevent deriving eternal belief, use now as the focus point.
                return d.nar.timeFocus();
            }
        }

        static Pair<Term, long[]> solveShiftBeliefDT(Derivation d, Pair<Term, long[]> p, int sign) {
            if (p == null)
                return null;

            int bdt = d.beliefTerm.dt();
            if (bdt!=DTERNAL && bdt!=0 && bdt!=XTERNAL) {

                long[] o = p.getTwo();
                long s = o[0];
                if (s != TIMELESS && s != ETERNAL) {
                    bdt *=sign;
                    o[0] += bdt;
                    o[1] += bdt;

                    immediateIfPast(d, o);
                }
            }
            return p;
        }

        private static void immediateIfPast(Derivation d, long[] o) {
            //immediate immenantize
            long NOW = d.time;
            if (o[0] < NOW) {
                long delta = o[1] - o[0];
                o[0] = NOW;
                o[1] = NOW + delta;
            }
        }

        private final Term term;

        TaskTimeMerge() {
            this.term = Atomic.the(name());
        }

        /** fallback */
        abstract long[] occurrence(Derivation d);

        public Term term() {
            return term;
        }

        abstract public Pair<Term, long[]> solve(Derivation d, Term x);

        protected Pair<Term, long[]> solveOccDT(Derivation d, Term x, Occurrify o) {
            ArrayHashSet<Event> solutions = o.solutions(x);
            Pair<Term, long[]> p = o.solveOccDT(solutions).get();
            if (p!=null && p.getTwo()[0]==TIMELESS) {
                //get the term it computed possible DT for
                x = p.getOne();
                p = null;
            }
            return p == null ? solveAuto(d, x) : p;
        }

        protected Pair<Term, long[]> solveOccDTWithGoalOverride(Derivation d, Term x, Occurrify occ) {
            //override for goal
            if (d.concPunc == GOAL) { //HACK
                return Task.solve(d, x);
            }

            return solveOccDT(d, x, d.occ(x));
        }

        /** gets the optional premise pre-filter for this consequence.  */
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

        /** failsafe mode */
        public Pair<Term, long[]> solveAuto(Derivation d, Term x) {

            if ((d.concPunc == BELIEF || d.concPunc == GOAL) && x.hasXternal())
                return null; //it would be invalid task or goal

            Task task = d.task;
            Task belief = d.belief;
            long s, e;
        /*if (task.isQuestOrQuestion() && (!task.isEternal() || belief == null)) {
            //inherit question's specific time directly
            s = task.start();
            e = task.end();
        } else*/
            boolean taskEvent =
                    //!task.term().op().temporal;
                    !(task.term().op() == CONJ);

            if (task.isEternal()) {
                if (belief == null || belief.isEternal()) {
                    //entirely eternal
                    s = e = ETERNAL;
                } else {
                    if (taskEvent) {
                        s = belief.start();
                        e = belief.end();
                    } else {
                        //transformed task term, should have been solved
                        return null;
                    }
                }
            } else {
                if (belief == null) {
                    //inherit task time
                    s = task.start();
                    e = task.end();

                } else if (belief.isEternal()) {
                    if (!task.isEternal()) {
                        //inherit task time
                        s = task.start();
                        e = task.end();
                    } else {
                        s = e = ETERNAL;
                    }
                    //                    //event: inherit task time
                    //                    boolean beliefEvent = belief == null || (
                    //                            !belief.term().op().temporal
                    //                    );
                    //                    if (beliefEvent) {
                    //                        s = task.start();
                    //                        e = task.end();
                    //                    } else {
                    //                        return null; //should have calculated solution normally
                    //                    }
                    //                }
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
                        //question: use the interval union - does this even happen
                        Longerval u = Longerval.union(task.start(), task.end(), belief.start(), belief.end());
                        s = u.start();
                        e = u.end();
                    }
                }
            }

//        //couldnt solve the start time, so inherit from task or belief as appropriate
//        if (!d.single && !te && (belief != null && !belief.isEternal())) {
//
//                //joint is a procedure for extending / blending non-temporal terms.
//                //since conj is temporal use strict
//                boolean strict = x.op()==CONJ;
//
//                if (strict) {
//                    Interval ii = Interval.intersect(task.start(), task.end(), belief.start(), belief.end());
//                    if (ii == null)
//                        return null; //too distant, evidence lacks
//
//                    s = ii.a;
//                    e = x.op()!=IMPL ? ii.b : ii.a;
//                } else {
//                    TimeFusion joint = new TimeFusion(task.start(), task.end(), belief.start(), belief.end());
////                    if (joint.factor <= Pri.EPSILON) //allow for questions/quests, if this ever happens
////                        return null;
//
//                    s = joint.unionStart;
//                    e = joint.unionEnd;
//                    d.concEviFactor *= joint.factor;
//                }
//
//
//        } else if (d.single || (!te && (belief == null || belief.isEternal()))) {
//            s = task.start();
//            e = task.end();
//        } else {
//            s = belief.start();
//            e = belief.end();
//        }

//                if (!eternalCheck(s))
//                    return null;

            return pair(x, new long[]{s, e});

        }

        public BeliefProjection projection() {
            return BeliefProjection.Task;
        }
    }


    public enum BeliefProjection {

        /** belief's truth at the time it occurred */
        Raw {

        },

        /** belief projected to task's occurrence time */
        Task {

        },

    }
}
//        //prefer a term which is not a repeat of the task or belief term
//        boolean aMatch = a.id.equals(d.taskTerm) || a.id.equals(d.beliefTerm);
//        boolean bMatch = b.id.equals(d.taskTerm) || b.id.equals(d.beliefTerm);
//        if (aMatch && !bMatch)
//            return b;
//        if (!aMatch && bMatch)
//            return a;

//        //heuristic:
//        float aSpec =
//                //((float) at.volume()) / (1+at.dtRange()); //prefer more specific "dense" temporal events rather than sprawling sparse run-on-sentences
//                at.volume(); //prefer volume
//        float bSpec =
//                //((float) bt.volume()) / (1+bt.dtRange());
//                bt.volume();
//        if (bSpec > aSpec)
//            return b;
//        else //if (aSpec < bSpec)
//            return a;
//        else {
//            //long distToNow = ...
//        }

//        Term tRoot = at.root();
//        if (!at.equals(tt)) {
//            score++;
//            if (!tRoot.equals(tt.root()))
//                score++;
//        }
//
//        if (!at.equals(bb)) {
//            score++;
//            if (!tRoot.equals(bb.root()))
//                score++;
//        }

//    }


//    @Override
//    protected Random random() {
//        return d.random;
//    }

//    void know(Termed x) {
//
//        Term xterm = x.term();
//        if (x instanceof Task)
//            know((Task) x);
//        else
//            know(xterm);
//
//    }

//
//}
//
////            Map<Term, LongHashSet> uniques = new HashMap();
////            alternates.forEach(x -> {
////                long w = x.when();
////                if (w!=TIMELESS && w!=ETERNAL)
////                    uniques.computeIfAbsent(x.id, xx -> new LongHashSet()).add(w);
////            });
////            if (!uniques.isEmpty()) {
////                //all alternates of the same term but at different points; so stretch a solution containing all of them
////
////                ArrayHashSet<Map.Entry<Term, LongHashSet>> uu = new ArrayHashSet<>(uniques.entrySet());
////                Map.Entry<Term, LongHashSet> h = uu.get(d.random);
////
////                Term st = h.getKey();
////                LongHashSet s = h.getValue();
////                occ[0] = s.min();
////                occ[1] = s.max() + st.dtRange();
////                return st;
////            } else {
//
//            if (d.single ? d.task.isEternal() : d.eternal) {
//                    event = solutions.get(d.random); //doesnt really matter which solution is chosen, in terms of probability of projection success
//                    } else {
//
//
//                    //choose event with least distance to task and belief occurrence so that projection has best propensity for non-failure
//
//                    //solutions.shuffle(d.random); //shuffle so that equal items are selected fairly
//
////                /* weight the influence of the distance to each
////                   according to its weakness (how likely it is to null during projection). */
////                float taskWeight =
////                        //task.isBeliefOrGoal() ? (0.5f + 0.5f * (1f - task.conf())) : 0f;
////                        0.5f;
////                float beliefWeight =
////                        //belief!=null ? (0.5f + 0.5f * (1f - belief.conf())) : 0;
////                        0.5f;
////
////                final float base = 1f/solutions.size();
////                event = solutions.roulette((e) -> {
////                    long when = e.when();
////                    if (when == TIMELESS)
////                        return base/2f;
////                    if (when == ETERNAL)
////                        return base; //prefer eternal only if a temporal solution does not exist
////
////                    long distance = 1;
////                    distance += task.minDistanceTo(when) * taskWeight;
////
////                    if (!d.single && belief!=null)
////                        distance += belief.minDistanceTo(when) * beliefWeight;
////
////                    return 1f/distance;
////                }, d.nar.random());
//                    }
////            }
//                    }