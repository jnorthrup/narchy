package nars.term.transform;

import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.container.TermContainer;
import org.jetbrains.annotations.Nullable;

import java.util.function.IntSupplier;

import static nars.Op.Temporal;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;

@FunctionalInterface
public interface Retemporalize extends CompoundTransform {

    @Override
    int dt(Compound x);

    //Retemporalize retemporalizeAllToDTERNAL = new RetemporalizeAll(DTERNAL);
    //Retemporalize retemporalizeDTERNALToZero = new RetemporalizeFromTo(DTERNAL, 0);
    Retemporalize retemporalizeAllToXTERNAL = new RetemporalizeAll(XTERNAL);
    Retemporalize retemporalizeXTERNALToDTERNAL = new RetemporalizeFromTo(XTERNAL, DTERNAL);
    Retemporalize retemporalizeXTERNALToZero = new RetemporalizeFromTo(XTERNAL, 0);

    Retemporalize retemporalizeRoot = x -> {
                TermContainer xs = x.subterms();

                //any inside impl/conjunctions will disqualify the simple DTERNAL root form, but only in the next layer

                switch (x.op()) {
                    case CONJ:
                        if ((xs.subs()==2) &&
                                xs.hasAny(Temporal) && xs.OR(y->y.isAny(Temporal)) ||
                                xs.sub(0).unneg().equals(xs.sub(1).unneg())) {
                            return XTERNAL;
                        } else {
                            return DTERNAL; //simple
                        }
                    case IMPL:
                        //impl pred is always non-neg
                        return xs.hasAny(Temporal) && xs.OR(y->y.isAny(Temporal)) ||
                               xs.sub(0).unneg().equals(xs.sub(1)) ? XTERNAL : DTERNAL;
                    default:
                        throw new UnsupportedOperationException();
                }
            };


    @Nullable
    @Override
    default Term transform(Compound x, Op op, int ignored) {
        if (!x.hasAny(Temporal)) {
            return x;
        } else {
            int dtCur = x.dt(), dtNext;
            if (op.temporal) {
                dtNext = dt(x);
                if (dtCur == dtNext && !x.subterms().hasAny(Temporal))
                    return x; //no change
            } else {
                dtNext = dtCur;
            }
            return CompoundTransform.super.transform(x, op, dtNext);
        }
    }


    @Deprecated
    final class RetemporalizeAll implements Retemporalize {

        final int targetDT;

        public RetemporalizeAll(int targetDT) {
            this.targetDT = targetDT;
        }

        @Override
        public final int dt(Compound x) {
            return targetDT;
        }
    }

    final class RetemporalizeFromTo implements Retemporalize {

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

    final class RetemporalizeFromToFunc implements Retemporalize {

        final int from;
        final IntSupplier to;

        public RetemporalizeFromToFunc(int from, IntSupplier to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public int dt(Compound x) {
            int dt = x.dt();
            return dt == from ? to.getAsInt() : dt;
        }
    }
}
