package jcog.test.control;

import nars.NAR;
import nars.term.Term;
import org.eclipse.collections.api.block.predicate.primitive.BooleanBooleanPredicate;
import org.eclipse.collections.api.block.predicate.primitive.BooleanPredicate;

import static nars.$.the;

public class BooleanChoiceTest extends MiniTest {

    private float reward;
    boolean prev = false;

    public BooleanChoiceTest(NAR n, BooleanBooleanPredicate goal) {
        this(n, the("x"), goal);
    }

    public BooleanChoiceTest(NAR n, Term action, BooleanBooleanPredicate goal) {
        super(n);

        actionPushButton(action, new BooleanPredicate() {
            @Override
            public boolean accept(boolean next) {
                boolean c = goal.accept(prev, next);
                prev = next;
                reward = c ? 1f : 0f;
                return next;
            }
        });

    }

    @Override
    protected float myReward() {
        float r = reward;
        reward = Float.NaN;
        return r;
    }

}
