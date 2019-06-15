package nars.term.util.transform;

import jcog.Util;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Interval;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.TreeSet;

import static nars.Op.*;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;

/**
 * defines target -> concept mapping.
 * generally, terms name concepts directly except in temporal cases there is a many to one target to concept mapping
 * effectively using concepts to 'bin' together varieties of related terms
 */
public class Conceptualization {

    public final static Retemporalize DirectXternal = new Untemporalization() {
        @Override
        protected Term transformConj(Compound y) {
            return y.dt(XTERNAL);
        }
    };

    /** distinguishes between sequences and non-sequences */
    public final static Retemporalize PreciseXternal = new Untemporalization() {
        @Override
        protected Term transformConj(Compound y) {
            int ydt = y.dt();
            return (ydt == DTERNAL || ydt == 0) ? y.dt(DTERNAL) : y.dt(XTERNAL);
        }
    };

    public final static Retemporalize FlattenAndDeduplicateConj = new Untemporalization() {
        @Override
        protected  Term transformConj(Compound y) {
            Subterms yy = y.subterms();
            if (yy.hasAny(CONJ) && yy.OR(yyy -> yyy/*.unneg()*/.op() == CONJ)) {
                //collapse any embedded CONJ which will inevitably have dt=XTERNAL
                UnifiedSet<Term> t = new UnifiedSet(yy.subs());
                for (Term yyy : yy) {
//                    yyy = yyy.unneg();
                    if (yyy.op() == CONJ) {
                        yyy.eventsAND((when, what)->{
                            if (what instanceof Interval) {
                                //strip interval in concept
                                Util.nop();
                            } else {
                                t.add(what);
                            }
                            return true;
                        }, 0, true, true);
                        ///yyy.subterms().forEach(t::add);
                    } else {
                        if (!(yyy instanceof Interval))
                            t.add(yyy);
                    }
                }

                if (yy.subs() != 1 && t.size() == 1) {
                    Term tf = t.getFirst();
                    return CONJ.the(XTERNAL, tf, tf);
                } else
                    return CONJ.the(XTERNAL, t);
            }
            return null;
        }
    };

    /** untested */
    public static final Retemporalize FlattenAndDeduplicateAndUnnegateConj = new Untemporalization() {
        @Override
        protected  Term transformConj(Compound y) {
            Subterms yy = y.subterms();
            if (yy.hasAny(CONJ) /*&& yy.OR(yyy -> yyy.unneg().op() == CONJ)*/) {
                TreeSet<Term> t = new TreeSet();
                yy.recurseTerms(x -> true, (yyy,parent)->{
                    if (parent.unneg().op()==CONJ && yyy.unneg().op()!=CONJ)
                        t.add(yyy);

                    return true;
                }, y);


                if (t.size() == 1 && yy.subs() != 1) {
                    Term tf = t.first();
                    return CONJ.the(XTERNAL, tf, tf);
                } else
                    return CONJ.the(XTERNAL, t);
            }
            return y;
        }


    };

    abstract private static class Untemporalization extends Retemporalize {

        abstract protected Term transformConj(Compound y);

        @Override
        public final Term transformTemporal(Compound x, int dtNext) {
            Op xo = x.op();

            if ((xo == INH || xo == SIM) && ((x.subterms().structureSurface()&CONJ.bit)!=0)) {
                //HACK
                return Retemporalize.retemporalizeXTERNALToDTERNAL.applyPosCompound(x);
            }

            int dt = xo.temporal ? XTERNAL : DTERNAL;

            if (x instanceof Compound && x.op() == CONJ) {
                Term c = transformConj(x);
                if (c!=null)
                    return c;
            }

            Term y = applyCompound(x, xo, dt);
            return y;

        }

        @Override
        public final int dt(Compound x) {
            return x.op().temporal ? XTERNAL : DTERNAL;
        }
    }


}
