package nars.task.util;

import jcog.event.On;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.term.Term;
import nars.term.control.PrediTerm;
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
    private PrediTerm<Term> term;
    //private final BoolPred<Truth> truth = null;
    private PrediTerm<Byte> punctuation;
    //private final BoolPred<LongLongPair> time = null;
    //private final BoolPred<Priority> budget = null;



//    abstract public static class TermMatch extends AtomicPred<Term> {
//
//        public final Term term;
//
//        public TermMatch(String s) throws Narsese.NarseseException {
//            this($.$(s));
//        }
//
//        public TermMatch(Term t) {
//            this.term = t;
//        }
//
//        @Override
//        abstract public boolean test(Term p);
//
//        @NotNull
//        @Override
//        public String toString() {
//            return getClass().getSimpleName() + '(' + term + ')';
//        }
//    }

    protected TaskMatch(@NotNull NAR n) {
        this.nar = n;
        this.on = n.onTask(this);
    }

    public void setTerm(PrediTerm<Term> term) {
        this.term = term;
    }

    public void setPunctuation(PrediTerm<Byte> punctuation) {
        this.punctuation = punctuation;
    }

    @NotNull
    @Override
    public String toString() {
        return id().toString();
    }

    //HACK
    public Term id() {
        return $.func(getClass().getSimpleName(),
            (term), (punctuation)
                //, $.nonNull(truth), $.nonNull(time), $.nonNull(budget)
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
    public void accept(@NotNull Task _x) {

        Task x = _x;

        test(x);




    }


    protected void onError(SoftException e) {
        //default: do nothing
    }

    /** accepts the next match
     *
     * @param task
     * @param xy
     * @return true for callee to continue matching, false to stop
     */
    abstract protected void accept(Task task, Map<Term, Term> xy);


}
