package nars.op.java;

import nars.$;
import nars.NAR;
import nars.term.Term;
import nars.time.Tense;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;
import java.util.function.Predicate;

/** labelled training episode */
public class Trick<X> {

    final static org.slf4j.Logger logger = LoggerFactory.getLogger(Trick.class);

    final String id;

    /** setup preconditions */
    final Consumer<X> pre;

    /** activity */
    final Consumer<X> action;

    /** validation */
    final Predicate<X> post;

    public Trick(String name, Consumer<X> pre, Consumer<X> action, Predicate<X> post) {
        this.id = name;
        this.pre = pre;
        this.action = action;
        this.post = post;
    }

    public boolean valid(X x) {
         return post.test(x);
    }

    public void train(X x, NAR n) {

        logger.info("training: {}", id);

        n.clear();
        Term LEARN = $.func("learn", $.the(id));
        n.believe(LEARN, Tense.Present); //label the learning episode which begins now
        pre.accept(x);

        int considerTime = n.dur() * 2;
        n.run(considerTime); //perceive the preconditions

        Term DO = $.func("do", $.the(id));
        n.believe(DO, Tense.Present); //label the activity that will happen next

        action.accept(x); //execute the task

        n.run(considerTime); //consider the execution

        n.believe(DO.neg(), Tense.Present); //done doing
        n.believe(LEARN.neg(), Tense.Present); //done learning

    }
}
