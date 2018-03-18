package nars.concept.action;

import jcog.math.FloatRange;
import nars.NAR;
import nars.NAct;
import nars.Task;
import nars.concept.dynamic.ScalarBeliefTable;
import nars.task.ITask;
import nars.task.signal.SignalTask;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.stream.Stream;

import static nars.Op.GOAL;
import static nars.truth.TruthFunctions.c2w;


/**
 * ActionConcept which is driven by Goals that are interpreted into feedback Beliefs
 */
public class GoalActionConcept extends ActionConcept {

    //private final Signal feedback;
    private final ScalarBeliefTable feedback;

    private final FloatRange curiosity;

    private final MotorFunction motor;


    public GoalActionConcept(@NotNull Term c, @NotNull NAct act, @NotNull MotorFunction motor) {
        this(c, act.nar(), act.curiosity(), motor);
    }

    public GoalActionConcept(@NotNull Term c, @NotNull NAR n, @NotNull FloatRange curiosity, @NotNull MotorFunction motor) {
        super(c, n);

        this.curiosity = curiosity;

//        this.action = new Signal(GOAL, n.freqResolution).pri(() -> n.priDefault(GOAL));
        //((SensorBeliefTable) goals).sensor = action;

        this.feedback = (ScalarBeliefTable)beliefs();
        //this.feedback = new Signal(BELIEF, resolution).pri(() -> n.priDefault(BELIEF));
        //((SensorBeliefTable) beliefs).sensor = feedback;


        this.motor = motor;
        //this.goals = newBeliefTable(nar, false); //pre-create


    }



    @Override
    public Stream<ITask> update(long pStart, long pEnd, int dur, NAR nar) {


//        long pStart =
//                //now;
//                start - dur / 2;
//        long pEnd =
//                //now;
//                start + dur / 2;
//                //now + dur;


        float cur = curiosity.floatValue();


        Truth goal;

        goal = this.goals().truth(pStart, pEnd, nar);

//        if (goals.size() > 0)
//            System.err.println(term + " " + goal.freq() + " " + goal.evi() + " " + goal.conf());

        boolean curi;
        if (nar.random().nextFloat() < cur * (1f - (goal!=null ? goal.conf() : 0))) {
//            // curiosity override
//
            float curiConf =
                    nar.confDefault(GOAL); //<- to max out expectation-driven action

                    //nar.confMin.floatValue() * 2;
//                    Math.max(goal != null ? goal.conf() : 0, //match goal conf
//                            //nar.confMin.floatValue() * 2
//                            nar.confDefault(GOAL)
//                    );
                    //nar.confMin.floatValue() * 4;

            curi = true;
            //nar.confDefault(GOAL) * CURIOSITY_CONF_FACTOR;
//                    Math.max(goal != null ? goal.conf() : 0,
//                            nar.confDefault(GOAL) * CURIOSITY_CONF_FACTOR);

            //nar.confMin.floatValue()*2);
//
////            float cc =
////                    //curiConf;
////                    curiConf - (goal != null ? goal.conf() : 0);
////            if (cc > 0) {
//
////                    ((float)Math.sin(
////                        hashCode() /* for phase shift */
////                            + now / (curiPeriod * (2 * Math.PI) * dur)) + 1f)/2f;
//
            goal = Truth.theDithered(nar.random().nextFloat(), c2w(curiConf), nar);
            //curiosityGoal = null;

//            curious = true;
//
////                Truth ct = $.t(f, cc);
////                goal = ct; //curiosity overrides goal
//
////                if (goal == null) {
////                    goal = ct;
////                } else {
////                    goal = Revision.revise(goal, ct);
////                }


        } else {
            curi = false;

            //action.set(term(), null, stamper, now, dur, nar);


            //HACK EXPERIMENT combine belief and goal
            //if (belief!=null) {

            //float hope = belief.eviEternalized();

            //if (goal == null) {
            //goal = belief.withEvi(hope); //what one images will happen maybe is what one wants
            //} else {
            //goal = Revision.revise(goal, belief.withEvi(hope), Math.abs(belief.freq()-goal.freq()), 0 );
            //}

            //}

//            fg = action.set(this, goal, stamper, now, dur, nar);
        }


        Truth belief = this.beliefs().truth(pStart, pEnd, nar);

        Truth feedback = this.motor.apply(belief, goal);

        Task feedbackBelief = feedback!=null ? this.feedback.add(feedback, pStart, pEnd, nar.time.nextStamp()) : null;

        Task curiosityGoal = null;
        if (curi && feedbackBelief!=null) {
            long curiosityStamp = nar.time.nextStamp();
            curiosityGoal = this.curiosity(nar,
                    //goal,
                    Truth.theDithered(feedbackBelief.freqMean(dur, pStart, pEnd), goal.evi(), nar),
                    term, pStart, pEnd, curiosityStamp);
        }

        return Stream.of(feedbackBelief, (ITask)curiosityGoal).filter(Objects::nonNull);
        //return Stream.of(fb, fg).filter(Objects::nonNull);
        //return Stream.of(fb).filter(Objects::nonNull);
    }


    static SignalTask curiosity(NAR nar, Truth goal, Term term, long pStart, long pEnd, long curiosityStamp) {
        long now = nar.time();

        SignalTask curiosity = new SignalTask(term, GOAL, goal, now, pStart, pEnd, curiosityStamp);
        //curiosity.setCyclic(true);
        curiosity.pri(nar.priDefault(GOAL));
        //curiosity.pri(0);

        return curiosity;
    }


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

    //    @Override
//    public boolean validBelief(@NotNull Task t, @NotNull NAR nar) {
//        if (!t.isEternal() && t.occurrence() > nar.time() + 1) {
//            System.err.println("prediction detected: " + (t.occurrence() - nar.time()));
//        }
//        return true;
//    }
//
//    @Override
//    public boolean validGoal(@NotNull Task t, @NotNull NAR nar) {
//        if (!t.isEternal() && t.occurrence() > nar.time() + 1) {
//            System.err.println("prediction detected: " + (t.occurrence() - nar.time()));
//        }
//        return true;
//    }


//    @Override
//    public @NotNull Task filterGoals(@NotNull Task t, @NotNull NAR nar, List<Task> displaced) {
//        return t;
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
