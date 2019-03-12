package nars.bag.leak;

import jcog.Paper;
import jcog.Skill;
import nars.NAR;
import nars.control.channel.CauseChannel;
import nars.task.ITask;

import java.util.function.BooleanSupplier;

/** LeakBack generates new tasks through its CauseChannel -
 *  and its value directly adjusts the throttle rate of the
 *  Leak it will receive. */
@Paper
@Skill({"Queing_theory","Feedback"})
abstract public class TaskLeakTransform extends TaskLeak  {

    protected final CauseChannel in;

    protected TaskLeakTransform(NAR nar, byte... puncs) {
        super(nar, puncs);
        this.in = nar.newChannel(this);//.buffered();
    }

    protected TaskLeakTransform(int capacity, NAR nar, byte... puncs) {
        super(capacity, nar, puncs);
        this.in = nar.newChannel(this);//.buffered();
    }


    @Override
    public void next(NAR nar, BooleanSupplier kontinue) {
        if (in == null) return; //HACK
        super.next(nar, kontinue);
        //in.commit();
    }

    protected final void input(ITask x) {
        in.input(x);
    }

    @Override public float value() {
        return in.value();
    }
}
