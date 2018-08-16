package nars.concept.action;

import nars.$;
import nars.NAR;
import nars.control.channel.CauseChannel;
import nars.table.dynamic.SensorBeliefTables;
import nars.task.ITask;
import nars.term.Term;
import nars.truth.Truth;

import java.util.Random;
import java.util.stream.Stream;

import static nars.Op.GOAL;
import static nars.truth.TruthFunctions.c2w;
import static nars.truth.TruthFunctions.w2c;


/**
 * ActionConcept which is driven by Goals that are interpreted into feedback Beliefs
 */
public class GoalActionConcept extends AbstractGoalActionConcept {

    private final MotorFunction motor;
    private final CauseChannel<ITask> in;

    private volatile float curiosityRate = 0;


    public GoalActionConcept(Term term, NAR n, MotorFunction motor) {
        super(term,
                new SensorBeliefTables(term, true, n.conceptBuilder),
                n.conceptBuilder.newTable(term, false),
                //new CuriosityGoalTable(term, false, n),
                n);


        this.motor = motor;


        in = n.newChannel(this);
    }





    @Override
    public void update(long prev, long now, long next, NAR nar) {

        super.update(prev, now, next, nar);

        Truth goal = actionTruth;

        boolean curi;
        Random rng = nar.random();
        if (rng.nextFloat() < curiosityRate) { // * (1f - (goal != null ? goal.conf() : 0))) {
            float curiConf =
                    //nar.confMin.floatValue() * 4;
                    //nar.confMin.floatValue();
                    w2c(c2w(nar.confDefault(GOAL))/2);
                    //nar.confDefault(GOAL);
            goal = $.t(rng.nextFloat(), curiConf);
            goal = goal.ditherFreq(resolution().floatValue());
            curi = true;
        } else {
            curi = false;
        }

        Truth fb = this.motor.apply(null, goal);

        in.input(
            Stream.of(
                    feedback(fb, now, next, nar),
                    (curi ? curiosity(goal, prev, now, nar) : null))


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



    public void curiosity(float c) {
        this.curiosityRate = c;
    }

}
