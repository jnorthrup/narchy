package nars.util.term;

import jcog.memoize.HijackMemoize;
import nars.term.Term;

public class HijackTermCache/*<I extends InternedCompound>*/ extends HijackMemoize<InternedCompound, Term> {

    public HijackTermCache(int capacity, int reprobes, boolean soft) {
        super(InternedCompound::compute, capacity, reprobes, soft);
    }

    @Override
    protected boolean keyEquals(Object k, Computation<InternedCompound, Term> p) {
        return p.equals(k);
    }

    @Override
    public float value(InternedCompound x) {
        return DEFAULT_VALUE * x.value();
    }

    @Override
    public Computation<InternedCompound, Term> computation(InternedCompound xy, Term y) {
        xy.set(y);
        xy.priSet(value(xy));
        return xy;
    }
}
