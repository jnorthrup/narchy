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

import com.google.common.io.ByteArrayDataOutput;
import jcog.data.sexpression.IPair;
import jcog.data.sexpression.Pair;
import nars.IO;
import nars.Op;
import nars.subterm.Subterms;
import nars.subterm.util.TermList;
import nars.term.anon.Anon;
import nars.unify.Unify;
import nars.util.term.transform.CompoundNormalization;
import nars.util.term.transform.Retemporalize;
import org.eclipse.collections.api.block.predicate.primitive.LongObjectPredicate;
import org.jetbrains.annotations.Nullable;
import org.roaringbitmap.RoaringBitmap;

import java.io.IOException;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static nars.Op.*;
import static nars.time.Tense.*;

/**
 * a compound term
 * TODO make this an interface extending Subterms
 */
public interface Compound extends Term, IPair, Subterms {


    static boolean equals(/*@NotNull*/ Compound a, @Nullable Term bb) {
        assert (a != bb) : "instance check should have already been performed before calling this";

        return
                (a.opX() == bb.opX())
                        &&
                        (a.dt() == bb.dt())
                        &&
                        (a.subterms().equals(bb.subterms()))
                ;
    }

    static String toString(Compound c) {
        return toStringBuilder(c).toString();
    }

    static StringBuilder toStringBuilder(Compound c) {
        StringBuilder sb = new StringBuilder(/* conservative estimate */ c.volume() * 2);
        try {
            c.appendTo(sb);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return sb;
    }

    Op op();

    /**
     * whether any subterms (recursively) have
     * non-DTernal temporal relation
     */
    @Override
    default boolean isTemporal() {
        return (dt() != DTERNAL && op().temporal)
                ||
                Term.super.isTemporal();
    }

    @Override
    default boolean containsRecursively(Term t, boolean root, Predicate<Term> inSubtermsOf) {
        return !impossibleSubTerm(t) && inSubtermsOf.test(this) && subterms().containsRecursively(t, root, inSubtermsOf);
    }

    /** deprecated; TODO move to SeparateSubtermsCompound interface and allow Compounds which do not have to generate this.  this sums up many of xjrn's suggestions  */
    @Override
    Subterms subterms();

    @Override
    default int hashCodeSubterms() {
        return subterms().hashCode();
    }


    @Override
    default int opX() {
        return Term.opX(op(), (short) volume());
    }

    @Override
    default void recurseTerms(Consumer<Term> v) {
        v.accept(this);
        subterms().recurseTerms(v);
    }


    
















    @Override
    default Term anon() {
        return new Anon(2).put(this);
    }

    @Override
    default boolean recurseTerms(Predicate<Term> aSuperCompoundMust, Predicate<Term> whileTrue, @Nullable Term parent) {
        return (!aSuperCompoundMust.test(this)) || (subterms().recurseTerms(aSuperCompoundMust, whileTrue, this));
    }


    @Override
    default boolean ORrecurse(Predicate<Term> p) {
        return p.test(this) || subterms().ORrecurse(p);
    }

    @Override
    default boolean ANDrecurse(Predicate<Term> p) {
        return p.test(this) && subterms().ANDrecurse(p);
    }











    default void appendTo(ByteArrayDataOutput out) {

        Op o = op();
        out.writeByte(o.id);
        subterms().append(out);
        if (o.temporal)
            out.writeInt(dt());

    }


    







    /**
     * unification matching entry point (default implementation)
     *
     * @param y compound to match against (the instance executing this method is considered 'x')
     * @param u the substitution context holding the match state
     * @return whether match was successful or not, possibly having modified subst regardless
     */
    @Override
    default boolean unify(/*@NotNull*/ Term y, /*@NotNull*/ Unify u) {
        return equals(y)
                ||
                (op() == y.op() && unifySubterms(y, u))
                ||
                y.unifyReverse(this, u);
    }

    default boolean unifySubterms(Term ty, Unify u) {
        if (!Terms.commonStructureTest(this, ty, u))
            return false;

        Subterms xsubs = subterms();
        Subterms ysubs = ty.subterms();

        int xs, ys;
        if ((xs = xsubs.subs()) != (ys = ysubs.subs()))
            return false;

        if (op().temporal) {
            
            int xdt = this.dt();
            int ydt = ty.dt();
            if (xdt!=ydt) {
                boolean xOrY;
                if (xdt == XTERNAL && ydt != XTERNAL) {
                    xOrY = false; 
                } else if (xdt != XTERNAL && ydt == XTERNAL) {
                    xOrY = true;
                } else {
                    if (xdt == DTERNAL && ydt != DTERNAL) {
                        xOrY = false;
                    } else if (xdt != DTERNAL && ydt == DTERNAL) {
                        xOrY = true;
                    } else {
                        return false; 
                    }
                }
            }

            if (xsubs.equals(ysubs))
                return true;
            
        }

        if (xs > 1 && isCommutative()) {
            
            boolean yCommutive = ty.isCommutative() && (ty.dt() != XTERNAL || ys != 2);
            return xsubs.unifyCommute(ysubs, yCommutive, u);
        } else {
            if (xs == 1) {
                return sub(0).unify(ysubs.sub(0), u);
            } else {
                return xsubs.unifyLinear(ysubs, u);
            }
        }
    }


    @Override
    default void appendTo(/*@NotNull*/ Appendable p) throws IOException {
        IO.Printer.append(this, p);
    }


    @Override
    default Term sub(int i, Term ifOutOfBounds) {
        return subterms().sub(i, ifOutOfBounds);
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
        Op op = op();
        if (op == CONJ) {
            int dt = dt();
            switch (dt) {
                case 0:
                case DTERNAL:
                case XTERNAL:
                    return true;
                
                default:
                    return false;
            }
        } else
            return op.commutative && subs() > 1;
    }






    @Override
    default void copyInto(/*@NotNull*/ Collection<Term> set) {
        subterms().copyInto(set);
    }






    @Override
    default boolean isNormalized() {
        return subterms().isNormalized();
    }

    /**
     * gets temporal relation value
     */
    @Override
    int dt();

    @Override
    default int eventCount() {
        return this.dt() != DTERNAL && op() == CONJ ? subterms().sum(Term::eventCount) : 1;
    }

    default Term replace(Term from, Term to) {
        if (this.equals(from))
            return to;
        if (!impossibleSubTerm(from)) {

            Subterms oldSubs = subterms();
            Subterms newSubs = oldSubs.replaceSubs(from, to);
            if (newSubs == null)
                return Null;
            if (!newSubs.equals(oldSubs)) {
                return op().the(dt(), (TermList) newSubs);
            }
        }
        return this;
    }

    default int subTimeOnly(Term x) {
        int[] t = subTimes(x);
        if (t == null || t.length != 1) return DTERNAL;
        return t[0];
    }

    /**
     * TODO return XTERNAL not DTERNAL on missing, it is more appropriate
     * expect the output array to be sorted
     */
    default int[] subTimes(Term x) {
        if (equals(x))
            return new int[] { 0 };


        int dt = dt();
        if (dt == XTERNAL || dt == DTERNAL)
            return null;

        Op op = op();
        if (op != CONJ)
            return null;

        if (impossibleSubTerm(x))
            return null;

        RoaringBitmap found = new RoaringBitmap();
        eventsWhile((when, what)->{
            if (what.equals(x)) {
                assert(when >= 0 && when < Integer.MAX_VALUE);
                found.add((int) when);
            }
            return true;
        }, 0, true, true, false, 0);

        return found.isEmpty() ? null : found.toArray();
    }


    @Override
    default Term dt(int nextDT) {
        return nextDT != dt() ? Op.dt(this, nextDT) : this;
    }










































































    /* collects any contained events within a conjunction*/
    @Override
    default boolean eventsWhile(LongObjectPredicate<Term> events, long offset, boolean decomposeConjParallel, boolean decomposeConjDTernal, boolean decomposeXternal, int level) {

        Op o = op();

        if (o == CONJ) {

            int dt = dt();
            boolean decompose = true;
            switch (dt) {
                case 0:
                    if (!decomposeConjParallel)
                        decompose = false;
                    break;
                case DTERNAL:
                    if (!decomposeConjDTernal)
                        decompose = false;
                    else
                        dt = 0;
                    break;
                case XTERNAL:
                    if (!decomposeXternal)
                        decompose = false;
                    else
                        dt = 0;
                    break;

            }

            if (decompose) {

                Subterms tt = subterms();
                int s = tt.subs();
                long t = offset;

                boolean changeDT = t != ETERNAL && t != TIMELESS;

                level++;

                boolean fwd  = dt >= 0;
                if (!fwd)
                    dt = -dt;

                for (int i = 0; i < s; i++) {
                    Term st = tt.sub(fwd ? i : (s-1)-i);
                    if (!st.eventsWhile(events, t,
                            decomposeConjParallel, decomposeConjDTernal, decomposeXternal,
                            level))
                        return false;

                    if (changeDT)
                        t += dt + st.dtRange();
                }


                return true;
            }

        }

        return events.accept(offset, this);
    }














    @Override
    default boolean hasXternal() {
        return dt() == XTERNAL ||
                subterms().hasXternal();
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

        Term y = new CompoundNormalization(
                this, varOffset).transformCompound(this);

//        LazyCompound yy = new LazyCompound();
//        new nars.util.term.transform.CompoundNormalization(this, varOffset)
//                .transform(this, yy);
//        Term y = yy.get();

        if (varOffset == 0 && y instanceof Compound) {
            y.subterms().setNormalized();
        }

        return y;
    }


    @Override
    default int dtRange() {
        Op o = op();
        switch (o) {

            case CONJ:
                Subterms tt = subterms();
                int l = tt.subs();
                if (l == 2) {
                    int dt = dt();

                    switch (dt) {
                        case DTERNAL:
                        case XTERNAL:
                        case 0:
                            dt = 0;
                            break;
                        default:
                            dt = Math.abs(dt);
                            break;
                    }

                    return tt.sub(0).dtRange() + (dt) + tt.sub(1).dtRange();

                } else {
                    int s = 0;


                    for (int i = 0; i < l; i++) {
                        s = Math.max(s, tt.sub(i).dtRange());
                    }

                    return s;
                }

            default:
                return 0;
        }

    }

    @Override
    @Nullable
    default Term temporalize(Retemporalize r) {
        return r.transformCompound(this);
    }

    /*@NotNull*/
    @Override
    default Term root() {
        return temporalize(Retemporalize.root);
    }


    @Override
    default Term concept() {

        Term term = unneg().root(); 

        Op op = term.op();
        assert (op != NEG): this + " concept() to NEG: " + unneg().root();
        if (!op.conceptualizable)
            return Null;


        Term term2 = term.normalize();
        if (term2 != term) {
            if (term2 == null)
                return Null;

            assert (term2.op() == op): term2 + " not a normal normalization of " + term;



            term = term2;
        }


        return term;
    }

    @Override
    default boolean equalsRoot(Term x) {
        if (this.equals(x))
            return true;

        
        if (
                opX() == x.opX()
                        &&
                        structure() == x.structure()
        ) {

            Term root = root();
            return (root != this && root.equals(x)) || root.equals(x.root());
        }

        return false;
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
        if (Terms.equals(term, replaced)) {
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















































































































































































