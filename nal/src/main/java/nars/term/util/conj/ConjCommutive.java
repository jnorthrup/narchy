package nars.term.util.conj;

import jcog.TODO;
import jcog.WTF;
import jcog.data.bit.MetalBitSet;
import jcog.util.ArrayUtil;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.term.Terms;
import nars.term.atom.Bool;
import nars.term.util.TermException;
import nars.term.util.builder.TermBuilder;

import java.util.*;

import static nars.Op.BOOL;
import static nars.Op.CONJ;
import static nars.term.Terms.commute;
import static nars.term.atom.Bool.*;
import static nars.time.Tense.*;

/**
 * utilities for working with commutive conjunctions (DTERNAL, parallel, and XTERNAL)
 */
public enum ConjCommutive {
    ;

    public static Term the(int dt, Term... u) {
        return the(Op.terms, dt, u);
    }

    public static Term the(TermBuilder B, int dt, Term... u) {
        return the(Op.terms, dt, true, false, u);
    }

    public static Term theSorted(TermBuilder B, int dt, Term... u) {
        return the(Op.terms, dt, false, false, u);
    }

    public static Term the(TermBuilder B, int dt, boolean sort, boolean direct, Term... u) {

        if (dt != DTERNAL && dt != 0)
            throw new WTF();

        //bool pre-filter
        MetalBitSet trueRemoved = null;
        for (int i = 0, uLength = u.length; i < uLength; i++) {
            Term x = u[i];
            if (x.op() == BOOL) {
                if (x == False)
                    return False;
                if (x == Null)
                    return Null;
                if (x == True) {
                    throw new TODO();
                }
            }
        }
        if (trueRemoved != null)
            u = ArrayUtil.removeAll(u, trueRemoved);

        if (sort)
            u = Terms.commute(u);

        if (u.length == 0)
            return True;
        else if (u.length == 1)
            return u[0];
        else if (u.length == 2) {
            //quick test
            Term a = u[0], b = u[1];
            if (a.equals(b))
                return u[0];
            else if (a.equalsNeg(b))
                return False;
            else if (a.unneg().op() != CONJ && b.unneg().op() != CONJ) {
                //fast construct for simple case, verified above to not contradict itself
                return conjDirect(B, dt, /*sorted*/u);
            }
        }

        //TODO fast 3-ary case

        MetalBitSet pos; //simple positive events
        MetalBitSet neg; //negative events
        MetalBitSet par; //mergeable conj parallel compounds
        MetalBitSet seq; //un-mergeable conj seq compounds
        MetalBitSet disj; //disjunctions


        SortedSet<Term> flatten = null;
        do {
            pos = neg = par = seq = disj = null;

            for (int i = 0, uLength = u.length; i < uLength; i++) {
                Term x = u[i];

                switch (x.op()) {
                    case CONJ:
                        int xdt = x.dt();
                        if //(xdt == dt || (dt == 0 && xdt == DTERNAL /* promote inner DTERNAL to parallel */)
                        (
                                (xdt == 0 || xdt == DTERNAL)

//                                (dt == DTERNAL && (xdt == 0 || xdt == DTERNAL))
//                                ||
//                                (dt == 0 && (xdt == DTERNAL))
                        ) {
                            par = set(par, i, uLength);
                        } else {
                            seq = set(seq, i, uLength);
                        }
                        break;
                    case NEG:
                        Term xu = x.unneg();
                        if (xu.op() == CONJ) {
//                            if (dt == 0 && xu.dt() == DTERNAL /* promote inner DTERNAL to parallel */) {
//                                u[i] = ((Compound)xu).dt(0, B).neg();
//                            }
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

            if (par != null) {
                if (flatten != null)
                    flatten.clear();
                for (int i = 0, uLength = u.length; i < uLength; i++) {
                    if (par.get(i)) {
                        Term x = u[i];
                        if (x == True) continue;
                        if (flatten == null) flatten = new TreeSet();
                        if (x.dt() != XTERNAL)
                            x.subterms().addAllTo(flatten);
                        else
                            flatten.add(x);
                    }
                }
                if (flatten != null) {
                    for (int i = 0, uLength = u.length; i < uLength; i++) {
                        if (!par.get(i))
                            flatten.add(u[i]);
                    }
                    u = commute(flatten);
                } else {
                    //just True's, remove the array elements
                    u = ArrayUtil.removeAll(u, par);
                    break;
                }
                if (u.length == 1)
                    return u[0];

            }

        } while (par != null);

        int pc = pos == null ? 0 : pos.cardinality(), nc = neg == null ? 0 : neg.cardinality();
        if (pc > 0 && nc > 0) {
            if (coNegate(pos, neg, u))
                return False;
        }

        if (pc + nc == u.length) {
            //simple mix of pos and/or negative (no seq, no disj) - only needed to have checked for co-negation
            return conjDirect(B, dt, u);
        }


        if (dt == DTERNAL) {
            //quick tests for seq and disj contradictions

            int seqCount = seq != null ? seq.cardinality() : 0;
            int coi = -1;
            while (seqCount > 0) {

                seqCount--;

                coi = seq.next(true, coi + 1, Integer.MAX_VALUE);
                Term co = u[coi];
                if (co.dt() == XTERNAL)
                    continue;

                int cos = co.subterms().structure();
                for (int i = u.length - 1; i >= 0; i--) {
                    if (i == coi) continue;
                    Term x = u[i]; //assert (x.op() != CONJ);
                    if (!Term.commonStructure(cos, x.unneg().structure()))
                        continue;


                    if (Conj.eventOf(co, x.neg()))
                        return False;
                    else if (direct && Conj.eventOf(co, x))
                        direct = false;

                }
            }


            //test if direct mode has an opportunity to reduce disjunctions, requiring un-direct
            if (direct) {
                int disjCount = disj != null ? disj.cardinality() : 0;

                coi = -1;
                while (disjCount > 0) {
                    disjCount--;

                    coi = disj.next(true, coi + 1, Integer.MAX_VALUE);

                    Term co = u[coi];
                    Term dun = co.unneg();
                    if (dun.dt() == XTERNAL) continue;

                    //boolean dseq = Conj.isSeq(dun);


                    Subterms dus = dun.subterms();
                    int dos = dus.structure();
                    for (int i = u.length - 1; i >= 0; i--) {
                        if (i == coi) continue;
                        Term x = u[i]; //assert (x.op() != CONJ);
                        if (!Term.commonStructure(dos, x.unneg().structure()))
                            continue;

                        if (Conj.eventOf(dun, x))
                            direct = false;

                        if (Conj.eventOf(dun, x.neg()))
                            direct = false;

                    }

                }
            }
        }


        switch (u.length) {
            case 0:
                return True;
            case 1:
                return u[0];
//            case 2:
//                if (seq == null && disj == null)
//                    return Conj.conjoin(B, u[0], u[1], dt == DTERNAL);
//                else
//                    break;
        }

        if (direct || (seq == null && disj == null))
            return conjDirect(B, dt, u); //done

        //TODO insertion ordering heuristic, combine with ConjLazy's construction
        long sdt = (dt == DTERNAL) ? ETERNAL : 0;

        ConjBuilder c = new Conj(u.length);

        for (int i = 0; i < u.length; i++) {
            boolean special = ((seq != null && seq.get(i)) || (disj != null && disj.get(i)));
            if (!special) {
                if (!c.add(sdt, u[i]))
                    return c.term();
            }
        }
        if (disj!=null) {
            for (int i = 0; i < u.length; i++) {
                if (disj.get(i))
                    if (!c.add(sdt, u[i]))
                        return c.term();
            }
        }
        if (seq!=null) {
            for (int i = 0; i < u.length; i++) {
                if (seq.get(i))
                    if (!c.add(sdt, u[i]))
                        return c.term();
            }
        }

        try {
            return c.term(B);
        } catch (StackOverflowError e) {
            //TEMPORARY for debug
            //System.err.println("conjcommutive stack overflow: " + Arrays.toString(u));
            //return Null;

            throw new TermException("conj commutive stack overflow", CONJ, dt, u);
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
//        if (!Term.commonStructure(x, incoming))
//            return false;

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

    public static Term conjDirect(TermBuilder b, int dt, Term[] u) {
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
                    args = Terms.commute(u);
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
                    args = commute(uux);
                }

            }

        }

        return b.theCompound(CONJ, XTERNAL, args);

    }
}
