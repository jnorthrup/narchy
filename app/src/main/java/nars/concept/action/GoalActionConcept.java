package nars.concept.action;

import nars.NAR;
import nars.Task;
import nars.control.channel.CauseChannel;
import nars.table.dynamic.CuriosityGoalTable;
import nars.table.dynamic.SeriesBeliefTable;
import nars.table.dynamic.SignalBeliefTable;
import nars.task.ITask;
import nars.task.signal.SignalTask;
import nars.term.Term;
import nars.truth.Truth;
import nars.truth.func.NALTruth;

import java.util.Objects;
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
    public void update(long pPrev, long pNow, NAR nar) {


        Truth goal;

        int dur = nar.dur();
        //long gStart = pNow - dur / 2, gEnd = pNow + Math.max(0, dur / 2 - 1);
        //long gStart = pNow, gEnd = pNow + Math.max(0, dur-1);
        //long gStart = pPrev + dur, gEnd = pNow + Math.max(0, dur -1);
        long gStart = pPrev, gEnd = pNow;

        goal =
                this.goals().truth(gStart, gEnd, nar);
                //goals().truthStored(gStart, gEnd, null, nar);


        boolean curi;
        if (nar.random().nextFloat() < curiosityRate) { // * (1f - (goal != null ? goal.conf() : 0))) {


//            float curiConf =
//
//
//                    Math.min(Param.TRUTH_MAX_CONF, nar.confMin.floatValue()
//
//                            * 8
//                    );
//
//
//            curi = true;
//
//
//            goal = Truth.theDithered(nar.random().nextFloat(), c2w(curiConf), nar);

            goal = NALTruth.Curiosity.apply(null, null, nar, 0);
            curi = true;

        } else {
            curi = false;


        }


        Truth belief = this.beliefs().truth(gStart, gEnd, nar);


        Truth feedback = this.motor.apply(belief, goal);

        SignalBeliefTable b = beliefs();
        Task feedbackBelief = feedback != null ?
                b.add(feedback, gStart, gEnd, this, nar) : null;

        Task curiosityGoal = null;
        if (curi && feedbackBelief != null) {
            curiosityGoal = curiosity(
                    goal,
                    term,
                    //pNow-dur/2, pNow+Math.max(0,dur/2-1),
                    gStart, gEnd,
                    nar);
        }

        b.clean(nar);

        in.input(
            Stream.of(feedbackBelief, (ITask) curiosityGoal).filter(Objects::nonNull)
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

    SignalTask curiosity(Truth truth, Term term, long pStart, long pEnd, NAR nar) {
        //SeriesBeliefTable.SeriesTask curiosity = new SeriesBeliefTable.SeriesTask(term, GOAL, goal, pStart, pEnd, curiosityStamp);

        SeriesBeliefTable.SeriesTask curiosity =
                goals().series.add(term, GOAL, pStart, pEnd, truth, nar.dur(), nar);

        if (curiosity!=null)
            curiosity.pri(nar.priDefault(GOAL));

        return curiosity;
    }

    public void curiosity(float c) {
        this.curiosityRate = c;
    }

}
