package nars.task.util;

import jcog.list.FasterList;
import nars.NAR;
import nars.Task;
import nars.concept.dynamic.ScalarBeliefTable;
import nars.control.MetaGoal;
import nars.table.BeliefTable;
import nars.table.DefaultBeliefTable;
import nars.task.signal.SignalTask;

import java.util.List;
import java.util.function.Consumer;

public class PredictionFeedback {

    //final BeliefTable table;

    static final boolean delete = true;

    /**
     * punish any held non-signal beliefs during the current signal task which has just been input.
     * time which contradict this sensor reading, and reward those which it supports
     */
    static public void feedbackSignal(SignalTask x, BeliefTable table, NAR nar) {
        if (x == null)
            return;

        int dur = nar.dur();
        long predictionLimit = nar.time() - dur / 2;

        long start = x.start();
        long end = x.end();


        float fThresh = nar.freqResolution.floatValue();

        List<Task> trash = new FasterList<>(8);

        ((DefaultBeliefTable) table).temporal.whileEach(start, end, (y)->{
            //if (!(y instanceof SignalTask)) {
            if (y.end() < predictionLimit)
                trash.add(y);
            //}
            return true; //continue
        });


        //test evidences etc outside of critical section that would lock the RTreeBeliefTable
        trash.forEach(y-> {
            if (absorb(x, y, start, end, dur, fThresh, nar)) {
                table.removeTask(y);
            }
        });

    }




    /**
     * TODO handle stretched tasks
     */
    public static void feedbackNonSignal(Task y, ScalarBeliefTable table, NAR nar) {

        if (table.isEmpty())
            return; //nothing to contradict

        int dur = nar.dur();
        long end = y.end();
        long predictionLimit = nar.time() - dur / 2;
        if (end >= predictionLimit)
            return; //dont absorb if at least part of the task predicts the future

        long start = y.start();


        List<SignalTask> signals = new FasterList<>(8);
        Consumer<Task> each = existing -> {
            //TODO or if the cause is purely this Cause id (to include pure revisions of signal tasks)
            if (existing instanceof SignalTask) {
                signals.add((SignalTask) existing);
            }
        };

        table.series.forEach(start, end, true, each);

        if (!signals.isEmpty()) {
            //TODO combine into one batch absorb function
            float fThresh = nar.freqResolution.floatValue();
            for (int i = 0, signalsSize = signals.size(); i < signalsSize; i++) {
                absorb(signals.get(i), y, start, end, dur, fThresh, nar);
            }
        }
    }

//    private static boolean signalOrRevisedSignalAbsorbs(Task existing, Task y) {
//        if (existing instanceof SignalTask)
//            return true;
//        if (existing.isCyclic())
//            return false;
//        if (existing.confMax() < y.confMin() || existing.originality() < y.originality())
//            return false;
//
//        return true;
//
////        short[] cc = existing.cause();
////        int n = cc.length;
////        switch (n) {
////            case 0: return false;
////            case 1: return cc[0] == cause;
////            default:
////                return false;
//////                for (short x : cc)
//////                    if (x != cause)
//////                        return false;
//////                return true;
////        }
//    }

//    /** true if next is stronger than current */
//    private static float strength(Task x, long start, long end, int dur) {
//        return
//                (x.evi(start,dur)+x.evi(end,dur)) //sampled at start & end
//        ;
//    }





    /**
     * rewards/punishes the causes of this task,
     * then removes it in favor of a stronger sensor signal
     * returns whether the 'y' task was absorbed into 'x'
     */
    static boolean absorb(SignalTask x, Task y, long start, long end, int dur, float fThresh, NAR nar) {
        if (x == y)
            return false;

        //maybe also factor originality to prefer input even if conf is lower but has more originality thus less chance for overlap
        float yEvi = y.evi(start, end, dur);
        float xEvi = x.evi(start, end, dur);


        float error = Math.abs(x.freq() - y.freq());
        float coherence;
        if (error <= fThresh) {
            coherence = +1;
        } else {
            coherence = -error;
        }
        float value = coherence * yEvi/(yEvi + xEvi);
        if (Math.abs(value) > Float.MIN_NORMAL) {
            MetaGoal.Accurate.learn(y.cause(), value, nar.causes);
        }

        if (delete) {
            y.delete(/*fwd: x*/); //forward to the actual sensor reading
            return true;
        } else {
            return false; //keep if correct and stronger
        }
    }

    private static float error(Task x, Task y, long start, long end, int dur) {
        //maybe also factor originality to prefer input even if conf is lower but has more originality thus less chance for overlap

        float yEvi = y.evi(start, end, dur);
        float xEvi = x.evi(start, end, dur);


        return Math.abs(x.freq() - y.freq());
    }
}
