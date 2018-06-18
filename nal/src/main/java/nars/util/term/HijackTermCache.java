package nars.util.term;

import jcog.memoize.HijackMemoize;
import jcog.pri.PriProxy;
import nars.term.Term;

public class HijackTermCache/*<I extends InternedCompound>*/ extends HijackMemoize<InternedCompound, Term> {

    public HijackTermCache(int capacity, int reprobes) {
        super(InternedCompound::compute, capacity, reprobes);
    }

    @Override
    protected boolean keyEquals(Object k, PriProxy<InternedCompound, Term> p) {
        return p.equals(k);
    }

    @Override
    public float value(InternedCompound x, Term y) {
        return DEFAULT_VALUE * x.value();
    }

    @Override
    public PriProxy<InternedCompound, Term> computation(InternedCompound xy, Term y) {
        xy.set(y);
        xy.priSet(value(xy, y));
        return xy;
    }
}
