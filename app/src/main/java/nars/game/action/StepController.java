package nars.game.action;

import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.eclipse.collections.api.tuple.primitive.IntObjectPair;
import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList;

import java.util.function.IntConsumer;

/**
 * increments/decrements within a finite set of powers-of-two so that harmonics
 * wont interfere as the resolution changes
 * <p>
 * TODO allow powers other than 2, ex: 1.618
 */
public class StepController implements IntConsumer, IntObjectPair<StepController> {

    final float[] v;
    private final FloatProcedure update;
    int x;

    public StepController(FloatProcedure update, float... steps) {
        v = steps;
        this.update = update;
    }

    public static StepController harmonic(FloatProcedure update, float min, float max) {

        FloatArrayList f = new FloatArrayList();
        float x = min;
        while (x <= max) {
            f.add(x);
            x *= 2.0F;
        }
        assert (f.size() > 1);
        return new StepController(update, f.toArray());

    }

    private void set(int i) {
        if (i < 0) i = 0;
        if (i >= v.length) i = v.length - 1;

        update.value(v[x = i]);

    }

    @Override
    public void accept(int aa) {


        switch (aa) {
            case 0:
                set(x - 1);
                break;
            case 1:
                set(x + 1);
                break;
            default:
                throw new RuntimeException("OOB");


        }
    }

    /**
     * number actions
     */
    @Override
    public int getOne() {
        return 2;
    }

    @Override
    public StepController getTwo() {
        return this;
    }

    @Override
    public int compareTo(IntObjectPair<StepController> o) {
        throw new UnsupportedOperationException();
    }
}
