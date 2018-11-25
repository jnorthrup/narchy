package nars.derive.op;

import jcog.WTF;
import jcog.data.bit.MetalBitSet;
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
import nars.term.atom.Atomic;
import nars.term.control.AbstractPred;
import nars.term.control.PREDICATE;
import nars.term.util.Conj;
import nars.time.Event;
import nars.time.Tense;
import nars.time.TimeGraph;
import nars.time.TimeSpan;
import nars.truth.func.NALTruth;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.BooleanObjectPair;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;
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
    private final transient UnifiedSet<Term>
            nextNeg = new UnifiedSet<>(8, 0.99f),
            nextPos = new UnifiedSet(8, 0.99f);








    private final Derivation d;


    private boolean decomposeEvents;


    public Occurrify(Derivation d) {
        this.d = d;
    }



    /**
     * whether a term is 'temporal' and its derivations need analyzed by the temporal solver:
     * if there is any temporal terms with non-DTERNAL dt()
     */
    public static boolean temporal(Term x) {
        return x.ORrecurse(z -> z instanceof Compound && z.dt() != DTERNAL);
    }

    @Override
    public int dt(int dt) {
        return Tense.dither(dt, d.ditherTime);
    }

    @Override
    @Deprecated
    protected Term dt(Term x, List<BooleanObjectPair<FromTo<Node<Event, TimeSpan>, TimeSpan>>> path, int dt) {
        int ddt = dt(dt);
        Term y = super.dt(x, path, ddt);
        if (ddt != dt && Param.ALLOW_UNDITHERED_DT_IF_DITHERED_FAILS && (y.op() != x.op())) {
            y = super.dt(x, path, dt);
        }
        return y;
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
        solutions.clear();
        autoNeg.clear();
    }


    private Occurrify reset(Term pattern) {
        return reset(pattern, true);
    }

    private Occurrify reset(boolean taskOccurrence, boolean beliefOccurrence, Term pattern) {
        boolean decomposeEvents = d.truthFunction != NALTruth.Identity;
        return reset(taskOccurrence, beliefOccurrence, pattern, decomposeEvents);
    }

    private Occurrify reset(Term pattern, boolean decomposeEvents) {
        return reset(true, true, pattern, decomposeEvents);
    }

    private Occurrify reset(boolean taskOccurrence, boolean beliefOccurrence, Term pattern, boolean decomposeEvents) {

        clear();

        boolean beliefNoOcc = !beliefOccurrence || (d.concSingle); // && (d.taskPunc!=QUESTION && d.taskPunc!=QUEST));

        long taskStart = taskOccurrence ? d.taskStart : TIMELESS,
                taskEnd = taskOccurrence ? d.taskEnd : TIMELESS,
                beliefStart = beliefNoOcc ? TIMELESS : d.beliefStart,
                beliefEnd = beliefNoOcc ? TIMELESS : d.beliefEnd;




//        if (!single) {
        boolean taskEte = taskStart == ETERNAL;
        boolean beliefEte = beliefStart == ETERNAL;
        if (taskEte && !beliefEte && beliefStart != TIMELESS) {
            taskStart = beliefStart; //use belief time for eternal task
            taskEnd = beliefEnd;
        } else if (beliefEte && !taskEte && taskStart != TIMELESS) {
            beliefStart = taskStart; //use task time for eternal belief
            beliefEnd = taskEnd;
        }
//        }


//        boolean reUse =
//                        this.decomposeEvents == decomposeEvents &&
//                        occurrenceQuad[0] == taskStart &&
//                        occurrenceQuad[1] == taskEnd &&
//                        occurrenceQuad[2] == beliefStart &&
//                        occurrenceQuad[3] == beliefEnd &&
//                        Objects.equals(taskTerm, curTaskTerm) &&
//                        Objects.equals(beliefTerm, curBeliefTerm) &&
//                        Objects.equals(autoNeg, autoNegNext);


        this.decomposeEvents = decomposeEvents;

        final Term taskTerm = d.taskTerm, beliefTerm = d.beliefTerm;
        if (taskTerm.hasAny(NEG) || beliefTerm.hasAny(NEG) || pattern.hasAny(NEG)) {
            setAutoNeg(pattern, taskTerm, beliefTerm);
        }

        Event taskEvent = (taskStart != TIMELESS) ?
                know(taskTerm, taskStart, taskEnd) :
                know(taskTerm);

        Event beliefEvent = (beliefStart != TIMELESS) ?
                know(beliefTerm, beliefStart, beliefEnd) :
                (!beliefTerm.equals(taskTerm)) ? know(beliefTerm) : taskEvent /* same term, reuse the same event */;


        retransform(taskEvent);

        if (!taskEvent.equals(beliefEvent))
            retransform(beliefEvent);

        knowIfRetransforms(pattern);


        return this;
    }

    private void retransform(Event e) {
        Term t = e.id;
        Term u = d.retransform(t);
        if (!t.equals(u) && termsEvent(u))
            link(e, 0, know(u));
    }

    private void knowIfRetransforms(Term t) {
        Term u = d.retransform(t);
        if (!t.equals(u) && termsEvent(u))
            link(know(t), 0, know(u));
    }

    final BiConsumer<Term, Compound> negRequire = (sub, sup) -> {
        Op so = sub.op();
        if (so == NEG) nextNeg.add(sub.unneg());
        else if (sup == null || (so == CONJ || sup.op() != NEG))
            nextPos.add(sub); //dont add the inner positive unneg'd term of a negation unless conj (ie. disj)
    };

    final BiConsumer<Term, Compound> negProvide = (sub, sup) -> {
        Op so = sub.op();
        if (so == NEG) nextNeg.remove(sub.unneg());
        else if (sup == null || (so == CONJ || sup.op() != NEG))
            nextPos.remove(sub); //dont add the inner positive unneg'd term of a negation unless conj (ie. disj)
    };

    private void setAutoNeg(Term pattern, Term taskTerm, Term beliefTerm) {

        assert (autoNeg.isEmpty());

        pattern.recurseTerms(negRequire);

        taskTerm.recurseTerms(negProvide);

        if (!beliefTerm.equals(taskTerm) && !nextPos.isEmpty() || !nextNeg.isEmpty()) {
            beliefTerm.recurseTerms(negProvide);

            nextPos.symmetricDifferenceInto(nextNeg, autoNeg /* should be clear */);

            nextPos.clear();
            nextNeg.clear();
        }
    }


    private static Pair<Term, long[]> solveThe(Event event) {
        long es = event.start();
        return pair(event.id,
                es == TIMELESS ? new long[]{TIMELESS, TIMELESS} : new long[]{es, event.end()});
    }

    private int triesRemain = 0;

    private boolean eachSolution(Event solution) {
        assert (solution != null);
        return triesRemain-- > 0;
    }

    private ArrayHashSet<Event> solutions(Term pattern) {

        triesRemain = Param.TEMPORAL_SOLVER_ITERATIONS;

        solve(pattern, false /* take everything */, this::eachSolution);

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

    private static int filterSolutions(ArrayHashSet<Event> solutions) {
        int ss = solutions.size();
        if (ss > 1) {
            MetalBitSet relative = MetalBitSet.bits(ss);
            MetalBitSet xternal = MetalBitSet.bits(ss);
            for (int i = 0; i < ss; i++) {
                Event s = solutions.get(i);
                if (s instanceof Relative)
                    relative.set(i);
                if (s.id.hasXternal())
                    xternal.set(i);
            }
            int xternals = xternal.cardinality();
            if (xternals > 0 && xternals < ss) {
                for (int i = 0; i < ss; i++)
                    if (xternal.get(i))
                        solutions.list.set(i, null);

                ((FasterList)solutions.list).removeNulls(); //HACK doesnt remove from the ArrayHashSet's Set
                ss = solutions.list.size();
            }
            if (ss > 1) {

                int occurrenceSolved = ((FasterList) solutions.list).count(t -> t instanceof Absolute);
                if (occurrenceSolved > 0 && occurrenceSolved < ss) {
                    if (solutions.removeIf(t -> t instanceof Relative))
                        ss = solutions.size();
                }
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
        return d.taskStart == ETERNAL && (d.concSingle || d.beliefStart == ETERNAL);
    }


    public enum TaskTimeMerge {


        /**
         * TaskRange is a specialization of Task timing that applies a conj's range to the result, effectively a union across its dt span
         */
        TaskRange() {
            @Override
            public Pair<Term, long[]> occurrence(Derivation d, Term x) {
                return solveDT(d, x, d.occ.reset(x));
            }

            @Override
            long[] occurrence(Derivation d) {
                assert (d.taskTerm.op() == CONJ);

                if (d.taskStart != ETERNAL) {
                    int r = d.taskTerm.eventRange();
                    long[] o = new long[]{d.taskStart, r + d.taskEnd};


                    if (r > 0 && d.concTruth != null) {
                        //decrease evidence by proportion of time expanded
                        float ratio = (float) (((double) d._task.range()) / (1 + o[1] - o[0]));
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
            public Pair<Term, long[]> occurrence(Derivation d, Term x) {


                Pair<Term, long[]> p = solveOccDT(d, x, d.occ.reset(x));
//                if (p != null) {
//                    if (immediatizable(d)) {
//                        if (!immediatize(p.getTwo(), d))
//                            return null;
//                    }
//                }
                return p;
            }

//            @Override
//            public PREDICATE<Derivation> filter() {
//                return intersectFilter;
//            }

            @Override
            long[] occurrence(Derivation d) {
                return rangeCombine(d,OccIntersect.Task);

//                Task T = d._task;
//                long ts = d.taskStart;
//                Task B = d._belief;
//                long bs = d.beliefStart;
//                if (ts != ETERNAL && bs != ETERNAL) {
//
////                    if (d.concSingle)
//                        //return new long[]{ts, ts+range}; //since this isn't raw, the truth has been projected to the task
////                    else {
//////                    if (d.concSingle || d.concPunc == GOAL)
//////                        return new long[]{ ts, T.end()};
//////
////
////                        return Longerval.intersectionArray(d.taskStart, d.taskEnd, d.beliefStart, d.beliefEnd);
////                    }
//
//
//                    //when might Union acceptable since intersection has alrady been tested. this includes when the tasks meet end-to-end in whch case union is just the loosest concatenation of them
////                    Longerval i = Longerval.union(d.taskStart, d.task.end(), d.beliefStart, d.belief.end());
////                    return new long[]{i.a, i.b};
//
//                } else if (ts == ETERNAL && B != null && bs != ETERNAL) {
//                    return new long[]{bs, B.end()};
//                } else if (ts != ETERNAL && (B == null || bs == ETERNAL)) {
//                    return new long[]{ts, T.end()};
//                } else {
//                    return new long[]{ETERNAL, ETERNAL};
//                }

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
            public Pair<Term, long[]> occurrence(Derivation d, Term x) {
                return solveShiftBeliefDT(d, solveDT(d, x, d.occ.reset(true, false, x, false)), +1);
            }


            @Override
            long[] occurrence(Derivation d) {
                return rangeCombine(d, OccIntersect.Task);
            }

        },
        TaskMinusBeliefDT() {
            @Override
            public Pair<Term, long[]> occurrence(Derivation d, Term x) {
                return solveShiftBeliefDT(d, solveDT(d, x, d.occ.reset(true, false, x, false)), -1);
            }

            @Override
            long[] occurrence(Derivation d) {
                return rangeCombine(d, OccIntersect.Task);
            }

        },



        /**
         * for use with ConjDropIfLatest
         */
        TaskInBelief() {
            @Override
            public Pair<Term, long[]> occurrence(Derivation d, Term x) {
                return x.hasXternal() ? solveDT(d, x, d.occ.reset(x)) : pair(x, occurrence(d)); //special
            }

            @Override
            long[] occurrence(Derivation d) {
                return OccConjDecompose(d, true);
            }

        },

        /**
         * for use with ConjDropIfEarliest
         */
        BeliefInTask() {
            @Override
            public Pair<Term, long[]> occurrence(Derivation d, Term x) {

                @Nullable Pair<Term, long[]> p = solveDT(d, x, d.occ.reset(x));
                if (p != null) {
                    long[] se = p.getTwo();
                    if (se[0] != ETERNAL) {
                        int shift = d.taskTerm.eventRange() - x.eventRange();
                        se[0] += shift;
                        se[1] += shift;
                    }
                }
                return p;
            }

            @Override
            long[] occurrence(Derivation d) {
                return OccConjDecompose(d, false);
            }
//            @Override
//            public BeliefProjection beliefProjection() {
//                return BeliefProjection.Raw; //belief
//            }
        },

        /**
         * used for conjunction structural decomposition
         */
        TaskSubEventPos() {
            @Override
            public Pair<Term, long[]> occurrence(Derivation d, Term x) {
                return solveSubEvent(d, x, false);
            }

            @Override
            long[] occurrence(Derivation d) {
                throw new UnsupportedOperationException();
            }

            @Override
            public BeliefProjection beliefProjection() {
                return BeliefProjection.Raw; //N/A structuraldeduction
            }
        },

        /**
         * used for single premise conjunction within conjunction result
         */
        TaskSubSequence() {
            @Override
            public Pair<Term, long[]> occurrence(Derivation d, Term x) {
                if (d.taskStart == ETERNAL) {
                    return pair(x, new long[]{ETERNAL, ETERNAL});
                }
                return solveSubSequence(x, d.taskTerm, d.taskStart, d.taskEnd);
            }

            @Override
            long[] occurrence(Derivation d) {
                throw new UnsupportedOperationException();
            }

            @Override
            public BeliefProjection beliefProjection() {
                return BeliefProjection.Raw; //N/A structuraldeduction
            }
        },

//        /**
//         * used for single premise conjunction within conjunction result
//         */
//        BeliefSubSequence() {
//            @Override
//            public Pair<Term, long[]> occurrence(Derivation d, Term x) {
//                if (d.taskStart == ETERNAL && d.beliefStart == ETERNAL)
//                    return pair(x, new long[]{ETERNAL, ETERNAL});
//
////                if (d.beliefStart == ETERNAL)
////                    return pair(x, new long[]{d.taskStart, d.taskEnd});
//
//                //return solveSubSequence(x, d.beliefTerm, d.beliefStart, d.beliefEnd);
//                return solveSubSequence(x, d.beliefTerm, d.taskStart, d.taskEnd);
//            }
//
//            @Override
//            long[] occurrence(Derivation d) {
//                throw new UnsupportedOperationException();
//            }
//            @Override
//            public BeliefProjection beliefProjection() {
//                return BeliefProjection.Raw; //N/A structuraldeduction
//            }
//        },


        /**
         * used for conjunction structural decomposition
         */
        TaskSubEventNeg() {
            @Override
            public Pair<Term, long[]> occurrence(Derivation d, Term x) {
                return solveSubEvent(d, x, true);
            }

            @Override
            long[] occurrence(Derivation d) {
                throw new UnsupportedOperationException();
            }

            @Override
            public BeliefProjection beliefProjection() {
                return BeliefProjection.Raw; //N/A structuraldeduction
            }
        },
        /**
         * for unprojected truth rules;
         * result should be left-aligned (relative) to the belief's start time
         */
        BeliefRelative() {
            @Override
            public Pair<Term, long[]> occurrence(Derivation d, Term x) {
                return solveDT(d, x, d.occ.reset(x, false));
            }

            @Override
            long[] occurrence(Derivation d) {
                return rangeCombine(d, OccIntersect.Belief);
            }

            @Override
            public BeliefProjection beliefProjection() {
                return BeliefProjection.Raw;
            }

        },
        /**
         * for unprojected truth rules;
         * result should be left-aligned (relative) to the task's start time
         * used by temporal induction rules
         */
        TaskRelative() {
            @Override
            public Pair<Term, long[]> occurrence(Derivation d, Term x) {
                return solveDT(d, x, d.occ.reset(x, false));
            }

            @Override
            long[] occurrence(Derivation d) {
                return rangeCombine(d, OccIntersect.Task);
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
            public Pair<Term, long[]> occurrence(Derivation d, Term x) {


                if (x.op() != CONJ || x.subs() != 2) {
                    //degenerated to non-conjunction. use the full solver
                    return solveOccDT(d, x, d.occ.reset(x, false));
                }

                long tTime = d.taskStart, bTime = d.beliefStart;

                if (tTime == ETERNAL && bTime == ETERNAL) {
                    return pair(x.dt(DTERNAL), new long[]{ETERNAL, ETERNAL});
                }
                if (tTime == ETERNAL) {
                    return pair(x.dt(0), new long[]{bTime, d.beliefEnd});
                }
                if (bTime == ETERNAL) {
                    return pair(x.dt(0), new long[]{tTime, d.taskEnd});
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
//                else if (taskTerm.equalsRoot(i))
//                    tt = i;
//                else if (taskTerm.equalsRoot(j))
//                    tt = j;
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

                long range = Math.max(Math.min(d._task.range(), d._belief.range()) - 1, 0);


                return pair(y, new long[]{earlyStart, earlyStart + range});

            }

            @Override
            long[] occurrence(Derivation d) {
                return rangeCombine(d, OccIntersect.Earliest);
            }

            @Override
            public BeliefProjection beliefProjection() {
                return BeliefProjection.Raw;
            }

        },

        Task() {
            @Override
            public Pair<Term, long[]> occurrence(Derivation d, Term x) {
                @Nullable Pair<Term, long[]> p = x.hasXternal() ? solveDT(d, x, d.occ.reset(x,false)) : pair(x, new long[2]);
                if (p != null) {
                    long[] o = p.getTwo();
                    if (d.taskStart != ETERNAL || d.occ.validEternal()) {
                        o[0] = d.taskStart;
                        o[1] = d.taskEnd;
                    } else {
                        o[0] = d.beliefStart;
                        o[1] = d.beliefEnd;
                    }
                }
                return p;
            }

            @Override
            long[] occurrence(Derivation d) {
                return new long[]{TIMELESS, TIMELESS}; //HACK to be rewritten by the above method
            }

        },

        /**
         * result occurs in the intersecting time interval, if exists; otherwise fails
         */
        Intersect() {
            @Override
            public Pair<Term, long[]> occurrence(Derivation d, Term x) {
                if (!x.hasXternal()) {
                    return pair(x, d.taskBeliefTimeIntersects);
                } else {
                    return solveOccDT(d, x, d.occ.reset(false,false,x,true)); //TODO maybe just solveDT
                }
            }

            /**
             * requires single premise, or if double premise that there is temporal intersection of task and belief
             */
            private final PREDICATE<Derivation> intersectFilter = new AbstractPred<>(Atomic.the("TimeIntersects")) {
                @Override
                public boolean test(Derivation d) {
                    return d.concSingle || d.taskBeliefTimeIntersects[0] != TIMELESS;
                }
            };


            @Override
            public PREDICATE<Derivation> filter() {
                return intersectFilter;
            }

            @Override
            long[] occurrence(Derivation d) {
                if (d._belief == null || d.concSingle || d.beliefStart == ETERNAL) {
                    return new long[]{d.taskStart, d.taskEnd};
                } else if (d.taskStart == ETERNAL) {
                    return new long[]{d.beliefStart, d.beliefEnd};
                } else {

                    assert (d._belief != null && d.beliefStart != TIMELESS);

                    long[] i = Longerval.intersectionArray(d.taskStart, d.taskEnd, d.beliefStart, d.beliefEnd);
                    if (i == null) {
                        throw new WTF("should have been filtered in Truthify");
                    }
                    return i;
                }
            }

        },

//        /**
//         * result occurs in the union time interval, and this always exists.
//         * the evidence integration applied in the truth calculation should
//         * reflect the loss of evidence from any non-intersecting time ranges
//         */
//        Union() {
//            @Override
//            public Pair<Term, long[]> occurrence(Derivation d, Term x) {
//                //return solveOccDTWithGoalOverride(d, x);
//                return solveOccDT(d, x, d.occ.reset(x));
//            }
//
//
//            @Override
//            long[] occurrence(Derivation d) {
//                Longerval i = Longerval.union(d.taskStart, d.taskEnd, d.beliefStart, d.beliefEnd);
//                return new long[]{i.a, i.b};
//            }
//
//        }
        ;

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

            long[] o = p.getTwo();
            long s = o[0];
            if (s != TIMELESS && s != ETERNAL) {

                int bdt = d.beliefTerm.dt();
                if (bdt != XTERNAL) {
                    if (bdt == DTERNAL)
                        bdt = 0;

                    bdt *= sign;


                    int subjDur = d.beliefTerm.sub(0).eventRange(); //impl subj dtRange
                    bdt += sign * subjDur;

                    o[0] += bdt;
                    o[1] += bdt;

//                    if (immediatizable(d))
//                        if (!immediatize(o, d))
//                            return null;
                }
            }
            return p;
        }

//        /**
//         * immanentize
//         */
//        private static boolean immediatize(long[] o, Derivation d) {
//
//
//            if (o[0] == ETERNAL) {
//                if (d.taskStart == ETERNAL && (d.concSingle || d.beliefStart == ETERNAL))
//                    return true; //both task and belief are eternal; keep eternal
//
//                throw new UnsupportedOperationException();
////                long NOW = d.time;
////                int rad = Math.round(d.dur * Param.GOAL_PROJECT_TO_PRESENT_RADIUS_DURS);
////                o[0] = NOW;
////                o[1] = NOW + rad;
////                return true;
//            }
//
//            long target =
//                    //d.time;
//                    d.taskStart != ETERNAL || d.beliefStart == TIMELESS ? d.taskStart : d.beliefStart;
//            //Math.max(d.taskStart, d.time);
//
//            if (o[1] < target) {
//
//                if (d.concPunc == GOAL || d.concPunc == BELIEF) {
//                    //starts and ends before now; entirely past
//                    // shift and project to present, "as-if" past-perfect/subjunctive tense
//
//                    //discount for projection
//                    long deltaT = Math.abs(target - o[0]); //project from end, closer to now if fully in the past
//                    float eStartFactor = Param.evi(1, deltaT, d.dur);
//                    if (!d.concTruthEviMul(eStartFactor, Param.ETERNALIZE_BELIEF_PROJECTED_FOR_GOAL_DERIVATION))
//                        return false; //insufficient evidence
//                }
//
////                long durMin = Math.min(o[1] - o[0], d.dur);
////                long range =
////                        //o[1] - o[0];
////                        o[1] > target + durMin ? o[1] - target
////                                :
////                                durMin;
////                o[0] = target;
////                o[1] = target + range;
//
//                long range = o[1] - o[0];
//                range = o[1] > target ? Math.min(range, o[1] - target) : 0;
//                o[0] = target;
//                o[1] = target + range;
//            }
//
//            return true;
//        }

        /**
         * fallback
         */
        abstract long[] occurrence(Derivation d);

        public Term term() {
            return term;
        }

        abstract public Pair<Term, long[]> occurrence(Derivation d, Term x);

        Pair<Term, long[]> solveOccDT(Derivation d, Term x, Occurrify o) {
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

        @Nullable Pair<Term, long[]> solveDT(Derivation d, Term x, Occurrify o) {
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
        Pair<Term, long[]> solveAuto(Term x, Derivation d) {

            if ((d.concPunc == BELIEF || d.concPunc == GOAL) && x.hasXternal())
                return null;
            long[] u = occurrence(d);
            return u != null ? pair(x, u) : null;

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

    @Nullable
    private static Pair<Term, long[]> solveSubSequence(Term x, Term src, long srcStart, long srcEnd) {


        if (src.eventRange() == 0) {
            return pair(x, new long[]{srcStart, srcEnd});
        } else {
            int offset = src.subTimeFirst(x);
            if (offset == DTERNAL) {
                offset = src.subTimeFirst(x.neg());
                if (offset == DTERNAL) {
                    final Term[] first = {null};
                    x.eventsWhile((when, what) -> {
                        first[0] = what;
                        return false;
                    }, 0, true, true, false, 0);
                    if (first[0] != null && !first[0].equals(x)) {
                        offset = src.subTimeFirst(first[0]);
                        if (offset == DTERNAL) {
                            offset = src.subTimeFirst(first[0].neg());
                        }
                    }
                }
            }

            if (offset != DTERNAL)
                return pair(x, new long[]{srcStart + offset, srcEnd + offset});

            if (Param.DEBUG)
                throw new WTF();

            return null;

        }
    }

    private static long[] OccConjDecompose(Derivation d, boolean taskInBelief) {
        long base;
        long range;
        if (d.taskStart == ETERNAL && d.beliefStart == ETERNAL) {
            return new long[]{ETERNAL, ETERNAL};
        }

        if (d.taskStart == ETERNAL) {
            base = d.beliefStart;
            range = d._belief.range() - 1;
        } else {
            if (d.beliefStart == ETERNAL)
                range = d._task.range() - 1;
            else
                range = Math.min(d._task.range(), d._belief.range()) - 1;

            base = taskInBelief || d.beliefStart == ETERNAL ? d.taskStart : d.beliefStart;
        }

        assert (base != ETERNAL && base != TIMELESS);

        int shift;

        if (taskInBelief) {

            Term inner = d.taskTerm, outer = d.beliefTerm;

            //TODO some cases: subTimeLast. also would help to specifically locate the pos/neg one

            shift = outer.subTimeFirst(inner);
            if (shift == DTERNAL) {
                shift = outer.subTimeFirst(inner.neg()); //try negative
                if (shift == DTERNAL)
                    return null; //TODO why if this happens
            }

            shift = -shift;

        } else {
            //shift = the occurrence of the 2nd event (since it was conjDropIfEarly)
            shift = 0; //applied later
        }

        long start = base + shift;

        return new long[]{start, start + range};
    }

//    private static boolean immediatizable(Derivation d) {
//        //return (d.taskPunc == GOAL || d.taskPunc == QUEST) && (d.concPunc == GOAL || d.concPunc == QUEST) && d.truthFunction != NALTruth.Identity;
//    }


    private static Pair<Term, long[]> solveSubEvent(Derivation d, Term x, boolean neg) {

        long[] w;
        if (d.taskStart == ETERNAL)
            w = new long[]{ETERNAL, ETERNAL};
        else {

            int[] offsets = d.taskTerm.subTimes(x.negIf(neg && x.op() != NEG /* not already negative, which is possible as a result from Termify */));
            if (offsets == null) {
                if (!neg && x.hasAny(CONJ)) {
                    //use the first event
                    Term[] first = new Term[1];
                    x.eventsWhile((when, what) -> {
                        first[0] = what;
                        return false;
                    }, 0, true, true, true, 0);
                    if (first[0] != x)
                        offsets = d.taskTerm.subTimes(first[0]);
                }

            }
            if (offsets == null || offsets.length == 0) {
                if (Param.DEBUG)
                    throw new WTF(); //seems to be something involving normalized/unnormalized variables not getting matched
                else
                    return null;
            }

            int offset = offsets[(offsets.length > 1) ? d.nar.random().nextInt(offsets.length) : 0];
                assert (offset != DTERNAL && offset != XTERNAL);
            w = new long[]{d.taskStart + offset, d.taskEnd + offset};
        }
        return pair(x, w);
    }


    private enum OccIntersect {
        Task, Belief, Earliest
    }

    private static long[] rangeCombine(Derivation d, OccIntersect mode) {
        long beliefStart = d.beliefStart;
        if (d._belief == null || d.beliefStart == ETERNAL)
            return occ(d._task);
        else if (d.taskStart == ETERNAL)
            return occ(d._belief);
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
















































































































