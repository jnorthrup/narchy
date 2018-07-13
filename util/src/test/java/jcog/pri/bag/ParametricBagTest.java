package jcog.pri.bag;

import jcog.pri.bag.impl.hijack.DefaultHijackBag;
import jcog.math.random.XoRoShiRo128PlusRandom;
import jcog.pri.PLink;
import jcog.pri.PriReference;
import jcog.pri.op.PriMerge;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectIntHashMap;
import org.junit.jupiter.api.Test;

class ParametricBagTest {

    @Test
    public void test1() {
        Sampler.RoundRobinSampler<PLink<String>> parametric = new Sampler.RoundRobinSampler<>();
        parametric.add(new PLink("x", 1));
        parametric.add(new PLink("y", 1));
        parametric.add(new PLink("z", 1));
        ParametricBag<String, PriReference<String>> b = new ParametricBag(
                new DefaultHijackBag<String>(PriMerge.plus, 10, 4),
                parametric);

        b.put(new PLink("a",0.9f));
        b.put(new PLink("b",0.1f));
        b.put(new PLink("c",0.1f));

        final int max = 100;
        ObjectIntHashMap<String> count = new ObjectIntHashMap(6);
        b.sample(new XoRoShiRo128PlusRandom(1), (PriReference<String> x)->{
            count.addToValue(x.get(),1);
            return count.sum() < max;
        });
        System.out.println(count);
    }

}