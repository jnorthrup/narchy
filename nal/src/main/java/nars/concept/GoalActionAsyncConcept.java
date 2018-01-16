package nars.concept;

import jcog.math.FloatSupplier;
import nars.NAR;
import nars.NAct;
import nars.Task;
import nars.control.CauseChannel;
import nars.task.ITask;
import nars.task.util.PredictionFeedback;
import nars.term.Term;
import nars.truth.Truth;
import nars.util.signal.Signal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.LongSupplier;
import java.util.stream.Stream;

import static nars.Op.BELIEF;


/**
 * ActionConcept which is driven by Goals that are interpreted into feedback Beliefs
 */
public class GoalActionAsyncConcept extends ActionConcept {


    public final Signal feedBelief;
//    @Deprecated public final Signal feedGoal;


    @NotNull
    private final BiConsumer<GoalActionAsyncConcept, Truth /* goal */> motor;
    final CauseChannel<ITask> in;

    private final long curiosityStamp;

    public GoalActionAsyncConcept(@NotNull Term c, @NotNull NAct act, CauseChannel<ITask> cause, @NotNull BiConsumer<GoalActionAsyncConcept, Truth /* goal */> motor) {
        super(c,
                //new SensorBeliefTable(n.conceptBuilder.newTemporalBeliefTable(c)),
                //new SensorBeliefTable(n.conceptBuilder.newTemporalBeliefTable(c)),
                null, null,
                act.nar());

        NAR n = act.nar();

//        this.action = new Signal(GOAL, n.truthResolution).pri(() -> n.priDefault(GOAL));
        //((SensorBeliefTable) goals).sensor = action;

        this.in = cause;
        curiosityStamp = n.time.nextStamp();

        //for dynamic change
        final FloatSupplier myResolution = () -> this.resolution.asFloat();

        this.feedBelief = new Signal(BELIEF, myResolution).pri(() -> n.priDefault(BELIEF));
        //((SensorBeliefTable) beliefs).sensor = feedback;

//        this.feedGoal = new Signal(GOAL, myResolution).pri(() -> n.priDefault(GOAL));

        this.motor = motor;
        //this.goals = newBeliefTable(nar, false); //pre-create

    }



    @Override
    public Stream<ITask> update(long now, int dur, NAR nar) {

        long pStart =
                //now;
                now - dur/2;
        long pEnd =
                //now;
                now + dur/2;

        Truth goal = this.goals().truth(pStart, pEnd, nar);

//            //HACK EXPERIMENT combine belief and goal
//            int shift =
//                    0;
//                    //+dur/2;
//            Truth belief = this.beliefs().truth(pStart+shift, pEnd+shift, nar);
//            if (belief!=null) {
//
//                float hope = belief.eviEternalized();
//
//                if (goal == null) {
//                    goal = belief.withEvi(hope); //what one images will happen maybe is what one wants
//                } else {
//                    goal = Revision.revise(goal, belief.withEvi(hope), Math.abs(belief.freq()-goal.freq()), 0 );
//                }
//
//            }

        this.motor.accept(this, goal);

        return null;
    }


    public void feedback(@Nullable Truth f, @Nullable Truth g, NAR nar) {

        long now = nar.time();
        int dur = nar.dur();


        Task fg;
//        long goalTime =
//                now;
//                //now-dur/2;
        long beliefTime =
                now;
                //now+dur/2;

        if (g!=null) {
//            //fg = feedGoal.task(term, g, goalTime-dur, goalTime, nar.time.nextStamp()); //allow the feedback goal (Ex: curiosity) to override, otherwise use the current goal
//            fg = feedGoal.set(this, g, stamper, goalTime, dur, nar);
            fg = GoalActionConcept.curiosity(nar, g, term, curiosityStamp);
        }
        else
            fg = null;

        LongSupplier stamper = nar.time::nextStamp;

        in.input(
            fg,
            feedBelief.set(this, f, stamper, beliefTime, dur, nar)
        );

        PredictionFeedback.accept(feedBelief.get() /* in case stretched */, beliefs, nar);
    }

    //not working yet:
//    public void resolution(float v) {
//        this.resolution = new MutableFloat(v);
//    }


    //    Truth[] linkTruth(long when, long now, float minConf) {
//        List<Truth> belief = $.newArrayList(0);
//        List<Truth> goal = $.newArrayList(0);
//
//        int dur = nar.dur();
//
//        int numTermLinks = termlinks().size();
//        if (numTermLinks > 0) {
//            float termLinkFeedbackRate = 1f / numTermLinks; //conf to priority boost conversion rate
//            termlinks().forEach(tll -> {
//                float g = linkTruth(tll.get(), belief, goal, when, now, dur);
//                if (g > 0)
//                    tll.priAdd(g * termLinkFeedbackRate);
//            });
//        }
//        int numTaskLinks = tasklinks().size();
//        if (numTaskLinks > 0) {
//            float taskLinkFeedbackRate = 1f / numTaskLinks; //conf to priority boost conversion rate
//            tasklinks().forEach(tll -> {
//                Task task = tll.get();
//                if (!task.isDeleted()) {
//                    float g = linkTruth(task.term(), belief, goal, when, now, dur);
//                    if (g > 0)
//                        tll.priAdd(g * taskLinkFeedbackRate);
//                }
//            });
//        }
//
//
//        Truth b = Revision.revise(belief, minConf);
//        Truth g = Revision.revise(goal, minConf);
//        //System.out.println(belief.size() + "=" + b + "\t" + goal.size() + "=" + g);
//
//        return new Truth[]{b, g};
//
//    }
//
//    private float linkTruth(Term t, List<Truth> belief, List<Truth> goal, long when, long now, int dur) {
//        float gain = 0;
//
//        t = nar.post(t);
//
//        Compound thisTerm = term();
//        if (t.op() == IMPL) {
//            //    B, (A ==> C), task(positive), time(decomposeBelief) |- subIfUnifiesAny(C,A,B), (Belief:Deduction, Goal:Induction)
//
//            Compound ct = (Compound) t;
//            Term postCondition = ct.term(1);
//
//            if (postCondition.equals(thisTerm)) {
//
//
//                //a termlink to an implication in which the postcondition is this concept
//                Concept implConcept = nar.concept(t);
//                if (implConcept != null) {
//
//                    //TODO match the task and subtract the dt
//                    Task it = implConcept.beliefs().match(when, now, dur); //implication belief
//                    if (it != null) {
//                        int dt = it.dt();
//                        if (dt == DTERNAL)
//                            dt = 0;
//
//                        Truth itt = it.truth();
//                        Term preCondition = nar.post(ct.term(0));
//
//                        gain += linkTruthImpl(itt, preCondition, when - dt, now, belief, goal, nar);
//                    }
//                }
//            }
//        } else if (t.op() == CONJ) {
//            //TODO
//        } else if (t.op() == EQUI) {
//            //TODO
//            Compound c = (Compound) t;
//            Term other = null;
//            boolean first = false;
//
//            //TODO handle negated case
//
//            if (c.term(0).equals(thisTerm)) {
//                other = c.term(1);
//                first = true;
//            } else if (c.term(1).equals(thisTerm)) {
//                other = c.term(0);
//                first = false;
//            }
//
//            if (other != null && !other.equals(thisTerm)) {
//
//                //a termlink to an implication in which the postcondition is this concept
//                Concept equiConcept = nar.concept(t);
//                if (equiConcept != null) {
//
//                    //TODO refactor to: linkTruthEqui
//
//
//                    //TODO match the task and subtract the dt
//                    Task it = equiConcept.beliefs().match(when, now, dur); //implication belief
//                    if (it != null) {
//                        int dt = it.dt();
//                        if (dt == DTERNAL)
//                            dt = 0;
//                        if (!first)
//                            dt = -dt;
//
//                        long whenActual = when + dt;
//
//                        Truth itt = it.truth();
//
//
//                        Concept otherConcept = nar.concept(other);
//                        if (otherConcept != null) {
//                            //    B, (A <=> C), belief(positive), time(decomposeBelief), neqCom(B,C) |- subIfUnifiesAny(C,A,B), (Belief:Analogy, Goal:Deduction)
//
//                            Truth pbt = otherConcept.belief(whenActual, now, nar.dur());
//                            if (pbt != null) {
//                                Truth y = TruthFunctions.analogy(pbt, itt, 0);
//                                if (y != null) {
//                                    belief.add(y);
//                                    gain += y.conf();
//                                }
//                            }
//
//                            Truth pgt = otherConcept.belief(whenActual, now, nar.dur());
//                            if (pgt != null) {
//                                Truth y = TruthFunctions.deduction(pbt, itt, 0);
//                                if (y != null) {
//                                    goal.add(y);
//                                    gain += y.conf();
//                                }
//
//                            }
//
//                        }
//                    }
//                }
//            }
//        }
//
//        return gain;
//    }
//
//
//    public static float linkTruthImpl(Truth itt, Term preCondition, long when, long now, List<Truth> belief, List<Truth> goal, NAR nar) {
//        float gain = 0;
//
//        boolean preCondNegated = preCondition.op() == NEG;
//
//        Concept preconditionConcept = nar.concept(preCondition);
//        if (preconditionConcept != null) {
//
//            //belief = deduction(pbt, it)
//            Truth pbt = preconditionConcept.belief(when, now, nar.dur());
//            if (pbt != null) {
//                Truth y = TruthFunctions.deduction(pbt.negIf(preCondNegated), itt, 0 /* gather anything */);
//                if (y != null) {
//                    belief.add(y);
//                    gain += y.conf();
//                }
//            }
//
//            //goal = induction(pgt, it)
//            Truth pgt = preconditionConcept.goal(when, now, nar.dur());
//            if (pgt != null) {
//                Truth y = TruthFunctions.induction(pgt.negIf(preCondNegated), itt, 0, /* gather anything */dur);
//                if (y != null) {
//                    goal.add(y);
//                    gain += y.conf();
//                }
//            }
//
//        }
//
//        return gain;
//    }

//    @Override
//    protected BeliefTable newBeliefTable(NAR nar, boolean beliefOrGoal, int eCap, int tCap) {
//        if (beliefOrGoal) {
//            //belief
//            return super.newBeliefTable(nar, beliefOrGoal, eCap, tCap);
//        } else {
//            //goal
//            return new DefaultBeliefTable(
//                    newEternalTable(eCap),
//                    newTemporalTable(tCap, nar)
//            ) {
//
//        }
//
//    }


//    @NotNull
//    @Override
//    protected BeliefTable newBeliefTable(int eCap, int tCap) {
//        return new SensorBeliefTable(tCap);
//    }
//
//    private final class SensorBeliefTable extends DefaultBeliefTable {
//
//        public SensorBeliefTable(int tCap) {
//            super(tCap);
//        }
//
//        @Override
//        public Truth truth(long when, long now) {
////            if (when == now || when == ETERNAL)
////                return sensor.truth();
//
//            // if when is between the last input time and now, evaluate the truth at the last input time
//            // to avoid any truth decay across time. this emulates a persistent latched sensor value
//            // ie. if it has not changed
//            if (nextFeedback !=null && when <= now && when >= nextFeedback.occurrence()) {
//                //now = when = sensor.lastInputTime;
//                return nextFeedback.truth();
//            } else {
//                return super.truth(when, now);
//            }
//        }
//
//        @Override
//        public Task match(@NotNull Task target, long now) {
//            long when = target.occurrence();
//
//            Task f = ActionConcept.this.nextFeedback;
//            if (f !=null && when <= now && when >= f.occurrence()) {
//                //but project it to the target time unchanged
//                return MutableTask.clone(f, now);
//            }
//
//            return super.match(target, now);
//        }
//
//        //        @Override
////        public Task match(@NotNull Task target, long now) {
////            long when = target.occurrence();
////            if (when == now || when == ETERNAL) {
////                sensor.
////                return sensor.truth();
////            }
////
////            return super.match(target, now);
////        }
//    }
//


}
