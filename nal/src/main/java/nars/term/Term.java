/*
 * Term.java
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
import jcog.Util;
import jcog.data.list.FasterList;
import nars.NAR;
import nars.Op;
import nars.The;
import nars.eval.Evaluation;
import nars.subterm.Subterms;
import nars.term.anon.Anom;
import nars.term.anon.AnonID;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.util.transform.MapSubst;
import nars.term.util.transform.Retemporalize;
import nars.time.Tense;
import nars.unify.Unify;
import org.eclipse.collections.api.block.predicate.primitive.LongObjectPredicate;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.eclipse.collections.impl.list.mutable.primitive.ByteArrayList;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.function.*;

import static nars.Op.*;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;


/**
 * The meaning "word or phrase used in a limited or precise sense"
 * is first recorded late 14c..
 * from Medieval Latin use of terminus to render Greek horos "boundary,"
 * employed in mathematics and logic.
 * Hence in terms of "in the language or phraseology peculiar to."
 * https://www.etymonline.com/word/term
 */
public interface Term extends Termlike, Termed, Comparable<Termed> {

    static <X> boolean pathsTo(Term that, ByteArrayList p, Predicate<Term> descendIf, Function<Term, X> subterm, BiPredicate<ByteList, X> receiver) {
        if (!descendIf.test(that))
            return true;

        Subterms superTerm = that.subterms();

        int ppp = p.size();

        int n = superTerm.subs();
        for (int i = 0; i < n; i++) {

            p.add((byte) i);

            Term s = superTerm.sub(i);

            boolean kontinue = true;
            X ss = subterm.apply(s);
            if (ss != null) {
                if (!receiver.test(p, ss))
                    kontinue = false;
            }

            if (s instanceof Compound) {//(s.subs() > 0) {
                if (!pathsTo(s, p, descendIf, subterm, receiver))
                    kontinue = false;
            }

            p.removeAtIndex(ppp);

            if (!kontinue)
                return false;
        }

        return true;
    }

    static Term nullIfNull(@Nullable Term maybeNull) {
        return (maybeNull == null) ? Bool.Null : maybeNull;
    }

    /**
     * opX function
     */
    static int opX(Op o, short subOp) {
        return (o.id << 16) | (subOp);
    }

    /**
     * for convenience, delegates to the byte function
     */
    @Deprecated
    static int opX(Op o, int subOp) {
        assert (subOp < Short.MAX_VALUE - 1);
        return opX(o, (short) subOp);
    }

    /** true if there is at least some type of structure in common */
    static boolean commonStructure(Termlike x, Termlike y) {
        int xStruct = x.structure();
        int yStruct = y.structure();
        return (xStruct & yStruct) != 0;
    }

    default Term term() {
        return this;
    }

    Op op();

    void appendTo(ByteArrayDataOutput out);

    @Override
    boolean equals(Object o);


    @Override
    int hashCode();

    /**
     * parent compounds must pass the descent filter before ts subterms are visited;
     * but if descent filter isnt passed, it will continue to the next sibling:
     * whileTrue must remain true after vistiing each subterm otherwise the entire
     * iteration terminates
     */
    default boolean recurseTerms(Predicate<Term> descendFilter, Predicate<Term> whileTrue, @Nullable Term /* Compound? */superterm) {
        return whileTrue.test(this);
    }


    /** whileTrue = BiPredicate<SubTerm,SuperTerm> */
    default boolean recurseTerms(Predicate<Compound> descendFilter, BiPredicate<Term,Compound> whileTrue, @Nullable Compound superterm) {
        return whileTrue.test(this, superterm);
    }

    /** convenience, do not override */
    default void recurseTerms(BiConsumer<Term,Compound> each) {
        recurseTerms(x -> true, (sub,sup)-> {
            each.accept(sub,sup);
            return true;
        }, null);
    }

    /** do not override */
    default void recurseTerms(Consumer<Term> each) {
        recurseTerms(a -> true, (sub)-> { each.accept(sub); return true; }, null);
    }

    default boolean hasXternal() {
        return (dt() == XTERNAL) || Termed.super.hasXternal();
    }

    @Override
    Term sub(int i);


    @Override
    int subs();


    @Nullable
    default Term replaceAt(ByteList path, Term replacement) {
        return replaceAt(path, 0, replacement);
    }

    @Nullable
    default Term replaceAt(ByteList path, int depth, Term replacement) {
        final Term src = this;
        int ps = path.size();
        if (ps == depth)
            return replacement;
        if (ps < depth)
            throw new RuntimeException("path overflow");

        Subterms css = src.subterms();

        int n = css.subs();

        byte which = path.get(depth);
        assert(which < n);

        Term x = css.sub(which);
        Term y = x.replaceAt(path, depth + 1, replacement);
        if (y == x) {
            return src; //unchanged
        } else {
            Term[] target = css.arrayClone();
            target[which] = y;
            return src.op().the(src.dt(), target);
        }
    }

    default boolean pathsTo(Predicate<Term> selector, Predicate<Term> descendIf, BiPredicate<ByteList, Term> receiver) {
        return pathsTo((Function<Term,Term>)(Term x) -> selector.test(x) ? x : null, descendIf, receiver);
    }


    default <X> boolean pathsTo(Function<Term, X> target, Predicate<Term> descendIf, BiPredicate<ByteList, X> receiver) {
        X ss = target.apply(this);
        if (ss != null && !receiver.test(Util.EmptyByteList, ss))
            return false;

        return this.subs() <= 0 ||
                pathsTo(this, new ByteArrayList(0), descendIf, target, receiver);
    }

    @Nullable
    default Term commonParent(List<ByteList> subpaths) {
        int subpathsSize = subpaths.size();
        assert (subpathsSize > 1);

        int shortest = Integer.MAX_VALUE;
        for (ByteList subpath: subpaths) {
            shortest = Math.min(shortest, subpath.size());
        }


        int i;
        done:
        for (i = 0; i < shortest; i++) {
            byte needs = 0;
            for (int j = 0; j < subpathsSize; j++) {
                byte pi = subpaths.get(j).get(i);
                if (j == 0) {
                    needs = pi;
                } else if (needs != pi) {
                    break done;
                }
            }

        }
        return i == 0 ? this : sub(subpaths.get(0), 0, i);

    }

    @Nullable
    default Term sub(ByteList path) {
        int p = path.size();
        return p > 0 ? sub(path, 0, p) : this;
    }

    /**
     * extracts a subterm provided by the address tuple
     * returns null if specified subterm does not exist
     */
    @Nullable
    default Term sub(byte... path) {
        int p = path.length;
        return p > 0 ? sub(0, p, path) : this;
    }

    @Nullable
    default Term sub(int start, int end, byte... path) {
        Term ptr = this;
        for (int i = start; i < end; i++) {
            if ((ptr = ptr.subSafe(path[i])) == Bool.Null)
                return null;
        }
        return ptr;
    }

    @Nullable
    default Term sub(ByteList path, int start, int end) {
        Term ptr = this;
        for (int i = start; i < end; i++) {
            if ((ptr = ptr.subSafe(path.get(i))) == Bool.Null)
                return null;
        }
        return ptr;
    }


    /**
     * Commutivity in NARS means that a Compound term's
     * subterms will be unique and arranged in order (compareTo)
     * <p>
     * <p>
     * commutative CompoundTerms: Sets, Intersections Commutative Statements:
     * Similarity, Equivalence (except the one with a temporal order)
     * Commutative CompoundStatements: Disjunction, Conjunction (except the one
     * with a temporal order)
     *
     * @return The default value is false
     */
    boolean isCommutative();


    /**
     * @param y       another term
     * @param ignored the unification context
     * @return whether unification succeeded
     */
    default boolean unify(Term y, Unify u) {
        return y instanceof Variable ? y.unify(this, u) : equals(y);
    }


    /**
     * true if the operator bit is included in the enabld bits of the provided vector
     */
    default boolean isAny(int bitsetOfOperators) {
        int s = op().bit;
        return (bitsetOfOperators & s) != 0;
    }

    void appendTo(Appendable w) throws IOException;

    default String structureString() {
        return String.format("%16s",
                Op.strucTerm(structure()))
                .replace(" ", "0");
    }

    default boolean isNormalized() {
        return true;
    }

    /**
     * computes the occurrence times of an event within a compound.
     * if equals or is the first event only, it will be [0]
     * null if not contained or indeterminate (ex: XTERNAL)
    */
    @Nullable
    default int[] subTimes(Term x) {
        int t = subTimeOnly(x);
        return t == DTERNAL ? null : new int[]{t};
    }

    /** returns the unique sub-event time of the given term,
     * or DTERNAL if not present or there is not one unique time. */
    default int subTimeOnly(Term x) {
        return equals(x) ? 0 : DTERNAL;
    }

    /** returns DTERNAL if not found */
    default int subTimeFirst(Term x) {
        final int[] time = new int[1];
        if (subTimesWhile(x, (w)->{
            //got it
            time[0] = w;
            return false;
        }) > 0) {
            return time[0];
        }
        return DTERNAL;
    }

    /**
     *
     * TODO make generic Predicate<Term> selector
     * TODO move down to Compound, provide streamlined Atomic impl */
    default int subTimesWhile(Term match, IntPredicate kontinue) {
        final int[] hits = {0};
        eventsWhile((when, what)->{
            if (what.equals(match)) {
                hits[0]++;
                assert(when >= 0 && when < Integer.MAX_VALUE);
                return kontinue.test((int)when);
            }
            return true;
        }, 0, true, true, false, 0);
        return hits[0];
    }




    /**
     * total span across time represented by a sequence conjunction compound
     */
    default int eventRange() {
        return 0;
    }

    default boolean pathsTo(Term target, BiPredicate<ByteList, Term> receiver) {
        return pathsTo(
                target::equals,
                x -> !x.impossibleSubTerm(target),
                receiver);
    }

    default boolean pathsTo(Term target, Predicate<Term> superTermFilter, BiPredicate<ByteList, Term> receiver) {
        return pathsTo(
                target::equals,
                x -> superTermFilter.test(x) && !x.impossibleSubTerm(target),
                receiver);
    }

    /**
     * operator extended:
     * operator << 8 | sub-operator type rank for determing compareTo ordering
     */
    int opX();

    /**
     * GLOBAL TERM COMPARATOR FUNCTION
     */
    @Override
    default int compareTo(Termed _y) {
        if (this == _y) return 0;

        return compareTo(_y.term().term());
    }

    default int compareTo(Term t) {

        //if (this == y) return 0;
        if (this.equals(t)) return 0;


        int volume = t.volume();
        int vc = Integer.compare(volume, this.volume());
        if (vc != 0)
            return vc;

        Op op = this.op();
        int oc = Integer.compare(op.id, t.op().id);
        if (oc != 0)
            return oc;

        if (volume == 1) {
            assert(this instanceof Atomic);

            if (this instanceof AnonID) {
                if (t instanceof AnonID){
                    return Integer.compare(hashCode(), t.hashCode()); //same op, same hashcode
                } else {
                    return -1;
                }
            } else {
                if (t instanceof AnonID)
                    return +1;
            }

            return Util.compare(
                    ((Atomic) this).bytes(),
                    ((Atomic) t).bytes()
            );

        } else {

            int c = Subterms.compare(subterms(), t.subterms());
            return c != 0 ? c : (op.temporal ? Integer.compare(dt(), t.dt()) : 0);
        }
    }

    default Subterms subterms() {
        return EmptySubterms;
    }

    /**
     * unwraps any negation superterm
     */
    default Term unneg() {
        return this;
    }


    @Deprecated default Term eval(NAR nar) {
        Term y = Evaluation.solveFirst(this, nar);
        return y == null ? this : y;
    }


    /**
     * includes itself in the count unless it's a CONJ sequence in which case it becomes the sum of the subterms event counts
     */
    default int eventCount() {
        return 1;
    }


    /* collects any contained events */
    @Deprecated
    default void events(Consumer<LongObjectPair<Term>> events) {
        eventsWhile((w, t) -> {
            events.accept(PrimitiveTuples.pair(w, t));
            return true;
        }, 0);
    }

    default FasterList<LongObjectPair<Term>> eventList(long offset, int dtDither) {
        return eventList(offset, dtDither, true, false);
    }

    /**
     * sorted by time; decomposes inner parallel conj
     * TODO make sorting optional
     */
    default FasterList<LongObjectPair<Term>> eventList(long offset, int dtDither, boolean decomposeParallel, boolean decomposeEternal) {
        FasterList<LongObjectPair<Term>> events = new FasterList(2);
        eventsWhile((w, t) -> {
            events.add(PrimitiveTuples.pair(
                    (dtDither > 1) ? Tense.dither(w, dtDither) : w,
                    t));
            return true;
        }, offset, decomposeParallel, decomposeEternal, false, 0);
        if (events.size() > 1) {
            events.sortThisByLong(LongObjectPair::getOne);
        }
        return events;
    }

    /**
     * event list, sorted by time
     * sorted by time; decomposes inner parallel conj
     */
    /* final */
    @Deprecated default FasterList<LongObjectPair<Term>> eventList() {
        return eventList(0, 1);
    }

    /* final */ /** conj events */
    default boolean eventsWhile(LongObjectPredicate<Term> whileEachEvent, long dt) {
        return eventsWhile(whileEachEvent, dt, true, false, false, 0);
    }

    default boolean eventsWhile(LongObjectPredicate<Term> whileEachEvent, long dt,
                                boolean decomposeConjParallel, boolean decomposeConjDTernal, boolean decomposeXternal, int level) {
        return whileEachEvent.accept(dt, this);
    }

//    /** recursively visits all conj and impl sub-conditions */
//    default boolean conditionsWhile(Predicate<Term> each) {
//
//        if (hasAny(Op.Conditional))
//            return each.test(this);  //short-cut, just this
//
//        return eventsWhile((w, what) -> {
//            if (!each.test(what))
//                return false;
//
//            what = what.unneg();
//
//            if (what.op()==IMPL) {
//                if (!each.test(what.sub(0)))
//                    return false;
//                if (!each.test(what.sub(1)))
//                    return false;
//            }
//
//            return true;
//        }, 0,true, true, true, 0);
//    }

//    default void conditionsEach(Consumer<Term> each) {
//        conditionsWhile((e)->{
//            each.accept(e);
//            return true;
//        });
//    }

    default void printRecursive() {
        printRecursive(System.out);
    }

    default void printRecursive(PrintStream out) {
        Terms.printRecursive(out, this);
    }

    default Term dt(int dt) {


        return this;
    }


    /**
     * returns this term in a form which can identify a concept, or Null if it can't
     * generally this is equivalent to root() but for compound it includes
     * unnegation and normalization steps. this is why conceptual() and root() are
     * different
     */
    default Term concept() {
        return this;
    }

    /**
     * the skeleton of a term, without any temporal or other meta-assumptions
     */
    default Term root() {
        return this;
    }

    default boolean equalsRoot(Term x) {
        return root().equals(x.root());
    }


    default int dt() {
        return DTERNAL;
    }

    default Term normalize(byte offset) {
        return this;
    }

    @Nullable
    default Term normalize() {
        return normalize((byte) 0);
    }


    @Nullable
    default Term replace(Map<? extends Term, Term> m) {
        return MapSubst.replace(this, m);
    }

    Term replace(Term from, Term to);

    default Term neg() {
        //return Neg.the(this);
        return NEG.the(this);
    }

    default Term negIf(boolean negate) {
        return negate ? neg() : this;
    }

    @Nullable
    @Deprecated
    Term temporalize(Retemporalize r);


    default Term anon() {
        return Anom.the(1);
    }

    @Override
    default int structure() {
        return Termed.super.structure() | op().bit;
    }

    default boolean the() {
        return this instanceof The;
    }

    default boolean equalsNeg(Term t) {
        if (this == t) {
            return false;
        } else if (t.op() == NEG) {
            return equals(t.unneg());
        } else if (op() == NEG) {
            return unneg().equals(t);
        } else {
            return false;
        }
    }

//    default boolean equalsNegRoot(Term t) {
//        if (this == t) {
//            return false;
//        } else if (t.op() == NEG) {
//            return equalsRoot(t.unneg());
//        } else if (op() == NEG) {
//            return unneg().equalsRoot(t);
//        } else {
//            return false;
//        }
//    }

    default MutableSet<Term> eventSet() {
        assert(op()==CONJ);
        MutableSet<Term> s = new UnifiedSet<>();
        eventsWhile((when, what)->{
            if (what!=Term.this)
                s.add(what);
            return true;
        }, 0, true, true, true, 0);
        return s;
    }



}

