package nars.concept;

import nars.$;
import nars.NAR;
import nars.Task;
import nars.table.EternalTable;
import nars.table.HijackTemporalBeliefTable;
import nars.table.HijackTemporalExtendedBeliefTable;
import nars.table.TemporalBeliefTable;
import nars.task.GeneratedTask;
import nars.term.Compound;
import nars.truth.Truth;
import nars.truth.TruthAccumulator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;


public abstract class ActionConcept extends WiredConcept implements Function<NAR,Task>, Consumer<NAR> {

    final TruthAccumulator
            beliefIntegrated = new TruthAccumulator(),
            goalIntegrated = new TruthAccumulator();

    public ActionConcept(@NotNull Compound term, @NotNull NAR n) {
        super(term, n);
        n.onCycle(this);
    }

    public static class CuriosityTask extends GeneratedTask {

        public CuriosityTask(Compound term, byte punc, Truth truth, long creation, long start, long end, long[] stamp) {
            super(term, punc, truth, creation, start, end, stamp);
        }
    }

    public static CuriosityTask curiosity(Compound term, byte punc, float conf, long next, NAR nar) {
        long now = nar.time();
        CuriosityTask t = new CuriosityTask(term, punc,
                $.t(nar.random().nextFloat(), conf),
                now,
                next,
            next + nar.dur(),
                new long[] { nar.time.nextStamp() }
        );
        t.budget( nar );
        return t;

    }

    @Override
    public void accept(NAR nar) {
        long now  = nar.time();
        int dur = nar.dur();
        beliefIntegrated.add( belief( now, dur ));
        goalIntegrated.add( goal( now, dur ));
    }


    @Override
    public TemporalBeliefTable newTemporalTable(int tCap, NAR nar) {
//        return new MyListTemporalBeliefTable(tCap * 2, tCap * 4);
        return new HijackTemporalBeliefTable( tCap );
    }

    @Override
    public EternalTable newEternalTable(int eCap) {
        return EternalTable.EMPTY;
    }


    /** produces a curiosity exploratoin task */
    @Nullable public abstract Task curiosity(float conf, long next, NAR nar);


    /** determines the feedback belief when desire or belief has changed in a MotorConcept
     *  implementations may be used to trigger procedures based on these changes.
     *  normally the result of the feedback will be equal to the input desired value
     *  although this may be reduced to indicate that the motion has hit a limit or
     *  experienced resistence
     * */
    @FunctionalInterface  public interface MotorFunction  {

        /**
         * @param desired current desire - null if no desire Truth can be determined
         * @param believed current belief - null if no belief Truth can be determined
         * @return truth of a new feedback belief, or null to disable the creation of any feedback this iteration
         */
        @Nullable Truth motor(@Nullable Truth believed, @Nullable Truth desired);

        /** all desire passes through to affect belief */
        MotorFunction Direct = (believed, desired) -> desired;

        /** absorbs all desire and doesnt affect belief */
        @Nullable MotorFunction Null = (believed, desired) -> null;
    }

    protected class MyListTemporalBeliefTable extends HijackTemporalExtendedBeliefTable {

        public MyListTemporalBeliefTable(int tCap, int historicCap) {
            super(tCap, historicCap);
        }

//        @Override
//        public float pri(@NotNull Task t) {
//            float mult = 1f;
//            if (t.start() > now)
//                mult = 4f;
//            return mult *  super.pri(t);
//        }

        @Override
        protected boolean save(Task t) {
//            if (t.isBelief())
//                return t instanceof SignalTask;
//            else
                return true; //accept all goals
        }

    }
}
