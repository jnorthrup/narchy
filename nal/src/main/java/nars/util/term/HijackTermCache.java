package nars.util.term;

import jcog.memoize.byt.ByteHijackMemoize;
import nars.Op;
import nars.term.Term;

public class HijackTermCache/*<I extends InternedCompound>*/ extends ByteHijackMemoize<InternedCompound, Term> {

    public HijackTermCache(int capacity, int reprobes) {
        super((x)->Op.terms.compoundInstance(Op.ops[x.op], x.dt, x.rawSubs.get()), capacity, reprobes);
    }

    //TODO
//    @Override
//    public float value(InternedCompound x, Term y) {
//        return DEFAULT_VALUE * x.value();
//    }

}
