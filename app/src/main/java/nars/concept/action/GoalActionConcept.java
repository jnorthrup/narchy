package nars.concept.action;

import nars.NAR;
import nars.agent.Game;
import nars.term.Term;
import nars.truth.Truth;


/**
 * ActionConcept which is driven by Goals that are interpreted into feedback Beliefs
 */
public class GoalActionConcept extends AbstractGoalActionConcept {

    private final MotorFunction motor;

    public GoalActionConcept(Term term, NAR n, MotorFunction motor) {
        super(term, n);

        this.motor = motor;
    }

    @Override
    public void update(Game a) {

        long prev = a.prev, now = a.now, next = a.next;

        //long agentDur = now - prev;
//        long dur = agentDur;
        //narDur; //Math.min(narDur, agentDur);
        //long s = prev, e = now;
        //long s = Math.min(prev + agentDur/2, now-agentDur/2), e = now + agentDur/2;
        //long s = prev + narDur/2, e = now + narDur/2;

        //long s = now - dur/2, e = now + dur/2;

        //long s = prev, e = now;
        //long s = now - dur, e = now;
        //long s = now - dur, e = now + dur;
        //long s = now, e = next;
        //long s = now - dur/2, e = next - dur/2;
        //long s = now - dur, e = next - dur;
        //long s = prev, e = next;
        //long agentDur = (now - prev);
        //long s = now - agentDur/2, e = now + agentDur/2;
        //long s = now - dur/2, e = now + dur/2;

        long feedbackShift =
                0;
                //n.dur();

        super.updatePrevNow(prev, now, a);

        Truth goal = actionTruth;

        Truth fb = this.motor.apply(null,
                //curi!=null ? (goal!=null ? Revision.revise(curi, goal) : curi) : goal
                goal
        );

        feedback(fb, prev + feedbackShift, now + feedbackShift, cause, a.nar());
    }




}
