package nars.derive.time;

import jcog.data.ArrayHashSet;
import jcog.data.ArraySet;
import jcog.list.FasterList;
import jcog.math.Interval;
import nars.Op;
import nars.Task;
import nars.control.Derivation;
import nars.task.Revision;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Bool;
import nars.term.subst.Subst;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;

import java.util.*;

import static nars.Op.CONJ;
import static nars.Op.IMPL;
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
    static final int TEMPORAL_ITERATIONS = 8;

    private final Task task, belief;

    private static final boolean knowTransformed = true;
    private final int dither;
    private final Derivation d;

    public DeriveTime(Derivation d, boolean single) {
        this.d = d;
        this.task = d.task;
        this.belief = d.belief; //!d.single ? d.belief : null;
        this.dither = Math.max(1, Math.round(d.nar.dtDither.floatValue() * d.dur));

        long taskStart = task.start();

        //Term taskTerm = polarizedTaskTerm(task);
        know(d, task, taskStart);


        if (!single && belief != null && !belief.equals(task)) {

            long beliefStart = belief.start();

            //Term beliefTerm = polarizedTaskTerm(belief);
            know(d, belief, beliefStart);

        } /*else if (!task.term().equals(d.beliefTerm)) {
            know(d, d.beliefTerm, TIMELESS);
        }*/

    }

    int dtDither(int dt) {
        if (dt == DTERNAL)
            return DTERNAL;
        if (dt == XTERNAL)
            return XTERNAL;

        if (dither > 1) {

            if (Math.abs(dt) < dither)
                return 0; //present moment

            //return Util.round(dt, dither);

        }

        return dt;
    }

    @Override
    protected Term dt(Term x, int dt) {
        int ddt = dtDither(dt);
        Term y = super.dt(x, ddt);
        if (y instanceof Bool && ddt!=dt) {
            //the dithered dt has destroyed it, so try the non-dithered (more precise) dt
            y = super.dt(x, dt);
        }
        return y;
    }

    public Term solve(Term pattern) {

//        if (taskStart == ETERNAL && task.isGoal() && belief!=null && !belief.isEternal()) {
//            //apply this as a temporal goal task at the present time, since present time does occur within the eternal task
//            taskStart = taskEnd = d.time;
//        }

        long[] occ = d.concOcc;

        Term tt = task.term();
        Term bb = d.beliefTerm;


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


        ArrayHashSet<Event> alternates = new ArrayHashSet(TEMPORAL_ITERATIONS);

        final int[] triesRemain = {TEMPORAL_ITERATIONS};

        solve(pattern, false /* take everything */, (solution) -> {
            assert (solution != null);
            //TODO test equivalence with task and belief terms and occurrences, and continue iterating up to a max # of tries if it produced a useless equivalent result

            Event first = alternates.first();
            if (first == null) {
                alternates.add(solution);
            } else {
                Event merged = merge(first, solution);
                if (merged == null) {
                    //add alternate
                    alternates.add(solution);
                } else if (merged == solution) {
                    //replace all, this is the first fully valid one
                    alternates.clear();
                    alternates.add(solution);
                }
            }

            return triesRemain[0]-- > 0;
        });


        Event event = alternates.first();
        if (event == null) {
            return solveRaw(pattern);
        } else if (alternates.size() > 1) {
            Map<Term, LongHashSet> uniques = new HashMap();
            alternates.forEach(x -> {
                long w = x.when();
                if (w!=TIMELESS && w!=ETERNAL)
                    uniques.computeIfAbsent(x.id, xx -> new LongHashSet()).add(w);
            });

            if (!uniques.isEmpty()) {
                //all alternates of the same term but at different points; so stretch a solution containing all of them

                ArrayHashSet<Map.Entry<Term, LongHashSet>> uu = new ArrayHashSet<>(uniques.entrySet());
                Map.Entry<Term, LongHashSet> h = uu.get(d.random);

                Term st = h.getKey();
                LongHashSet s = h.getValue();
                occ[0] = s.min();
                occ[1] = s.max() + st.dtRange();
                return st;
            } else {
                event = alternates.get(d.random);
            }
        }

        long es = event.when();
        Term st = event.id;
        if (es == TIMELESS) {
            return solveRaw(st);
        }

        occ[0] = es;
        occ[1] = es + (es!=ETERNAL ? st.dtRange() : 0);

        if (es == ETERNAL) {
            if (task.isEternal() && (belief==null || belief.isEternal())) {
                //its supposed to be eternal
            } else {

                if (task.isEternal() && (belief != null && !belief.isEternal())) {
                    es = belief.start();
                } else if (!task.isEternal() && (belief != null && belief.isEternal())) {
                    es = task.start();
                } else {
                    throw new RuntimeException("temporalization fault");
                }
                occ[0] = es;
                occ[1] = es + st.dtRange();
            }
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

    /** eternal check: eternals can only be derived from completely eternal premises */
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
        boolean te = task.isEternal();
        //couldnt solve the start time, so inherit from task or belief as appropriate
        if (!d.single && !te && (belief != null && !belief.isEternal())) {

                //joint is a procedure for extending / blending non-temporal terms.
                //since conj is temporal use strict
                boolean strict = x.op()==CONJ;

                if (strict) {
                    Interval ii = Interval.intersect(task.start(), task.end(), belief.start(), belief.end());
                    if (ii == null)
                        return null; //too distant, evidence lacks

                    s = ii.a;
                    e = x.op()!=IMPL ? ii.b : ii.a;
                } else {
                    Revision.TaskTimeJoint joint = new Revision.TaskTimeJoint(task.start(), task.end(), belief.start(), belief.end(), d.nar);
//                    if (joint.factor <= Pri.EPSILON) //allow for questions/quests, if this ever happens
//                        return null;

                    s = joint.unionStart;
                    e = joint.unionEnd;
                    d.concEviFactor *= joint.factor;
                }


        } else if (d.single || (!te && (belief == null || belief.isEternal()))) {
            s = task.start();
            e = task.end();
        } else {
            s = belief.start();
            e = belief.end();
        }

        eternalCheck(s);

        occ[0] = s;
        occ[1] = e;

        return x;
    }

    /**
     * heuristic for deciding a derivation result from among the calculated options
     */
    protected Event merge(Event a, Event b) {
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

//    /**
//     * negate if negated, for precision in discriminating positive/negative
//     */
//    static Term polarizedTaskTerm(Task t) {
//        Truth tt = t.truth();
//        return t.term().negIf(tt != null && tt.isNegative());
//    }

    @Override
    protected Random random() {
        return d.random;
    }

    void know(Subst d, Termed x, long start) {

        if (x instanceof Task)
            know((Task) x);
        else
            know(x.term());

        if (knowTransformed) {
            Term y = //x.transform(d);
                    x.term().eval(d);
            if (y != null && !y.equals(x) && !(y instanceof Bool)) {
                know(y, start);
            }
        }
    }


}
