package nars.task.util;

import jcog.list.FasterList;
import jcog.math.Interval;
import nars.NAR;
import nars.Task;
import nars.control.MetaGoal;
import nars.table.BeliefTable;
import nars.table.DefaultBeliefTable;
import nars.task.NALTask;
import nars.task.SignalTask;
import nars.truth.Stamp;

import java.util.List;

public class PredictionFeedback {

    //final BeliefTable table;


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

        final SignalTask[] strongestSignal = new SignalTask[1];
        float[] strongest = new float[] { Float.NaN };
        ((DefaultBeliefTable) table).temporal.whileEach(start, end, (nextSignal) -> {
            if (nextSignal instanceof SignalTask) {
                SignalTask currentSignal = strongestSignal[0];
                float s = strength(y, nextSignal);
                if (currentSignal==null || s > strongest[0]) {
                    strongestSignal[0] = ((SignalTask) nextSignal);
                    strongest[0] = s;
                }
            }
            return true;
            //TODO early exit with a Predicate form of this query method
        });
        SignalTask signal = strongestSignal[0];
        if (signal == null)
            return; //just beliefs against beliefs

//        long when = y.nearestTimeTo(nar.time());
//        int dur = nar.dur();
//        Truth x = signal.truth(when, dur);
//        if (x == null)
//            return; //nothing to compare it with

        absorb(signal, y, start, end, nar);
    }

    /** true if next is stronger than current */
    private static float strength(Task x, Task y) {
        return
                y.evi() * (1 + Interval.intersectLength(x.start(), x.end(), y.start(), y.end()))
        ;
    }

    /**
     * punish any held non-signal beliefs during the current signal task which has just been input.
     * time which contradict this sensor reading, and reward those which it supports
     */
    static void feedbackNewSignal(SignalTask signal, BeliefTable table, NAR nar) {

        int dur = nar.dur();

        long start = signal.start();
        long end = signal.end();

        List<Task> trash = new FasterList(0);
        ((DefaultBeliefTable) table).temporal.whileEach(start, end, (y) -> {

            if (y instanceof SignalTask)
                return true; //ignore previous signaltask

            if (absorb(signal, y, start, end, nar))
                trash.add(y);

            return true; //continue
        });

        trash.forEach(table::removeTask);
    }

    /**
     * measures frequency similarity of two tasks. -1 = dissimilar .. +1 = similar
     * also considers the relative task time range
     */
    public static float coherence(Task actual, Task predict) {
        float xFreq = actual.freq();
        float yFreq = predict.freq();
        float overtime = Math.max(0, predict.range() - actual.range()); //penalize predictions spanning longer than the actual signal because we aren't checking in that time range for accuracy, it could be wrong before and after the signal
        return 2f * ((1f - Math.abs(xFreq - yFreq) / (1f + overtime))-0.5f);
        //TruthFunctions.freqSimilarity(xFreq, y.freq());
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
        float value = coherence(x,y) * yEvi/(yEvi + xEvi) * (1f-overlap) * strength;
        if (value > 0) {
            MetaGoal.learn(MetaGoal.Accurate, y.cause(), value, nar);
        }

        ((NALTask) y).delete(x); //forward to the actual sensor reading
        return true;
    }

}
