package jcog.optimize;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AutoOptimizeTest {

    public static class Model {
        public final SubModel sub = new SubModel();
        public int tweakInt = 0;
        public final int untweakInt = 0;
        public float tweakFloat = 0;

        public float score() {
            return (float) (
                    tweakInt +
                    Math.sin(-1 + tweakFloat) * tweakFloat +
                    (1f/(1f+sub.tweakFloatSub)));
        }
    }

    public static class SubModel {
        public float tweakFloatSub;
    }

    @Test
    public void test1() {
        AutoOptimize<Model> a = new AutoOptimize(Model::new);
        assertEquals(3, a.tweaks.size());
        Optimize.Result r = a.run(10, Model::score);
        r.print();
    }
}