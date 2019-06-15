package nars.term.util.conj;

import jcog.data.bit.MetalBitSet;
import nars.Op;
import nars.term.Neg;
import nars.term.Term;
import nars.term.Terms;
import nars.term.atom.Bool;
import nars.term.compound.Sequence;
import nars.term.util.builder.TermBuilder;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectByteHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

import static nars.Op.CONJ;
import static nars.term.atom.Bool.*;
import static nars.time.Tense.*;

/**
 * utilities for working with commutive conjunctions (DTERNAL, parallel, and XTERNAL)
 */
public enum ConjPar {
    ;

    public static Term the(TermBuilder B, int dt, boolean sort, Term... xx) {
        if (xx.length == 1)
            return xx[0];
        else if (xx.length == 2) {
            //fast 2-ary non-conj case
            Term a = xx[0], b = xx[1];
            if (a == Null || b == Null) return Null;
            if (!a.hasAny(CONJ.bit) && !b.hasAny(CONJ.bit)) {
                if (a.equals(b)) return a;
                if (a.equalsNeg(b)) return False;
                return B.newCompound(CONJ, DTERNAL, sort ? Terms.commute(xx) : xx);
            }
        }

        Term d = disjunctiveFactor(xx, dt, B);
        if (d!=null)
            return d;

        ConjTree ct = new ConjTree();
        long sdt = dt==DTERNAL ? ETERNAL : 0;
        int remain = xx.length;
        for (int i = xx.length - 1; i >= 0; i--) {
            Term x = xx[i];
            if (x.unneg().op() != CONJ) {
                remain--;
                if (!ct.add(sdt, x))
                    break;
            }
        }
        if (remain > 0 && ct.terminal==null) {
            for (int i = xx.length - 1; i >= 0; i--) {
                Term x = xx[i];
                if (x.unneg().op() == CONJ) {
                    if (!ct.add(sdt, x))
                        break;
                }
            }
        }

        return ct.term(B);
    }

    @Nullable
    public static Term disjunctiveFactor(Term[] xx, int dt, TermBuilder B) {
        @Deprecated MetalBitSet cond = null;
        int n = xx.length;
        for (int i = 0, xxLength = n; i < xxLength; i++) {
            Term x = xx[i];
            if (x instanceof Neg) {
                Term xu = x.unneg();
                if (!(xu instanceof Sequence) && xu.op() == CONJ && xu.dt()==DTERNAL) {
                    if (cond == null) cond = MetalBitSet.bits(n);
                    cond.set(i);
                }
            }
        }
        if (cond != null) {
            int d = cond.cardinality();
            if (d == n) {
            //if (d > 1) {
                ObjectByteHashMap<Term> i = new ObjectByteHashMap(d);
                int j = -1;
                boolean anyFull = false;
                for (int k = 0; k < d; k++) {
                    j = cond.next(true, j+1, n);
                    Term dc = xx[j];
                    for (Term ct : dc.unneg().subterms()) {
                        Term ctn = ct.neg();
                        if (i.containsKey(ctn)) {
                            //disqualify both permanently since factoring them would cancel each other out
                            i.put(ct, Byte.MIN_VALUE);
                            i.put(ctn, Byte.MIN_VALUE);
                        } else {
                            byte z = i.updateValue(ct, (byte) 0, (v) -> (v >= 0) ? (byte) (v + 1) : v);
                            anyFull |= (z == d);
                        }
                    }

                }
                if (anyFull) {
                    i.values().removeIf(b -> b < d);
                    if (!i.isEmpty()) {
                        Set<Term> common = i.keySet();
                        Term factor = B.conj(common.toArray(Op.EmptyTermArray));

                        if (factor instanceof Bool)
                            return factor;

                        xx = xx.clone(); //dont modify input array
                        j = -1;
                        for (int k = 0; k < d; k++) {
                            j = cond.next(true, j + 1, n);
                            Term[] xxj = xx[j].unneg().subterms().subsIncluding(s -> !common.contains(s));
                            if (xxj.length == 0)
                                return null; //eliminated TODO detect sooner
                            xx[j] = (xxj.length == 1 ? xxj[0] :
                                            B.conj(xxj)
                                                ).neg();
                        }
                        return B.conj(dt, factor, B.conj(xx).neg()).neg();
                    }
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
