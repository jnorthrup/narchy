package nars.term.util;

import jcog.memoize.byt.ByteHijackMemoize;
import nars.term.Term;

import java.util.function.Function;

import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;

public class HijackTermCache/*<I extends InternedCompound>*/ extends ByteHijackMemoize<InternedCompound, Term> {

    public HijackTermCache(Function<InternedCompound,Term> f, int capacity, int reprobes) {
        super(f, capacity, reprobes, false);
    }


    @Override
    public float value(InternedCompound x, Term y) {
        float base = 1f/ bag.reprobes, mult;
        switch (x.dt) {
            case 0:
            case DTERNAL:
            case XTERNAL:
                mult = 1f;
                break;
            default:

                mult = 1/3f;
                break;
        }

        return base * mult;
    }

}
