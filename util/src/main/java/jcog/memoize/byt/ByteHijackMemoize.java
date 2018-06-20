package jcog.memoize.byt;

import jcog.memoize.HijackMemoize;
import jcog.pri.PriProxy;

import java.util.function.Function;

public class ByteHijackMemoize<X extends ByteKey,Y> extends HijackMemoize<X,Y> {

    public ByteHijackMemoize(Function<X, Y> f, int capacity, int reprobes) {
        super(f, capacity, reprobes);
    }

    @Override
    public final PriProxy<X, Y> computation(X x, Y y) {
        return x.intern(y, value(x, y));
    }

    @Override
    protected final boolean keyEquals(Object k, PriProxy p) {
        return p.equals(k);
    }


}
