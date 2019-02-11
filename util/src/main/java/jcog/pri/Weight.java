package jcog.pri;

/** adjustable mutable weight value; non-atomic */
public class Weight implements Prioritizable {
    public float pri;


    public Weight(float initial) {
        pri(initial);
    }


    protected void clear() {
        pri(Float.NaN);
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
