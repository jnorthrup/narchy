package nars.term.util.conj;

import jcog.Util;
import jcog.data.list.FasterList;
import nars.derive.Derivation;
import nars.op.UniSubst;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.term.util.Image;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.map.mutable.primitive.LongObjectHashMap;

import java.util.List;

import static nars.Op.CONJ;
import static nars.Op.VAR_DEP;
import static nars.term.atom.Bool.*;
import static nars.time.Tense.*;

public class ConjMatch {

    /**
     * returns the prefix or suffix sequence of a specific matched subevent
     */
    public static Term beforeOrAfter(Term conj, Term event, boolean beforeOrAfter, Derivation d, int ttl /*, unifyOrEquals, includeMatchedEvent */) {


        if (conj.op() != CONJ || conj.dt()==XTERNAL)
            return Null;

        event = Image.imageNormalize(event);

        int varBits =
                //VAR_DEP.bit | VAR_INDEP.bit;
                VAR_DEP.bit;

        if (event.volume() >= conj.volume() || !Term.commonStructure( (event.structure()&(~varBits)),(conj.structure()&(~varBits))))
            return Null;

        return beforeOrAfterSeq(conj, event, beforeOrAfter, varBits, d, ttl);
    }

    private static Term beforeOrAfterSeq(Term conj, Term event, boolean beforeOrAfter, int varBits, Derivation d, int ttl) {

        //sequence or commutive

        ConjLazy x = events(conj, beforeOrAfter);

        int n = x.size();
        assert (n > 1);

        long[] matchedTime = new long[] { Long.MAX_VALUE, Long.MIN_VALUE };

        if (event.op()!=CONJ && event.dt()!=XTERNAL) {
            //simple event
            if (!conj.impossibleSubTerm(event)) {
                remove(event, x, matchedTime);
            }
        } else {
            //remove matching parallel/sequence conjunctions
            ConjLazy y = events(event, beforeOrAfter);
            assert(y.size() > 1);
            long[] yRange = Util.minmax(y::when, 0, y.size());
            if (yRange[0] == yRange[1]) {
                //PARALLEL EVENT
                //find what time in 'x' contains the most events from y, if any
                LongObjectHashMap<List<Term>> found = new LongObjectHashMap<>();
                ImmutableSet<Term> yTerms = Sets.immutable.withAll(y); //the terms
                for (int i = 0; i < n; i++) {
                    Term xi = x.get(i);
                    if (yTerms.contains(xi)) {
                        found.getIfAbsentPut(x.when(i), ()->new FasterList(1)).add(xi);
                    }
                }
                int nFound = found.size();
                LongObjectPair<List<Term>> b;
                switch (nFound) {
                    case 0: return Null;
                    case 1:
                        b = found.keyValuesView().getOnly();
                        break;
                    default:
                        int mostMatched = found.maxBy(List::size).size();
                        RichIterable<LongObjectPair<List<Term>>> best = found.keyValuesView().select(xx -> xx.getTwo().size() == mostMatched);
                        if (best.size() > 1) {
                            MutableList<LongObjectPair<List<Term>>> bb = best.toList();
                            b = bb.get(d.random.nextInt(mostMatched));
                        } else {
                            b = best.getOnly();
                        }
                        break;

                }

                long bWhen = b.getOne();
                matchedTime[0] = matchedTime[1] = bWhen;
                boolean rem = x.removeIf((when,what)-> (when == bWhen && yTerms.contains(what)));
                assert(rem);

            } else {
                //sequence
                //TODO
                if (event.subs()==2 && !event.subterms().hasAny(CONJ)) {
                    Term a = event.eventFirst();
                    assert(a!=event);
                    Term b = null;
                    int bDT = XTERNAL;

                    //HACK simple 2-ary exact sequence match
                    boolean found = false;
                    findPair: for (int i = 0; i < n; i++) {
                        if (x.get(i).equals(a)) {
                            if (b == null) { b = event.eventLast(); bDT = event.subTimeFirst(b) - event.subTimeLast(a); }
                            long aWhen = x.when(i);
                            long bWhen = aWhen + bDT;
                            for (int j = 0; j < n; j++) {
                                if (i == j) continue;

                                //exact time match, but here a dur tolerance could be allowed
                                if (x.when(j) == bWhen && x.get(j).equals(b)) {
                                    x.removeThe(i); x.removeThe(j);
                                    found = true;
                                    matchedTime[0] = Math.min(aWhen, bWhen);
                                    matchedTime[1] = Math.max(aWhen, bWhen);
                                    break findPair;
                                }
                            }
                        }
                    }
                    if (!found)
                        return Null;
                } else {
                    return Null;
                }
            }
        }


        int n0 = n;
        n = x.size();
        if (n0!=n && matchedTime[0]!=TIMELESS) {
            //something removed;
            //include only the other events occurring at the same time as matchExact but not those after it
            if (x.isEmpty())
                return True;

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
                    s.setTTL(ttl);

                    Term yy = s.unifySubst(xx, event, xx, varBits, false);
                    if (yy!=null) {
                        if (yy == False)
                            continue; //fail


                        //s.xy.forEach(d.xy::setAt);


                        Term z;
                        if (n == 2) {
                            z = x.get(1-matchUnify).replace(s.xy); //TODO keep trying if target fails to transform
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

    private static ConjLazy events(Term conj, boolean beforeOrAfter) {
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
        return x;
    }

    private static void remove(Term event, ConjLazy x, long[] matchedTime) {
        x.removeIf((when, what) -> {
            if (what.equals(event)) {
                matchedTime[0] = Math.min(matchedTime[0], when);
                matchedTime[1] = Math.max(matchedTime[1], when);
                return true;
            }
            return false;
        });
    }


//        {
//            Map<Term, Termed> n = new HashMap<>(Builtin.statik.length);
//            for (Termed s : Builtin.statik) {
//                if (s instanceof Functor.InlineFunctor)
//                    n.put(s.target(), s);
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
