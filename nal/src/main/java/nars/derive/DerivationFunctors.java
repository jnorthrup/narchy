package nars.derive;

import jcog.bloom.hash.HashProvider;
import jcog.data.list.FasterList;
import nars.Builtin;
import nars.NAR;
import nars.op.Equal;
import nars.op.SetFunc;
import nars.op.SubUnify;
import nars.term.Functor;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atomic;
import nars.term.util.Conj;
import nars.term.util.Image;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static nars.Op.*;
import static nars.term.atom.Bool.Null;

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
                (Functor) nar.concept("conjWithoutAll"),
                (Functor) nar.concept("conjWithoutPosOrNeg"),
                (Functor) nar.concept("conjDropIfEarliest"),
                (Functor) nar.concept("conjDropIfLatest"),
                (Functor) nar.concept("chooseUnifiableSubEvent"),
                (Functor) nar.concept("dropAnyEvent"),
                (Functor) nar.concept("without"),
                (Functor) nar.concept("withoutPosOrNeg"),
                new Functor.AbstractInlineFunctor2("conjBefore") {
                    @Override
                    protected Term apply(Term conj, Term event) {
                        Term x = beforeOrAfter(conj, event, true, d, 4);
                        if (x == null)
                            return Null;
                        return x;
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

//        {
//            Map<Term, Termed> n = new HashMap<>(Builtin.statik.length);
//            for (Termed s : Builtin.statik) {
//                if (s instanceof Functor.InlineFunctor)
//                    n.put(s.term(), s);
//            }
//            this.staticFunctors = Maps.immutable.ofMap(n);
//        }


    final static HashProvider<Atomic> fastAtomHash = new HashProvider<Atomic>() {
        @Override
        public int hash1(Atomic element) {
            return element.hashCode();
        }

        @Override
        public int hash2(Atomic element) {
            return element.bytes().length;
        }
    };


    /**
     * returns the prefix or suffix sequence of a specific matched subevent
     */
    public static Term beforeOrAfter(Term conj, Term event, boolean beforeOrAfter, Derivation d, int ttl /*, unifyOrEquals, includeMatchedEvent */) {
        int varBits = VAR_DEP.bit | VAR_INDEP.bit;

        if (conj.op() != CONJ || event.volume() > conj.volume())
            return null;

        if (Conj.isSeq(conj)) {
            FasterList<LongObjectPair<Term>> x = conj.eventList();
            if (!beforeOrAfter)
                x.reverse();

            int n = x.size();

            if (!conj.impossibleSubTerm(event)) {
                int matchExact = -1;
                assert (n > 1);
                for (int i = 1; i < n; i++) {
                    LongObjectPair<Term> xi = x.get(i);
                    if (xi.getTwo().equals(event)) {
                        matchExact = i;
                        break;
                    }
                }
                if (matchExact != -1) {
                    if (n == 2) {
                        return x.get(0).getTwo();
                    } else {
                        //include any other events occurring at the same time as matchExact but not those after it
                        LongObjectPair<Term> me = x.remove(matchExact);
                        long meTime = me.getOne();
                        x.removeIf(xx -> xx.getOne() > meTime);

                        return Conj.conj(x);
                    }
                }
            }

            //try to unify if variables present
            //TODO only unif
            boolean eVar = event.hasAny(varBits);
            if (eVar || (conj.hasAny(varBits) /*&& x.anySatisfy(1, n, z -> z.getTwo().hasAny(varBits)))*/)) {
                //TODO use SubUnify correctly (ie. termutes via tryMatch )
                SubUnify s =
                    //new SubUnify(d.random);
                    d.myUniSubst.u;
                for (int matchUnify = 1; matchUnify < n; matchUnify++) {
                    s.ttl = ttl;
                    LongObjectPair<Term> xi = x.get(matchUnify);
                    Term ti = xi.getTwo();
                    if (eVar || ti.hasAny(varBits)) {

                        s.clear();

                        if (event.unify(ti, s)) {

                            s.xy.forEach(d.xy::set);

                            if (n == 2) {
                                assert (matchUnify == 1);
                                return x.get(0).getTwo().replace(s.xy);
                            } else {

                                //include any other events occurring at the same time as matchExact but not those after it

                                LongObjectPair<Term> me = x.remove(matchUnify);
                                long meTime = me.getOne();
                                x.removeIf(xx -> xx.getOne() > meTime);

                                n = x.size();
                                Conj y = new Conj(n);
                                for (int j = 0; j < n; j++) {
                                    if (!y.add(x.get(j).getOne(), x.get(j).getTwo().replace(s.xy)))
                                        break; //fail
                                }
                                return y.term();
                            }
                        }
                    }
                }

            }


//            Term match = earlyOrLate ? conj.eventFirst() : conj.eventLast(); //TODO look inside parallel conj event
//            if (strict && (match.equals(event) || (!eventVars && !match.hasAny(varBits) /* TODO temporal subterms? */)))
//                return null;
//
//            if (!Terms.possiblyUnifiable(match, event, strict, varBits))
//                return null;
//
//            SubUnify s = new SubUnify(rng);
//            s.ttl = ttl;
//            if (match.unify(event, s)) {
//                //TODO try diferent permutates tryMatch..
//                Term dropped = withoutEarlyOrLate(conj, match, earlyOrLate);
//                Term dropUnified = dropped.replace(s.xy);
//                if (!strict || !dropUnified.equals(dropped)) {
//                    return dropUnified;
//                }
//            }
        } else {
            //?? should apply to parallel?
        }

        return null; //TODO
    }


}
