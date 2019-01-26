package jcog.pri;

/** adjustable mutable weight value; non-atomic */
public class Weighted implements Prioritizable {
    public float pri;

    public Weighted() {

    }

    public Weighted(float initial) {
        this.pri = initial;
    }


    protected void clear() {
        pri = Float.NaN;
    }

    @Override
    public float pri(float p) {
        return this.pri = p;
    }

    @Override
    public float pri() {
        return pri;
    }
}
