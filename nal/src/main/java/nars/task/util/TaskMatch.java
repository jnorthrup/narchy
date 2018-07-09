package nars.task.util;

import jcog.event.On;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.term.Term;
import nars.term.Variable;
import nars.term.control.PREDICATE;
import nars.util.SoftException;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Generic handler for matching individual Tasks's
 */
abstract public class TaskMatch  implements Consumer<Task>, Predicate<Task> {

    protected final NAR nar;
    private final On on;
    private PREDICATE<Term> term;
    
    private PREDICATE<Byte> punctuation;
    
    

























    protected TaskMatch(@NotNull NAR n) {
        this.nar = n;
        this.on = n.onTask(this);
    }

    public void setTerm(PREDICATE<Term> term) {
        this.term = term;
    }

    public void setPunctuation(PREDICATE<Byte> punctuation) {
        this.punctuation = punctuation;
    }

    @NotNull
    @Override
    public String toString() {
        return id().toString();
    }

    
    public Term id() {
        return $.func(getClass().getSimpleName(),
            (term), (punctuation)
                
            );
    }

    public void off() {
        this.on.off();
    }

    @Override
    public boolean test(Task t) {
        return term == null || term.test(t.term());
    }

    @Override
    public void accept( Task x) {

        test(x);




    }


    protected void onError(SoftException e) {
        
    }

    /** accepts the next match
     *
     * @param task
     * @param xy
     * @return true for callee to continue matching, false to stop
     */
    abstract protected void accept(Task task, Map<Variable, Term> xy);


}
