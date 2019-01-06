package nars.term.util.transform;

import nars.Op;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termlike;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

import static nars.Op.CONJ;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;


public abstract class Retemporalize extends TermTransform.NegObliviousTermTransform {


    public static final Retemporalize retemporalizeAllToXTERNAL = new RetemporalizeAll(XTERNAL);
////    Retemporalize retemporalizeAllToZero = new RetemporalizeAll(0);
    public static final Retemporalize retemporalizeXTERNALToDTERNAL = new RetemporalizeFromTo(XTERNAL, DTERNAL);
    public static final Retemporalize retemporalizeXTERNALToZero = new RetemporalizeFromTo(XTERNAL, 0);


    /**
     * un-temporalize
     */
    public final static Retemporalize root = new Retemporalize() {

        @Override
        public Term transformTemporal(Compound x, int dtNext) {
            Op xo = x.op();
            // && dtNext == DTERNAL ? XTERNAL : DTERNAL);
            int dt = xo.temporal ? XTERNAL : DTERNAL;
            Term y = x.transform(this, xo, dt);
            if (y instanceof Compound && y.op()==CONJ) {
                Subterms yy = y.subterms();
                if (yy.OR(yyy -> yyy.op()==CONJ)) {
                    //collapse any embedded CONJ which will inevitably have dt=XTERNAL
                    UnifiedSet<Term> t = new UnifiedSet(yy.subs());
                    for (Term yyy : yy) {
                        if (yyy.op()==CONJ) {
                            yyy.subterms().forEach(t::add);
                        } else {
                            t.add(yyy);
                        }
                    }
                    if (yy.subs() != 1 && t.size() == 1) {
                        Term tf = t.getFirst();
                        return Op.terms.theCompound(CONJ, XTERNAL, tf, tf);
                    } else
                        return CONJ.the(XTERNAL, t);
                }
            }
            return y;
        }

        @Override
        public int dt(Compound x) {
            return x.op().temporal ? XTERNAL : DTERNAL;
        }
    };


    abstract int dt(Compound x);

    @Nullable
    @Override
    protected final Term transformNonNegCompound(final Compound x) {
        return requiresTransform(x) ? transformTemporal(x, dt(x)) : x;
    }


    protected Term transformTemporal(Compound x, int dtNext) {
        int xdt = x.dt();
        if (xdt == dtNext && !requiresTransform(x.subterms()))
            return x;
        else {
            Op xo = x.op();
            int n = xo.temporal ? dtNext : DTERNAL;
            if (n == xdt)
                return super.transformNonNegCompound(x); //fast fail if dt doesnt change
            else {
                return x.transform(this, xo, n);
            }
        }
    }

    /**
     * conditions on which recursive descent is required; this is the most general case.
     * some implementations will have more specific cases that can elide the
     * need for descent. ex: isTemporal() is narrower than x.hasAny(Op.Temporal)
     */
    protected static boolean requiresTransform(Termlike x) {
        return x.hasAny(Op.Temporal);
    }

    @Deprecated
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


