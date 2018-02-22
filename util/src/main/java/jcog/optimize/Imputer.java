package jcog.optimize;

import com.google.common.base.Joiner;

import java.util.List;

/** general-purpose POJO imputer context */
public class Imputer {

    public <X> Tweaks<X> learn(X example, String... tags) {

        Class clz = example.getClass();
        Tweaks<X> t = new Tweaks<>(example).learn();
        for (Tweak<X,?> u : t.tweaks) {
            Object v = u.get(example);
            System.out.println(clz + " * " + Joiner.on(',').join(tags) + " ==> " + u + " = " + v);
        }
        return t;
    }

    /** returns a 'report' of the tweaks applied */
    public List<Tweak> apply(Object x, String... tags) {
        return List.of();
    }

}
