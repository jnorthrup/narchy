package nars.term.transform;

import nars.Op;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Bool;
import org.jetbrains.annotations.Nullable;

import static nars.Op.*;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;

@FunctionalInterface
public interface Retemporalize extends TermTransform.NegObliviousTermTransform {

    int dt(Compound x);

    @Nullable
    @Override
    default Term transformCompound(final Compound x) {
        if (x.op().temporal || x.hasAny(Temporal)) {
            return transformTemporal(x, dt(x));
        } else {
            return x;
        }
    }

    //Retemporalize retemporalizeAllToDTERNAL = new RetemporalizeAll(DTERNAL);
    //Retemporalize retemporalizeDTERNALToZero = new RetemporalizeFromTo(DTERNAL, 0);
    Retemporalize retemporalizeAllToXTERNAL = new RetemporalizeAll(XTERNAL);
    Retemporalize retemporalizeAllToZero = new RetemporalizeAll(0);
    Retemporalize retemporalizeXTERNALToDTERNAL = new RetemporalizeFromTo(XTERNAL, DTERNAL);
    Retemporalize retemporalizeXTERNALToZero = new RetemporalizeFromTo(XTERNAL, 0);

    Retemporalize retemporalizeRoot = new Retemporalize() {


        @Override
        public @Nullable Term transformCompound(Compound x, Op op, int dt) {
            assert(dt == XTERNAL || dt == DTERNAL);
            return xternalIfNecessary(x, Retemporalize.super.transformCompound(x, op, dt), dt);
        }

        @Override
        public Term transformTemporal(Compound x, int dtNext) {
            return xternalIfNecessary(x, Retemporalize.super.transformTemporal(x, dtNext), dtNext);
        }

        Term xternalIfNecessary(Compound x, Term y, int dtTried) {

            if (x!=y) {
                //quick tests, not exhaustive
                //opX, for compounds, includes the subterms in the comparison
                //if ((y.opX() != x.opX()) || (y.volume()!=x.volume()) || y.structure()!=x.structure()) {
                boolean corrupted = false;
                Op op;
                if (y == null || y instanceof Bool || (op = x.op())!=y.op()) {
                    corrupted = true;
                } else {
                    if (op == CONJ) {
                        //verify event count remains the same

                        corrupted = y.structure()!=x.structure() || x.eventCount()!=y.eventCount();
                    } else if (op == IMPL){
                        //compare subj and pred separately
                        for (int i = 0; i < 2; i++) {
                            Term a = x.sub(i);
                            Term b = y.sub(i);
                            if (!a.equals(b) && (((a.opX() != b.opX()) || (a.volume() != b.volume()) || a.structure() != b.structure() || x.eventCount()!=y.eventCount()))) {
                                corrupted = true;
                                break;
                            }
                        }
                    }
                }

                if (corrupted) {
                    //oops; deformed - we need XTERNAL
                    return Retemporalize.super.transformCompound(x, x.op(), dtTried == DTERNAL ? XTERNAL : DTERNAL);
                }
            }
            return y;
        }

        @Override
        public int dt(Compound x) {
            return DTERNAL;

            //any inside impl/conjunctions will disqualify the simple DTERNAL root form, but only in the next layer

//            switch (x.op()) {
//                case CONJ: {
//                    int dt = x.dt();
//
//                    if ((dt == DTERNAL || dt == 0) || !x.subterms().hasAny(CONJ))
//                        return DTERNAL;
////                    Subterms xs = x.subterms();
////                    int n = xs.subs();
//
////                    if (xs.OR(isCommutiveConjOrImpl))
//                    return XTERNAL;
//
////                    if (((n == 2) &&
////                            (xs.isTemporal() ||
////                                    (xs.sub(0).unneg().equals(xs.sub(1).unneg()))
////                                    //Op.equalsOrContainEachOther(xs.sub(0), xs.sub(1))
////                            )
////                    )) {
////                        return XTERNAL;
////                    }
////
////                    return DTERNAL; //simple, if possible
//                }
//                case IMPL: {
//                    int dt = x.dt();
//
//                    if ((dt == DTERNAL || dt == 0) || !x.subterms().hasAny(CONJ))
//                        return DTERNAL;
//
////                    Subterms xs = x.subterms();
////                    //impl pred is always non-neg
////                    return xs.hasAny(CONJ) ||
////                            xs.sub(0).unneg().equals(xs.sub(1)) ? XTERNAL : DTERNAL;
//                    return XTERNAL;
//                }
//                default:
//                    //throw new UnsupportedOperationException();
//                    return DTERNAL; //non-temporal etc
//            }

        }
    };



    default Term transformTemporal(Compound x, int dtNext) {
        Subterms xx = x.subterms();

        if (x.dt() == dtNext && !xx.hasAny(Temporal))
            return x; //no change

        Op op = x.op();
//        if (dtNext == XTERNAL && op == CONJ && xx.hasAny(CONJ)) {
//            //recursive conj, decompose to events
//            ArrayHashSet<Term> s = new ArrayHashSet();
//            x.eventsWhile((when, zz) -> {
//                if (!zz.equals(x)) {
//                    s.add(zz);
//                }
//                return true;
//            }, 0, false, false, false, 0);
//            List<Term> sl = s.list;
//            int sln = sl.size();
//            if (sln > 1) {
//                for (int i = 0; i < sln; i++) {
//                    Term sli = sl.get(i).transform(Retemporalize.this);
//                    if (sli == null)
//                        return null; //fail
//                    sl.set(i, sli);
//                }
//                Term y = CONJ.the(XTERNAL, sl);
//                if (x!=y && x.equals(y))
//                    return x;
//                else
//                    return y;
//            }
//        }


        return TermTransform.NegObliviousTermTransform.super.transformCompound(x, op, dtNext);
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

//    Predicate<Term> isCommutiveConjOrImpl = xx -> {
//        switch (xx.op()) {
//            case CONJ:
//                int xdt = xx.dt();
//                if (xdt == DTERNAL || xdt == 0)
//                    return true;
//                break;
//            case IMPL:
//                return true;
//        }
//        return xx.hasAny(IMPL);
//    };
}


