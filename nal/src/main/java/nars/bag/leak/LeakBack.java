package nars.bag.leak;

import jcog.Paper;
import jcog.Skill;
import nars.NAR;
import nars.control.channel.BufferedCauseChannel;
import nars.task.ITask;

/** LeakBack generates new tasks through its CauseChannel -
 *  and its value directly adjusts the throttle rate of the
 *  Leak it will receive. */
@Paper
@Skill({"Queing_theory","Feedback"})
abstract public class LeakBack extends TaskLeak {

    private final static float INITIAL_RATE = 1f;

    protected final BufferedCauseChannel in;


    protected LeakBack(int capacity, NAR nar) {
        super(capacity, INITIAL_RATE, nar);
        this.in = nar.newChannel(this).buffered();
    }

    @Override
    protected boolean full() {
        boolean full = in.full();
        return full;
    }

    @Override
    protected int next(NAR nar, int iterations) {
        int i = super.next(nar, iterations);
        if (i > 0) {
            in.commit();
        }
        return i;
    }

    protected void input(ITask x) {
        in.input(x);
    }

    @Override public float value() {
        return in.value();
    }
}
