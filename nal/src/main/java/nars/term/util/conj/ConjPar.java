package nars.term.util.conj;

import jcog.Util;
import jcog.data.bit.MetalBitSet;
import jcog.data.list.FasterList;
import jcog.util.ArrayUtil;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Neg;
import nars.term.Term;
import nars.term.Terms;
import nars.term.atom.Bool;
import nars.term.compound.Sequence;
import nars.term.util.builder.TermBuilder;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.primitive.ObjectIntPair;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectByteHashMap;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectIntHashMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.IntStream;

import static nars.Op.CONJ;
import static nars.Op.INH;
import static nars.term.atom.Bool.*;
import static nars.time.Tense.*;

/**
 * utilities for working with commutive conjunctions (DTERNAL, parallel, and XTERNAL)
 */
public enum ConjPar {
    ;

    public static Term the(TermBuilder B, int dt, boolean sort, Term... t) {
        if (t.length == 1)
            return t[0];
        if (t.length == 2) {
            //fast 2-ary tests
            Term a = t[0], b = t[1];
            if (a == Null || b == Null) return Null;
            if (a == False) return False;
            if (b == False) return False;
            if (a == True) return b;
            if (b == True) return a;
            if (a.equals(b)) return a;
            if (a.equalsNeg(b)) return False;
        }

        Term xt = xternalDistribute(dt, t, B);
        if (xt!=null)
            return xt;

        Term h = inhBundle(t, B);
        if (h!=null)
            return h;

        Term d = disjunctiveFactor(t, dt, B);
        if (d!=null)
            return d;


        if (t.length == 2) {
            //fast 2-ary non-conj case
            Term a = t[0], b = t[1];
            if (a.dt()!=XTERNAL && b.dt()!=XTERNAL && !a.hasAny(CONJ.bit) && !b.hasAny(CONJ.bit)) {
                return B.newCompound(CONJ, DTERNAL, sort ? Terms.commute(t) : t);
            }
        }



        ConjTree ct = new ConjTree();
        long sdt = dt==DTERNAL ? ETERNAL : 0;
        int remain = t.length;
        for (int i = t.length - 1; i >= 0; i--) {
            Term x = t[i];
            if (x.unneg().op() != CONJ) {
                remain--;
                if (!ct.add(sdt, x))
                    break;
            }
        }
        if (remain > 0 && ct.terminal==null) {
            for (int i = t.length - 1; i >= 0; i--) {
                Term x = t[i];
                if (x.unneg().op() == CONJ) {
                    if (!ct.add(sdt, x))
                        break;
                }
            }
        }

        return ct.term(B);
    }

    @Nullable private static Term xternalDistribute(int dt, Term[] xx, TermBuilder B) {
        int xternalCount = 0;
        int lastXternal = -1;
        for (int i = 0, xxLength = xx.length; i < xxLength; i++) {
            Term t = xx[i];
            Op to = t.op();
            if (to == CONJ && t.dt() == XTERNAL) {
                lastXternal = i;
                xternalCount++;
            }
        }

        //distribute to XTERNAL
        {

            if (xternalCount == 1) {
                //distribute to xternal components
                Term x = xx[lastXternal];
                Term[] y = ArrayUtil.remove(xx, lastXternal);
                Term Y = the(B, dt, true, y);
                if (Y == True) return x;
                int xs = x.subs();
                return B.conj(XTERNAL, Util.map(xxx -> B.conj(dt, xxx, Y), new Term[xs],
                        x.subterms().arrayShared()));
            }
        }

        return null;
    }

    @Nullable private static Term inhBundle(Term[] xx, TermBuilder B) {
        //NAL3 bundle
        MetalBitSet ii = MetalBitSet.bits(xx.length);
        for (int i = 0, xxLength = xx.length; i < xxLength; i++) {
            Term t = xx[i];
            Op to = t.op();
            if (t.unneg().op() == INH) {
                ii.set(i);
            }
        }
        int inhCount = ii.cardinality();
        if (inhCount <= 1)
            return null;

        //simple case
        if (inhCount == xx.length && inhCount == 2) {
            Term aa = xx[0];
            Term bb = xx[1];
            Term a = aa.unneg();
            Term b = bb.unneg();
            Term pred = a.sub(1);
            if (pred.equals(b.sub(1))) {
                //common pred: union
                Term i = INH.the(B, Op.DISJ(B, a.sub(0).negIf(aa instanceof Neg), b.sub(0).negIf(bb instanceof Neg)), pred);
                //TODO test for invalid
                return i;
            }
            Term subj = a.sub(0);
            if (subj.equals(b.sub(0))) {
                //common subj: intersection
                return INH.the(B, subj, CONJ.the(B, a.sub(1).negIf(aa instanceof Neg), b.sub(1).negIf(bb instanceof Neg)));
            }
        }

        Term other = inhCount < xx.length ? CONJ.the(B, IntStream.range(0, xx.length).filter(r -> !ii.get(r)).mapToObj(r -> xx[r]).toArray(Term[]::new)) : null;
        if (other == False || other == Null)
            return other;
        List<Term> all = new FasterList(4);
        if (other!=null)
            all.add(other);

        //counts are stored as follows: subj are stored as normal, pred are stored as negated
        ObjectIntHashMap<Term> counts = new ObjectIntHashMap(inhCount);
        for (int i = 0, xxLength = xx.length; i < xxLength; i++) {
            if (ii.get(i)) {
                Subterms sp = xx[i].unneg().subterms();
                counts.addToValue(sp.sub(0), +1);
                counts.addToValue(sp.sub(1).neg(), +1);
            }
        }
        MutableList<ObjectIntPair<Term>> sp = counts.keyValuesView().select(t -> t.getTwo() > 1).toList();
        if (sp.isEmpty())
            return null;

        FasterList<Term> xxx = new FasterList();
        for (int i = 0; i < xx.length; i++) {
            if (ii.get(i))
                xxx.add(xx[i]);
        }

        if (sp.size()>1)
            sp.sortThis(Comparator.comparingInt((ObjectIntPair<Term> i) -> -i.getTwo()).thenComparing(ObjectIntPair::getOne));


        for (ObjectIntPair<Term> j : sp) {
            int xxxn = xxx.size();
            if (xxxn < 2)
                break; //done
            Term jj = j.getOne();
            int subjOrPred = !(jj instanceof Neg) ? 0 :1;
            jj = jj.unneg();
            MutableSet<Term> components = new UnifiedSet(xxxn);
            for (Term xxxi : xxx) {
                Subterms xxi = xxxi.unneg().subterms();
                if (xxi.sub(subjOrPred).equals(jj)) {
                    Term c = xxxi;
                    if (components.contains(c.neg()))
                        return False; //contradiction detected
                    components.add(c);
                }
            }
            if (components.size() <= 1)
                continue;

            components.forEach(xxx::removeInstance);
            TreeSet<Term> c2 = new TreeSet();
            components.forEach(z -> c2.add(z.unneg().sub(1 - subjOrPred).negIf(z instanceof Neg)));


            Term cc;
            if (subjOrPred==0) {
                cc = INH.the(B, jj, CONJ.the(B, c2));
            } else {
                cc = INH.the(B, Op.DISJ(B, c2.toArray(Op.EmptyTermArray)), jj);
            }
            if (cc == False || cc == Null)
                return cc;
            if (cc!=True)
                all.add(cc);
        }
        all.addAll(xxx);
        return CONJ.the(all);
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
