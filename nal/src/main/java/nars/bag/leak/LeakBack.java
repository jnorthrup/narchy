package nars.bag.leak;

import jcog.Paper;
import jcog.Skill;
import jcog.math.FloatParam;
import nars.NAR;
import nars.control.CauseChannel;
import nars.task.ITask;

/** LeakBack generates new tasks through its CauseChannel -
 *  and its value directly adjusts the throttle rate of the
 *  Leak it will receive. */
@Paper
@Skill({"Queing_theory","Feedback"})
abstract public class LeakBack extends TaskLeak {

    final static float INITIAL_RATE = 1f;

    protected final CauseChannel<ITask> out;

    protected LeakBack(int capacity, NAR nar) {
        super(capacity, INITIAL_RATE, nar);
        this.out = nar.newCauseChannel(this);
    }

    public void feedback(ITask x) {
        out.input(x);
    }
    

    @Override public float value() {
        return out.value();
    }
}
