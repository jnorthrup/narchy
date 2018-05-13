package nars.util.term.transform;

import nars.Op;
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
            assert (dt == XTERNAL || dt == DTERNAL);
            return xternalIfNecessary(x, Retemporalize.super.transformCompound(x, op, dt), dt);
        }

        @Override
        public Term transformTemporal(Compound x, int dtNext) {
            return xternalIfNecessary(x, Retemporalize.super.transformTemporal(x, dtNext), dtNext);
        }

        Term xternalIfNecessary(Compound x, Term y, int dtTried) {
            if (x != y && corrupted(x, y)) {
                //oops; deformed - we need XTERNAL
                return Retemporalize.super.transformCompound(x, x.op(), dtTried == DTERNAL ? XTERNAL : DTERNAL);
            }
            return y;
        }

        /** verifies events remain unchanged */
        private boolean corrupted(Term x, Term y) {
            Op xop;
            if (y == null || y instanceof Bool || ((xop = x.op())!=y.op())) {
                return true;
            }

            if (!x.equals(y)) {

                switch (xop) {
                    case CONJ:
                        return conjCorrupted(x, y);
                    case IMPL:
                        return implCorrupted(x, y);
                    default:
                        return (x.subs() != y.subs()) || (y.volume() != x.volume()) || y.structure() != x.structure(); //etc
                }
            }

            return false;
        }

        private boolean implCorrupted(Term x, Term y) {
            if (y.op() != IMPL || x.structure() != y.structure() || x.volume() != y.volume())
                return true;
            //compare subj and pred separately

            for (int i = 0; i < 2; i++) {
                Term a = x.sub(i);
                Term b = y.sub(i);
                if (corrupted(a, b)) {
                    return true;
                }
            }
            return false;
        }

        private boolean conjCorrupted(Term x, Term y) {
            boolean corrupted;
            if (y.structure() != x.structure()) {
                corrupted = true;
            } else {
                switch (x.dt()) {
                    case 0:
                    case DTERNAL:
                    case XTERNAL:
                        corrupted = (x.subs() != y.subs() || x.volume() != y.volume());
                        break;
                    default:
//                        System.out.println(x + " " + recursiveEvents(x));
//                        System.out.println(y + " " + recursiveEvents(y));
//                        System.out.println();
                        corrupted = recursiveEvents(x) != recursiveEvents(y);
                        break;
                }
            }
            return corrupted;
        }

        int recursiveEvents(Term x) {
            if (x.op() == NEG && x.unneg().op()==CONJ) {
                x = x.unneg();
            } else if (x.op() != CONJ) {
                return 1;
            }

            return x.intifyShallow((s,t) -> {
                switch (t.op()) {
                    case NEG:
                        return s + recursiveEvents(t.unneg()); //blow past through it
                    case CONJ:
                        return s + recursiveEvents(t); //t.sum(this::recursiveEvents);
                    default:
                        return s + 1;
                }
            }, 0);
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
        if (x.dt() == dtNext && !x.subterms().hasAny(Temporal))
            return x; //no change
        else
            return TermTransform.NegObliviousTermTransform.super.transformCompound(x, x.op(), dtNext);
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


