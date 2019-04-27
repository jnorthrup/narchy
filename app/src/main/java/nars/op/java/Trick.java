package nars.op.java;

import nars.NAL;
import nars.NAR;

import java.util.function.Consumer;
import java.util.function.Predicate;

/** labelled training episode */
public class Trick<X> {



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

    public synchronized void train(X x, NAL<NAL<NAR>> n) {





//        Term LEARN = $.func("learn", $.the(id));
//        n.believe(LEARN, Tense.Present);

        pre.accept(x);




//        Term DO = $.func("do", $.the(id));
//        n.believe(DO, Tense.Present);

        action.accept(x); 



//        n.believe(DO.neg(), Tense.Present);
//        n.believe(LEARN.neg(), Tense.Present);



    }
}
