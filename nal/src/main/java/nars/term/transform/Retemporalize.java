package nars.term.transform;

import jcog.data.ArrayHashSet;
import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.anon.DirectCompoundTransform;
import nars.term.sub.Subterms;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.IntSupplier;

import static nars.Op.CONJ;
import static nars.Op.Null;
import static nars.Op.Temporal;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;

@FunctionalInterface
public interface Retemporalize extends DirectCompoundTransform {

    @Override
    int dt(Compound x);

    //Retemporalize retemporalizeAllToDTERNAL = new RetemporalizeAll(DTERNAL);
    //Retemporalize retemporalizeDTERNALToZero = new RetemporalizeFromTo(DTERNAL, 0);
    Retemporalize retemporalizeAllToXTERNAL = new RetemporalizeAll(XTERNAL);
    Retemporalize retemporalizeAllToZero = new RetemporalizeAll(0);
    Retemporalize retemporalizeXTERNALToDTERNAL = new RetemporalizeFromTo(XTERNAL, DTERNAL);
    Retemporalize retemporalizeXTERNALToZero = new RetemporalizeFromTo(XTERNAL, 0);

    Retemporalize retemporalizeRoot = x -> {
        Subterms xs = x.subterms();

        //any inside impl/conjunctions will disqualify the simple DTERNAL root form, but only in the next layer

        switch (x.op()) {
            case CONJ:

                int n = xs.subs();
                if (((n == 2) &&
                        (
                            xs.isTemporal()
                                ||
                            (xs.sub(0).unneg().equals(xs.sub(1).unneg())))
                        )) {
                    return XTERNAL;
                } else {
                    return DTERNAL; //simple
                }
            case IMPL:
                //impl pred is always non-neg
                return xs.hasAny(CONJ) ||
                        xs.sub(0).unneg().equals(xs.sub(1)) ? XTERNAL : DTERNAL;
            default:
                throw new UnsupportedOperationException();
        }
    };


    @Nullable
    @Override
    default Term transform(final Compound x, Op op, int dt) {
        int dtNext;
        if (op.temporal) {
            dtNext = dt(x);
            Subterms xx = x.subterms();
            if (op == CONJ && xx.hasAny(CONJ)) {
                //recursive conj, decompose to events
                ArrayHashSet<Term> s = new ArrayHashSet();
                x.eventsWhile((when, zz) -> {
                    if (!zz.equals(x)) {
                        s.add(zz);
                    }
                    return true;
                }, 0, false, false, false, 0);
                if (s.size() > 1) {
                    List<Term> sl = s.list;
                    sl.replaceAll((zz) -> zz.transform(Retemporalize.this));
                    return CONJ.the(XTERNAL, sl);
                }
            }
            if (dt == dtNext && !xx.hasAny(Temporal))
                return x; //no change

        } else {
            if (!x.hasAny(Temporal))
                return x;

            assert (dt == DTERNAL);
            dtNext = DTERNAL;
        }

        return DirectCompoundTransform.super.transform(x, op, dtNext);
    }

    @Override
    default Term transform(Compound x) {
        if (!x.hasAny(Temporal)) {
            return x;
        } else {
            return DirectCompoundTransform.super.transform(x);
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
