package nars.task.util;

import jcog.list.FasterList;
import nars.NAR;
import nars.Task;
import nars.control.MetaGoal;
import nars.table.BeliefTable;
import nars.table.DefaultBeliefTable;
import nars.task.NALTask;
import nars.task.signal.SignalTask;
import nars.truth.Stamp;

import java.util.List;

public class PredictionFeedback {

    //final BeliefTable table;


    static final boolean delete = true;
    static final float strength = 1;

    /*public PredictionFeedback(BeliefTable table) {
        this.table = table;
    }*/

    static public void accept(Task x, BeliefTable table, NAR nar) {
        if (x == null)
            return;

        if (x instanceof SignalTask) {
            feedbackNewSignal((SignalTask) x, table, nar);
        } else {
            feedbackNewBelief(x, table, nar);
        }
    }

    /**
     * TODO handle stretched tasks
     */
    static void feedbackNewBelief(Task y, BeliefTable table, NAR nar) {

        long start = y.start();
        long end = y.end();
        int dur = nar.dur();

        final SignalTask[] strongestSignal = new SignalTask[1];
        float[] strongest = new float[] { y.evi(start, end, dur) };
        ((DefaultBeliefTable) table).temporal.whileEach(start, end, (nextSignal) -> {
            //TODO or if the cause is purely this Cause id (to include pure revisions of signal tasks)
            if (nextSignal instanceof SignalTask && !nextSignal.isDeleted()) {
                float nextStrength = strength(nextSignal, start, end, dur);
                if (nextStrength > strongest[0]) {
                    strongestSignal[0] = ((SignalTask) nextSignal);
                    strongest[0] = nextStrength;
                }
            }
            return true;
        });
        SignalTask signal = strongestSignal[0];
        if (signal == null)
            return;

//        long when = y.nearestTimeTo(nar.time());
//        int dur = nar.dur();
//        Truth x = signal.truth(when, dur);
//        if (x == null)
//            return; //nothing to compare it with

        absorb(signal, y, start, end, nar);
    }

    /** true if next is stronger than current */
    private static float strength(Task x, long start, long end, int dur) {
        return
                (x.evi(start,dur)+x.evi(end,dur)) //sampled at start & end
        ;
    }

    /**
     * punish any held non-signal beliefs during the current signal task which has just been input.
     * time which contradict this sensor reading, and reward those which it supports
     */
    static void feedbackNewSignal(SignalTask signal, BeliefTable table, NAR nar) {

        long start = signal.start();
        long end = signal.end();

        List<Task> trash = new FasterList(0);
        ((DefaultBeliefTable) table).temporal.whileEach(start, end, (y) -> {

            if (y instanceof SignalTask)
                return true; //ignore previous signaltask

            if (absorb(signal, y, start, end, nar)) {
                trash.add(y);
            }

            return true; //continue
        });

        trash.forEach(table::removeTask);
    }




    /**
     * rewards/punishes the causes of this task,
     * then removes it in favor of a stronger sensor signal
     * returns whether the 'y' task was absorbed into 'x'
     */
    public static boolean absorb(Task x, Task y, long start, long end, NAR nar) {
        if (x == y)
            return false;

        //maybe also factor originality to prefer input even if conf is lower but has more originality thus less chance for overlap
        int dur = nar.dur();
        float yEvi = y.evi(start, end, dur);
        float xEvi = x.evi(start, end, dur);
        if (yEvi > xEvi)
            return false;

        float overlap = Stamp.overlapFraction(x.stamp(), y.stamp());
        float coherence = 2f * ((1f - Math.abs(x.freq() - y.freq())) - 0.5f);
        float value = coherence * yEvi/(yEvi + xEvi) * (1f-overlap) * strength;
        if (Math.abs(value) > Float.MIN_NORMAL) {
            MetaGoal.learn(MetaGoal.Accurate, y.cause(), value, nar);
        }

        if (delete)
            ((NALTask) y).delete(x); //forward to the actual sensor reading
        return true;
    }

}
