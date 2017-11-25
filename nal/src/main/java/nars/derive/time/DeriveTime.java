package nars.derive.time;

import jcog.math.Interval;
import jcog.pri.Pri;
import nars.Op;
import nars.Task;
import nars.control.Derivation;
import nars.task.Revision;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Bool;
import nars.term.subst.Subst;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

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
    public static final int TEMPORAL_ITERATIONS = 8;

    private final Task task, belief;

    protected static final boolean knowTransformed = true;
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


        if (d.single) {
            //single

            if (!tt.isTemporal()) {
                //simple case: inherit task directly
                occ[0] = task.start();
                occ[1] = task.end();
                return pattern;
            }


        } else {
            //double

        }


        Event[] best = new Event[1];

        final int[] triesRemain = {TEMPORAL_ITERATIONS};

        solve(pattern, false /* take everything */, (solution) -> {
            assert (solution != null);
            //TODO test equivalence with task and belief terms and occurrences, and continue iterating up to a max # of tries if it produced a useless equivalent result

            Event current = best[0];
            best[0] = (current == null) ? solution : merge(current, solution);

            return triesRemain[0]-- > 0;
        });

        Event event = best[0];
        if (event == null) {
            return solveRaw(pattern);
        }

        long es = event.start();
        Term st = event.id;
        if (es == TIMELESS) {
            return solveRaw(st);
        }
        occ[0] = es;
        occ[1] = event.end();


        Op eop = st.op();
        if (!eop.conceptualizable) {
            return null;
        }

        return st;
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

                if (!strict) {
                    Interval ii = Interval.intersect(task.start(), task.end(), belief.start(), belief.end());
                    if (ii == null)
                        return null; //too distant, evidence lacks

                    s = ii.a;
                    e = x.op()!=IMPL ? ii.b : ii.a;
                } else {
                    Revision.TaskTimeJoint joint = new Revision.TaskTimeJoint(task.start(), task.end(), belief.start(), belief.end(), d.nar);
                    if (joint.factor <= Pri.EPSILON)
                        return null;

                    s = joint.unionStart;
                    e = joint.unionEnd;
                    d.concConfFactor *= joint.factor;
                }


        } else if (d.single || !te || belief == null || belief.isEternal()) {
            s = task.start();
            e = task.end();
        } else {
            s = belief.start();
            e = belief.end();
        }

        occ[0] = s;
        occ[1] = e;

        return x;
    }

    /**
     * heuristic for deciding a derivation result from among the calculated options
     */
    protected static Event merge(Event a, Event b) {
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

        //heuristic: prefer more specific "dense" temporal events rather than sprawling sparse run-on-sentences
        float aSpec = ((float) at.volume()) / at.dtRange();
        float bSpec = ((float) bt.volume()) / bt.dtRange();
        if (bSpec > aSpec)
            return b;
        else //if (aSpec < bSpec)
            return a;
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
