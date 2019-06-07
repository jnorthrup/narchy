package nars.term.util.conj;

import jcog.data.bit.MetalBitSet;
import nars.term.Neg;
import nars.term.Term;
import nars.term.Terms;
import nars.term.atom.Bool;
import nars.term.util.builder.TermBuilder;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectByteHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

import static nars.Op.CONJ;
import static nars.term.atom.Bool.True;
import static nars.time.Tense.*;

/**
 * utilities for working with commutive conjunctions (DTERNAL, parallel, and XTERNAL)
 */
public enum ConjCommutive {
    ;

    public static Term the(TermBuilder B, int dt, boolean sort, Term... xx) {
        if (xx.length == 1)
            return xx[0];

        Term d = disjunctiveFactor(xx, B);
        if (d!=null)
            return d;

        ConjTree ct = new ConjTree();
        long sdt = (dt == DTERNAL) ? ETERNAL : 0;
        for (int i = xx.length - 1; i >= 0; i--) {
            Term x = xx[i];
            if (x.unneg().op() != CONJ)
                if (!ct.add(sdt, x))
                    break;
        }
        if (ct.terminal==null) {
            for (int i = xx.length - 1; i >= 0; i--) {
                Term x = xx[i];
                if (x.unneg().op() == CONJ)
                    if (!ct.add(sdt, x))
                        break;
            }
        }

//        for (Term x : xx) {
//            if (!ct.add(sdt, x))
//                break;
//        }
        return ct.term(B);
    }

    @Nullable
    public static Term disjunctiveFactor(Term[] xx, TermBuilder B) {
        MetalBitSet cond = null;
        int n = xx.length;
        for (int i = 0, xxLength = n; i < xxLength; i++) {
            Term x = xx[i];
            if (x instanceof Neg) {
                Term xu = x.unneg();
                if (xu.op() == CONJ && xu.dt()==DTERNAL) {
                    if (cond == null) cond = MetalBitSet.bits(n);
                    cond.set(i);
                }
            }
        }
        if (cond != null) {
            int d = cond.cardinality();
            if (d > 1) {
                ObjectByteHashMap<Term> i = new ObjectByteHashMap(d);
                int j = -1;
                for (int k = 0; k < d; k++) {
                    j = cond.next(true, j+1, n);
                    Term dc = xx[j];
                    for (Term ct : dc.unneg().subterms())
                        i.addToValue(ct, (byte)1);

                }
                i.values().removeIf(b -> b < d);
                if (!i.isEmpty()) {
                    Set<Term> common = i.keySet();
                    Term factor = CONJ.the(B, DTERNAL, common);

                    if (factor instanceof Bool)
                        return factor;

                    xx = xx.clone(); //dont modify input array
                    j = -1;
                    for (int k = 0; k < d; k++) {
                        j = cond.next(true, j+1, n);
                        Term dc = xx[j];
                        xx[j] = B.conj(true, DTERNAL, dc.unneg().subterms().subsIncluding(s -> !common.contains(s))).neg();
                    }
                    return B.conj(DTERNAL, factor, B.conj(DTERNAL, xx).neg()).neg();
                }
            }
        }
        return null;
    }


    public static Term theXternal(TermBuilder b, Term[] u) {
        int ul = u.length;
        Term[] args;
        switch (ul) {
            case 0:
                return True;

            case 1:
                return u[0];

            default: {
                Term[] uux = Terms.commute(u);
                if (uux.length == 1) {
                    args = new Term[]{uux[0], uux[0]}; //repeat
                } else {
                    args = uux;
                }
            }

        }

        return b.newCompound(CONJ, XTERNAL, args);

    }
}
