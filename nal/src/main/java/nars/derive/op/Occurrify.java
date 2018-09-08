package nars.derive.op;

import jcog.data.graph.FromTo;
import jcog.data.graph.Node;
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
import nars.term.control.AbstractPred;
import nars.term.control.PREDICATE;
import nars.term.util.Conj;
import nars.term.util.Image;
import nars.time.Event;
import nars.time.Tense;
import nars.time.TimeGraph;
import nars.time.TimeSpan;
import nars.truth.func.NALTruth;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.BooleanObjectPair;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static jcog.WTF.WTF;
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
            //TaskTimeMerge.Intersect;
            TaskTimeMerge.Default;


    public static final ImmutableMap<Term, TaskTimeMerge> merge;

    static {
        MutableMap<Term, TaskTimeMerge> tm = new UnifiedMap<>(8);
        for (TaskTimeMerge m : TaskTimeMerge.values()) {
            tm.put(Atomic.the(m.name()), m);
        }
        merge = tm.toImmutable();
    }


    /**
     * re-used
     */
    private final transient UnifiedSet<Term> nextNeg = new UnifiedSet<>(8, 0.99f), nextPos = new UnifiedSet(8, 0.99f);
    private final transient MutableSet<Term> autoNegNext = new UnifiedSet<>(8, 0.99f);

    /**
     * temporary set for filtering duplicates
     */
    private final Set<Event> seen = new UnifiedSet(Param.TEMPORAL_SOLVER_ITERATIONS * 2, 0.99f);
    private final Set<Term> expandedUntransforms = new UnifiedSet<>();
    private final Set<Term> expanded = new UnifiedSet<>();

    private final Derivation d;


    private transient Task curTask;
    private transient Termed curBelief;
    private transient Map<Term, Term> prevUntransform = Map.of();
    private transient boolean single;
    private transient boolean taskDominant;
    private boolean decomposeEvents;


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

    /** whether a term is 'temporal' and its derivations need analyzed by the temporal solver:
     *          if there is any temporal terms with non-DTERNAL dt()
     */
    public static boolean temporal(Term x) {
        boolean[] nonEternal = new boolean[1];
        x.recurseTerms(z -> z instanceof Compound && z.hasAny(Op.Temporal),
                z -> { if (z.dt() != DTERNAL) { nonEternal[0] = true; return false;  } else { return true; } },
                null);
        return nonEternal[0];
    }

    @Override
    public int dt(int dt) {
        return Tense.dither(dt, d.ditherTime);
    }

    @Override
    @Deprecated protected Term dt(Term x, List<BooleanObjectPair<FromTo<Node<Event, TimeSpan>, TimeSpan>>> path, int dt) {
        int ddt = dt(dt);
        Term y = super.dt(x, path, ddt);
        if ((y.op()!=x.op()) && ddt != dt) {
            y = super.dt(x, path, dt);
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


        if (end == start) {
            return event(tt, start, true);
        } else {
            return event(tt, start, end, true);
        }
    }

    @Override
    protected boolean decomposeAddedEvent(Event event) {
        return decomposeEvents;
    }

    public Occurrify reset(Term pattern) {
        boolean taskDominant = d.concSingle || (d._task.isGoal() && !d._task.isEternal());
        return reset(pattern, taskDominant, true);
    }

    public Occurrify reset(Term pattern, boolean taskDominant, boolean decomposeEvents) {


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

            setAutoNeg(pattern, taskTerm, single, beliefTerm);
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
                this.decomposeEvents == decomposeEvents &&
                this.taskDominant == taskDominant &&
                this.single == single &&
                        Objects.equals(autoNeg, autoNegNext) &&
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

            this.taskDominant = taskDominant;
            this.decomposeEvents = decomposeEvents;
            this.single = single;

            expanded.clear();
            expandedUntransforms.clear();

            autoNeg.clear();
            autoNeg.addAll(autoNegNext);

            know(task, taskStart, taskEnd);



            if (!(this.single)) {
                know(task, taskStart, taskEnd);

                if (!belief.equals(task)) {
                    if (taskDominant) {
                        know(beliefTerm);
                    } else {
                        know(belief, beliefStart, beliefEnd);
                    }
                }
            }

            if (!nextUntransform.isEmpty()) {
                this.prevUntransform = Map.copyOf(nextUntransform);

                nextUntransform.forEach((x, y) -> {
                    if (!y.isAny(Op.BOOL.bit | Op.INT.bit)) {
                        link(shadow(x), 0, shadow(y)); //weak
                    }
                });
            } else {
                this.prevUntransform = Map.of();
            }

        }

        return this;
    }

    private void setAutoNeg(Term pattern, Term taskTerm, boolean single, Term beliefTerm) {
        assert (nextPos.isEmpty() && nextNeg.isEmpty());

        UnifiedSet<Term> pp = nextPos;
        UnifiedSet<Term> nn = nextNeg;

        BiConsumer<Term, Compound> require = (sub, sup) -> {
            if (sub.op() == NEG) nn.add(sub.unneg());
            else if (sup == null || sup.op() != NEG)
                pp.add(sub); //dont add the inner positive unneg'd term of a negation
        };
        pattern.recurseTerms(require);

        BiConsumer<Term, Compound> provide = (sub, sup) -> {
            if (sub.op() == NEG) nn.remove(sub.unneg());
            else if (sup == null || sup.op() != NEG)
                pp.remove(sub); //dont add the inner positive unneg'd term of a negation
        };
        taskTerm.recurseTerms(provide);
        if (!single && (!pp.isEmpty() || !nn.isEmpty()))
            beliefTerm.recurseTerms(provide);

        pp.symmetricDifferenceInto(nn, autoNegNext);

        pp.clear();
        nn.clear();
    }


    @Override
    protected void onNewTerm(Term t) {
        if (!expanded.add(t))
            return;

        super.onNewTerm(t);

        Event tt = shadow(t);

        Term u = d.untransform(t);
        if (u != t && u != null && !(u instanceof Bool) && !u.equals(t)) {
            expandedUntransforms.add(u);
            expanded.add(u);
            link(tt, 0, shadow(u));
        }
        Term v = Image.imageNormalize(t);
        if (v != t && !v.equals(t)) {
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
        int ss = filterSolutions(solutions);
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
        if (ss > 0)
            ss = filterSolutions(solutions);

        switch (ss) {
            case 0:
                return () -> null;
            case 1:
                return () -> solveThe(solutions.first());
            default:
                return () -> solveThe(solutions.get(random()));
        }
    }

    private int filterSolutions(ArrayHashSet<Event> solutions) {
        int ss = solutions.size();
        if (ss > 1) {
            int occurrenceSolved = ((FasterList) solutions.list).count(t -> t instanceof Absolute);
            if (occurrenceSolved > 0 && occurrenceSolved < ss) {
                if (solutions.removeIf(t -> t instanceof Relative))
                    ss = solutions.size();
            }
//            if (ss > 1) {
//                return filterOOB(solutions);
//            }
        }
        return ss;
    }

//    /**
//     * prefer results which are within the model's range of known absolute timepoints, not outside of it
//     */
//    private int filterOOB(ArrayHashSet<Event> solutions) {
//        int ss = solutions.size();
//        if (ss <= 1)
//            return ss;
//
//        long min = Long.MAX_VALUE, max = Long.MAX_VALUE;
//
//        for (Event ee : byTerm.values()) {
//            if (!(ee instanceof Absolute))
//                continue;
//            Absolute a = (Absolute) ee;
//            long s = a.start();
//            if (s == ETERNAL)
//                continue; //skip eternal
//            long e = a.end();
//            min = Math.min(min, s);
//            max = Math.max(max, e);
//        }
//        if (min == Long.MAX_VALUE) return ss; //nothing could change
//
//        RoaringBitmap contained = new RoaringBitmap();
//        RoaringBitmap intersect = new RoaringBitmap();
//        RoaringBitmap outside = new RoaringBitmap();
//        List<Event> list = solutions.list;
//        for (int ei = 0, listSize = list.size(); ei < listSize; ei++) {
//            Event e = list.get(ei);
//            if (e instanceof Absolute) {
//                Absolute a = ((Absolute) e);
//                //if (Longerval.contains...)
//                if (a.containedIn(min, max))
//                    contained.add(ei);
//                if (a.intersectsWith(min, max))
//                    intersect.add(ei);
//                else
//                    outside.add(ei);
//            }
//        }
//
//        if (!outside.isEmpty() && (!intersect.isEmpty() || !contained.isEmpty())) {
//            outside.forEach((int o) -> solutions.list.remove(o)); //TODO solutions.remove(int)
//            return solutions.size();
//        }
//        //TODO remove intersects?
//
//        return ss;
//    }

    /**
     * eternal check: conditions under which an eternal result might be valid
     */
    boolean validEternal() {
        return d.task.isEternal() || (d.belief != null && d.belief.isEternal());
    }


    /**
     * requires single premise, or if double premise that there is temporal intersection of task and belief
     */
    private static final PREDICATE<Derivation> intersectFilter = new AbstractPred<>(Atomic.the("TimeIntersects")) {
        @Override
        public boolean test(Derivation d) {
            return d.concSingle || d.taskBeliefTimeIntersects;
        }
    };


//    /** requires single premise, or if double premise separation of task and
//     * belief time when the task is not a belief (goal or question/quest) */
//    private static final PREDICATE<Derivation> intersectFilterIfBelief = new AbstractPred<>(Atomic.the("TimeIntersects")) {
//        @Override
//        public boolean test(Derivation d) {
//            return (d.concSingle || d.taskPunc != BELIEF) || d.taskBeliefTimeIntersects;
//        }
//    };


    public enum TaskTimeMerge {


        /**
         * TaskRange is a specialization of Task timing that applies a conj's range to the result, effectively a union across its dt span
         */
        TaskRange() {
            @Override
            public Pair<Term, long[]> solve(Derivation d, Term x) {
                return solveDT(d, x, d.occ.reset(x));
            }

            @Override
            long[] occurrence(Derivation d) {
                assert (d.taskTerm.op() == CONJ);

                if (!d.task.isEternal()) {
                    int r = d.taskTerm.eventRange();
                    long[] o = new long[]{d.task.start(), r + d.task.end()};


                    if (r > 0 && d.concTruth != null) {
                        //decrease evidence by proportion of time expanded
                        float ratio = (float) (((double) d.task.range()) / (1 + o[1] - o[0]));
                        if (!d.concTruthEviMul(ratio, false))
                            return null;
                    }

                    return o;
                } else {
                    return new long[]{ETERNAL, ETERNAL};
                }
            }

        },

        /**
         * Task Dominant. the belief's temporality is secondary, unless task is eternal and belief is temporal
         * modulating the task.
         */
        Default() {
            @Override
            public Pair<Term, long[]> solve(Derivation d, Term x) {

                boolean isIdentity = d.truthFunction == NALTruth.Identity;

                Pair<Term, long[]> p = solveOccDT(d, x, d.occ.reset(x, true,  !isIdentity));
                if (p != null) {
                    byte punc = d.concPunc;

                    //prevent altering identity transforms
                    if ((punc == GOAL) && !isIdentity) {
                        if (!immediatize(p.getTwo(), d))
                            return null;
                    }
                }
                return p;
            }

            @Override
            public PREDICATE<Derivation> filter() {
                return intersectFilter;
            }

            @Override
            long[] occurrence(Derivation d) {
                Task T = d._task;
                long ts = d.taskStart;
                Task B = d._belief;
                long bs = d.beliefStart;
                if (ts != ETERNAL && bs != ETERNAL) {

                    if (d.concSingle)
                        return new long[]{ ts, T.end()};
//                    if (d.concSingle || d.concPunc == GOAL)
//                        return new long[]{ ts, T.end()};
//

                    long[] i = Longerval.intersectionArray(d.taskStart, d.task.end(), d.beliefStart, d.belief.end());
                    return i;


//                    //TODO unionArray
//                    Longerval i = Longerval.union(d.taskStart, d.task.end(), d.beliefStart, d.belief.end());
//                    return new long[]{i.a, i.b};
                } else if (ts == ETERNAL && B!=null && bs!=ETERNAL) {
                    return new long[]{ bs, B.end()};
                } else if (ts != ETERNAL && (B == null || bs == ETERNAL)) {
                    return new long[]{ ts, T.end()};
                } else {
                    return new long[] { ETERNAL, ETERNAL };
                }

            }
        },

//        /**
//         * happens in current present focus. no projection
//         */
//        TaskInstant() {
//            @Override
//            public Pair<Term, long[]> solve(Derivation d, Term x) {
//
//                Pair<Term, long[]> p = solveDT(d, x, d.occ.reset(x));
//                if (p != null) {
//
//                    //immediate future, dont interfere with present
//                    int durs = 1;
//                    //2;
//                    long[] when = d.nar.timeFocus(d.nar.time() + d.dur * durs);
//
//                    System.arraycopy(when, 0, p.getTwo(), 0, 2);
//                }
//                return p;
//            }
//
//            @Override
//            long[] occurrence(Derivation d) {
//                return occ(d.task);
//            }
//
//        },


        TaskPlusBeliefDT() {
            @Override
            public Pair<Term, long[]> solve(Derivation d, Term x) {
                return solveShiftBeliefDT(d, solveDT(d, x, d.occ.reset(x,false,false)), +1);
            }


            @Override
            long[] occurrence(Derivation d) {
                return occIntersect(d, OccIntersect.Task);
            }

        },
        TaskMinusBeliefDT() {
            @Override
            public Pair<Term, long[]> solve(Derivation d, Term x) {
                return solveShiftBeliefDT(d, solveDT(d, x, d.occ.reset(x,false,false)), -1);
            }

            @Override
            long[] occurrence(Derivation d) {
                return occIntersect(d, OccIntersect.Task);
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

                return solveDT(d, x, d.occ.reset(x,false,false));
            }

            @Override
            long[] occurrence(Derivation d) {
                return occIntersect(d, OccIntersect.Task);
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
                return solveDT(d, x, d.occ.reset(x,false,false));
            }

            @Override
            long[] occurrence(Derivation d) {
                return occIntersect(d, OccIntersect.Belief);
            }

            @Override
            public BeliefProjection beliefProjection() {
                return BeliefProjection.Raw;
            }

        },

        /**
         * unprojected span, for sequence induction
         * events are not decomposed as this could confuse the solver unnecessarily.  automatic autoneg?
         */
        Relative() {
            @Override
            public Pair<Term, long[]> solve(Derivation d, Term x) {


                if (x.op()!=CONJ || x.subs()!=2) {
                    //degenerated to non-conjunction. use the full solver
                    return solveOccDT(d, x, d.occ.reset(x, false, false));
                }

                long tTime = d.taskStart;
                long bTime = d.beliefStart;

                if (tTime == ETERNAL && bTime == ETERNAL) {
                    return pair(x.dt(DTERNAL), new long[] { ETERNAL, ETERNAL });
                }
                if (tTime == ETERNAL) {
                    return pair(x.dt(0), new long[] {bTime, d.belief.end() });
                }
                if (bTime == ETERNAL) {
                    return pair(x.dt(0), new long[] { tTime, d.task.end() });
                }


                Term i = x.sub(0), j = x.sub(1);
                Term tt;

                //infer which component is earlier
                Term taskTerm = d.taskTerm, beliefTerm = d.beliefTerm;
                if (taskTerm.equals(i) || taskTerm.equals(beliefTerm))
                    tt = i;
                else if (taskTerm.equals(j))
                    tt = j;
                else if (taskTerm.equalsNeg(i) && !beliefTerm.equals(i.unneg()))
                    tt = i;
                else if (taskTerm.equalsNeg(j) && !beliefTerm.equals(j.unneg()))
                    tt = j;
                else if (taskTerm.equalsRoot(i))
                    tt = i;
                else if (taskTerm.equalsRoot(j))
                    tt = j;
                else
                    return null; //TODO more cases

                Term bb = (tt == i) ? j : i;

//                long firstStart = ((first == i) ? d.task : d.belief).start();
//                long secondStart = (first == i  ? d.belief : d.task).start();
                Term y;
                long earlyStart = Math.min(tTime, bTime);
                if (tTime == earlyStart)
                    y = Conj.the(tt, 0, bb, Tense.dither(bTime - tTime, d.nar));
                else
                    y = Conj.the(bb, 0, tt, Tense.dither(tTime - bTime, d.nar));

                long range = Math.max(Math.min(d.task.range(), d.belief.range())-1, 0);



                return pair(y, new long[] { earlyStart , earlyStart  + range });

            }

            @Override
            long[] occurrence(Derivation d) {
                return occIntersect(d, OccIntersect.Earliest);
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
                return solveOccDT(d, x, d.occ.reset(x));
            }

            @Override
            public PREDICATE<Derivation> filter() {
                return intersectFilter;
            }

            @Override
            long[] occurrence(Derivation d) {
                if (d.concSingle || (d.belief==null || d.beliefStart == ETERNAL)) {
                    return new long[]{d.taskStart, d.task.end()};
                } else if (d.taskStart == ETERNAL && d.belief!=null) {
                    return new long[]{d.beliefStart, d.belief.end()};
                } else {

                    long[] i = Longerval.intersectionArray(d.taskStart, d.task.end(), d.beliefStart, d.belief.end());
                    if (i == null) {
                        //if (Param.DEBUG)
                        //assert(false == intersectFilter.test(d));
                        throw WTF("shouldnt happen");
                    }
                    return i;
                }
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
                return solveOccDT(d, x, d.occ.reset(x));
            }


            @Override
            long[] occurrence(Derivation d) {
                Longerval i = Longerval.union(d.taskStart, d.task.end(), d.beliefStart, d.belief.end());
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
                        bdt += d.beliefTerm.sub(0).eventRange(); //impl subj dtRange
                    } else if (sign == -1) {
                        bdt -= d.beliefTerm.sub(0).eventRange(); //impl subj dtRange
                    }

                    o[0] += bdt;
                    o[1] += bdt;

                    if (d.concPunc == GOAL /*|| d.concPunc == QUEST*/)
                        if (!immediatize(o, d))
                            return null;
                }
            }
            return p;
        }

        /**
         * immanentize
         */
        private static boolean immediatize(long[] o, Derivation d) {

            long NOW = d.time;

            int rad = Math.round(d.dur * Param.GOAL_PROJECT_TO_PRESENT_RADIUS_DURS);
            if (o[0] == ETERNAL) {
                if (d.task.isEternal() && (d.belief == null || d.belief.isEternal()))
                    return true; //both task and belief are eternal; keep eternal

                o[0] = NOW - rad;
                o[1] = NOW + rad;
                return true;
            }

            if (o[0] < NOW && o[1] < NOW) {

                int dur = d.dur;

                if (d.concPunc == BELIEF || d.concPunc == GOAL) {
                    //starts and ends before now; entirely past
                    // shift and project to present, "as-if" past-perfect/subjunctive tense

                    //discount for projection
                    long deltaT = Math.abs(NOW - o[1]); //project from end, closer to now if fully in the past
                    float eStartFactor = Param.evi(1, deltaT, dur);
                    if (!d.concTruthEviMul(eStartFactor, Param.ETERNALIZE_BELIEF_PROJECTED_FOR_GOAL_DERIVATION))
                        return false; //insufficient evidence
                }

                o[0] = NOW;
                o[1] = NOW + rad; //allow only fixed time: benefit of the doubt
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
            if (!solutions.isEmpty()) {
                Pair<Term, long[]> p = o.solveOccDT(solutions).get();
                if (p != null) {
                    if (p.getTwo()[0] != TIMELESS) {
                        return p;
                    } else {
                        return solveAuto(p.getOne(), d);
                    }
                }
            }
            return solveAuto(x, d);
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

        @Nullable
        protected Pair<Term, long[]> solveDT(Derivation d, Term x, Occurrify o) {
            ArrayHashSet<Event> solutions = o.solutions(x);
            Term p = o.solveDT(x, solutions);
//            if (p == null)
//                p = x;

            long[] occ = occurrence(d);
            return occ == null ? null : pair(p, occ);
        }

        /**
         * failsafe mode
         */
        public Pair<Term, long[]> solveAuto(Term x, Derivation d) {

            if ((d.concPunc == BELIEF || d.concPunc == GOAL) && x.hasXternal())
                return null;
            long[] u = occurrence(d);
            if (u != null) {
                return pair(x, u);
            } else {
                return null;
            }

//            Task task = d.task;
//            Task belief = d.concSingle ? null : d.belief;
//            long s, e;
//        /*if (task.isQuestOrQuestion() && (!task.isEternal() || belief == null)) {
//
//            s = task.start();
//            e = task.end();
//        } else*/
//            boolean taskConj =
//                    !(task.term().op() == CONJ);
//
//            if (task.isEternal()) {
//                if (belief == null || belief.isEternal()) {
//
//                    s = e = ETERNAL;
//                } else {
//                    if (taskConj) {
//                        s = belief.start();
//                        e = belief.end();
//                    } else {
//
//                        return null;
//                    }
//                }
//            } else {
//                if (belief == null) {
//
//                    s = task.start();
//                    e = task.end();
//
//                } else if (belief.isEternal()) {
//                    if (!task.isEternal()) {
//
//                        s = task.start();
//                        e = task.end();
//                    } else {
//                        s = e = ETERNAL;
//                    }
//
//
//                } else {
//                    byte p = d.concPunc;
//                    if ((p == BELIEF || p == GOAL)) {
//                        boolean taskEvi = !task.isQuestionOrQuest();
//                        boolean beliefEvi = !belief.isQuestionOrQuest();
//                        if (taskEvi && beliefEvi) {
//                            long[] u = occurrence(d);
//                            if (u != null) {
//                                s = u[0];
//                                e = u[1];
//                            } else {
//                                return null;
//                            }
//                        } else if (taskEvi) {
//                            s = task.start();
//                            e = task.end();
//                        } else if (beliefEvi) {
//                            s = belief.start();
//                            e = belief.end();
//                        } else {
//                            throw new UnsupportedOperationException("evidence from nowhere?");
//                        }
//                    } else {
//
//                        Longerval u = Longerval.union(task.start(), task.end(), belief.start(), belief.end());
//                        s = u.start();
//                        e = u.end();
//                    }
//                }
//            }
//
//
//            return pair(x, new long[]{s, e});

        }

        public BeliefProjection beliefProjection() {
            return BeliefProjection.Task;
        }
    }


    private enum OccIntersect {
        Task, Belief, Earliest
    }

    private static long[] occIntersect(Derivation d, OccIntersect mode) {
        if (d.belief == null || d.belief.isEternal())
            return occ(d.task);
        else if (d.task.isEternal())
            return occ(d.belief);
        else {
            long taskStart = d.task.start();
            long beliefStart = d.belief.start();

            long start;
            switch (mode) {
                case Earliest:
                    start = Math.min(taskStart, beliefStart);
                    break;
                case Task:
                    start = d.task.start();
                    break;
                case Belief:
                    start = d.belief.start();
                    break;
                default:
                    throw new UnsupportedOperationException();
            }

            //minimum range = intersection
            return new long[]{
                    start,
                    start + Math.min(d.task.end() - taskStart, d.belief.end() - beliefStart)
            };
        }
    }

    private static long[] occ(Task t) {
        return new long[]{t.start(), t.end()};
    }


    /**
     * TODO do derivation truth calculation in implemented method of this enum
     */
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
















































































































