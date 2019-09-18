package nars.op;

import jcog.Paper;
import jcog.Skill;
import jcog.util.ArrayUtil;
import nars.NAR;
import nars.Task;
import nars.attention.What;
import nars.control.channel.CauseChannel;
import nars.derive.Derivation;
import nars.derive.action.NativeTaskFireAction;
import nars.term.Term;

import static nars.Op.*;

/** LeakBack generates new tasks through its CauseChannel -
 *  and its value directly adjusts the throttle rate of the
 *  Leak it will receive. */
@Paper
@Skill({"Queing_theory","Feedback"})
abstract public class TaskLeakTransform extends NativeTaskFireAction {

    protected final CauseChannel<Task> in;
    private final byte[] puncs;


    protected TaskLeakTransform(NAR nar, byte... puncs) {

        taskPunc(puncs);

        this.puncs = puncs;
        this.in = nar.newChannel(this);//.buffered();
    }

    @Override public float pri(Derivation d) {
        return 0.5f + in.value();
    }

    public void taskPunc(byte... puncs) {
        if (puncs==null || puncs.length == 0)
            return; //no filtering

        taskPunc(
            ArrayUtil.indexOf(puncs, BELIEF)!=-1,
            ArrayUtil.indexOf(puncs, GOAL)!=-1,
            ArrayUtil.indexOf(puncs, QUESTION)!=-1,
            ArrayUtil.indexOf(puncs, QUEST)!=-1
        );
    }


    abstract protected boolean filter(Term next);


}
