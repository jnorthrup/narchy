package jcog.pri;

import jcog.Util;

/**
 * priority link: numeric link restricted to 0..1.0 range
 */
public class PLink<X> extends NLink<X> {

    public PLink(X x, float p) {
        super(x, p);
    }

    @Override
    public float v(float x) {
        return Util.unitize(x);
    }

    @Override
    public String toString() {
        return '$' + super.toString();
    }



}
