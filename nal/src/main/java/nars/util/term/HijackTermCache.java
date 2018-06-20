package nars.util.term;

import jcog.memoize.byt.ByteHijackMemoize;
import nars.term.Term;

import java.util.function.Function;

public class HijackTermCache/*<I extends InternedCompound>*/ extends ByteHijackMemoize<InternedCompound, Term> {

    public HijackTermCache(Function<InternedCompound,Term> f, int capacity, int reprobes) {
        super(f, capacity, reprobes);
    }

    //TODO
//    @Override
//    public float value(InternedCompound x, Term y) {
//        return DEFAULT_VALUE * x.value();
//    }

}
