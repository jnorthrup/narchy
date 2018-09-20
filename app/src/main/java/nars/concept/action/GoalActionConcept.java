package nars.concept.action;

import jcog.Util;
import nars.$;
import nars.NAR;
import nars.control.channel.CauseChannel;
import nars.control.proto.Remember;
import nars.table.dynamic.SensorBeliefTables;
import nars.task.ITask;
import nars.task.Revision;
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

        pCuri = new RandomPhasor(n.time(), n.random());
    }


    @Override
    public void add(Remember r, NAR n) {

//        //TEMPORARY
//        Param.DEBUG = true;
//        if (r.input.isGoal() && !r.input.isEternal() && r.input.range() > 1000*15) {
//            System.out.println(r.input.proof());
//        }

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

        Truth curi = curiosity(n);

        Truth fb = this.motor.apply(null, curi!=null ? (goal!=null ? Revision.revise(curi, goal) : curi) : goal);

        in.input(
            Stream.of(
                    feedback(fb, now, next, n),
                    (curi!=null ? curiosity($.t(fb.freq(), curiConf) /*goal*/, prev, now, n) : null))


        );
    }

    protected Truth curiosity0(NAR n) {
        Random rng = n.random();
        if (rng.nextFloat() < curiosityRate) { // * (1f - (goal != null ? goal.conf() : 0))) {

            Truth curi = $.t(rng.nextFloat(), curiConf);
            curi = curi.ditherFreq(resolution().floatValue());
            return curi;
        } else {
            return null;
        }
    }

    static class RandomPhasor {
        float fMin, fMax;
        float f, theta;
        long lastUpdate;
        float phaseShiftAmount = 1;
        float freqChangeAmount = 0.1f;

        public RandomPhasor(long start, Random random) {
            this.fMax = 1;
            this.lastUpdate = start;
            theta = (float) (random.nextFloat() * Math.PI*2); //TODO random
        }

        public float update(long t, int dur, Random rng) {
            fMin = 1f/(dur*dur);
            theta +=  (1 + (rng.nextFloat()-0.5f)*2f * phaseShiftAmount);
            f += Util.clamp( (rng.nextFloat()-0.5f)*2f * freqChangeAmount , fMin, fMax);
            return (float) ((1 + Math.sin( theta + (t*Math.PI*2) * f))/2.0);
        }
    }
    final RandomPhasor pCuri;
    protected Truth curiosity(NAR n) {
        Random rng = n.random();
        if (rng.nextFloat() < curiosityRate) { // * (1f - (goal != null ? goal.conf() : 0))) {

            return $.t(pCuri.update(n.time(), n.dur(), n.random()), curiConf);
        } else {
            return null;
        }
    }

}
