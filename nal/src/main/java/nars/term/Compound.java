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
import nars.Op;
import nars.The;
import nars.io.TermAppender;
import nars.subterm.Subterms;
import nars.term.anon.Anon;
import nars.term.compound.UnitCompound;
import nars.term.util.TermTransformException;
import nars.term.util.builder.TermBuilder;
import nars.term.util.conj.Conj;
import nars.term.util.conj.ConjSeq;
import nars.term.util.conj.ConjUnify;
import nars.term.util.transform.MapSubst;
import nars.term.util.transform.TermTransform;
import nars.unify.Unify;
import nars.unify.UnifyAny;
import nars.unify.UnifyFirst;
import org.eclipse.collections.api.block.function.primitive.IntObjectToIntFunction;
import org.eclipse.collections.api.block.predicate.primitive.LongObjectPredicate;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import static nars.Op.CONJ;
import static nars.time.Tense.*;

/**
 * a compound target
 * TODO make this an interface extending Subterms
 */
public interface Compound extends Term, IPair, Subterms {

    static boolean equals(/*@NotNull*/ Compound A, Object b, boolean compareHashCode) {
        if (A == b) return true;

        if (((b instanceof Compound) && (!compareHashCode || (A.hashCode() == b.hashCode())))) {
            Compound B = (Compound) b;
            int ao = A.opID();
            return
                (ao == B.opID()) &&
                equalSubs(A, B) &&
                (!Op.the(ao).temporal || (A.dt() == B.dt()));
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
        return c.appendTo(sb);
    }

    /**
     * reference impl for compound hashcode
     */
    static int hash(Compound c) {
        return hash(
                c.opID(),
                c.hashCodeSubterms()
        );
    }

    static int hash(int opID, int hashCodeSubterms) {
        return Util.hashCombine(hashCodeSubterms, opID);
    }

    static int hash1(int opID, Term onlySubterm) {
        return hash(opID, Subterms.hash(onlySubterm));
    }

    static int opX(short volume, byte op, byte subs) {
        return ((volume & 0b11111111111) << (16 + 5))
                |
                (op << 16)
                |
                subs;
    }

//    @Override
//    default boolean AND(Predicate<Term> p) {
//        return Subterms.super.AND(p);
//    }
//
//    @Override
//    default boolean OR(Predicate<Term> p) {
//        return Subterms.super.OR(p);
//    }

//    @Override
//    default Op op() {
//        return null;
//    }

    @Override
    boolean equals(Object o);

    @Override
    int hashCode();

    @Override
    default int intifyRecurse(IntObjectToIntFunction<Term> reduce, int v) {
        return reduce.intValueOf(subterms().intifyRecurse(reduce, v), this);
    }

    @Override
    boolean recurseTerms(Predicate<Term> inSuperCompound, Predicate<Term> whileTrue, @Nullable Compound superterm);

    @Override
    boolean recurseTermsOrdered(Predicate<Term> inSuperCompound, Predicate<Term> whileTrue, Compound parent);

    default boolean unifiesRecursively(Term x) {
        return unifiesRecursively(x, (y)->true);
    }

    /** TODO test */
    default boolean unifiesRecursively(Term x, Predicate<Term> preFilter) {

        if (x instanceof Compound) {
//            int xv = x.volume();
            if (!hasAny(Op.Variable) /*&& xv > volume()*/)
                return false; //TODO check

            UnifyAny u = new UnifyAny();

            //if (u.unifies(this, x)) return true;

            int xOp = x.opID();
            return !subterms().recurseTerms(s->s.hasAny(1<<xOp)/*t->t.volume()>=xv*/, s->{
                if (s instanceof Compound && s.opID()==xOp) {
                    return !preFilter.test(s) || !x.unify(s, u);
                }
                return true;
            }, this);
        } else {
            return x instanceof Variable || containsRecursively(x);
        }
    }



    @Override
    boolean recurseTerms(Predicate<Compound> aSuperCompoundMust, BiPredicate<Term, Compound> whileTrue, @Nullable Compound superterm);

    /**
     * very fragile be careful here
     */
    @Override
    default /* final */ boolean containsRecursively(Term x, boolean root, @Nullable Predicate<Term> inSubtermsOf) {
        return (inSubtermsOf == null || inSubtermsOf.test(this)) && subtermsContainsRecursively(x, root, inSubtermsOf);
    }

    boolean subtermsContainsRecursively(Term x, boolean root, Predicate<Term> inSubtermsOf);

    /**
     * deprecated; TODO move to SeparateSubtermsCompound interface and allow Compounds which do not have to generate this.  this sums up many of xjrn's suggestions
     */
    @Override
    Subterms subterms();

    /** for direct access to subterms only; may return this instance if SameSubtermsCompound */
    default Subterms subtermsDirect() {
        return subterms();
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
    default Term anon() {
        return new Anon(/* TODO size estimate */).put(this);
    }



    @Override
    default Term sub(int i) {
        return null;
    }

    @Override
    default int subs() {
        return 0;
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
                (y instanceof UnifyFirst && y.unify(this, u))
                ||
                (y instanceof Compound && (equals(y) || (opID() == y.opID() && unifySubterms((Compound)y, u))))

                ;
    }


    default boolean unifySubterms(Compound y, Unify u) {

        Compound x = this;
        Subterms xx = x.subterms();
        Subterms yy = y.subterms();

        Op o = op();
        if (o.temporal) {
            if (!u.unifyDT(x, y))
                return false;

            if (xx.equals(yy))
                return true; //compound equality would have been true if non-temporal

            if (o == CONJ)
                return ConjUnify.unifyConj(x, y, xx, yy, u);
        }


        int xs = xx.subs();
        if (xs != yy.subs())
            return false;

        if (xs == 1)
            return xx.sub(0).unify(y.sub(0), u);

        if (!Subterms.possiblyUnifiableAssumingNotEqual(xx, yy, u.varBits))
            return false;

        if (o.commutative /* subs>1 */)
            return Subterms.unifyCommute(xx, yy, u);
        else
            return Subterms.unifyLinear(xx, yy, u);
    }

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

    /**
     * gets temporal relation value
     */
    @Override
    int dt();

    @Override
    boolean isNormalized();

    default int subStructure() {
        return Subterms.super.structure();
    }

    /**
     * replaces the 'from' target with 'to', recursively
     */
    default Term replace(Term from, Term to) {

        return MapSubst.replace(from, to, this);

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


    @Override
    default boolean eventsOR(LongObjectPredicate<Term> each, long offset, boolean decomposeConjDTernal, boolean decomposeXternal) {
        return !eventsAND((when,what)-> !each.accept(when, what), offset, decomposeConjDTernal, decomposeXternal);
    }

    /**
     * iterates contained events within a conjunction
     */
    @Override
    default boolean eventsAND(LongObjectPredicate<Term> each, long offset, boolean decomposeConjDTernal, boolean decomposeXternal) {


        boolean decompose;
        int dt;

        if (opID() != CONJ.id) {
            decompose = false;
            dt = DTERNAL;
        } else {

            dt = dt();
            switch (dt) {

                case DTERNAL:
                    if (ConjSeq.isFactoredSeq(this)) {
                        Subterms ss = subterms();

                        //distribute the factored inner sequence
                        MetalBitSet eteComponents = ConjSeq.seqEternalComponents(ss);
                        Term seq = ConjSeq.seqTemporal(ss, eteComponents);

                        int sdt = seq.dt();
                        //non-special sequence
                        boolean unfactor = sdt != XTERNAL || decomposeXternal;
                        if (unfactor) {
                            Term factor = ConjSeq.seqEternal(ss, eteComponents);

                            return seq.eventsAND(
                                    (!decomposeConjDTernal) ?
                                            (when, what) -> {
                                                //combine the component with the eternal factor
                                                Term distributed = CONJ.the(what, factor);

                                                if (distributed.op() != CONJ)
                                                    throw new TermTransformException(Compound.this, distributed, "invalid conjunction factorization"
                                                    );

                                                return each.accept(when, distributed);
                                            }
                                            :
                                            (when, what) ->
                                                    //provide the component and the eternal separately, at the appropriate time
                                                    each.accept(when, what) && each.accept(when, factor)

                                    , offset, decomposeConjDTernal, decomposeXternal);

                        }

                    }

                    if (decompose = decomposeConjDTernal)
                        dt = 0;

                    break;
//            case 0:
//                decompose = decomposeConjParallel;
//                break;
                case XTERNAL:
                    if (decompose = decomposeXternal)
                        dt = 0;
                    break;
                default:
                    decompose = true;
                    break;
            }
        }

        if (!decompose) {
            return each.accept(offset, this);
        } else {

            Subterms ee = subterms();

            long t = offset;

            boolean changeDT = dt != 0 && t != ETERNAL && t != TIMELESS /* motionless in time */;


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
                if (!ei.eventsAND(each, t, decomposeConjDTernal, decomposeXternal))
                    return false;

                if (changeDT && i < s)
                    t += dt + ei.eventRange();
            }


            return true;
        }
    }


    @Override
    default boolean hasXternal() {
        return dt() == XTERNAL || Subterms.super.hasXternal();
    }

    @Override
    @Nullable
    default Term normalize(byte varOffset) {
        if (varOffset == 0 && this.isNormalized())
            return this;

        return Op.terms.normalize(this, varOffset);
    }


    @Override default Term root() { return Op.terms.root(this);  }
    @Override default Term concept() { return TermBuilder.concept(this);  }


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
        return subStructure() | opBit();
    }

    default Term dt(int dt) {
        return dt(dt, Op.terms);
    }

    default Term dt(int nextDT, TermBuilder b) {
        return b.dt(this, nextDT);
    }

    @Override
    default boolean equalsRoot(Term x) {
        if (!(x instanceof Compound))
            return false;

        if (this.equals(x))
            return true;

        if (op() != x.op() || !hasAny(Op.Temporal)
                || structure()!=x.structure())
            return false;

        Term root = root(), xRoot;
        return (root != this && root.equals(x)) || (((xRoot = x.root())) != x && root.equals(xRoot));
    }


    @Override default /* final */ Term transform(TermTransform t) {
//        if (t instanceof RecursiveTermTransform) {
//            return transform((RecursiveTermTransform) t, null, XTERNAL);
//            //return transformBuffered(t, null, 256); //<- not ready yet
//        } else
            return t.applyCompound(this);
    }


//    /** global default transform procedure: can decide semi-optimal transform implementation
//     *  TODO not ready yet
//     * */
//    default Term transformBuffered(TermTransform transform, @Nullable TermBuffer l, int volMax) {
////        try {
//            if (l == null)
//                l = new TermBuffer();
//            else
//                l.clear();
//
//            return l.appendCompound(this, transform, volMax) ? l.term() : Null;
//
////        } catch (TermException t) {
////            if (NAL.DEBUG)
////                throw t;
////            //continue below
////        } catch (RuntimeException e) {
////            throw new TermException(e.toString(), this);
////            //return Null;
////        }
////
////        return transform.apply(this);
//    }



    default Term eventFirst() {
        if (Conj.isSeq(this)) {
            final Term[] first = new Term[1];
            eventsAND((when, what) -> {
                first[0] = what;
                return false; //done got first
            }, 0, false, false);
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
            eventsAND((when, what) -> {
                last[0] = what;
                return true; //HACK keep going to end
            }, 0, false, false);
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















































































































































































