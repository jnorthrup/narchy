package nars.term.util.conj;

import jcog.WTF;
import jcog.data.bit.MetalBitSet;
import jcog.util.ArrayUtils;
import nars.Op;
import nars.term.Term;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static nars.Op.CONJ;
import static nars.term.Terms.sorted;
import static nars.term.atom.Bool.*;
import static nars.time.Tense.*;

/** utilities for working with commutive conjunctions (DTERNAL, parallel, and XTERNAL) */
public enum ConjCommutive {;

    public static Term the(int dt, Term... u) {
        return theSorted(dt, sorted(u));
    }
    public static Term the(int dt, Collection<Term> u) {
        return theSorted(dt, sorted(u));
    }

    /** assumes u is sorted */
    public static Term theSorted(int dt, Term... u) {
        if (u.length == 0)
            return True;
        if (u.length == 1)
            return u[0];

        if (dt != DTERNAL && dt != 0)
            throw new WTF();

        if (u.length == 2) {
            //quick test
            Term a = u[0], b = u[1];
            //if (Term.commonStructure(a, b)) {
                if (a.equals(b))
                    return u[0];
                if (a.equalsNeg(b))
                    return False;
            //}



            if (a.unneg().op() != CONJ && b.unneg().op()!=CONJ) {
                //fast construct for simple case, verified above to not contradict itself
                return conjDirect(dt, /*sorted*/u);
            }
        }

        //TODO fast 3-ary case

        MetalBitSet pos; //simple positive events
        MetalBitSet neg; //negative events
        MetalBitSet conjMerge; //mergeable conj compounds
        MetalBitSet seq; //un-mergeable conj compounds
        MetalBitSet disj; //disjunctions

        Set<Term> flatten = null;
        do {
            pos = neg = conjMerge = seq = disj = null;

            for (int i = 0, uLength = u.length; i < uLength; i++) {
                Term x = u[i];

                switch (x.op()) {
                    case BOOL:
                        if (x == False) return False;
                        if (x == Null) return Null;
                        if (x == True) conjMerge = set(conjMerge, i, uLength);
                        break;
                    case CONJ:
                        int xdt = x.dt();
                        if (xdt == dt /*|| (dt == 0 && xdt ==DTERNAL)*/ /* promote inner DTERNAL to parallel */) {
                            conjMerge = set(conjMerge, i, uLength);
                        } else {
                            //TODO handle promotion of &&/&| as conjMerge rather than conjOther
                            seq = set(seq, i, uLength);
                        }
                        break;
                    case NEG:
                        if (x.unneg().op() == CONJ) {
                            disj = set(disj, i, uLength);
                        } else {
                            neg = set(neg, i, uLength);
                        }
                        break;
                    default:
                        pos = set(pos, i, uLength);
                        break;
                }
            }

            if (conjMerge != null) {
                if (flatten!=null)
                    flatten.clear();
                for (int i = 0, uLength = u.length; i < uLength; i++) {
                    if (conjMerge.get(i)) {
                        Term x = u[i];
                        if (x == True) continue;
                        if (flatten == null) flatten = new UnifiedSet(uLength*2);
                        x.subterms().forEach(flatten::add);
                    }
                }
                if (flatten!=null) {
                    for (int i = 0, uLength = u.length; i < uLength; i++) {
                        if (!conjMerge.get(i))
                            flatten.add(u[i]);
                    }
                    u = sorted(flatten);
                } else {
                    //just True's, remove the array elements
                    u = ArrayUtils.removeAll(u, conjMerge);
                    break;
                }
                if (u.length==1)
                    return u[0];

            }

        } while (conjMerge!=null);

        if (pos!=null && neg!=null) {
            if (coNegate(pos, neg, u))
                return False;
        }

        if (pos!=null && pos.cardinality() == u.length) {
            //all pos
            //assertNot2(u);
            return conjDirect(dt, u);
        }

        if (neg!=null && neg.cardinality() == u.length) {
            //assertNot2(u);
            return conjDirect(dt, u);
        }


        if (pos!=null && neg!=null && (pos.cardinality() + neg.cardinality()) == u.length) {

            //mix of pos and negative, check for co-negation

            return conjDirect(dt, u);
        }

        int seqCount = seq!=null ? seq.cardinality() : 0;
        if (seqCount==1) {
            if (disj==null) {

                //try simple cases
                int coi = seq.first(true);
                Term co = u[coi];

                if ((dt == DTERNAL) || (co.dt() == DTERNAL)) {
                    int indep = 0, elim = 0;
                    for (int i = 0; i < u.length; i++) {
                        if (i == coi) continue;
                        Term x = u[i];
                        assert (x.op() != CONJ);
                        if (!conflict(co, x)) {
                            if (absorbCompletelyByFirstLayer(co, x)) {
                                elim++;
                            } else if (!absorb(co, x)) {
                                indep++;
                            }

                        }
                    }

                    if (indep == u.length-1) {
//                        //promote dternal wrapped parallel disjunction to parallel
//                        if (dt == DTERNAL && co.dt()==0)
//                            return theSorted(0, u); //need to start over (recurse)

                        return conjDirect(dt, u); //all independent
                    }

                    if (elim == u.length-1)
                        return co; //all absorbed


                } else if (dt == 0) {
                    if (co.dt() == XTERNAL) {
                        //allow because there is no way to know what the correspondence of timing is
                        return conjDirect(dt, u);
                    }
                }
            }

        }
        if (u.length == 2) {
            //TODO exclude the case with disj and conjOther
            int dd;
            if (disj!=null && disj.cardinality()==1 && seqCount ==0)
                dd = disj.first(true);
            else if (dt == DTERNAL && seqCount ==1)
                dd = seq.first(true);
            else
                dd = -1;

            if (dd!=-1) {
                Term d = u[dd];
                Term x = u[1 - dd];
                Term cj = Conj.conjoin(d, x, dt == DTERNAL);
                if (cj == null)
                    throw new WTF();
                return cj;
            }

        }

        long sdt = dt == DTERNAL ? ETERNAL : 0;
        //try {

            //iterate in reverse order to add the smaller (by volume) items first

            if (seq!=null) {
                Conj c = new Conj(u.length);
                //add the non-conj terms at ETERNAL last.
                //then if the conjOther is a sequence, add it at zero
                for (int i = u.length-1; i >= 0; i--) {
                    if (seq.get(i)) {
                        if (!c.add(sdt, u[i]))
                            return c.term(); //fail
                    }
                }
                for (int i = u.length-1; i >= 0; i--) {
                    if (!seq.get(i))
                        if (!c.add(sdt, u[i]))
                            return c.term(); //fail
                }
                return c.term();

            } else {
                switch (u.length) {
                    case 0:
                        return True;
                    case 1:
                        return u[0];
                    case 2:
                        return Conj.conjoin(u[0], u[1], dt == DTERNAL);
                    default: {
                        Conj c = new Conj(u.length);
                        for (int i = u.length-1; i >= 0; i--) {
                            if (!c.add(sdt, u[i]))
                                break;
                        }
                        return c.term();
                    }
                }
            }
//        } catch (StackOverflowError e) {
//            throw new WTF("StackOverflow: && " + sdt + " " + Arrays.toString(u)); //TEMPORARY
//        }
    }

    private static boolean absorbCompletelyByFirstLayer(Term co, Term x) {
        return co.AND(cc -> absorb(cc, x));
    }

    /** tests hypothetically distributing the incoming term to all the events in conj */
    private static boolean conflict(Term x, Term incoming) {
        if (x.equals(incoming))
            return false;
        if (x.equalsNeg(incoming))
            return true;
        if (!Term.commonStructure(x, incoming))
            return false;

        if (x.op() == CONJ) {
            return x.OR(cc -> conflict(cc, incoming));
        }

        return false;
    }

    /** tests whether the term is absorbed */
    private static boolean absorb(Term x, Term incoming) {
        if (x.equals(incoming))
            return true;

//        if (!x.containsRecursively(incoming))
//            return false;

        if (x.op() == CONJ)
            return x.OR(cc -> absorb(cc, incoming));
        else
            return false;

    }

//    static void assertNot2(Term[] u) {
//        if (u.length == 2)
//            throw new WTF("why wasnt this simple case caught earlier");
//    }

    static Term conjDirect(int dt, Term[] u) {
        return Op.terms.theCompound(CONJ, dt, u);
    }

    static boolean coNegate(MetalBitSet pos, MetalBitSet neg, Term[] u) {
        int P = pos.cardinality();
        if (P == 1) {
            int pn = pos.first(true);
            Term p = u[pn];
            for (int i = 0; i < u.length; i++) {
                if (i!=pn && u[i].unneg().equals(p))
                    return true;
            }
            return false;
        } else {
            int N = neg.cardinality();
            if (N == 1) {
                int nn = neg.first(true);
                Term un = u[nn].unneg();
                for (int i = 0; i < u.length; i++) {
                    if (i!=nn && un.equals(u[i]))
                        return true;
                }
                return false;
            } else {
                Set<Term> s;
                boolean pn;
                int size;
                if (P <= N) {
                    pn = true;
                    size = P;
                } else {
                    pn = false;
                    size = N;
                }
                s = new HashSet(size);
                for (int i = 0; i < u.length; i++) {
                    if (pos.get(i)==pn) {
                        Term ui = u[i];
                        s.add(pn ? ui : ui.unneg());
                    }
                }
                for (int i = 0; i < u.length; i++) {
                    if (pos.get(i)!=pn) {
                        Term ui = u[i];
                        if (s.contains(pn ? ui.unneg() : ui))
                            return true;
                    }
                }
                return false;
            }
        }
    }

    static MetalBitSet set(MetalBitSet disj, int i, int uLength) {
        if (disj == null) disj = MetalBitSet.bits(uLength);
        disj.set(i);
        return disj;
    }


    public static boolean contains(Term container, Term x) {
        if (x.op()==CONJ && !Conj.isSeq(x)) {
            //test for containment of all x's components
            if (x.subterms().AND(container::contains))
                return true;
        }
        return container.contains(x);
    }
}
