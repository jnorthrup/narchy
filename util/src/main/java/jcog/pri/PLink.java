package jcog.pri;

import jcog.Util;
import org.jetbrains.annotations.Nullable;

/**
 * priority link: numeric link restricted to 0..1.0 range
 */
public class PLink<X> extends NLink<X> {


    public PLink(X x, float p) {
        super(x, p);
    }

    @Override
    public float priSet(float p) {
        return super.priSet(Util.unitize(p));
    }

    @Nullable @Override
    public PLink<X> clonePri() {
        float p = pri;
        return (p==p) ? new PLink<>(id, p) : null;
    }


    @Override
    public String toString() {
        return "$" + super.toString();
    }



}
