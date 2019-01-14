package nars.term.util.conj;

import nars.derive.Derivation;
import nars.op.UniSubst;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.term.util.Image;

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

        long[] matchedTime = new long[] { Long.MAX_VALUE, Long.MIN_VALUE };

        if (event.op()!=CONJ) {
            //simple event
            if (!conj.impossibleSubTerm(event)) {
                x.removeIf((when, what) -> {
                    if (what.equals(event)) {
                        matchedTime[0] = Math.min(matchedTime[0], when);
                        matchedTime[1] = Math.max(matchedTime[1], when);
                        return true;
                    }
                    return false;
                });
            }
        } else {
            //remove matching parallel/sequence conjunctions
            //TODO
        }


        int n0 = n;
        n = x.size();
        if (n0!=n) {
            //something removed;
            //include only the other events occurring at the same time as matchExact but not those after it
            x.removeIf(beforeOrAfter ?
                (when, what) -> when > matchedTime[0]
                :
                (when, what) -> when < matchedTime[1]
            );

            return x.term();
        }


        //try to unify if variables present
        //TODO only unif
        boolean eVar = event.hasAny(varBits);
        if (eVar || (conj.hasAny(varBits) /*&& x.anySatisfy(1, n, z -> z.getTwo().hasAny(varBits)))*/)) {
            //TODO use SubUnify correctly (ie. termutes via tryMatch )
            UniSubst.MySubUnify s = d.uniSubst.u;
            nextUnifiable: for (int matchUnify = 0; matchUnify < n; matchUnify++) {
                Term xx = x.get(matchUnify);
                if (eVar || xx.hasAny(varBits)) {

                    s.reset(varBits, false);

                    Term yy = s.unifySubst(xx, event, xx, ttl, varBits, false);
                    if (yy!=null) {
                        if (yy == False)
                            continue; //fail


                        //s.xy.forEach(d.xy::set);


                        Term z;
                        if (n == 2) {
                            z = x.get(1-matchUnify).replace(s.xy); //TODO keep trying if term fails to transform
                        } else {

                            //include any other events occurring at the same time as matchExact but not those after it

                            boolean includeMatched = false; //TODO can be a parameter
                            long xTime = x.when(matchUnify);

                            ConjLazy y = new ConjLazy(x.size());
                            for (int j = 0; j < n; j++) {
                                if (matchUnify == j && !includeMatched) continue; //skip the matched event
                                long jw = x.when(j);
                                if (beforeOrAfter && jw > xTime) continue;
                                if (!beforeOrAfter && jw < xTime) continue;
                                Term jj = x.get(j).replace(s.xy);
                                if (jj == Null || jj == False) {
                                    continue nextUnifiable;
                                }
                                y.add(jw, jj);
                            }

                            if (includeMatched)
                                y.add(x.when(matchUnify), yy);

                            z = y.term();
                        }

                        if (z!=null && !(z instanceof Bool)) {
                            s.xy.forEach(d.retransform::put);
                            return z;
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
