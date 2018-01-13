package nars.derive.time;

import jcog.Util;
import jcog.data.ArrayHashSet;
import nars.Op;
import nars.Task;
import nars.control.Derivation;
import nars.task.TimeFusion;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Bool;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static nars.Op.CONJ;
import static nars.time.Tense.*;


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
 *
 * @param eviGain length-1 float array. the value will be set to 1f by default
 */
public class DeriveTime extends TimeGraph {

    //    private final static Logger logger = LoggerFactory.getLogger(DeriveTime.class);
    static final int TEMPORAL_ITERATIONS = 16;

    private final Task task, belief;

    private final int dither;
    private final Derivation d;

    final Map<Term, ArrayHashSet<Event>> cache;

    @Override
    public void clear() {
        super.clear();
        cache.clear();
    }

    DeriveTime(DeriveTime copy, Term transformedTask, Term transformedBelief) {
        this.cache = null;
        this.d = copy.d;
        this.task = copy.task;
        this.belief = copy.belief;
        this.dither = copy.dither;
        this.byTerm.putAll(copy.byTerm); //TODO wrap rather than copy
        if (transformedTask != null) {
            Event y = know(transformedTask, task.start());
            //link(know(task.term(), task.start()), 0, y);
            if (autoNegEvents && transformedTask.op()!=CONJ)
                know(transformedTask.neg(), task.start());
            //link(know(task.term().neg(), task.start()), 0, yNeg);
        }
        if (transformedBelief != null) {
            if (belief!=null) {
                Event y = know(transformedBelief, belief.start());
                //link(know(belief.term(), belief.start()), 0, y);
                if (autoNegEvents && transformedBelief.op()!=CONJ)
                    know(transformedBelief.neg(), belief.start());
                //link(know(belief.term().neg(), belief.start()), 0, yNeg);
            }
        }
    }

//    DeriveTime(DeriveTime copy, Map<Term, Term> dyn) {
//        this.cache = null;
//        this.d = copy.d;
//        this.task = copy.task;
//        this.belief = copy.belief;
//        this.dither = copy.dither;
//        dyn.forEach((x,y)->{
//           link(know(x), 0, know(y));
//        });
//    }

    public DeriveTime(Derivation d, boolean single) {
        this.d = d;
        this.task = d.task;
        this.belief = d.belief; //!d.single ? d.belief : null;
        this.dither = Math.max(1, Math.round(d.nar.dtDither.floatValue() * d.dur));
        this.cache = new HashMap();

        know(task);

        if (!single && d._belief != null && !d._belief.equals(d._task)) {

            know(belief);

        } /*else if (!task.term().equals(d.beliefTerm)) {
            know(d, d.beliefTerm, TIMELESS);
        }*/

    }

    /**
     * if current state of the derivation produced novel terms
     */
    public DeriveTime get() {
//        if (!d.xyDyn.isEmpty()) {
//            return new DeriveTime(this, d.xyDyn);
//        }

        Term td = ifDynamic(d.task);
        Term bd = d.belief != null ? ifDynamic(d.belief) : null /*ifDynamic(d.beliefTerm)*/;
        boolean tChange = td != null;
        boolean bChange = bd != null;
        if (tChange || bChange) {
            return new DeriveTime(this, tChange ? td : null, bChange ? bd : null);
        } else {
        return this;
        }
    }

    Term ifDynamic(Termed x) {
        Term xterm = x.term();
        Term y = xterm.eval(d);
        if (y != null && !(y instanceof Bool) && !y.equals(xterm)) {
            Collection<Event> existing = byTerm.get(y);
            for (Event ee : existing)
                if (ee instanceof Absolute)
                    return null; //transformed but already known (maybe only return 'x' if absolute times are known here)

            return y;
        } else {
            return null;
        }
    }

    int dtDither(int dt) {
        if (dt == DTERNAL)
            return DTERNAL;
        if (dt == XTERNAL)
            return XTERNAL;

        if (dither > 1) {

            if (Math.abs(dt) < dither)
                return 0; //present moment

            return Util.round(dt, dither);

        }

        return dt;
    }

    @Override
    protected Term dt(Term x, int dt) {
        int ddt = dtDither(dt);
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

    public Term solve(Term pattern) {

//        if (taskStart == ETERNAL && task.isGoal() && belief!=null && !belief.isEternal()) {
//            //apply this as a temporal goal task at the present time, since present time does occur within the eternal task
//            taskStart = taskEnd = d.time;
//        }

        long[] occ = d.concOcc;

//        Term tt = polarizedTaskTerm(task);
//        Term bb = d.beliefTerm;


//        if (d.single) {
//            //single
//
////            if (!tt.isTemporal()) {
////                //simple case: inherit task directly
////                occ[0] = task.start();
////                occ[1] = task.end();
////                return pattern;
////            }
//
//
//        } else {
//            //double
//
//        }

        ArrayHashSet<Event> solutions = cache != null ? solveCached(pattern) : solveAll(pattern);

        Event event = solutions.first();
        if (event == null) {
            return solveRaw(pattern);
        } else if (solutions.size() > 1) {
//            Map<Term, LongHashSet> uniques = new HashMap();
//            alternates.forEach(x -> {
//                long w = x.when();
//                if (w!=TIMELESS && w!=ETERNAL)
//                    uniques.computeIfAbsent(x.id, xx -> new LongHashSet()).add(w);
//            });
//            if (!uniques.isEmpty()) {
//                //all alternates of the same term but at different points; so stretch a solution containing all of them
//
//                ArrayHashSet<Map.Entry<Term, LongHashSet>> uu = new ArrayHashSet<>(uniques.entrySet());
//                Map.Entry<Term, LongHashSet> h = uu.get(d.random);
//
//                Term st = h.getKey();
//                LongHashSet s = h.getValue();
//                occ[0] = s.min();
//                occ[1] = s.max() + st.dtRange();
//                return st;
//            } else {

            if (d.single ? d.task.isEternal() : d.eternal) {
                event = solutions.get(d.random); //doesnt really matter which solution is chosen, in terms of probability of projection success
            } else {

                event = solutions.get(d.random);


                //choose event with least distance to task and belief occurrence so that projection has best propensity for non-failure

                //solutions.shuffle(d.random); //shuffle so that equal items are selected fairly

//                /* weight the influence of the distance to each
//                   according to its weakness (how likely it is to null during projection). */
//                float taskWeight =
//                        //task.isBeliefOrGoal() ? (0.5f + 0.5f * (1f - task.conf())) : 0f;
//                        0.5f;
//                float beliefWeight =
//                        //belief!=null ? (0.5f + 0.5f * (1f - belief.conf())) : 0;
//                        0.5f;
//
//                final float base = 1f/solutions.size();
//                event = solutions.roulette((e) -> {
//                    long when = e.when();
//                    if (when == TIMELESS)
//                        return base/2f;
//                    if (when == ETERNAL)
//                        return base; //prefer eternal only if a temporal solution does not exist
//
//                    long distance = 1;
//                    distance += task.minDistanceTo(when) * taskWeight;
//
//                    if (!d.single && belief!=null)
//                        distance += belief.minDistanceTo(when) * beliefWeight;
//
//                    return 1f/distance;
//                }, d.nar.random());
            }
//            }
        }

        long es = event.when();
        Term st = event.id;
        if (es == TIMELESS) {
            return solveRaw(st);
        }

        occ[0] = es;
        occ[1] = es;

        if (es == ETERNAL) {

            if (task.isEternal() && (belief == null || belief.isEternal())) {
                //its supposed to be eternal
            } else {
                //throw new RuntimeException("temporalization fault");
                return null;
            }

//            if (task.isEternal() && (belief==null || belief.isEternal())) {
//                //its supposed to be eternal
//            } else {
//
//                if (task.isEternal() && (belief != null && !belief.isEternal())) {
//                    es = belief.start();
//                } else if (!task.isEternal() && (belief != null && belief.isEternal())) {
//                    es = task.start();
//                } else {
//                    throw new RuntimeException("temporalization fault");
//                }
//                occ[0] = es;
//                occ[1] = es;
//            }
        }
//        if (occ[0] != ETERNAL) {
//            if (st.op()!=CONJ && occ[1]==occ[0]) {
//                //HACK lengthen non-conjunction to match the non-eternal timing of an eternal + temporal premise
//                //TODO this should be handled by TimeGraph entirely
//                Task matchRange = null;
//                if (belief!=null && !belief.isEternal() && task.isEternal())
//                    matchRange = belief;
//                else if (!task.isEternal() && (belief==null || belief.isEternal()))
//                    matchRange = task;
//                //else: //TODO use intersection of task and belief
//                if (matchRange!=null) {
//                    long l = matchRange.range();
//                    occ[1] = occ[0] + l;
//                }
//
//            }
//        }

        eternalCheck(occ[0]);

        Op eop = st.op();
        return !eop.conceptualizable ? null : st;

    }


    protected ArrayHashSet<Event> solveCached(Term pattern) {
        return cache.computeIfAbsent(pattern, this::solveAll);
    }


    protected ArrayHashSet<Event> solveAll(Term pattern) {
        ArrayHashSet<Event> solutions = new ArrayHashSet(TEMPORAL_ITERATIONS);

        final int[] triesRemain = {TEMPORAL_ITERATIONS};

        solve(pattern, false /* take everything */, (solution) -> {
            assert (solution != null);
            //TODO test equivalence with task and belief terms and occurrences, and continue iterating up to a max # of tries if it produced a useless equivalent result

            Event first = solutions.first();
            if (first == null) {
                solutions.add(solution);
            } else {
                Event merged = merge(first, solution);
                if (merged == null) {
                    //add alternate
                    solutions.add(solution);
                } else if (merged == solution) {
                    //replace all, this is the first fully valid one
                    solutions.clear();
                    solutions.add(solution);
                }
            }

            return triesRemain[0]-- > 0;
        });

        return !solutions.isEmpty() ? solutions : ArrayHashSet.EMPTY;
    }

    /**
     * eternal check: eternals can only be derived from completely eternal premises
     */
    private void eternalCheck(long l) {
        if (l == ETERNAL) {
            //if ((!d.task.isEternal()) && !(d.belief != null && !d.belief.isEternal()))
            if (!d.task.isEternal() || (!d.single && !d.belief.isEternal()))
                throw new RuntimeException("ETERNAL leak");
        }
    }

    /**
     * as a backup option
     */
    private Term solveRaw(Term x) {
        long[] occ = d.concOcc;
        long s, e;
        boolean taskEvent = !task.term().op().temporal;
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

            boolean beliefEvent = belief != null && !belief.term().op().temporal;
            if (belief == null || (belief.isEternal())) {
                if (!taskEvent) {
                    //transformed task term, should have been solved
                    return null;
                } else {
                    //event: inherit task time
                    if (beliefEvent) {
                        s = task.start();
                        e = task.end();
                    } else {
                        return null; //should have solution
                    }
                }
            } else {
//                if (taskEvent && beliefEvent) {
                //two events: fuse time
                assert (!belief.isEternal());
                TimeFusion joint = new TimeFusion(task.start(), task.end(), belief.start(), belief.end());
                //                    if (joint.factor <= Pri.EPSILON) //allow for questions/quests, if this ever happens
                //                        return null;

                s = joint.unionStart;
                e = joint.unionEnd;
                if (s == ETERNAL)
                    throw new RuntimeException("why eternal");
                d.concEviFactor *= joint.factor;
//                } else {
//                    //either task or belief were temporal, so should have been solved
//                    return null;
//                }
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

        eternalCheck(s);

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

    }


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


}
