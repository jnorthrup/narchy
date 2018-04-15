package nars.bag.leak;

import jcog.Paper;
import jcog.Skill;
import nars.NAR;
import nars.control.channel.ThreadBufferedCauseChannel;
import nars.task.ITask;

/** LeakBack generates new tasks through its CauseChannel -
 *  and its value directly adjusts the throttle rate of the
 *  Leak it will receive. */
@Paper
@Skill({"Queing_theory","Feedback"})
abstract public class LeakBack extends TaskLeak {

    final static float INITIAL_RATE = 1f;

    public final ThreadBufferedCauseChannel<ITask> in;


    protected LeakBack(int capacity, NAR nar) {
        super(capacity, INITIAL_RATE, nar);
        this.in = nar.newChannel(this).threadBuffered();
    }

    @Override
    protected int next(NAR nar, int work) {
        int i = super.next(nar, work);
        if (i > 0) {
            in.get().commit();
        }
        return i;
    }

    public void input(ITask x) {
        in.get().input(x);
    }

    @Override public float value() {
        return in.value();
    }
}
