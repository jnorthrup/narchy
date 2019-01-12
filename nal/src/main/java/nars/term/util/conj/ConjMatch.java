package nars.term.util.conj;

import nars.derive.Derivation;
import nars.op.UniSubst;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.term.util.Image;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;

import static nars.Op.CONJ;
import static nars.Op.VAR_DEP;
import static nars.term.atom.Bool.False;
import static nars.term.atom.Bool.Null;
import static nars.time.Tense.*;

public class ConjMatch {

    /**
     * returns the prefix or suffix sequence of a specific matched subevent
     */
    public static Term beforeOrAfter(Term conj, Term event, boolean beforeOrAfter, Derivation d, int ttl /*, unifyOrEquals, includeMatchedEvent */) {


        if (conj.op() != CONJ || conj.dt()==XTERNAL)
            return Null;

        event = Image.imageNormalize(event);

        if (event.volume() >= conj.volume())
            return Null;

        return beforeOrAfterSeq(conj, event, beforeOrAfter, d, ttl);
    }

    private static Term beforeOrAfterSeq(Term conj, Term event, boolean beforeOrAfter, Derivation d, int ttl) {
        int varBits =
                //VAR_DEP.bit | VAR_INDEP.bit;
                VAR_DEP.bit;

        //sequence or commutive


        ConjLazy x;
        if (Conj.isSeq(conj)) {

            x = ConjLazy.events(conj);
            if (!beforeOrAfter)
                x.reverse(); //match from opposite direction
        } else {
            //conj.dt() == DTERNAL || conj.dt() == 0

            Subterms ss = conj.subterms();
            x = new ConjLazy(ss.subs());
            long when = (conj.dt() == DTERNAL) ? ETERNAL : 0;
            for (Term cc : ss)
                x.add(when, cc);
        }

        int n = x.size();
        assert (n > 1);

        long leadOcc = x.when(0);
        boolean leadingEventParallel = (x.when(1) == leadOcc);

        //skip a leading non-parallel event, but dont skip any if parallel
        int parallelLead = leadingEventParallel ? 0 : 1;

        if (!conj.impossibleSubTerm(event)) {
            int matchExact = -1;
            for (int i = parallelLead; i < n; i++) {
                if (x.get(i).equals(event)) {
                    matchExact = i;
                    break;
                }
            }
            if (matchExact != -1) {
                if (n == 2) {
                    return x.get(1-matchExact);
                } else {
                    //include any other events occurring at the same time as matchExact but not those after it
                    LongObjectPair<Term> me = x.removeEvent(matchExact);
                    long meTime = me.getOne();
                    x.removeIf(
                            beforeOrAfter ?
                                    (when, what) -> when > meTime
                                    :
                                    (when, what) -> when < meTime
                    );

                    return x.term();
                }
            }
        }

        //try to unify if variables present
        //TODO only unif
        boolean eVar = event.hasAny(varBits);
        if (eVar || (conj.hasAny(varBits) /*&& x.anySatisfy(1, n, z -> z.getTwo().hasAny(varBits)))*/)) {
            //TODO use SubUnify correctly (ie. termutes via tryMatch )
            UniSubst.MySubUnify s = d.uniSubst.u;
            for (int matchUnify = parallelLead; matchUnify < n; matchUnify++) {
                s.ttl = ttl;
                Term ti = x.get(matchUnify);
                if (eVar || ti.hasAny(varBits)) {

                    s.reset(varBits, false);

                    if (event.unify(ti, s)) {

                        //s.xy.forEach(d.xy::set);
                        s.xy.forEach(d.retransform::put);

                        if (n == 2) {
                            return x.get(1-matchUnify).replace(s.xy); //TODO keep trying if term fails to transform
                        } else {

                            //include any other events occurring at the same time as matchExact but not those after it

                            LongObjectPair<Term> me = x.removeEvent(matchUnify);
                            long meTime = me.getOne();
                            x.removeIf(
                                    beforeOrAfter ?
                                            (when,what) -> when > meTime
                                            :
                                            (when,what) -> when < meTime
                            );

                            n = x.size();
                            for (int j = 0; j < n; j++) {
                                Term jj = x.get(j).replace(s.xy);
                                if (jj == False) {
                                    return False; //TODO keep trying on different event?
                                }
                                x.set(j, jj);
                            }
                            return x.term();
                        }
                    }
                }
            }

        }

        return Null;
    }


//        {
//            Map<Term, Termed> n = new HashMap<>(Builtin.statik.length);
//            for (Termed s : Builtin.statik) {
//                if (s instanceof Functor.InlineFunctor)
//                    n.put(s.term(), s);
//            }
//            this.staticFunctors = Maps.immutable.ofMap(n);
//        }


//    final static HashProvider<Atomic> fastAtomHash = new HashProvider<Atomic>() {
//        @Override
//        public int hash1(Atomic element) {
//            return element.hashCode();
//        }
//
//        @Override
//        public int hash2(Atomic element) {
//            return element.bytes().length;
//        }
//    };
}
