package nars.derive.op;

import nars.derive.Derivation;
import nars.derive.util.DerivationFailure;
import nars.term.Term;
import nars.term.buffer.EvalTermBuffer;

import java.util.function.Predicate;

import static nars.derive.util.DerivationFailure.Success;
import static nars.time.Tense.ETERNAL;

public class UnifyMatchFork extends EvalTermBuffer implements Predicate<Derivation> {

    protected Taskify taskify;

    public UnifyMatchFork() {
    }

    public void reset(Taskify taskify) {
        this.taskify = taskify;
    }

    @Override
    public boolean test(Derivation d) {

        d.nar.emotion.deriveUnified.increment();

        Term x = taskify.termify.pattern(d);

        Term y = d.transformDerived.apply(x); //x.transform(d.transformDerived, this, workVolMax);


        if (Success == DerivationFailure.failure(y, (byte) 0 /* dont consider punc consequences until after temporalization */, d)) {
            if (d.temporal)
                taskify.temporalTask(y, d);
            else
                taskify.taskify(y, ETERNAL, ETERNAL, d);
        }

        return true; //tried.size() < forkLimit;
    }

//    public static class DeferredUnifyMatchFork extends UnifyMatchFork {
//
//        @Override
//        public boolean test(Derivation d) {
//            d.post.put(new PostDerivation(d, DeferredUnifyMatchFork.super::test));
//            return true;
//        }
//    }


}
