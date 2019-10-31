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
import jcog.data.list.FasterList;
import jcog.data.sexpression.IPair;
import nars.Op;
import nars.subterm.Subterms;
import nars.subterm.TermList;
import nars.term.compound.UnitCompound;
import nars.term.util.builder.TermBuilder;
import nars.term.util.transform.TermTransform;
import nars.unify.Unify;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.function.primitive.IntObjectToIntFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntObjectPredicate;
import org.eclipse.collections.api.block.predicate.primitive.LongObjectPredicate;
import org.eclipse.collections.api.block.predicate.primitive.ObjectIntPredicate;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
import org.eclipse.collections.api.set.MutableSet;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.function.*;
import java.util.stream.Stream;

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

        //avoid instantiating dummy subterms instance
        return a instanceof UnitCompound || b instanceof UnitCompound ?
            a.subs() == 1 && b.subs() == 1 && a.sub(0).equals(b.sub(0)) :
            a.subterms().equals(b.subterms());
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
    int intifyRecurse(int v, IntObjectToIntFunction<Term> reduce);

    @Override
    boolean recurseTerms(Predicate<Term> inSuperCompound, Predicate<Term> whileTrue, @Nullable Compound superterm);

    @Override
    boolean recurseTermsOrdered(Predicate<Term> inSuperCompound, Predicate<Term> whileTrue, Compound parent);

    boolean unifiesRecursively(Term x);

    /** TODO test */
    boolean unifiesRecursively(Term x, Predicate<Term> preFilter);


    @Override
    boolean recurseTerms(Predicate<Compound> aSuperCompoundMust, BiPredicate<Term, Compound> whileTrue, @Nullable Compound superterm);

    /**
     * very fragile be careful here
     */
    @Override /* final */ boolean containsRecursively(Term x, boolean root, @Nullable Predicate<Term> inSubtermsOf);

    boolean subtermsContainsRecursively(Term x, boolean root, Predicate<Term> inSubtermsOf);

    /**
     * deprecated; TODO move to SeparateSubtermsCompound interface and allow Compounds which do not have to generate this.  this sums up many of xjrn's suggestions
     */
    @Override
    Subterms subterms();

    /** for direct access to subterms only; may return this instance if SameSubtermsCompound */
    Subterms subtermsDirect();

    @Override
    boolean the();

    @Override
    boolean these();

    @Override
    Term anon();


    @Override
    Term sub(int i);

    @Override
    int subs();

    @Override
    boolean ORrecurse(Predicate<Term> p);

    @Override
    boolean ANDrecurse(Predicate<Term> p);


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
    boolean unify(Term y, Unify u);


    boolean unifySubterms(Compound y, Unify u);

    @Override
    void appendTo(/*@NotNull*/ Appendable p) throws IOException;

    @Override
    @Nullable Object _car();

    /**
     * cdr or 'rest' function for s-expression interface when arity > 1
     */
    @Override
    @Nullable Object _cdr();


    /*@NotNull*/
    @Override
    Object setFirst(Object first);

    /*@NotNull*/
    @Override
    Object setRest(Object rest);

    @Override
    boolean isCommutative();

    /**
     * gets temporal relation value
     */
    @Override
    int dt();

    @Override
    boolean isNormalized();

    int subStructure();

    /**
     * replaces the 'from' target with 'to', recursively
     */
    Term replace(Term from, Term to);

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
    boolean eventsOR(LongObjectPredicate<Term> each, long offset, boolean decomposeConjDTernal, boolean decomposeXternal);

    /**
     * iterates contained events within a conjunction
     */
    @Override
    boolean eventsAND(LongObjectPredicate<Term> each, long offset, boolean decomposeConjDTernal, boolean decomposeXternal);


    @Override
    boolean hasXternal();

    @Override
    @Nullable Term normalize(byte varOffset);


    @Override
    Term root();

    @Override
    Term concept();


    @Override
    int eventRange();


    @Override
    int structure();

    Term dt(int dt);

    Term dt(int nextDT, TermBuilder b);

    @Override
    boolean equalsRoot(Term x);


    @Override /* final */ Term transform(TermTransform t);


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



    Term eventFirst();

    /**
     * TODO optimize
     */
    Term eventLast();


    @Override
    Predicate<Term> containing();

    @Override
    boolean contains(Term x);

    @Override
    boolean containsInstance(Term t);

    @Override
    @Nullable Term subSub(byte[] path);

    @Override
    @Nullable Term subSub(int start, int end, byte[] path);

    @Override
    @Nullable Term subSubUnsafe(int start, int end, byte[] path);

    @Override
    boolean containsAll(Subterms ofThese);

    @Override
    boolean containsAny(Subterms ofThese);

    @Override
    <X> X[] array(Function<Term, X> map, IntFunction<X[]> arrayizer);

    @Override
    int subEventRange(int i);

    @Override
    @Nullable Term subRoulette(FloatFunction<Term> subValue, Random rng);

    @Override
    @Nullable Term sub(Random rng);

    @Override
    Subterms remove(Term event);

    @Override
    void forEachI(ObjectIntProcedure<Term> t);

    @Override
    <X> void forEachWith(BiConsumer<Term, X> t, X argConst);

    @Override
    Subterms commuted();

    @Override
    boolean isSorted();

    @Override
    boolean isCommuted();

    @Override
    Iterator<Term> iterator();

    @Override
    boolean subEquals(int i, /*@NotNull*/ Term x);

    @Override /*@NotNull*/ SortedSet<Term> toSetSorted();

    @Override
    @SuppressWarnings("LambdaUnfriendlyMethodOverload") /*@NotNull*/ SortedSet<Term> toSetSorted(UnaryOperator<Term> map);

    @Override /*@NotNull*/ SortedSet<Term> toSetSorted(Predicate<Term> t);

    @Override
    Term[] arrayShared();

    @Override
    Term[] arrayClone();

    @Override
    Term[] arrayClone(Term[] target);

    @Override
    Term[] arrayClone(Term[] target, int from, int to);

    @Override /*@NotNull*/ TermList toList();

    @Override /*@NotNull*/ MutableSet<Term> toSet();

    @Override
    @Nullable <C extends Collection<Term>> C collect(Predicate<Term> ifTrue, C c);

    @Override
    void setNormalized();

    /*@NotNull*/
    @Override
    Set<Term> recurseSubtermsToSet(Op _onlyType);

    /*@NotNull*/
    @Override
    boolean recurseSubtermsToSet(int inStructure, /*@NotNull*/ Collection<Term> t, boolean untilAddedORwhileNotRemoved);

    @Override
    boolean equalTerms(/*@NotNull*/ Subterms c);

    @Override
    void addAllTo(Collection target);

    @Override
    void addAllTo(FasterList target);

    @Override /* final */ boolean impossibleSubStructure(int structure);

    @Override
    Term[] terms(/*@NotNull*/ IntObjectPredicate<Term> filter);

    @Override
    void forEach(Consumer<? super Term> action, int start, int stop);

    @Override
    void forEach(Consumer<? super Term> action);

    @Override
    boolean subIs(int i, Op o);

    @Override
    int count(Predicate<Term> match);

    @Override
    boolean countEquals(Predicate<Term> match, int n);

    @Override
    int count(Op matchingOp);

    @Override
    boolean subIsOrOOB(int i, Op o);

    @Override /* final */ int indexOf(/*@NotNull*/ Term t);

    @Override
    int indexOf(/*@NotNull*/ Term t, int after);

    @Override
    @Nullable Term subFirst(Predicate<Term> match);

    @Override
    boolean impossibleSubTerm(Termlike target);

    @Override
    boolean impossibleSubTerm(int structure, int volume);

    @Override
    Stream<Term> subStream();

    @Override
    int hashCodeSubterms();

    @Override
    MetalBitSet indicesOfBits(Predicate<Term> match);

    @Override
    Term[] subsIncluding(Predicate<Term> toKeep);

    @Override
    Term[] subsIncluding(MetalBitSet toKeep);

    @Override
    Term[] removing(MetalBitSet toRemove);

    @Override
    @Nullable Term[] subsIncExc(MetalBitSet s, boolean includeOrExclude);

    /*@NotNull*/
    @Override
    Term[] subRangeArray(int from, int to);

    @Override
    int indexOf(/*@NotNull*/ Predicate<Term> p);

    @Override
    int indexOf(/*@NotNull*/ Predicate<Term> p, int after);

    @Override
    boolean AND(/*@NotNull*/ Predicate<Term> p);

    @Override
    boolean OR(/*@NotNull*/ Predicate<Term> p);

    @Override
    boolean ANDi(/*@NotNull*/ ObjectIntPredicate<Term> p);

    @Override
    boolean ORi(/*@NotNull*/ ObjectIntPredicate<Term> p);

    @Override
    <X> boolean ORwith(/*@NotNull*/ BiPredicate<Term, X> p, X param);

    @Override
    <X> boolean ANDwith(/*@NotNull*/ BiPredicate<Term, X> p, X param);

    @Override
    <X> boolean ANDwithOrdered(/*@NotNull*/ BiPredicate<Term, X> p, X param);

    @Override
    Subterms reversed();

    @Override
    Term[] removing(int index);

    @Override
    int hashWith(Op op);

    @Override
    int hashWith(/*byte*/int op);

    @Override
    @Nullable Term[] removing(Term x);

    @Override
    Subterms transformSub(int which, UnaryOperator<Term> f);

    @Override
    @Nullable Subterms transformSubs(UnaryOperator<Term> f, Op superOp);

    @Override
    boolean containsPosOrNeg(Term x);

    @Override
    boolean containsNeg(Term x);
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















































































































































































