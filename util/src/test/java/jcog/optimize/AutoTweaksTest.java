package jcog.optimize;

import jcog.math.FloatRange;
import jcog.math.Range;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AutoTweaksTest {

    public static class Model {
        public final SubModel sub = new SubModel();

        public final FloatRange floatRange = new FloatRange(0.5f, 0, 10);

        @Range(min=0, max=5/*, step=1f*/) //autoInc will apply
        public float tweakFloat = 0;

        @Range(min=-2, max=+2, step=1f)
        public int tweakInt = 0;

        public final int untweakInt = 0;

        public float score() {
            return (float) (
                    tweakInt +
                    Math.sin(-1 + tweakFloat) * tweakFloat +
                    (1f/(1f+sub.tweakFloatSub)))
                    + floatRange.floatValue();
        }
    }

    public static class SubModel {
        @Range(min=0, max=3, step=0.05f)
        public float tweakFloatSub;
    }

    @Test
    public void test1() {
        AutoTweaks<Model> a = new AutoTweaks(Model::new);
        assertEquals(5, a.all.size());
//        assertEquals(4, a.ready.size());
        Result<Model> r = a.optimize(64, Model::score);
        r.print();
        r.tree(3, 4).print();
        assertTrue(r.best().getOne() > 5f);
    }
}