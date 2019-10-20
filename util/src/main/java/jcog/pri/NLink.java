package jcog.pri;

import org.eclipse.collections.api.block.function.primitive.FloatFunction;

import static jcog.Texts.n4;

/**
 * immutable object + mutable number pair;
 * considered in a 'deleted' state when the value is NaN
 */
public class NLink<X> extends AtomicPri implements PriReference<X> {

    public final X id;

    public NLink(X x, float v) {
        super(v);
        this.id = x;
    }


    @Override
    public boolean equals(Object that) {
        return (this == that) || id.equals(
                (that instanceof NLink) ? ((NLink) that).id
                        :
                        that
        );
    }


    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public final X get() {
        return id;
    }


    @Override
    public String toString() {
        return n4(pri()) + ' ' + id;
    }

    public float priElse(FloatFunction<X> prioritization) {
        var p = pri();
        return p != p ? priSetAndGet(prioritization.floatValueOf(id)) : p;
    }

}
