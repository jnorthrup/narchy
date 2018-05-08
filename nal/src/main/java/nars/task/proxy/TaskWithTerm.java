package nars.task.proxy;

import nars.Task;
import nars.task.TaskProxy;
import nars.term.Term;

import static nars.Op.NEG;

/** accepts a separate term as a facade to replace the apparent content term of
 * a proxied task
  */
public class TaskWithTerm extends TaskProxy {

    public final Term term;


    public TaskWithTerm(Term term, Task task) {
        super(task);
        if(term.op()==NEG)
            throw new RuntimeException("task must not be named wit NEG term: " + term + " via " + task);
        this.term = term;
    }

//        @Override
//        public float freq() {
//            if (isBeliefOrGoal()) {
//                float f = super.freq();
//                if (neg) f = 1-f;
//                return f;
//            }
//            return Float.NaN;
//        }
//
//        @Override
//        public @Nullable Truth truth() {
//            return isBeliefOrGoal() ? new PreciseTruth(freq(), conf()) : null;
//        }

    @Override
    public Term term() {
        return term;
    }

}
