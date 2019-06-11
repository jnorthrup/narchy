package nars.task.proxy;

import nars.Task;
import nars.term.Term;

public class ImageTask extends SpecialTermTask {
    public ImageTask(Term t, Task x) {
        super(t, x);
    }

    @Override
    protected boolean inheritCyclic() {
        return true; //cyclic propagation is fine here but only here
    }
}
