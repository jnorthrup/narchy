package jcog.pri;

import jcog.sort.FloatRank;

/** aka MutableNLink */
public final class Ranked<X> extends Weight {
    public X x;

    public Ranked() {
        this(null);
    }

    public Ranked(X x) {
        super(Float.NaN);
        set(x);
    }

    public Ranked<X> set(X x, float pri) {
        set(x);
        pri(pri);
        return this;
    }

    public void clear() {
        super.clear();
        x = null;
    }

    public Ranked<X> set(X x) {
        this.x = x;
        return this;
    }

    public final float apply(FloatRank<X> rank, float min) {
        float p = this.pri;
        if (p == p)
            return p;
        else
            return this.pri = rank.rank(x, min);
    }

}
