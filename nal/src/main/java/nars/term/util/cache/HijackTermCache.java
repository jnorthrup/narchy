package nars.term.util.cache;

import jcog.memoize.byt.ByteHijackMemoize;
import nars.term.Term;

import java.util.function.Function;

public class HijackTermCache/*<I extends InternedCompound>*/ extends ByteHijackMemoize<Intermed, Term> {

    public HijackTermCache(Function<Intermed,Term> f, int capacity, int reprobes) {
        super(f, capacity, reprobes, false);
    }


//    @Override
//    public float value(InternedCompound x, Term y) {
//        float base = 1f/ bag.reprobes, mult;
//        switch (x.dt) {
//            case 0:
//            case DTERNAL:
//            case XTERNAL:
//                mult = 1f;
//                break;
//            default:
//
//                mult = 1/3f;
//                break;
//        }
//
//        return base * mult;
//    }

}
