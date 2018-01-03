package nars.term.transform;

import jcog.data.ArrayHashSet;
import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.term.sub.Subterms;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

import static nars.Op.*;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;

@FunctionalInterface
public interface Retemporalize extends CompoundTransform {


    Predicate<Term> isCommutiveConjOrImpl = xx -> {
        switch (xx.op()) {
            case CONJ:
                int xdt = xx.dt();
                if (xdt == DTERNAL || xdt == 0)
                    return true;
                break;
            case IMPL:
                return true;
        }
        return xx.hasAny(IMPL);
    };

    @Override
    int dt(Compound x);

    //Retemporalize retemporalizeAllToDTERNAL = new RetemporalizeAll(DTERNAL);
    //Retemporalize retemporalizeDTERNALToZero = new RetemporalizeFromTo(DTERNAL, 0);
    Retemporalize retemporalizeAllToXTERNAL = new RetemporalizeAll(XTERNAL);
    Retemporalize retemporalizeAllToZero = new RetemporalizeAll(0);
    Retemporalize retemporalizeXTERNALToDTERNAL = new RetemporalizeFromTo(XTERNAL, DTERNAL);
    Retemporalize retemporalizeXTERNALToZero = new RetemporalizeFromTo(XTERNAL, 0);

    Retemporalize retemporalizeRoot = new Retemporalize() {

        @Override
        public Term transform(Compound x) {
            return transform(x, x.op(), dt(x));
        }

        @Override
        public @Nullable Term transform(Compound x, Op op, int dt) {

            Term c1 = Retemporalize.super.transform(x, op, dt);
            if (op.temporal) {
                if (((dt != XTERNAL) && (c1 == null)) || (c1 instanceof Bool) ||
                        (
                                //quick tests, not exhaustive
                                (c1.op() != op) || (c1.volume()!=x.volume()) || c1.subs() != x.subs())
                        ) {
                    return transformTemporal(x, XTERNAL); //oops; deformed - we need XTERNAL
                }
            }
            return c1;
        }

        @Override
        public int dt(Compound x) {
            //any inside impl/conjunctions will disqualify the simple DTERNAL root form, but only in the next layer

            switch (x.op()) {
                case CONJ: {
                    int dt = x.dt();

                    if ((dt == DTERNAL || dt == 0) || !x.subterms().hasAny(CONJ))
                        return DTERNAL;
//                    Subterms xs = x.subterms();
//                    int n = xs.subs();

//                    if (xs.OR(isCommutiveConjOrImpl))
                    return XTERNAL;

//                    if (((n == 2) &&
//                            (xs.isTemporal() ||
//                                    (xs.sub(0).unneg().equals(xs.sub(1).unneg()))
//                                    //Op.equalsOrContainEachOther(xs.sub(0), xs.sub(1))
//                            )
//                    )) {
//                        return XTERNAL;
//                    }
//
//                    return DTERNAL; //simple, if possible
                }
                case IMPL: {
                    int dt = x.dt();

                    if ((dt == DTERNAL || dt == 0) || !x.subterms().hasAny(CONJ))
                        return DTERNAL;

//                    Subterms xs = x.subterms();
//                    //impl pred is always non-neg
//                    return xs.hasAny(CONJ) ||
//                            xs.sub(0).unneg().equals(xs.sub(1)) ? XTERNAL : DTERNAL;
                    return XTERNAL;
                }
                default:
                    //throw new UnsupportedOperationException();
                    return DTERNAL; //non-temporal etc
            }

        }
    };

    @Nullable
    @Override
    default Term transform(final Compound x, Op op, int _dt) {
        if (op.temporal) {
            return transformTemporal(x, dt(x));
        } else {
            return CompoundTransform.super.transform(x, op, DTERNAL);
        }
    }

    default Term transformTemporal(Compound x, int dtNext) {
        Subterms xx = x.subterms();
        Op op = x.op();
        if (dtNext == XTERNAL && op == CONJ && xx.hasAny(CONJ)) {
            //recursive conj, decompose to events
            ArrayHashSet<Term> s = new ArrayHashSet();
            x.eventsWhile((when, zz) -> {
                if (!zz.equals(x)) {
                    s.add(zz);
                }
                return true;
            }, 0, false, false, false, 0);
            List<Term> sl = s.list;
            int sln = sl.size();
            if (sln > 1) {
                for (int i = 0; i < sln; i++) {
                    Term sli = sl.get(i).transform(Retemporalize.this);
                    if (sli == null)
                        return null; //fail
                    sl.set(i, sli);
                }
                return CONJ.the(XTERNAL, sl);//.transform(this);
            }
        }
        if (x.dt() == dtNext && !xx.hasAny(Temporal))
            return x; //no change

        return CompoundTransform.super.transform(x, op, dtNext);
    }

    @Override
    default Term transform(Compound x) {
        if (!x.hasAny(Temporal)) {
            return x;
        } else {
            return CompoundTransform.super.transform(x);
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

//    final class RetemporalizeFromToFunc implements Retemporalize {
//
//        final int from;
//        final IntSupplier to;
//
//        public RetemporalizeFromToFunc(int from, IntSupplier to) {
//            this.from = from;
//            this.to = to;
//        }
//
//        @Override
//        public int dt(Compound x) {
//            int dt = x.dt();
//            return dt == from ? to.getAsInt() : dt;
//        }
//    }
}
