package jcog.test.control;

import nars.$;
import nars.NAR;
import org.eclipse.collections.api.block.predicate.primitive.BooleanBooleanPredicate;

import java.util.function.BooleanSupplier;

public class BooleanReactionTest extends MiniTest {

    /** next input */
    boolean i = false;

    private float reward;

    /** reward: (input, action) -> reward */
    public BooleanReactionTest(NAR n, BooleanSupplier input, BooleanBooleanPredicate reward) {
        super(n);

        sense($.inh("i", id), ()-> this.i = input.getAsBoolean());

        actionPushButton($.inh("o", id), (o) -> {
            boolean c = reward.accept(i, o);
            this.reward = c ? 1f : 0f;
            return o;
        });

    }

    @Override
    protected float myReward() {
        float r = reward;
        reward = (float) 0;
        return r;
    }

}
