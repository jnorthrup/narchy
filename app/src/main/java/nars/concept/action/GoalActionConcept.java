package nars.concept.action;

import nars.$;
import nars.NAR;
import nars.control.channel.CauseChannel;
import nars.control.proto.Remember;
import nars.table.dynamic.SensorBeliefTables;
import nars.task.ITask;
import nars.term.Term;
import nars.truth.Truth;

import java.util.Random;
import java.util.stream.Stream;


/**
 * ActionConcept which is driven by Goals that are interpreted into feedback Beliefs
 */
public class GoalActionConcept extends AbstractGoalActionConcept {

    private final MotorFunction motor;
    private final CauseChannel<ITask> in;




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
    public void add(Remember r, NAR n) {
//        Param.DEBUG = true;
//        if (r.input!=null && r.input.isGoal()) {
//            if (r.input.range() > n.dur() * 256) {
//                System.err.println("caught long goal task: " + r.input);
//                System.err.println(r.input.proof());
//            }
//        }
        super.add(r, n);
    }

    @Override
    public void update(long prev, long now, long next, NAR n) {

        super.update(prev, now, next, n);

        Truth goal = actionTruth;

        boolean curi;
        Random rng = n.random();
        if (rng.nextFloat() < curiosityRate) { // * (1f - (goal != null ? goal.conf() : 0))) {

            goal = $.t(rng.nextFloat(), curiConf);
            goal = goal.ditherFreq(resolution().floatValue());
            curi = true;
        } else {
            curi = false;
        }

        Truth fb = this.motor.apply(null, goal);

        in.input(
            Stream.of(
                    feedback(fb, now, next, n),
                    (curi ? curiosity($.t(fb.freq(), curiConf) /*goal*/, prev, now, n) : null))


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



}
