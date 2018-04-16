package nars.derive.step;

import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SortedSetMultimap;
import jcog.data.ArrayHashSet;
import jcog.list.FasterList;
import jcog.math.Longerval;
import nars.Param;
import nars.Task;
import nars.derive.Derivation;
import nars.util.time.TimeGraph;
import nars.unify.match.EllipsisMatch;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Bool;
import nars.term.compound.util.Conj;
import nars.term.var.VarPattern;
import nars.util.time.Tense;
import nars.truth.Truth;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.LongLongPair;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectByteHashMap;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static nars.Op.*;
import static nars.util.time.Tense.ETERNAL;
import static nars.util.time.Tense.TIMELESS;
import static org.eclipse.collections.impl.tuple.Tuples.pair;


/**
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
public class DeriveTime extends TimeGraph {

    private final Task task, belief;

    private final Derivation d;

    /**
     * temporary set for filtering duplicates
     */
    private final HashSet<Event> seen;


    /**
     * on .get(), also sets the occurrence time and any other derivation state
     */
    final Map<Term, Supplier<Term>> cache;

    @Override
    public void clear() {
        super.clear();
        cache.clear();
    }

    /**
     * for dynamic
     */
    DeriveTime(DeriveTime copy) {
        this.cache = null;
        this.d = copy.d;
        this.task = copy.task;
        this.belief = copy.belief;
        this.seen = copy.seen;

        //TODO optimized clone of the copy's graph
        //this.byTerm.putAll(copy.byTerm);
        //this.nodes...

        //for now, just do manual reconstruct

        copy.byTerm.forEach((x, event) -> {
            addNode(event);

            Term y = x.eval(d);
            if (y != x && !y.equals(x) && y.op().conceptualizable) {
                link(know(x), 0, know(y));
            }

        });

        //link all non-pattern var substitutions
        d.xy.forEach((x, y) -> {
            if (!(x instanceof VarPattern) && x.op().conceptualizable && !(x instanceof EllipsisMatch)) {
                link(know(x), 0, know(y));
            }
        });


    }

//    private void knowTransformed(Term tt, Task t) {
//        Iterable<Event> ee = know(t, tt);
//        if (autoNegEvents && tt.op() != CONJ) {
//            for (Event y : ee)
//                link(know(tt.neg()), 0, y);
//        }
//    }


    public DeriveTime(Derivation d, boolean single) {
        this.d = d;
        this.cache = new HashMap(0);
        this.seen = new HashSet<>();

        this.belief = !single ? d.belief : null;

        //determine whether to auto-neg
        Term tt = (this.task = d.task).term();
        Term bb = !single ? (belief).term() : null;
        //HACK autoNeg only the specific terms which appear as both
        if (tt.hasAny(NEG) || (bb != null && bb.hasAny(NEG))) {
            ObjectByteHashMap<Term> events = new ObjectByteHashMap();
            eventPolarities(tt, events);
            if (bb != null)
                eventPolarities(bb, events);

            Set<Term> mixed = new HashSet();
            events.forEachKeyValue((k, v) -> {
                if (v == 0) {
                    mixed.add(k);
                }
            });
            if (!mixed.isEmpty()) {
                //autoNeg = (x) -> !mixed.contains(x);
                autoNeg = (x) -> false; //safer
            }
            //!(events.anySatisfy((x)->x==0)); //safe to autoNeg if no mixed polarities present
        } else {
            //safe to autoNeg since no negations are present anyway
            //no need to change default setting
        }

        if (!single) {
            boolean taskTime, beliefTime;
//            Op tto = tt.op();
//            Op bbo = bb.op();
//            if (tto==IMPL && bbo!=IMPL && !(task.isEternal() && !belief.isEternal())) {
//                beliefTime = true;
//                taskTime = false;
//            } else

//                if (tto!=IMPL && bbo==IMPL && !(belief.isEternal() && !task.isEternal())) {
//                beliefTime = false;
//                taskTime = true;
//            } else

            {
                beliefTime = taskTime = true;
            }

            if (taskTime) {
                know(task, d.taskTruth, d.taskAt);
            } else {
                know(tt);
            }


            if (!d.belief.equals(d.task)) {
                if (beliefTime) {
                    if (d.beliefAt==TIMELESS) {
                        //wtf
                        know(bb);
                    } else {
                        know(belief, d.beliefTruth, d.beliefAt);
                    }
                } else {
                    know(bb);
                }
            }
        } else {
            know(task, d.taskTruth, d.taskAt);
        }


    }

    void eventPolarities(Term tt, ObjectByteHashMap<Term> events) {

        tt.eventsWhile((w, t) -> {
            byte polarity;
            if (t.op() == NEG) {
                polarity = (byte) -1;
                t = t.unneg();
            } else
                polarity = (byte) +1;
            if (events.containsKey(t)) {
                byte p = events.get(t);
                if (p != polarity) {
                    events.put(t, (byte) 0);
                }
            } else {
                events.put(t, polarity);
            }
            return true;
        }, 0, true, true, true, 0);
    }


    /**
     * if current state of the derivation produced novel terms as a result of substitutions, etc
     */
    public DeriveTime get() {
        if (ifDynamic(task) != null || (belief != null && ifDynamic(belief) != null)) {
            return new DeriveTime(this);
        } else {
            return this;
        }
    }

    Term ifDynamic(Termed xt) {
        Term x = xt.term();
        Term y = x.eval(d);
        if (y != null && !(y instanceof Bool) && !y.equals(x)) {
            Collection<Event> existing = byTerm.get(y);
            for (Event ee : existing)
                if (ee instanceof Absolute)
                    return null; //transformed but already known (maybe only return 'x' if absolute times are known here)

            return y;
        } else {
            return null;
        }
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

//    public Term solveAndProject(Term pattern) {
//        Term c = solve(pattern);
//
//        if (c!=null && (d.taskPunct==BELIEF || d.taskPunct==GOAL)) {
//            TruthOperator f = d.truthFunction;
//            if (f == null)
//                return c; //not sure why this happens
//
//            long[] occ = d.concOcc;
//
//            float minConf = d.confMin;
//            boolean project = false;
//            Truth taskTruth = d.taskTruth;
//            int dur = d.dur;
//            ///if (!d.task.isDuringAll(occ)) {
//               // Truth tt = d.task.truth( d.task.furthestTimeOf( occ[0], occ[1]), dur, minConf);
//            if (!d.task.isDuringAny(occ)) {
//                Truth tt = d.task.truth( occ[0], occ[1], dur, minConf);
//                if (tt == null) {
//                    return null; //fail
//                } else if (!tt.equals(taskTruth)) {
//                    project = true;
//                    taskTruth = tt;
//                }
//            }
//            Truth beliefTruth;
//            if (!d.single && d.belief!=null) {
//                beliefTruth = d.beliefTruth;
////                if (!d.belief.isDuringAll(occ)) {
////                    Truth bb = d.belief.truth(d.belief.furthestTimeOf(occ[0], occ[1]), dur, minConf);
//                if (!d.belief.isDuringAny(occ)) {
//                    Truth bb = d.belief.truth(occ[0], occ[1], dur, minConf);
//                    if (bb == null) {
//                        return null; //fail
//                    } else if (!bb.equals(beliefTruth)) {
//                        project = true;
//                        beliefTruth = bb;
//                    }
//                }
//            } else {
//                beliefTruth = null;
//            }
//
//            if (project) {
//                //recalculate truth
//                Truth projected = f.apply(taskTruth, beliefTruth, d.nar);
//                if (projected == null)
//                    return null; //fail
//                d.concTruth = projected;
//            }
//        }
//        return c;
//    }


    @Override
    protected Random random() {
        return d.random;
    }

//    /**
//     * temporary override patches
//     */
//    protected Term override(Term pattern) {
//        //case ConjEventA: a conjunction pattern consisting of 2 precisely known events separated by an XTERNAL
//        if (pattern.op() == CONJ) {
//            if (pattern.dt() == XTERNAL) {
//                Term a = pattern.sub(0);
//                Event ae = absolute(a);
//                if (ae != null) {
//                    Term b = pattern.sub(1);
//                    Event be = absolute(b);
//                    if (be != null) {
//                        long aew = ae.when();
//                        long bew = be.when();
//                        if (aew == ETERNAL ^ bew == ETERNAL) {
//                            //mix of eternal and temporal, so simultaneous at the temporal
//                            long occ;
//                            if (aew == ETERNAL) {
//                                occ = bew;
//                            } else {
//                                occ = aew;
//                            }
//                            d.concOcc[0] = d.concOcc[1] = occ;
//                            //return Op.conjMerge(ae.id, 0, be.id, 0);
//                            return CONJ.the(ae.id, be.id);
//
//                        } else if (aew == ETERNAL) {
//                            //both eternal, so dternal
//                            d.concOcc[0] = d.concOcc[1] = ETERNAL;
//                            return pattern.dt(DTERNAL);
//                        } else {
//                            //both events
//                            long occ, dt;
//                            Term t;
//                            if (aew < bew) {
//                                dt = bew - aew;
//                                occ = aew;
//                                t = Op.conjMerge(ae.id, 0, be.id, (int) dt);
//                            } else {
//                                dt = aew - bew;
//                                occ = bew;
//                                t = Op.conjMerge(be.id, 0, ae.id, (int) dt);
//                            }
//                            if (Math.abs(dt) < Integer.MAX_VALUE - 1) {
//                                d.concOcc[0] = d.concOcc[1] = occ;
//                                return t;
//                            }
//                        }
//                    }
//                }
//            }
//        }
//
//        return null;
//    }

    public void know(Task t, Truth tr, long when) {
        if (when == TIMELESS)
            throw new RuntimeException();
        //assert (when != TIMELESS);

        Term tt = t.term();


        int taken = 0;
        if (when != ETERNAL && (!t.isEternal() && t.range() > 1)) {
            LongHashSet sampled = new LongHashSet(3);

            //HACK all points in time where the task's truth (used in the derivation's truth calculation) is constant are eligible to be sampled
            //TODO parameter for max subsampling
            long ts = t.start();
            taken += knowIfSameTruth(t, tt, tr, ts, sampled);

            long te = t.end();
            taken += knowIfSameTruth(t, tt, tr, te, sampled);

            long tm = t.mid();
            taken += knowIfSameTruth(t, tt, tr, tm, sampled);
        }

        if (taken == 0) {
            //use the direct point only
            event(tt, when, true);
        }
    }

    private int knowIfSameTruth(Task t, Term tt, Truth tr, long w, LongHashSet sampled) {
        if (sampled.add(w)) {
            if (t.isQuestionOrQuest() || tr.equalsIn(t.truth(w, d.dur), d.nar)) {
                event(tt, w, true);
                return 1;
            }
        }
        return 0;
    }


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

    public Term solve(Term pattern) {
        assert (pattern.op().conceptualizable);

//        Term overrideSolution = override(pattern);
//        if (overrideSolution != null) {
//            return overrideSolution;
//        }

        d.concOcc[0] = d.concOcc[1] = ETERNAL; //reset just in case

//        long[] occ = d.concOcc;

        Supplier<Term> solution = cache != null ? solveCached(pattern) : solveAll(pattern);
        if (solution == null)
            return null;
        else
            return solution.get();


    }


    @Nullable
    Term solveThe(Event event) {
        Term st = event.id;

        long es = event.when();
        if (es == TIMELESS) {
            return solveRaw(st);
        } else {
            if (!eternalCheck(es))
                return null;
            d.concOcc[0] = d.concOcc[1] = es;
            return st;
        }
    }


    @Nullable
    Function<long[], Term> solveMerged(ArrayHashSet<Event> solutions, int dur) {
        int ss = solutions.size();
        if (ss <= 1) return null; //callee will use the only solution by default

        boolean hasConjSeq = solutions.OR(x -> x.id.op()==CONJ && x.id.eventCount() > 0);
        if (hasConjSeq) {
            Conj c = null;
            List<Event> list = solutions.list;
            for (int i = 0, listSize = list.size(); i < listSize; i++) {
                Event e = list.get(i);
                long w = e.when();
                if (w!=TIMELESS) {
                    if (c == null)
                        c = new Conj(); //lazy

                    if (!c.add(e.id, w)) {
                        c = null;
                        break;
                    }
                }
            }
            if (c!=null) {
                //valid result
                Term t = c.term();
                if (t.op().conceptualizable) {
                    //assert(!(t instanceof Bool));
                    if (t.volume() <= d.termVolMax) {
                        long start = c.shift();
                        return (se) -> {
                            se[0] = se[1] = start;
                            return t;
                        };
                    }
                }
            }
        }

        //fallback method:

        SortedSetMultimap<Term, LongLongPair> m = MultimapBuilder.hashKeys(ss).treeSetValues().build();
        solutions.forEach(x -> {
            long w = x.when();
            if (w != TIMELESS)
                m.put(x.id, PrimitiveTuples.pair(w, w));
        });
        int ms = m.size();
        switch (ms) {
            case 0:
                return null;
            case 1:
                Map.Entry<Term, LongLongPair> ee = m.entries().iterator().next();
                LongLongPair ww = ee.getValue();
                long s = ww.getOne();
                long e = ww.getTwo();
                return (w) -> {
                    w[0] = s;
                    w[1] = e;
                    return ee.getKey();
                };

        }
        FasterList<Pair<Term, long[]>> choices = new FasterList(ms);

        //coalesce adjacent events
        m.asMap().forEach((t, cw) -> {
            int cws = cw.size();
            if (cws > 1) {
                long[][] ct = new long[cws][2];
                int i = 0;
                for (LongLongPair p : cw) {
                    long[] cc = ct[i++];
                    cc[0] = p.getOne();
                    cc[1] = p.getTwo();
                }
                //TODO more complete comparison
                long[] prev = ct[0];
                for (int j = 1; j < cws; j++) {
                    long[] next = ct[j];
                    if (prev[0] == ETERNAL) {
                        assert (j == 1);
                        assert (ct[0][0] == ETERNAL);
                        ct[0] = null; //ignore eternal solution amongst other temporal solutions
                    } else if (Math.abs(prev[1] - next[0]) <= dur) {
                        prev[1] = next[1]; //stretch
                        ct[j] = null;
                        continue;
                    }
                    prev = next;
                }
                for (int j = 0; j < cws; j++) {
                    long[] nn = ct[j];
                    if (nn != null)
                        choices.add(pair(t, nn));
                }
            } else {
                LongLongPair f = ((SortedSet<LongLongPair>) cw).first();
                choices.add(pair(t, new long[]{f.getOne(), f.getTwo()}));
            }
        });

        if (choices.size() > 1) {
//            //attempt sequence construction
//            Conj c = new Conj();
//            for (Pair<Term, long[]> x : choices){
//                long[] tt = x.getTwo();
//                if (!c.add(x.getOne(), tt[0],tt[1],2,dur))
//                    break;
//            }
//            Term seq = c.term();
//            if (seq.op().conceptualizable && seq.volume() < d.termVolMax) {
//                return (w) -> {
//                    long when = c.shift();
//                    w[0] = when;
//                    w[1] = when;
//                    return seq;
//                };
//            }

            //random fallback
            return (w) -> {
                Pair<Term, long[]> pp = choices.get(d.random);
                long[] cw = pp.getTwo();
                w[0] = cw[0];
                w[1] = cw[1];
                return pp.getOne();
            };

        } else {
            Pair<Term, long[]> c = choices.get(0);
            long[] cw = c.getTwo();
            Term cct = c.getOne();
            return (w) -> {
                w[0] = cw[0];
                w[1] = cw[1];
                return cct;
            };
        }
    }

//    @Nullable
//    static Term solveMerged0(ArrayHashSet<Event> solutions, int dur, long[] occ) {
//
//
//        final TreeSet<Term> eternals = new TreeSet();
//        solutions.forEach(s -> {
//            if (s.when() == ETERNAL) {
//                eternals.add(s.id);
//            }
//        });
//        final TreeSet<Term> eeternals = !eternals.isEmpty() ? eternals : null;
//
//        Term first = null;
//        boolean differentTimedTerms = false;
//        long min = Long.MAX_VALUE, max = Long.MIN_VALUE;
//        boolean timeless = false, timed = false;
////        int temporals = 0;
//
////        if (eeternals != null) {
////            solutions.removeIf(s -> s.when() == TIMELESS && eeternals.contains(s.id));
////        }
//
//        //remove any events that have been absorbed as eternals:
//        Iterator<Event> ii = solutions.iterator();
//        while (ii.hasNext()) {
//            Event e = ii.next();
//
//            long w = e.when();
//            if (w == TIMELESS) {
//
//                timeless = true;
//
//            } else if (w != ETERNAL) {
//                timed = true;
//                min = Math.min(min, w);
//                max = Math.max(max, w);
//                if (eeternals != null) {
//                    eeternals.remove(e.id); //prefer the temporal version, being more specific
//                }
//            }
//
//            if (first == null)
//                first = e.id;
//            else {
//                if (!first.equals(e.id)) {
//                    differentTimedTerms = true;
//                }
//            }
//            //if (e.id.op()==IMPL)
//            //if (e.id.op().temporal)
////            if (e.id.hasAny(Op.Temporal)) //is or has
////                temporals++;
//        }
//
////        Term eternalComponent = (eeternals == null) ? null : CONJ.the(DTERNAL, eeternals);
////
////        if (eternalComponent instanceof Bool)
////            eternalComponent = null; //ignore it
//
//        if (!differentTimedTerms) {
//            if (timed && eeternals == null && (max - min <= dur)) {
////                if (first.op().temporal) {
////                    occ[0] = occ[1] = min;
////                } else {
//                occ[0] = min;
//                occ[1] = max;
////                }
//                return first;
//            }
//            if (eeternals != null) {
//                return null;
//            }
//            if (timeless && eeternals == null) {
//                occ[0] = occ[1] = TIMELESS;
//                return first;
//            }
//
////            if (timeless && !timed) {
////                occ[0] = occ[1] = TIMELESS;
////                return eternalComponent != null ? CONJ.the(eternalComponent, first) : first;
////            } else if (timed && (max - min) <= dur) {
////                //all temporal and within a duration
////                occ[0] = min;
////                occ[1] = max;
////                if (eternalComponent != null) {
////                    Term c = CONJ.the(eternalComponent, first);
////                    if (c.dtRange()!=(max-min))
////                        return null; //some shift occurred, new occurrence needs recalculated
////                    else
////                        return c; //ok right time
////                } else {
////                    return first;
////                }
////            }
//        }
//
//        //TODO something like below but ensures correct occurence time in case of shift
//        return null;
////
////        if (timeless) {
////            if (!timed && eternalComponent == null) {
////                return null; //all timeless
////            } else {
////                //TODO mix of timeless and timed, can merge the time events at least and choose from either at random
////                //solutions.removeIf(x -> x.when() == TIMELESS); //ignore timeless events
////                return null;
////            }
////        }
////
////        if (temporals > 0) {
////            //TODO implications can be combined if they share common subj or predicate?
////            return null; //dont combine implication events
////        }
////
////
////
////        if (solutions.isEmpty()) {
////            occ[0] = occ[1] = ETERNAL;
////            return eternalComponent;
////        }
////
////
////        //construct sequence
////        Term temporalComponent = Op.conjEvents((FasterList) solutions.list);
////
////        if (temporalComponent instanceof Bool) {
////            return null; //the components may be ok individually
////        } else if (temporalComponent != null) {
////            occ[0] = min; //sequence start
////            occ[1] = min;
////
////            if (eternalComponent != null) {
////                return CONJ.the(eternalComponent, temporalComponent);
////            } else {
////                return temporalComponent;
////            }
////        } else {
////            return null;
////        }
////
//    }
//

    protected Supplier<Term> solveCached(Term pattern) {
        return cache.computeIfAbsent(pattern, this::solveAll);
    }


    protected Supplier<Term> solveAll(Term pattern) {
        ArrayHashSet<Event> solutions = new ArrayHashSet(Param.TEMPORAL_SOLVER_ITERATIONS*4);

        final int[] triesRemain = {Param.TEMPORAL_SOLVER_ITERATIONS};
        //final boolean[] rejectRelative = {false};

        Predicate<Event> each = (solution) -> {
            assert (solution != null);
            solutions.add(solution);
//            if (!solution.id.op().conceptualizable) {
//                //skip
//            } else if (solution instanceof Relative && rejectRelative[0]) {
//                //skip
//            } else {
//
//                if (!rejectRelative[0] && solution instanceof Absolute) {
//                    solutions.removeIf(x -> x instanceof Relative); //remove existing relative solutions now that an absolute exists
//                    rejectRelative[0] = true;
//                }
//
//                //TODO test equivalence with task and belief terms and occurrences, and continue iterating up to a max # of tries if it produced a useless equivalent result
//
//                Event first = solutions.first();
//
//                if (first == null) {
//                    solutions.add(solution);
//                } else {
//                    Event merged = merge(first, solution);
//                    if (merged == null) {
//                        //add alternate
//                        solutions.add(solution);
//                    } else if (merged == solution) {
//                        //replace all, this is the first fully valid one
//                        solutions.clear();
//                        solutions.add(solution);
//                    }
//                }
//            }

            return triesRemain[0]-- > 0;
        };

        seen.clear();
        solve(pattern, false /* take everything */, seen, each);

//        boolean neg = false;
//        if (solutions.isEmpty() && pattern.op()==NEG) {
//            solve(pattern.unneg(), false, each);
//            neg = true;
//        }

        Supplier<Term> x = solution(pattern, solutions);
//        if (x!=null && neg) {
//            return ()-> {
//                Term y = x.get();
//                return y.neg();
//            };
//        } else {
        return x;
//        }
    }

    protected Supplier<Term> solution(Term pattern, ArrayHashSet<Event> solutions) {
        int ss = solutions.size();
        if (ss == 0) {
            return () -> solveRaw(pattern);
        }


        int timed = ((FasterList) solutions.list).count(t -> t instanceof Absolute);
        if (timed > 0 && timed < ss) {
            if (solutions.removeIf(t -> t instanceof Relative)) //filter timeless
                ss = solutions.size();
        }

        switch (ss) {
            case 0:
                return () -> solveRaw(pattern);
            case 1:
                return () -> solveThe(solutions.first());
            default:

                //return solveRandomOne(solutions);
                //FasterList<Event> solutionsCopy = new FasterList(solutions.list);

                Function<long[], Term> tt = solveMerged(solutions, d.dur);
                if (tt == null) {
                    return () -> solveThe(solutions.get(random())); //choose one at random
                } else {
                    long[] when = new long[]{TIMELESS, TIMELESS};
                    Term ttt = tt.apply(when);
                    if (ttt == null || when[0] == TIMELESS)
                        return null; //should not happen
                    return () -> {
                        d.concOcc[0] = when[0];
                        d.concOcc[1] = when[1];
                        return ttt;
                    };
                }
        }
    }

    /**
     * eternal check: eternals can only be derived from completely eternal premises
     */
    private boolean eternalCheck(long l) {
        if (l == ETERNAL) {
            //if ((!d.task.isEternal()) && !(d.belief != null && !d.belief.isEternal()))
            return d.task.isEternal() || (d.belief != null && d.belief.isEternal());
        }
        return true;
    }

    /**
     * as a backup option
     */
    private Term solveRaw(Term x) {
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
                if ((p==BELIEF || p == GOAL)) {
                    boolean taskEvi = !task.isQuestionOrQuest();
                    boolean beliefEvi = !belief.isQuestionOrQuest();
                    if (taskEvi && beliefEvi) {
                        //EviDensity density = new EviDensity(d.dur, task, belief);
                        long[] u = Tense.union(task, belief);
                        s = u[0];
                        e = u[1];
                        //d.concEviFactor *= density.factor(); //TODO eternalization margin?
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
                    //question: use the interval union
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

        if (!eternalCheck(s))
            return null;

        long[] occ = d.concOcc;
        occ[0] = s;
        occ[1] = e;

        return x;
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

        long bstart = b.when();
        if (bstart != TIMELESS) {
            long astart = a.when();
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