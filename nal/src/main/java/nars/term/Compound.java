/*
 * CompoundTerm.java
 *
 * Copyright (C) 2008  Pei Wang
 *
 * This file is part of Open-NARS.
 *
 * Open-NARS is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Open-NARS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Open-NARS.  If not, see <http:
 */
package nars.term;

import jcog.Util;
import jcog.data.bit.MetalBitSet;
import jcog.data.sexpression.IPair;
import jcog.data.sexpression.Pair;
import nars.$;
import nars.Op;
import nars.The;
import nars.io.TermAppender;
import nars.subterm.Subterms;
import nars.term.anon.Anon;
import nars.term.compound.UnitCompound;
import nars.term.util.TermException;
import nars.term.util.builder.TermBuilder;
import nars.term.util.conj.Conj;
import nars.term.util.transform.AbstractTermTransform;
import nars.term.util.transform.MapSubst;
import nars.term.util.transform.Retemporalize;
import nars.unify.Unify;
import org.eclipse.collections.api.block.predicate.primitive.LongObjectPredicate;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collection;
import java.util.SortedSet;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import static nars.Op.*;
import static nars.time.Tense.*;

/**
 * a compound target
 * TODO make this an interface extending Subterms
 */
public interface Compound extends Term, IPair, Subterms {

    @Override
    default boolean AND(Predicate<Term> p) {
        return Subterms.super.AND(p);
    }

    @Override
    default boolean OR(Predicate<Term> p) {
        return Subterms.super.OR(p);
    }

    @Override
    default boolean contains(Term t) {
        return Subterms.super.contains(t);
    }


    static boolean equals(/*@NotNull*/ Compound A, Object b, boolean compareHashCode) {
        if (A == b) return true;

        if (((b instanceof Compound) && (!compareHashCode || (A.hashCode() == b.hashCode())))) {
            Compound B = (Compound) b;
            Op ao = A.op();
            if (ao == B.op()) {
                return
                        equalSubs(A, B) && (!ao.temporal || (A.dt() == B.dt()))
                        ;
            }
        }

        return false;
    }

    static boolean equalSubs(Compound a, Compound b) {

        if (a instanceof UnitCompound || b instanceof UnitCompound) {
            //avoid instantiating dummy subterms instance
            return a.subs() == 1 && b.subs() == 1 && a.sub(0).equals(b.sub(0));
        } else
            return a.subterms().equals(b.subterms());
    }

    static String toString(Compound c) {
        return toStringBuilder(c).toString();
    }

    static StringBuilder toStringBuilder(Compound c) {
        StringBuilder sb = new StringBuilder(
                ///* conservative estimate */ c.volume() * 2
                64
        );
        try {
            c.appendTo(sb);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return sb;
    }


    Op op();


    /** very fragile be careful here */
    @Override default /* final */ boolean containsRecursively(Term x, boolean root, Predicate<Term> inSubtermsOf) {
        return !impossibleSubTerm(x) && inSubtermsOf.test(this) &&
                subtermsContainsRecursively(x, root, inSubtermsOf);
    }
    default boolean subtermsContainsRecursively(Term x, boolean root, Predicate<Term> inSubtermsOf) {
        return subterms().containsRecursively(x, root, inSubtermsOf);
    }


    /**
     * deprecated; TODO move to SeparateSubtermsCompound interface and allow Compounds which do not have to generate this.  this sums up many of xjrn's suggestions
     */
    @Override
    Subterms subterms();


    /**
     * reference impl for compound hashcode
     */
    static int hashCode(Compound c) {
        return Util.hashCombine(
                c.hashCodeSubterms(),
                c.op().id
        );
    }


    @Override
    default int hashCodeSubterms() {
        return subterms().hashCode();
    }

    @Override
    default boolean the() {
        return this instanceof The && subterms().these();
    }

    @Override
    default boolean these() {
        return this.the();
    }

    @Override
    default int opX() {
        //return Term.opX(op(), (short) volume());
        //return Term.opX(op(), (short) subs());

        //upper 11 bits: volume
        //next 5: op
        //lower 16: ? subs ? structure hash
        short volume = (short) volume();
        byte op = op().id;
        byte subs = (byte) subs();
        return opX(volume, op, subs);
    }

    static int opX(short volume, byte op, byte subs) {
        return ((volume & 0b11111111111) << (16 + 5))
                |
                (op << 16)
                |
                subs;
    }


    @Override
    default Term anon() {
        return new Anon(/* TODO size estimate */).put(this);
    }


    @Override
    default boolean recurseTerms(Predicate<Term> inSuperCompound, Predicate<Term> whileTrue, Compound superterm) {
        return !inSuperCompound.test(this) ||
                whileTrue.test(this) && subterms().recurseTerms(inSuperCompound, whileTrue, this);
    }

    @Override
    default boolean recurseTermsOrdered(Predicate<Term> inSuperCompound, Predicate<Term> whileTrue, Compound parent) {
        return !inSuperCompound.test(this) ||
                whileTrue.test(this) && subterms().recurseTermsOrdered(inSuperCompound, whileTrue, this);
    }

    @Override
    default boolean recurseTerms(Predicate<Compound> aSuperCompoundMust, BiPredicate<Term, Compound> whileTrue, @Nullable Compound superterm) {
        return !aSuperCompoundMust.test(this) ||
                whileTrue.test(this, superterm) && subterms().recurseTerms(aSuperCompoundMust, whileTrue, this);
    }


    @Override
    default boolean ORrecurse(Predicate<Term> p) {
        return p.test(this) || subterms().ORrecurse(p);
    }

    @Override
    default boolean ANDrecurse(Predicate<Term> p) {
        return p.test(this) && subterms().ANDrecurse(p);
    }


    /**
     * //     * unification matching entry point (default implementation)
     * //     *
     * //     * @param y compound to match against (the instance executing this method is considered 'x')
     * //     * @param u the substitution context holding the match state
     * //     * @return whether match was successful or not, possibly having modified subst regardless
     * //
     */
//    @Override
//    default boolean unify(/*@NotNull*/ Term y, /*@NotNull*/ Unify u) {
//        return (this == y) || unifyForward(y, u) || ((y instanceof Variable) && y.unify(this, u));
//
//    }
    @Override
    default boolean unify(Term y, Unify u) {
        return (this == y)
                ||
                (y instanceof Compound && (equals(y) || (op() == y.op() && unifySubterms(y, u))))
                ||
                (y instanceof Variable && y.unify(this, u));
    }


    default boolean unifySubterms(Term y, Unify u) {

        Term x = this;

        Subterms xx = subterms(), yy = y.subterms();

        int xs = xx.subs();
        if (xs == 1)
            return xx.sub(0).unify(yy.sub(0), u);

        Op o = op();

        int ys = yy.subs();

        if (xs != ys) {
            if (o == CONJ) {
                int xdt = dt(), ydt = y.dt();
                if (xdt != ydt) {
                    if (xdt == XTERNAL || ydt == XTERNAL) {

                        if (!Subterms.possiblyUnifiable(x, y, u.varBits))
                            return false;

                        if (xdt == XTERNAL) {
                            SortedSet<Term> yyy = y.eventSet();
                            if ((ys = yyy.size()) != xs)
                                return false; //TODO permute if possiblyUnifiable
                            yy = $.vFast(yyy.toArray(EmptyTermArray));
                        } else /*if (ydt == XTERNAL)*/ {
                            SortedSet<Term> xxx = x.eventSet();
                            if ((xs = xxx.size()) != ys)
                                return false; //TODO permute if possiblyUnifiable
                            xx = $.vFast(xxx.toArray(EmptyTermArray));
                        }

                        return xx.equals(yy) || Subterms.unifyCommute(xx, yy, u);
                    }

                    return false; //differing #subterms and neither is XTERNAL
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }

        if (!Subterms.possiblyUnifiable(xx, yy, u))
            return false;

        if (o.temporal) {
            int xdt = x.dt(), ydt = y.dt();
            boolean xSpecific = (xdt != XTERNAL && xdt != DTERNAL);
            boolean ySpecific = (ydt != XTERNAL && ydt != DTERNAL);

            if (xSpecific && ySpecific) {
                if (xdt != ydt && !u.unifyDT(xdt, ydt))
                    return false;

                if (o == CONJ)
                    return (xdt >= 0) == (ydt >= 0) ? Subterms.unifyLinear(xx, yy, u) : Subterms.unifyLinear(xx, yy.reversed(), u);
            }


            //compound equality would have been true if non-temporal
            if (xdt != ydt && xx.equals(yy))
                return true;

            if (!xSpecific && !ySpecific && !u.var(xx) && !u.var(yy)) {
                //both constant
                if (!xx.hasAny(Op.CONJ.bit) && !yy.hasAny(Op.CONJ.bit))
                    return false; //both constant (excl CONJ); no possibility of unify
            }
        }

        if (o.commutative /* subs>1 */) {
            return Subterms.unifyCommute(xx, yy, u);
        } else { //TODO if temporal, compare in the correct order
            return Subterms.unifyLinear(xx, yy, u);
        }

//            if (result) {
//                if (xSpecific^ySpecific && u instanceof Derivation) {
//                    //one is not specified.  specify via substitution
//                    Derivation du = (Derivation) u;
//                    if (!xSpecific) {
//                        du.refinements.put(x, x.dt(ydt));
//                    } else {
//                        du.refinements.put(y, y.dt(xdt));
//                    }
//                }
//
//                return true;
//            }
//            return false;

    }

//
//    static boolean unifyXternal(Subterms xx, Subterms yy, Unify u) {
//        if (xx.equals(yy))
//            return true;
//
//        int n = xx.subs();
//        if (yy.subs()!=n)
//            return false;
//
//        for (int i = 0; i < n; i++) {
//            if (!xx.sub(i).unify(yy.sub(i), u))
//                return false;
//        }
//        return true;
////        Term xr = x.root();
////        return xr.equals(x) && (xr.equals(y) || xr.equals(y.root()));
//    }


    @Override
    default void appendTo(/*@NotNull*/ Appendable p) throws IOException {
        TermAppender.append(this, p);
    }

    @Nullable
    @Override
    default Object _car() {

        return sub(0);
    }

    /**
     * cdr or 'rest' function for s-expression interface when arity > 1
     */
    @Nullable
    @Override
    default Object _cdr() {
        int len = subs();
        switch (len) {
            case 1:
                throw new RuntimeException("Pair fault");
            case 2:
                return sub(1);
            case 3:
                return new Pair(sub(1), sub(2));
            case 4:
                return new Pair(sub(1), new Pair(sub(2), sub(3)));
        }


        Pair p = null;
        for (int i = len - 2; i >= 0; i--) {
            p = new Pair(sub(i), p == null ? sub(i + 1) : p);
        }
        return p;
    }


    /*@NotNull*/
    @Override
    default Object setFirst(Object first) {
        throw new UnsupportedOperationException();
    }

    /*@NotNull*/
    @Override
    default Object setRest(Object rest) {
        throw new UnsupportedOperationException();
    }

    @Override
    default boolean isCommutative() {
        //Op op = op();
        return /*op == CONJ ? dtSpecial(dt()) : */op().commutative && subs() > 1;
    }


    @Override
    default void addAllTo(/*@NotNull*/ Collection<Term> set) {
        subterms().addAllTo(set);
    }


    @Override
    default boolean isNormalized() {
        return subterms().isNormalized();
    }

    default int structureSub() {
        return subterms().structure();
    }

    /**
     * gets temporal relation value
     */
    @Override
    int dt();


    /**
     * replaces the 'from' target with 'to', recursively
     */
    default Term replace(Term from, Term to) {
        if (this.equals(from))
            return to;
        if (impossibleSubTerm(from))
            return this;
        return MapSubst.replace(from, to).apply(this);

//
//        Subterms oldSubs = subterms();
//        Subterms newSubs = oldSubs.replaceSub(from, to, op());
//
//        if (newSubs == oldSubs)
//            return this;
//
//        if (newSubs == null)
//            return Bool.Null;
//
//        int dt = dt();
//        Op o = op();
//        if (newSubs instanceof TermList) {
//            return o.the(dt, ((TermList) newSubs).arrayKeep());
//        } else {
//            return o.the(dt, newSubs);
//        }
//
    }

//    @Deprecated default int subTimeOnly(Term event) {
//        int[] t = subTimes(event, 1);
//        if (t == null || t.length != 1) return DTERNAL;
//        return t[0];
//    }

//    default int[] subTimes(Term event) {
//        return subTimes(event, Integer.MAX_VALUE);
//    }

//    /**
//     * TODO return XTERNAL not DTERNAL on missing, it is more appropriate
//     * expect the output array to be sorted
//     */
//    default int[] subTimes(Term event, int max) {
//        assert (max > 0);
//
//        if (equals(event))
//            return new int[]{0};
//
//        if (op() != CONJ || impossibleSubTerm(event))
//            return null;
//
//        //int dt = dt();
//        if (!Conj.isSeq(this)) {
//            int[] tt = null;
//            boolean needDedup = false;
//            for (Term x : subterms()) {
//                int[] ss = x.subTimes(event);
//                if (ss != null) {
//                    if (ss.length > max)
//                        return null;
//                    if (tt == null)
//                        tt = ss;
//                    else if (!Arrays.equals(ss, tt)) {
//                        int undupN = ss.length + tt.length;
//                        tt = ArrayUtils.addAll(ss, tt);
//                        needDedup = tt.length != undupN;
//                        if (!needDedup && tt.length > max)
//                            return null;
//                    }
//                }
//            }
//            if (needDedup) {
//                tt = ArrayUtils.removeDuplicates(tt);
//                if (tt.length > max)
//                    return null;
//            } else {
//                if (tt != null) {
//                    if (tt.length > max)
//                        return null;
//                    if (tt.length > 1)
//                        Arrays.sort(tt);
//                }
//            }
//            return tt;
//        } else {
//
//
//            int[][] found = new int[1][];
//            subTimesWhile(event, (when) -> {
//                found[0] = found[0] == null ? new int[]{when} : ArrayUtils.add(found[0], when);
//                return true;
//            });
//
//            return found[0];
//        }
//    }




    /**
     * iterates contained events within a conjunction
     */
    @Override
    default boolean eventsWhile(LongObjectPredicate<Term> each, long offset, boolean decomposeConjParallel, boolean decomposeConjDTernal, boolean decomposeXternal) {

        Op o = op();
        if (o != CONJ)
            return each.accept(offset, this);

        int dt = dt();
        boolean decompose;
        switch (dt) {

            case DTERNAL:
                if (Conj.isFactoredSeq(this)) {
                    Subterms ss = subterms();

                    //distribute the factored inner sequence
                    MetalBitSet eteComponents = Conj.seqEternalComponents(ss);
                    Term seq = Conj.seqTemporal(ss, eteComponents);
                    boolean unfactor;
                    int sdt = seq.dt();
                    switch (sdt) {
                        case 0:
                            unfactor = decomposeConjParallel;
                            break;
                        case XTERNAL:
                            unfactor = decomposeXternal;
                            break;
                        default://non-special sequence
                            unfactor = true;
                            break;
                    }
                    if (unfactor) {
                        Term factor = Conj.seqEternal(ss, eteComponents);

                        return seq.eventsWhile((when, what) -> {

                            int w = DTERNAL;
                            if ((w == DTERNAL && !decomposeConjDTernal) || (w != DTERNAL && !decomposeConjParallel)) {
                                //combine the component with the eternal factor
                                Term distributed = CONJ.the(w, what, factor);

                                if (distributed.op()!=CONJ)
                                    throw new TermException("invalid conjunction factorization", Compound.this);

//                                    assert (!(distributed instanceof Bool));
                                return each.accept(when, distributed);
                            } else {
                                //provide the component and the eternal separately, at the appropriate time
                                return each.accept(when, what) && each.accept(when, factor);
                            }

                        }, offset, decomposeConjParallel, decomposeConjDTernal, decomposeXternal);

                    }

                }

                if (decompose = decomposeConjDTernal)
                    dt = 0;

                break;
            case 0:
                decompose = decomposeConjParallel;
                break;
            case XTERNAL:
                if (decompose = decomposeXternal)
                    dt = 0;
                break;
            default:
                decompose = true;
                break;
        }

        if (decompose) {

            Subterms ee = subterms();

            long t = offset;

            boolean changeDT = t != ETERNAL && t != TIMELESS && dt != 0 /* motionless in time */;


            boolean fwd;
            if (changeDT) {
                fwd = dt >= 0;
                if (!fwd)
                    dt = -dt;
            } else {
                fwd = true;
            }

            int s = ee.subs() - 1;
            for (int i = 0; i <= s; i++) {
                Term ei = ee.sub(fwd ? i : s - i);
                if (!ei.eventsWhile(each, t, decomposeConjParallel, decomposeConjDTernal, decomposeXternal))
                    return false;

                if (changeDT && i < s)
                    t += dt + ei.eventRange();
            }


            return true;
        } else {
            return each.accept(offset, this);
        }
    }


    @Override
    default boolean hasXternal() {
        return dt() == XTERNAL || Subterms.super.hasXternal();
    }

    @Override
    default Term unneg() {
        return op() == NEG ? sub(0) : this;
    }

    @Override
    @Nullable
    default Term normalize(byte varOffset) {
        if (varOffset == 0 && this.isNormalized())
            return this;

        return Op.terms.normalize(this, varOffset);
    }

    @Override
    default Term root() {
        return Op.terms.root(this);
    }


    @Override
    default Term concept() {
        return Op.terms.concept(this);
    }

    @Override
    default int eventRange() {
        if (op() == CONJ) {
            int dt = dt();
            if (dt == XTERNAL)
                return 0; //unknown actual range; logically must be considered as point-like event

            Subterms tt = subterms();
            int l = tt.subs();
            if (l == 2) {

                switch (dt) {
                    case DTERNAL:
                    case 0:
                        dt = 0;
                        break;
                    default:
                        dt = Math.abs(dt);
                        break;
                }

                return tt.subEventRange(0) + (dt) + tt.subEventRange(1);

            } else {
                int s = 0;


                for (int i = 0; i < l; i++) {
                    s = Math.max(s, tt.subEventRange(i));
                }

                return s;
            }
        }
        return 0;

    }

    @Override
    default int structure() {
        return Subterms.super.structure() | opBit();
    }

    default Term dt(int dt) {
        return dt(dt, Op.terms);
    }

    default Term dt(int nextDT, TermBuilder b) {
        return b.dt(this, nextDT);
    }


    @Override
    @Nullable
    default Term temporalize(Retemporalize r) {
        return r.applyCompound(this);
    }


    @Override
    default boolean equalsRoot(Term x) {
        if (this.equals(x))
            return true;

        Op o = op();
        if (o != x.op())
            return false;

        if (!o.temporal && !hasAny(Op.Temporal))
            return false;

        if (structure() == x.structure()) {
            Term root = root(), xRoot;
            return (root != this && root.equals(x)) || (((xRoot = x.root())) != x && root.equals(xRoot));
        }

        return false;
    }


    default Term transform(AbstractTermTransform f) {
        return f.applyCompound(this);
    }


    default Term eventFirst() {
        if (Conj.isSeq(this)) {
            final Term[] first = new Term[1];
            eventsWhile((when, what) -> {
                first[0] = what;
                return false; //done got first
            }, 0, false, false, false);
            return first[0];
        }
        return this;
    }

    /**
     * TODO optimize
     */
    default Term eventLast() {
        if (Conj.isSeq(this)) {
            final Term[] last = new Term[1];
            eventsWhile((when, what) -> {
                last[0] = what;
                return true; //HACK keep going to end
            }, 0, false, false, false);
            return last[0];
        }
        return this;
    }

}























































































    /*
    @Override
    public boolean equals(final Object that) {
        return (that instanceof Term) && (compareTo((Term) that) == 0);
    }
    */









































































































































































    /* UNTESTED
    public Compound clone(VariableTransform t) {
        if (!hasVar())
            throw new RuntimeException("this VariableTransform clone should not have been necessary");

        Compound result = cloneVariablesDeep();
        if (result == null)
            throw new RuntimeException("unable to clone: " + this);

        result.transformVariableTermsDeep(t);

        result.invalidate();

        return result;
    } */


/**
 * override in subclasses to avoid unnecessary reinit
 */
    /*public CompoundTerm _clone(final Term[] replaced) {
        if (Terms.equals(target, replaced)) {
            return this;
        }
        return clone(replaced);
    }*/





















































    /*static void shuffle(final Term[] list, final Random randomNumber) {
        if (list.length < 2)  {
            return;
        }


        int n = list.length;
        for (int i = 0; i < n; i++) {
            
            int r = i + (randomNumber.nextInt() % (n-i));
            Term tmp = list[i];    
            list[i] = list[r];
            list[r] = tmp;
        }
    }*/

/*        public static void shuffle(final Term[] ar,final Random rnd)
        {
            if (ar.length < 2)
                return;



          for (int i = ar.length - 1; i > 0; i--)
          {
            int index = randomNumber.nextInt(i + 1);
            
            Term a = ar[index];
            ar[index] = ar[i];
            ar[i] = a;
          }

        }*/















































































































































































