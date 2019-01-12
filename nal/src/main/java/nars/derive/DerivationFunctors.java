package nars.derive;

import nars.Builtin;
import nars.NAR;
import nars.Param;
import nars.op.Equal;
import nars.op.SetFunc;
import nars.op.UniSubst;
import nars.subterm.Subterms;
import nars.term.Functor;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atomic;
import nars.term.util.Conj;
import nars.term.util.Image;
import nars.term.util.LazyConj;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static nars.Op.*;
import static nars.term.atom.Bool.False;
import static nars.term.atom.Bool.Null;
import static nars.time.Tense.*;

public enum DerivationFunctors {
    ;

    public static Function<Atomic, Functor> get(Derivation d) {

        Map<Atomic, Functor> m = new HashMap<>();

        for (Termed s : Builtin.statik)
            if (s instanceof Functor.InlineFunctor)
                add(m, s);

        NAR nar = d.nar;


        Functor[] derivationFunctors = new Functor[]{
                d.myUniSubst,
                d.mySubst,
                d.polarizeFunc,
                d.polarizeTask,
                d.polarizeBelief,
                d.polarizeRandom,
                Image.imageExt,
                Image.imageInt,
                Image.imageNormalize,
                SetFunc.union,
                SetFunc.unionSect,
                SetFunc.differ,
                SetFunc.intersect,
                Equal.the,
                (Functor) nar.concept("unneg"),
                (Functor) nar.concept("negateEvents"),
                (Functor) nar.concept("conjWithout"),
                (Functor) nar.concept("conjWithoutPosOrNeg"),
                (Functor) nar.concept("chooseUnifiableSubEvent"),
//                (Functor) nar.concept("dropAnyEvent"),
                (Functor) nar.concept("without"),
                (Functor) nar.concept("withoutPosOrNeg"),
                new Functor.AbstractInlineFunctor2("conjBefore") {
                    @Override protected Term apply(Term conj, Term event) {
                        Term x = beforeOrAfter(conj, event, true, d, Param.TTL_CONJ_BEFORE_AFTER);
                        return x == null ? Null : x;
                    }
                },
                new Functor.AbstractInlineFunctor2("conjAfter") {
                    @Override protected Term apply(Term conj, Term event) {
                        Term x = beforeOrAfter(conj, event, false, d, Param.TTL_CONJ_BEFORE_AFTER);
                        return x == null ? Null : x;
                    }
                }
        };

        for (Termed x : derivationFunctors) //override any statik's
            add(m, x);


//        MetalBloomFilter<Atomic> pre = new MetalBloomFilter<>(fastAtomHash, m.size()*2, 2);
//        for (Atomic x : m.keySet()) {
//            pre.add(x);
//        }

        return //Maps.immutable.ofMap(m)::get;
                m::get;
        //x -> pre.contains(x) ? m.get(x) : null;
    }

    private static Functor add(Map<Atomic, Functor> m, Termed x) {
        return m.put((Atomic) x.term(), (Functor) x);
    }


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
        int varBits = VAR_DEP.bit | VAR_INDEP.bit;

        //sequence or commutive


        LazyConj x;
        if (Conj.isSeq(conj)) {

            x = LazyConj.events(conj);
            if (!beforeOrAfter)
                x.reverse(); //match from opposite direction
        } else {
            //conj.dt() == DTERNAL || conj.dt() == 0

            Subterms ss = conj.subterms();
            x = new LazyConj(ss.subs());
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
            UniSubst.MySubUnify s = d.myUniSubst.u;
            for (int matchUnify = parallelLead; matchUnify < n; matchUnify++) {
                s.ttl = ttl;
                Term ti = x.get(matchUnify);
                if (eVar || ti.hasAny(varBits)) {

                    s.reset(varBits, false);

                    if (event.unify(ti, s)) {

                        s.xy.forEach(d.xy::set);

                        if (n == 2) {
                            return x.get(1-matchUnify).replace(s.xy);
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
