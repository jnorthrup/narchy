package nars.concept.action;

import nars.$;
import nars.NAR;
import nars.concept.TaskConcept;
import nars.control.channel.CauseChannel;
import nars.table.dynamic.CuriosityGoalTable;
import nars.table.dynamic.SeriesBeliefTable;
import nars.table.dynamic.SignalBeliefTable;
import nars.task.ITask;
import nars.term.Term;
import nars.truth.Truth;

import java.util.Objects;
import java.util.Random;
import java.util.stream.Stream;

import static nars.Op.GOAL;


/**
 * ActionConcept which is driven by Goals that are interpreted into feedback Beliefs
 */
public class GoalActionConcept extends ActionConcept {

    private final MotorFunction motor;
    private final CauseChannel<ITask> in;

    private volatile float curiosityRate = 0;


    public GoalActionConcept(Term term, NAR n, MotorFunction motor) {
        super(term,
                new SignalBeliefTable(term, true, n.conceptBuilder),
                new CuriosityGoalTable(term, false, n),
                n);

        this.motor = motor;

        in = n.newChannel(this);
    }

    @Override
    public CuriosityGoalTable goals() {
        return (CuriosityGoalTable) super.goals();
    }

    @Override
    public SignalBeliefTable beliefs() {
        return (SignalBeliefTable) super.beliefs();
    }

    @Override
    public float dexterity(long start, long end, NAR n) {
        Truth t = goals().truthStored(start, end, null, n);
        return t!=null ? t.conf() : 0;
    }


    @Override
    public void update(long prev, long now, long next, NAR nar) {




        //int dur = nar.dur();
        //long gStart = pNow - dur / 2, gEnd = pNow + Math.max(0, dur / 2 - 1);
        //long gStart = pNow, gEnd = pNow + Math.max(0, dur-1);
        //long gStart = pPrev + dur, gEnd = pNow + Math.max(0, dur -1);
        //long gStart = prev, gEnd = now;

        Truth goal = this.goals()
                .truth(prev, now /*next*/, nar);
                //.truthStored(prev, now /*next*/, null, nar);

//        if (goal==null) {
//            //self fulfilling prophecy
//            Truth beliefAhead = this.beliefs().truth(now, next, nar);
//            goal = beliefAhead;
//        }

        boolean curi;
        Random rng = nar.random();
        if (rng.nextFloat() < curiosityRate) { // * (1f - (goal != null ? goal.conf() : 0))) {


            float curiConf = nar.confMin.floatValue();
            goal = $.t(rng.nextFloat(), curiConf);
            goal = goal.ditherFreq(resolution().floatValue());
            curi = true;

        } else {
            curi = false;
        }






        Truth feedback = this.motor.apply(null, goal);

        SignalBeliefTable b = beliefs();
        ITask feedbackBelief = feedback != null ?
                b.add(feedback, now, next, this, nar) : null;

        ITask curiosityGoal = null;
        if (curi && feedbackBelief != null) {
            curiosityGoal = curiosity(goal,
                    prev, now,
                    this, nar);
        }

        in.input(
            Stream.of(feedbackBelief, curiosityGoal).filter(Objects::nonNull)
        );
    }
//
//    @Override
//    public void add(Remember r, NAR n) {
//        if (r.punc()==GOAL) {
//            System.out.println("goal sdfjlsj: " + r);
//        }
//        super.add(r, n);
//    }

    ITask curiosity(Truth truth, long pStart, long pEnd, TaskConcept c, NAR nar) {
        //SeriesBeliefTable.SeriesTask curiosity = new SeriesBeliefTable.SeriesTask(term, GOAL, goal, pStart, pEnd, curiosityStamp);

        SeriesBeliefTable.SeriesTask curiosity =
                goals().series.add(term, GOAL, pStart, pEnd, truth, nar.dur(), nar);

        if (curiosity!=null) {
            curiosity.pri(nar.priDefault(GOAL));
            return curiosity.input(c);
        } else {
            return null;
        }

    }

    public void curiosity(float c) {
        this.curiosityRate = c;
    }

}
