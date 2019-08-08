package nars.link;

import nars.Task;
import nars.derive.Derivation;
import nars.term.Term;

public abstract class DynamicTaskLink extends AtomicTaskLink {

    public DynamicTaskLink(Term src) {
        super(src);
    }
    public DynamicTaskLink(Term from, Term to) {
        super(from, to);
    }




    @Override
    abstract public Term target(Task task, Derivation d);

}
