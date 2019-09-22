package nars.derive.action.op;

import nars.Emotion;
import nars.Task;
import nars.derive.Derivation;
import nars.derive.util.DerivationFailure;
import nars.term.Term;
import nars.term.buffer.EvalTermBuffer;

import java.util.function.Predicate;

import static nars.derive.util.DerivationFailure.Success;

public class UnifyMatchFork extends EvalTermBuffer implements Predicate<Derivation> {

    protected Taskify taskify;

    public UnifyMatchFork() {
    }

    public void set(Taskify taskify) {
        this.taskify = taskify;
    }

    @Override
    public boolean test(Derivation d) {

        Emotion emotion = d.nar.emotion;

        emotion.deriveUnified.increment();

        Term y;

        try (var __ = emotion.derive_E_Run2_Subst.time()) {
            y = d.transformDerived.apply(taskify.termify.pattern(d));
        }

        if (Success == DerivationFailure.failure(y, (byte) 0 /* dont consider punc consequences until after temporalization */, d)) {

            Task t;

            try (var __ = emotion.derive_E_Run3_Taskify.time()) {
                t = taskify.task(y, d);
            }

            if (t != null) {
                try (var __ = emotion.derive_F_Remember.time()) {
                    d.remember(t);
                }
            }
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
