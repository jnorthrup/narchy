package jcog.pri;

/** aka MutableNLink */
public class Ranked<X> extends Weight {
    public X x;

    public Ranked() {
        this(null);
    }

    public Ranked(X x) {
        super(Float.NaN);
        this.x = x;
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
}
