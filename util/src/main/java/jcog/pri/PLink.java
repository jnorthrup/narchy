package jcog.pri;

/**
 * priority link: numeric link restricted to 0..1.0 range
 */
public class PLink<X> extends NLink<X> implements UnitPrioritizable {

    public PLink(X x, float p) {
        super(x, p);
    }

    @Override
    protected final boolean unit() {
        return true;
    }

    @Override
    public String toString() {
        return '$' + super.toString();
    }

}
