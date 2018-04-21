package nars.util.term;

import jcog.memoize.HijackMemoize;
import nars.term.Term;

public class TermCache/*<I extends InternedCompound>*/ extends HijackMemoize<InternedCompound, Term> {

    public TermCache(int capacity, int reprobes, boolean soft) {
        super(InternedCompound::compute, capacity, reprobes, soft);
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
