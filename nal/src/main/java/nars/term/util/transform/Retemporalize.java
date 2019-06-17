package nars.term.util.transform;

import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;


public abstract class Retemporalize extends AbstractTermTransform.NegObliviousTermTransform {


    public static final Retemporalize retemporalizeAllToXTERNAL = new RetemporalizeAll(XTERNAL);
////    Retemporalize retemporalizeAllToZero = new RetemporalizeAll(0);
    public static final Retemporalize retemporalizeXTERNALToDTERNAL = new RetemporalizeFromTo(XTERNAL, DTERNAL);
    public static final Retemporalize retemporalizeXTERNALToZero = new RetemporalizeFromTo(XTERNAL, 0);


    abstract int dt(Compound x);

    @Nullable
    @Override
    protected final Term applyPosCompound(final Compound x) {
        return x.hasAny(Op.Temporal) ? transformTemporal(x, dt(x)) : x;
    }


    protected Term transformTemporal(Compound x, int dtNext) {
        int xdt = x.dt();
//        if (xdt == dtNext && !requiresTransform(x.subterms()))
//            return x;
//        else {
            Op xo = x.op();
            int n = xo.temporal ? dtNext : DTERNAL;
            if (n == xdt)
                return super.applyPosCompound(x); //fast fail if dt doesnt change
            else {
                return x.transform(this, xo, n);
            }
//        }
    }

    public final static class RetemporalizeAll extends Retemporalize {

        final int targetDT;

        public RetemporalizeAll(int targetDT) {
            this.targetDT = targetDT;
        }

        @Override
        public final int dt(Compound x) {
            return targetDT;
        }
    }



    public final static class RetemporalizeFromTo extends Retemporalize {

        final int from, to;

        public RetemporalizeFromTo(int from, int to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public int dt(Compound x) {
            int dt = x.dt();
            return dt == from ? to : dt;
        }
    }


}


