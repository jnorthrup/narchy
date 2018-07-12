package jcog.pri;

import static jcog.Texts.n4;

/**
 * immutable object + mutable number pair;
 * considered in a 'deleted' state when the value is NaN
 */
public class NLink<X> extends Pri implements PriReference<X> {

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
    public boolean isDeleted() {
        float p = pri();
        return p != p;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    final public X get() {
        return id;
    }


    @Override
    public String toString() {
        return n4(pri()) + " " + id;
    }


}
