package nars.util.term;

import jcog.memoize.HijackMemoize;
import nars.subterm.Subterms;

public class SubtermsCache extends HijackMemoize<InternedSubterms, Subterms> {

    public SubtermsCache(int capacity, int reprobes) {
        super(InternedSubterms::compute, capacity, reprobes, false);
    }

    @Override
    public float value(InternedSubterms x) {
        return DEFAULT_VALUE * x.value();
    }

    @Override
    public Computation<InternedSubterms, Subterms> computation(InternedSubterms xy, Subterms y) {
        xy.set(y);
        xy.priSet(value(xy));
        return xy;
    }

}
