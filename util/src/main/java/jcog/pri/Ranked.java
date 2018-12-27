package jcog.pri;

/** aka MutableNLink */
public class Ranked<X> {
    public X x;
    public float pri;

    public Ranked() {
        this(null);
    }

    public Ranked(X x) {
        this.x = x;
        this.pri = Float.NaN;
    }

    public Ranked<X> set(X x, float pri) {
        set(x);
        this.pri = pri;
        return this;
    }

    public void clear() {
        x = null;
        pri = Float.NaN;
    }

    public Ranked<X> set(X x) {
        this.x = x;
        return this;
    }
}
