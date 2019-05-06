package nars.term.util.conj;

import jcog.WTF;
import jcog.data.bit.MetalBitSet;
import jcog.util.ArrayUtil;
import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.term.util.builder.TermBuilder;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.*;

import static nars.Op.CONJ;
import static nars.term.Terms.sorted;
import static nars.term.atom.Bool.*;
import static nars.time.Tense.*;

/**
 * utilities for working with commutive conjunctions (DTERNAL, parallel, and XTERNAL)
 */
public enum ConjCommutive {;

    public static Term the(int dt, Term... u) {
        return the(Op.terms, dt, u);
    }

    public static Term the(TermBuilder B, int dt, Term... u) {
        return the(Op.terms, dt, false, false, u);
    }

    public static Term theSorted(TermBuilder B, int dt, Term... u) {
        return the(Op.terms, dt, true, false, u);
    }

    public static Term the(TermBuilder B, int dt, boolean sort, boolean direct, Term... u) {
        if (sort)
            u = sorted(u);

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


            if (a.unneg().op() != CONJ && b.unneg().op() != CONJ) {
                //fast construct for simple case, verified above to not contradict itself
                return conjDirect(B, dt, /*sorted*/u);
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
                        if (xdt == dt || (dt == 0 && xdt == DTERNAL /* promote inner DTERNAL to parallel */)) {
                            conjMerge = set(conjMerge, i, uLength);
                        } else {
                            //TODO handle promotion of &&/&| as conjMerge rather than conjOther
                            seq = set(seq, i, uLength);
                        }
                        break;
                    case NEG:
                        Term xu = x.unneg();
                        if (xu.op() == CONJ) {
                            if (dt == 0 && xu.dt() == DTERNAL /* promote inner DTERNAL to parallel */) {
                                u[i] = ((Compound)xu).dt(0, B).neg();
                            }
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
                if (flatten != null)
                    flatten.clear();
                for (int i = 0, uLength = u.length; i < uLength; i++) {
                    if (conjMerge.get(i)) {
                        Term x = u[i];
                        if (x == True) continue;
                        if (flatten == null) flatten = new UnifiedSet(uLength * 2);
                        x.subterms().forEach(flatten::add);
                    }
                }
                if (flatten != null) {
                    for (int i = 0, uLength = u.length; i < uLength; i++) {
                        if (!conjMerge.get(i))
                            flatten.add(u[i]);
                    }
                    u = sorted(flatten);
                } else {
                    //just True's, remove the array elements
                    u = ArrayUtil.removeAll(u, conjMerge);
                    break;
                }
                if (u.length == 1)
                    return u[0];

            }

        } while (conjMerge != null);

        if (pos != null && neg != null) {
            if (coNegate(pos, neg, u))
                return False;
        }

        int pc = pos==null ? 0 : pos.cardinality(), nc = neg == null ? 0 : neg.cardinality();
        if (pc + nc == u.length) {
            //mix of pos and negative (no seq) - only needed to have checked for co-negation
            return conjDirect(B, dt, u);
        }

        int seqCount = seq != null ? seq.cardinality() : 0;
        if (seqCount == 1) {
            if (disj == null) {

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

                    if (indep == u.length - 1)
                        return conjDirect(B, dt, u); //all independent

                    if (elim == u.length - 1)
                        return co; //all absorbed


                } /*else if (dt == 0 || dt == DTERNAL) {
                    if (co.dt() == XTERNAL) {
                        //allow because there is no way to know what the correspondence of timing is
                        return conjDirect(dt, u);
                    }
                }*/
            }

        }
//        if (u.length == 2) {
//            //TODO exclude the case with disj and conjOther
//            int dd;
//            if (disj != null && disj.cardinality() == 1 && seqCount == 0)
//                dd = disj.first(true);
//            else if (dt == DTERNAL && seqCount == 1)
//                dd = seq.first(true);
//            else
//                dd = -1;
//
//            if (dd != -1) {
//                Term d = u[dd];
//                Term x = u[1 - dd];
//                Term cj = Conj.conjoin(B, d, x, dt == DTERNAL);
//                if (cj == null)
//                    throw new WTF();
//                return cj;
//            }
//
//        }

        if (direct)
            return conjDirect(B, dt, u); //done
        else {

            long sdt = dt == DTERNAL ? ETERNAL : 0;

            //iterate in reverse order to add the smaller (by volume) items first

            if (seq != null) {
                ConjBuilder c = new Conj(u.length);
                //add the non-conj terms at ETERNAL last.
                //then if the conjOther is a sequence, add it at zero
                for (boolean bs : new boolean[] { true, false }) {
                    for (int i = u.length - 1; i >= 0; i--) {
                        if (bs == seq.get(i))
                            if (!c.add(sdt, u[i]))
                                return c.term(); //fail
                    }
                }
                return c.term();

            } else {
                switch (u.length) {
                    case 0:
                        return True;
                    case 1:
                        return u[0];
                    case 2:
                        return Conj.conjoin(B, u[0], u[1], dt == DTERNAL);
                    default: {
                        ConjBuilder c = new Conj(u.length);
                        for (int i = u.length - 1; i >= 0; i--) {
                            if (!c.add(sdt, u[i]))
                                break;
                        }
                        return c.term(B);
                    }
                }
            }

        }
    }

    private static boolean absorbCompletelyByFirstLayer(Term co, Term x) {
        return co.subterms().ANDwith(ConjCommutive::absorb, x);
    }

    /**
     * tests hypothetically distributing the incoming target to all the events in conj
     */
    private static boolean conflict(Term x, Term incoming) {
        if (x.equals(incoming))
            return false;
        if (x.equalsNeg(incoming))
            return true;
        if (!Term.commonStructure(x, incoming))
            return false;

        return x.op() == CONJ && x.subterms().ORwith(ConjCommutive::conflict, incoming);

    }

    /**
     * tests whether the target is absorbed
     */
    private static boolean absorb(Term x, Term incoming) {
        if (x.equals(incoming))
            return true;

//        if (!x.containsRecursively(incoming))
//            return false;

        return x.op() == CONJ && x.subterms().ORwith(ConjCommutive::absorb, incoming);

    }

//    static void assertNot2(Term[] u) {
//        if (u.length == 2)
//            throw new WTF("why wasnt this simple case caught earlier");
//    }

    private static Term conjDirect(TermBuilder b, int dt, Term[] u) {
        if (dt == 0)
            dt = DTERNAL;

        return b.theCompound(CONJ, dt, u);
    }

    private static boolean coNegate(MetalBitSet pos, MetalBitSet neg, Term[] u) {
        int P = pos.cardinality();
        if (P == 1) {
            int pn = pos.first(true);
            Term p = u[pn];
            for (int i = 0; i < u.length; i++) {
                if (i != pn && u[i].unneg().equals(p))
                    return true;
            }
            return false;
        } else {
            int N = neg.cardinality();
            if (N == 1) {
                int nn = neg.first(true);
                Term un = u[nn].unneg();
                for (int i = 0; i < u.length; i++) {
                    if (i != nn && un.equals(u[i]))
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
                    if (pos.get(i) == pn) {
                        Term ui = u[i];
                        s.add(pn ? ui : ui.unneg());
                    }
                }
                for (int i = 0; i < u.length; i++) {
                    if (pos.get(i) != pn) {
                        Term ui = u[i];
                        if (s.contains(pn ? ui.unneg() : ui))
                            return true;
                    }
                }
                return false;
            }
        }
    }

    private static MetalBitSet set(MetalBitSet disj, int i, int uLength) {
        if (disj == null) disj = MetalBitSet.bits(uLength);
        disj.set(i);
        return disj;
    }


    public static boolean contains(Term container, Term x) {
        if (x.op() == CONJ && !Conj.isSeq(x)) {
            //test for containment of all x's components
            if (x.subterms().AND(container::contains))
                return true;
        }
        return container.contains(x);
    }

    public static Term theXternal(TermBuilder b, Term[] u) {
        int ul = u.length;
        Term[] args;
        switch (ul) {
            case 0:
                return Bool.True;

            case 1:
                return u[0];

            case 2:
                //special case: simple arity=2
                if (!u[0].equals(u[1])) // && !unfoldableInneralXternalConj(u[0]) && !unfoldableInneralXternalConj(u[1])) {
                    args = sorted(u);
                else
                    args = new Term[]{u[0], u[0]}; //repeat
                break;

            default: {

                TreeSet<Term> uux = new TreeSet();
                Collections.addAll(uux, u);

                if (uux.size() == 1) {
                    Term only = uux.first();
                    args = new Term[]{only, only}; //repeat
                } else {
                    args = sorted(uux);
                }

            }

        }

        return b.theCompound(CONJ, XTERNAL, args);

    }
}
