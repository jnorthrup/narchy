package nars.term.util.conj;

import jcog.Util;
import jcog.data.bit.MetalBitSet;
import jcog.util.ArrayUtil;
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
import java.util.function.Predicate;

import static nars.Op.CONJ;
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
            if (a.equalsNeg(b)) return False;
            if (a.equals(b)) return a;
        }

        var xt = xternalDistribute(dt, t, B);
        if (xt!=null)
            return xt;

//        Term h = inhBundle(t, B);
//        if (h!=null)
//            return h;

        var d = disjunctiveFactor(t, dt, B);
        if (d!=null)
            return d;


        if (t.length == 2) {
            //fast 2-ary non-conj case
            Term a = t[0], b = t[1];
            if (a.dt()!=XTERNAL && b.dt()!=XTERNAL && !a.hasAny(CONJ.bit) && !b.hasAny(CONJ.bit)) {
                return B.newCompound(CONJ, DTERNAL, sort ? Terms.commute(t) : t);
            }
        }


        var ct = new ConjTree();
        var sdt = dt==DTERNAL ? ETERNAL : 0;
        var remain = t.length;
        for (var i = t.length - 1; i >= 0; i--) {
            var x = t[i];
            if (x.unneg().op() != CONJ) {
                remain--;
                if (!ct.add(sdt, x))
                    break;
            }
        }
        if (remain > 0 && ct.terminal==null) {
            for (var i = t.length - 1; i >= 0; i--) {
                var x = t[i];
                if (x.unneg().op() == CONJ) {
                    if (!ct.add(sdt, x))
                        break;
                }
            }
        }

        return ct.term(B);
    }

    private static @Nullable Term xternalDistribute(int dt, Term[] xx, TermBuilder B) {
        var xternalCount = 0;
        var lastXternal = -1;
        for (int i = 0, xxLength = xx.length; i < xxLength; i++) {
            var t = xx[i];
            var to = t.op();
            if (to == CONJ && t.dt() == XTERNAL) {
                lastXternal = i;
                xternalCount++;
            }
        }

        //distribute to XTERNAL

        if (xternalCount == 1) {
            //distribute to xternal components
            var x = xx[lastXternal];
            var y = ArrayUtil.remove(xx, lastXternal);
            var Y = the(B, dt, true, y);
            if (Y == True) return x;
            return B.conj(XTERNAL, Util.map(xxx -> B.conj(dt, xxx, Y),
                    new Term[x.subs()],
                    x.subterms().arrayShared()));
        }

        return null;
    }

//    @Nullable public static Term inhBundle(Term[] xx, TermBuilder B) {
//        //NAL3 bundle
//        MetalBitSet ii = MetalBitSet.bits(xx.length);
//        for (int i = 0, xxLength = xx.length; i < xxLength; i++) {
//            Term t = xx[i];
//            Op to = t.op();
//            if (t.unneg().op() == INH) {
//                ii.set(i);
//            }
//        }
//        int inhCount = ii.cardinality();
//        if (inhCount <= 1)
//            return null;
//
//        //simple case
//        if (inhCount == xx.length && inhCount == 2) {
//            Term aa = xx[0];
//            Term bb = xx[1];
//            Term a = aa.unneg();
//            Term b = bb.unneg();
//            Term pred = a.sub(1);
//            if (pred.equals(b.sub(1))) {
//                //common pred: union
//                Term i = INH.the(B, Op.DISJ(B, a.sub(0).negIf(aa instanceof Neg), b.sub(0).negIf(bb instanceof Neg)), pred);
//                //TODO test for invalid
//                return i;
//            }
//            Term subj = a.sub(0);
//            if (subj.equals(b.sub(0))) {
//                //common subj: intersection
//                return INH.the(B, subj, CONJ.the(B, a.sub(1).negIf(aa instanceof Neg), b.sub(1).negIf(bb instanceof Neg)));
//            }
//        }
//
//        Term other = inhCount < xx.length ? CONJ.the(B, IntStream.range(0, xx.length).filter(r -> !ii.get(r)).mapToObj(r -> xx[r]).toArray(Term[]::new)) : null;
//        if (other == False || other == Null)
//            return other;
//        List<Term> all = new FasterList(4);
//        if (other!=null)
//            all.add(other);
//
//        //counts are stored as follows: subj are stored as normal, pred are stored as negated
//        ObjectIntHashMap<Term> counts = new ObjectIntHashMap(inhCount);
//        for (int i = 0, xxLength = xx.length; i < xxLength; i++) {
//            if (ii.get(i)) {
//                Subterms sp = xx[i].unneg().subterms();
//                counts.addToValue(sp.sub(0), +1);
//                counts.addToValue(sp.sub(1).neg(), +1);
//            }
//        }
//        MutableList<ObjectIntPair<Term>> sp = counts.keyValuesView().select(t -> t.getTwo() > 1).toList();
//        if (sp.isEmpty())
//            return null;
//
//        FasterList<Term> xxx = new FasterList();
//        for (int i = 0; i < xx.length; i++) {
//            if (ii.get(i))
//                xxx.add(xx[i]);
//        }
//
//        if (sp.size()>1)
//            sp.sortThis(Comparator.comparingInt((ObjectIntPair<Term> i) -> -i.getTwo()).thenComparing(ObjectIntPair::getOne));
//
//
//        MutableSet<Term> components = null;
//        for (ObjectIntPair<Term> j : sp) {
//            int xxxn = xxx.size();
//            if (xxxn < 2)
//                break; //done
//            if (components!=null)
//                components.clear();
//            Term jj = j.getOne();
//            int subjOrPred = !(jj instanceof Neg) ? 0 :1;
//            jj = jj.unneg();
//            for (Term xxxi : xxx) {
//                Subterms xxi = xxxi.unneg().subterms();
//                if (xxi.sub(subjOrPred).equals(jj)) {
//                    Term c = xxxi;
//                    if (components!=null) {
//                        if (components.contains(c.neg()))
//                            return False; //contradiction detected
//                    } else {
//                        components = new UnifiedSet();
//                    }
//                    components.add(c);
//                }
//            }
//            if (components==null || components.size() <= 1)
//                continue;
//
//            components.forEach(xxx::removeInstance);
//            MetalTreeSet<Term> c2 = new MetalTreeSet();
//            components.forEach(z -> c2.add(z.unneg().sub(1 - subjOrPred).negIf(z instanceof Neg)));
//
//
//            Term cc = subjOrPred == 0 ?
//                INH.the(B, jj, CONJ.the(B, c2)) :
//                INH.the(B, Op.DISJ(B, c2.toArray(Op.EmptyTermArray)), jj);
//            if (cc == False || cc == Null)
//                return cc;
//            if (cc!=True)
//                all.add(cc);
//        }
//        all.addAll(xxx);
//        return CONJ.the(all);
//    }

    public static @Nullable Term disjunctiveFactor(Term[] xx, int dt, TermBuilder B) {
        @Deprecated MetalBitSet cond = null;
        var n = xx.length;
        for (var i = 0; i < n; i++) {
            var x = xx[i];
            if (x instanceof Neg) {
                var xu = x.unneg();
                if (!(xu instanceof Sequence) && xu.op() == CONJ && xu.dt()==DTERNAL) {
                    if (cond == null) cond = MetalBitSet.bits(n);
                    cond.set(i);
                }
            }
        }
        if (cond == null)
            return null;

        var d = cond.cardinality();
        if (d == n) {
        //if (d > 1) {
            ObjectByteHashMap<Term> i = new ObjectByteHashMap(d);
            var j = -1;
            var anyFull = false;
            for (var k = 0; k < d; k++) {
                j = cond.next(true, j+1, n);
                var dc = xx[j];
                for (var ct : dc.unneg().subterms()) {
                    var ctn = ct.neg();
                    if (i.containsKey(ctn)) {
                        //disqualify both permanently since factoring them would cancel each other out
                        i.put(ct, Byte.MIN_VALUE);
                        i.put(ctn, Byte.MIN_VALUE);
                    } else {
                        var z = i.updateValue(ct, (byte) 0, (v) -> (v >= 0) ? (byte) (v + 1) : v);
                        anyFull |= (z == d);
                    }
                }

            }
            if (anyFull) {
                i.values().removeIf(b -> b < d);
                if (!i.isEmpty()) {
                    var common = i.keySet();
                    var factor = B.conj(common.toArray(Op.EmptyTermArray));

                    if (factor instanceof Bool)
                        return factor;

                    xx = xx.clone(); //dont modify input array
                    j = -1;

                    Predicate<Term> commonDoesntContain = s -> !common.contains(s);

                    for (var k = 0; k < d; k++) {
                        j = cond.next(true, j + 1, n);

                        var xxj = xx[j].unneg().subterms().subsIncluding(commonDoesntContain);
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
        return null;
    }



}
