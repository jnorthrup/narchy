package nars.concept.action;

import nars.NAR;
import nars.Task;
import nars.control.channel.CauseChannel;
import nars.table.dynamic.SignalBeliefTable;
import nars.task.ITask;
import nars.task.signal.SignalTask;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.stream.Stream;


/**
 * ActionConcept which is driven by Goals that are interpreted into feedback Beliefs
 */
public class GoalActionAsyncConcept extends ActionConcept {

    private final BiConsumer<GoalActionAsyncConcept, Truth /* goal */> motor;

    final CauseChannel<ITask> in;

    public GoalActionAsyncConcept(@NotNull Term c, NAR nar, CauseChannel<ITask> cause, @NotNull BiConsumer<GoalActionAsyncConcept, Truth /* goal */> motor) {
        super(c, nar);

        this.in = cause;


        this.motor = motor;
    }


    @Override
    public Stream<ITask> update(long pPrev, long pNow, int dur, NAR nar) {


        long gStart, gEnd;
        //gStart = pNow - dur / 2; gEnd = pNow + dur / 2;
        gStart = pNow;
        gEnd = pNow + dur;

        Truth goal = this.goals().truth(gStart, gEnd, nar);


        this.motor.accept(this, goal);

        return null;
    }


    public void feedback(@Nullable Truth f, @Nullable Truth g, long lastUpdate, long now, NAR nar) {


        long start = lastUpdate;
        long end = now;

        Task fg;
        if (g != null) {


            fg = null;
        } else
            fg = null;

        int dur = nar.dur();
        SignalBeliefTable beliefs = (SignalBeliefTable) beliefs();
        SignalTask fb = beliefs.add(f, start, end, dur, this, nar);

        in.input(
                fg,
                fb
        );

        beliefs.clean(nar);
    }


}
