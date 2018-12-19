package nars.term.util;

import jcog.TODO;
import jcog.WTF;
import jcog.data.bit.MetalBitSet;
import jcog.util.ArrayUtils;
import nars.term.Term;
import nars.term.util.builder.HeapTermBuilder;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static nars.Op.CONJ;
import static nars.term.Terms.sorted;
import static nars.term.atom.Bool.*;
import static nars.time.Tense.*;

public enum ConjCommutive {;

    public static Term the(int dt, Term[] u) {
        throw new TODO();
    }

    /** assumes u is sorted */
    public static Term theSorted(int dt, Term[] u) {
        if (u.length == 0)
            return True;
        if (u.length == 1)
            return u[0];

        if (dt != DTERNAL && dt != 0)
            throw new WTF();

        if (u.length == 2) {
            //quick test
            Term a = u[0], b = u[1];
            if (Term.commonStructure(a, b)) {
                if (a.equals(b))
                    return u[0];
                if (a.equalsNeg(b))
                    return False;
            }

            if (a.unneg().op() != CONJ && b.unneg().op()!=CONJ) {
                //fast construct for simple case, verified above to not contradict itself
                return HeapTermBuilder.the.theCompound(CONJ, dt, /*sorted*/a, b);
            }
        }

        //TODO fast 3-ary case

        MetalBitSet pos; //simple positive events
        MetalBitSet neg; //negative events
        MetalBitSet conjMerge; //mergeable conj compounds
        MetalBitSet conjOther; //un-mergeable conj compounds
        MetalBitSet disj; //disjunctions
        do {
            pos = neg = conjMerge = conjOther = disj = null;

            for (int i = 0, uLength = u.length; i < uLength; i++) {
                Term x = u[i];

                switch (x.op()) {
                    case BOOL:
                        if (x == False) return False;
                        if (x == Null) return Null;
                        if (x == True) conjMerge = set(conjMerge, i, uLength);
                        break;
                    case CONJ:
                        if (x.dt() == dt) {
                            conjMerge = set(conjMerge, i, uLength);
                        } else {
                            //TODO handle promotion of &&/&| as conjMerge rather than conjOther
                            conjOther = set(conjOther, i, uLength);
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
                Set<Term> flatten = null;
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

            }

        } while (conjMerge!=null);

        if (pos!=null && pos.cardinality() == u.length) {
            //all pos
            //assertNot2(u);
            return conjDirect(dt, u);
        } else if (neg!=null && neg.cardinality() == u.length) {
            //assertNot2(u);
            return conjDirect(dt, u);
        } else if (pos!=null && neg!=null && (pos.cardinality()+ neg.cardinality()) == u.length) {
            //assertNot2(u);
            //mix of pos and negative, check for co-negation
            if (!coNegate(pos, neg, u))
                return conjDirect(dt, u);
            else
                return False;
        } else {

            if ((conjOther!=null && conjOther.cardinality()==1)) {
                if (disj==null) {

                    //try simple cases
                    int coi = conjOther.first(true);
                    Term co = u[coi];

                    if ((dt == DTERNAL) || (dt == 0 && co.dt() == DTERNAL)) {
                        int indep = 0, elim = 0;
                        for (int i = 0; i < u.length; i++) {
                            if (i == coi) continue;
                            Term x = u[i];
                            assert (x.op() != CONJ);
                            if (!conflict(co, x)) {
                                if (!absorb(co, x)) {
                                    indep++;
                                } else if (absorbCompletelyByFirstLayer(co, x)) {
                                    elim++;
                                }
                            }
                        }

                        if (indep == u.length-1)
                            return conjDirect(dt, u); //all independent

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

            long sdt = dt == DTERNAL ? ETERNAL : 0;
            try {
                Conj c = new Conj(u.length);
                for (Term term : u) {
                    if (!c.add(sdt, term))
                        break;
                }
                return c.term();
            } catch (StackOverflowError e) {
                throw new WTF("StackOverflow: && " + sdt + " " + Arrays.toString(u)); //TEMPORARY
            }
        }
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

        if (!x.containsRecursively(incoming))
            return false;

        if (x.op() == CONJ)
            return x.OR(cc -> absorb(cc, incoming));
        else
            return false;

    }

    static void assertNot2(Term[] u) {
        if (u.length == 2)
            throw new WTF("why wasnt this simple case caught earlier");
    }

    static Term conjDirect(int dt, Term[] u) {
        return HeapTermBuilder.the.theCompound(CONJ, dt, u);
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
                    if (i!=nn && u[i].equals(un))
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


}
