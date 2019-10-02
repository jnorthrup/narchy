package nars.op;

import jcog.Paper;
import jcog.Skill;
import nars.NAR;
import nars.derive.action.TaskTransformAction;
import nars.term.Term;

/** LeakBack generates new tasks through its CauseChannel -
 *  and its value directly adjusts the throttle rate of the
 *  Leak it will receive. */
@Paper
@Skill({"Queing_theory","Feedback"})
abstract public class TaskLeakTransform extends TaskTransformAction {


    protected TaskLeakTransform(NAR nar, byte... puncs) {
        super();
        single(); //all but command
        taskPunc(puncs);
    }





    abstract protected boolean filter(Term next);


}
