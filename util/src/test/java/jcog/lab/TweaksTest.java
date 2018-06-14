package jcog.lab;

import jcog.math.FloatRange;
import jcog.math.Range;

public class TweaksTest {

    public static class Model {
        public final SubModel sub = new SubModel();

        public final FloatRange floatRange = new FloatRange(0.5f, 0, 10);

        @Range(min=0, max=5/*, step=1f*/) 
        public float tweakFloat = 0;

        @Range(min=-4, max=+4, step=2f)
        public int tweakInt = 0;

        public final int untweakInt = 0;

        public float score() {
            return (float) (
                    tweakInt +
                    Math.sin(-1 + tweakFloat) * tweakFloat +
                    (1f/(1f+sub.tweakFloatSub)))
                    + floatRange.floatValue();
        }

        public float score2() {
            return (float)(Math.cos(floatRange.floatValue()/4f));
        }

    }

    public static class SubModel {
        @Range(min=0, max=3, step=0.05f)
        public float tweakFloatSub;
    }

//    @Test
//    public void testSingleObjective() {
//        Variables<Model> a = new Variables<>(Model::new).discover();
//        a.tweaks.forEach(
//                System.out::println
//        );
//        assertTrue(a.tweaks.size() >= 4);
//
//
//        Lab.Result r = a.optimize((m)->m.score()).run(25);
//        assertEquals(5, r.data.attrCount());
//
//        r.print();
//        r.tree(3, 4).print();
//        ImmutableList best = r.best();
//        assertTrue(((Number) best.get(0)).doubleValue() >= 5f);
//
//        r.data.print();
//    }
//
//    @Test
//    public void testMultiObjective() {
//        Variables<Model> a = new Variables<>(Model::new).discover();
//
//
//
//        Lab.Result r = a.optimize((m->m), ((FloatFunction<Model>)(m->m.score())), m->m.score2()).run(25);
//        assertEquals(7, r.data.attrCount());
//
//        r.print();
//        r.tree(3, 6).print();
//
//        r.data.print();
//    }
}