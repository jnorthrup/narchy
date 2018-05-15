package jcog.pri;

/** caches hashcode in field */
public class PLinkHashCached<X> extends PLink<X>{

    public final int hash;

    public PLinkHashCached(X x, float p) {
        super(x, p);
        this.hash = x.hashCode();
    }

    @Override
    public final int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(Object that) {
        return hash == that.hashCode() && super.equals(that);
    }
}
