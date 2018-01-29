package jcog.optimize;

import jcog.math.Range;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AutoTweaksTest {

    public static class Model {
        public final SubModel sub = new SubModel();

        @Range(min=0, max=5, step=0.1f)
        public float tweakFloat = 0;

        @Range(min=-2, max=+2, step=1f)
        public int tweakInt = 0;

        public final int untweakInt = 0;

        public float score() {
            return (float) (
                    tweakInt +
                    Math.sin(-1 + tweakFloat) * tweakFloat +
                    (1f/(1f+sub.tweakFloatSub)));
        }
    }

    public static class SubModel {
        @Range(min=0, max=3, step=0.05f)
        public float tweakFloatSub;
    }

    @Test
    public void test1() {
        AutoTweaks<Model> a = new AutoTweaks(Model::new);
        assertEquals(3, a.all.size());
        Result<Model> r = a.optimize(10, Model::score);
        r.print();
        r.tree(3, 4).print();
        assertTrue(r.best().getOne() > 5f);
    }
}