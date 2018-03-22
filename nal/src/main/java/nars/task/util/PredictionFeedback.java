package nars.task.util;

import jcog.list.FasterList;
import nars.NAR;
import nars.Task;
import nars.concept.dynamic.ScalarBeliefTable;
import nars.control.MetaGoal;
import nars.table.BeliefTable;
import nars.table.DefaultBeliefTable;
import nars.task.NALTask;
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
    static public void feedbackNewSignal(SignalTask x, BeliefTable table, NAR nar) {
        if (x == null)
            return;

        long start = x.start();
        long end = x.end();

        int dur = nar.dur();
        float fThresh = nar.freqResolution.floatValue();

        List<Task> trash = new FasterList(8);
        Consumer<Task> each = y -> {
            if (!(y instanceof SignalTask)) {
                if (absorb(x, y, start, end, dur, fThresh, nar)) {
                    trash.add(y);
                }
            }
        };

        scan(table, start, end, each);


        trash.forEach(table::removeTask);

    }

    public static void scan(BeliefTable table, long start, long end, Consumer<Task> each) {
        if (table instanceof ScalarBeliefTable) {
            ((ScalarBeliefTable)table).series.forEach(start, end, false, each);
        } else {
            ((DefaultBeliefTable) table).temporal.whileEach(start, end, (tt)->{ each.accept(tt); return true; });
        }
    }


    /**
     * TODO handle stretched tasks
     */
    public static void feedbackNewBelief(Task y, BeliefTable table, NAR nar) {

        long start = y.start();
        long end = y.end();
        int dur = nar.dur();

        List<SignalTask> signals = new FasterList<>(8);
        Consumer<Task> each = existing -> {
            //TODO or if the cause is purely this Cause id (to include pure revisions of signal tasks)
            if (existing instanceof SignalTask) {
                signals.add((SignalTask) existing);
            }
        };

        scan(table, start, end, each);

        if (signals.isEmpty())
            return;
        else {
            //TODO combine into one batch absorb function
            float fThresh = nar.freqResolution.floatValue();
            for (int i = 0, signalsSize = signals.size(); i < signalsSize; i++)
                absorb(signals.get(i), y, start, end, dur, fThresh, nar);
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
        if (!x.intersects(y))
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
            ((NALTask) y).delete(x); //forward to the actual sensor reading
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
