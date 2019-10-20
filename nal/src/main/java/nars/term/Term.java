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


import jcog.TODO;
import jcog.Util;
import jcog.data.set.MetalTreeSet;
import nars.Op;
import nars.Idempotent;
import nars.subterm.Subterms;
import nars.term.anon.Anom;
import nars.term.anon.IntrinAtomic;
import nars.term.atom.Atomic;
import nars.term.atom.theBool;
import nars.term.atom.theInt;
import nars.term.util.builder.TermBuilder;
import nars.term.util.conj.Conj;
import nars.term.util.transform.MapSubst;
import nars.term.util.transform.TermTransform;
import nars.time.Tense;
import nars.unify.Unify;
import org.eclipse.collections.api.block.predicate.primitive.LongObjectPredicate;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.eclipse.collections.impl.list.mutable.primitive.ByteArrayList;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.function.*;

import static nars.Op.CONJ;
import static nars.time.Tense.DTERNAL;


/**
 * The meaning "word or phrase used in a limited or precise sense"
 * is first recorded late 14c..
 * from Medieval Latin use of terminus to render Greek horos "boundary,"
 * employed in mathematics and logic.
 * Hence in terms of "in the language or phraseology peculiar to."
 * https://www.etymonline.com/word/term
 */
public interface Term extends Termlike, Termed, Comparable<Term> {

    static <X> boolean pathsTo(Term that, ByteArrayList p, Predicate<Term> descendIf, Function<Term, X> subterm, BiPredicate<ByteList, X> receiver) {
        if (!descendIf.test(that))
            return true;

        if (!(that instanceof Compound))
            return false;

        var superTerm = ((Compound)that).subtermsDirect();

        var ppp = p.size();

        var n = superTerm.subs();
        for (var i = 0; i < n; i++) {

            p.add((byte) i);

            var s = superTerm.sub(i);

            var kontinue = true;
            var ss = subterm.apply(s);
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
        return (maybeNull == null) ? theBool.Null : maybeNull;
    }

    /**
     * true if there is at least some type of structure in common
     */
    static boolean commonStructure(Termlike x, Termlike y) {
        return commonStructure(x.structure(), y.structure());
    }

    static boolean commonStructure(int xStruct, int yStruct) {
        return xStruct==yStruct || ((xStruct & yStruct) != 0);
    }



    default Term term() {
        return this;
    }

    Op op();

    default   int opBit() {
        return 1<<opID();
    }

    default int opID() {
        return op().id;
    }


    @Override
    boolean equals(Object o);


    @Override
    int hashCode();

    /**
     * parent compounds must pass the descent filter before ts subterms are visited;
     * but if descent filter isnt passed, it will continue to the next sibling:
     * whileTrue must remain true after vistiing each subterm otherwise the entire
     * iteration terminates
     *
     * implementations are not obligated to visit in any particular order, or to repeat visit a duplicate subterm
     * for that, use recurseTermsOrdered(..)
     */
    boolean recurseTerms(Predicate<Term> inSuperCompound, Predicate<Term> whileTrue, @Nullable Compound /* Compound? */superterm);

    boolean recurseTermsOrdered(Predicate<Term> inSuperCompound, Predicate<Term> whileTrue, Compound parent);

    /** TODO move to Subterms */
    @Deprecated default  boolean containsRecursively(Term t) {
        return containsRecursively(t, null);
    }

    /** TODO move to Subterms */
    @Deprecated default   boolean containsRecursively(Term t, @Nullable Predicate<Term> inSubtermsOf) {
        return containsRecursively(t, false, inSubtermsOf);
    }



    /**
     * if root is true, the root()'s of the terms will be compared
     * TODO move to Subterms
     */
    @Deprecated boolean containsRecursively(Term t, boolean root, @Nullable Predicate<Term> inSubtermsOf);

    Term transform(TermTransform t);
//    Term transform(TermTransform t, @Nullable TermBuffer b, int volMax);

	default int hashCodeShort() {
        var h = hashCode();
	    return ((h & 0xffff) ^ (h >>> 16));
    }

	enum TermWalk {
        Left, //prev subterm
        Right, //next subterm
        Down, //descend, recurse, or equivalent to Right if atomic
        Stop //CUT
    }
    default boolean recurseTerms(BiFunction<Compound,Term,TermWalk> whileTrue, Compound superterm) {
        throw new TODO();
    }

    /**
     * convenience, do not override (except in Atomic)
     */
    default   boolean recurseTermsOrdered(Predicate<Term> whileTrue) {
        return recurseTermsOrdered(x->true, whileTrue, null);
    }

    /**
     * whileTrue = BiPredicate<SubTerm,SuperTerm>
     * implementations are not obligated to visit in any particular order, or to repeat visit a duplicate subterm
     */
    boolean recurseTerms(Predicate<Compound> aSuperCompoundMust, BiPredicate<Term, Compound> whileTrue, @Nullable Compound superterm);





    /**
     * convenience, do not override (except in Atomic)
     */
    default void recurseTerms(BiConsumer<Term, Compound> each) {
        recurseTerms(x -> true, (sub, sup) -> {
            each.accept(sub, sup);
            return true;
        }, null);
    }

    /**
     * convenience, do not override (except in Atomic)
     */
    default void recurseTerms(Consumer<Term> each) {
        recurseTerms(a -> true, (sub) -> {
            each.accept(sub);
            return true;
        }, null);
    }





    @Override
    Term sub(int i);


    @Override
    int subs();


    default @Nullable Term replaceAt(ByteList path, Term replacement) {
        return replaceAt(path, 0, replacement);
    }

    default @Nullable Term replaceAt(ByteList path, int depth, Term replacement) {
        var ps = path.size();
        if (ps == depth)
            return replacement;
        if (ps < depth)
            throw new RuntimeException("path overflow");

        var src = (Compound) this;
        var css = src.subtermsDirect();

        var n = css.subs();

        var which = path.get(depth);
        assert (which < n);

        var x = css.sub(which);
        var y = x.replaceAt(path, depth + 1, replacement);
        if (y == x) {
            return src; //unchanged
        } else {
            var target = css.arrayClone();
            target[which] = y;
            return src.op().the(src.dt(), target);
        }
    }

    default boolean pathsTo(Predicate<Term> selector, Predicate<Term> descendIf, BiPredicate<ByteList, Term> receiver) {
        return pathsTo((UnaryOperator<Term>) (x) -> selector.test(x) ? x : null, descendIf, receiver);
    }


    default <X> boolean pathsTo(Function<Term, X> target, Predicate<Term> descendIf, BiPredicate<ByteList, X> receiver) {
        var ss = target.apply(this);
        if (ss != null && !receiver.test(Util.EmptyByteList, ss))
            return false;

        return this.subs() <= 0 ||
                pathsTo(this, new ByteArrayList(0), descendIf, target, receiver);
    }

    default Term commonParent(List<ByteList> subpaths) {
        var subpathsSize = subpaths.size(); //assert (subpathsSize > 1);

        var shortest = Integer.MAX_VALUE;
        for (var subpath : subpaths)
            shortest = Math.min(shortest, subpath.size());


        int i;
        done:
        for (i = 0; i < shortest; i++) {
            byte needs = 0;
            for (var j = 0; j < subpathsSize; j++) {
                var pi = subpaths.get(j).get(i);
                if (j == 0) {
                    needs = pi;
                } else if (needs != pi) {
                    break done;
                }
            }

        }
        return i == 0 ? this : subPath(subpaths.get(0), 0, i);

    }

    default @Nullable Term subPath(ByteList path) {
        var p = path.size();
        return p > 0 ? subPath(path, 0, p) : this;
    }

    /**
     * extracts a subterm provided by the address tuple
     * returns null if specified subterm does not exist
     */
    default @Nullable Term subPath(byte... path) {
        var p = path.length;
        return p > 0 ? subPath(0, p, path) : this;
    }

    default @Nullable Term subPath(int start, int end, byte... path) {
        var ptr = this;
        for (var i = start; i < end; i++) {
            if ((ptr = ptr.subSafe(path[i])) == theBool.Null)
                return null;
        }
        return ptr;
    }

    default @Nullable Term subPath(ByteList path, int start, int end) {
        var ptr = this;
        for (var i = start; i < end; i++) {
            if ((ptr = ptr.subSafe(path.get(i))) == theBool.Null)
                return null;
        }
        return ptr;
    }


    /**
     * Commutivity in NARS means that a Compound target's
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
     * @param y       another target
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
        return (opBit() & bitsetOfOperators) != 0;
    }


    void appendTo(Appendable w) throws IOException;

    default StringBuilder appendTo(StringBuilder s) {
        try {
            appendTo((Appendable)s);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return s;
    }

    default String structureString() {
        return String.format("%16s",
                Op.strucTerm(structure()))
                .replace(" ", "0");
    }

    default boolean isNormalized() {
        return true;
    }

//    /**
//     * computes the occurrence times of an event within a compound.
//     * if equals or is the first event only, it will be [0]
//     * null if not contained or indeterminate (ex: XTERNAL)
//     */
//    @Nullable
//    @Deprecated default int[] subTimes(Term x) {
//        int t = subTimeOnly(x);
//        return t == DTERNAL ? null : new int[]{t};
//    }
//
//    /**
//     * returns the unique sub-event time of the given target,
//     * or DTERNAL if not present or there is not one unique time.
//     */
//    @Deprecated default int subTimeOnly(Term x) {
//        return equals(x) ? 0 : DTERNAL;
//    }

    /**
     * returns DTERNAL if not found
     */
    default int subTimeFirst(Term x) {
        int[] time = { DTERNAL };
        subTimesWhile(x, (w) -> {
            time[0] = w; //got it
            return false; //stop
        });
        return time[0];
    }

    /**
     * returns DTERNAL if not found
     * TODO optimize traversal
     */
    default int subTimeLast(Term x) {
        int[] time = { DTERNAL };
        subTimesWhile(x, (w) -> {
            time[0] = Math.max(time[0], w); //got it
            return true; //keep going
        });
        return time[0];
    }

    /**
     * TODO make generic Predicate<Term> selector
     * TODO move down to Compound, provide streamlined Atomic impl
     */
    default boolean subTimesWhile(Term match, IntPredicate each) {
        if (equals(match))
            return each.test(0);


        if (op() == CONJ) {
            if (Conj.isSeq(this)) {
//                final int[] hits = {0};
                eventsAND((when, what) -> {
                    if (what.equals(match)) {
//                        hits[0]++;
                        return each.test(Tense.occToDT(when));
                    } else {
                        if (Term.this != what && what.op() == CONJ) { //HACK unwrap this better to avoid unnecessary recursion
                            var subWhen = what.subTimeFirst(match);
                            if (subWhen != DTERNAL) {
//                                hits[0]++;
                                return each.test(Tense.occToDT(when + subWhen));
                            }
                        }
                    }
                    return true;
                }, 0, match.op()!=CONJ || match.dt()!=DTERNAL, true);
                return true;
            } else {
                if (contains(match))
                    return each.test(0);
            }
        }
        return true;
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


    default int compareTo(Term t) {
        if (this == t) return 0;

        boolean a = this instanceof Atomic, b = t instanceof Atomic;
        if (a && !b) {
            return +1;
        } else if (b && !a) {
            return -1;
        } else if (!b) {
            var vc = Integer.compare(t.volume(), this.volume());
            if (vc != 0)
                return vc;
        }

        var op = this.op();
        var oc = Integer.compare(op.id, t.opID());
        if (oc != 0)
            return oc;

        if (a) {

            if (this instanceof theInt /*&& t instanceof Int*/)
                return Integer.compare(((theInt) this).i, ((theInt) t).i);

            if (this instanceof IntrinAtomic && t instanceof IntrinAtomic)
                return Integer.compare(hashCode(), t.hashCode()); //same op, same hashcode


            return Util.compare(
                ((Atomic) this).bytes(),
                ((Atomic) t).bytes()
            );

        } else {
            //COMPOUND

            var c = Subterms.compare(
                ((Compound)this).subtermsDirect(),
                ((Compound)t).subtermsDirect()
            );
            return c != 0 ? c : (op.temporal ? Integer.compare(dt(), t.dt()) : 0);
        }
    }

    Subterms subterms();

    /**
     * unwraps negation - only changes if this term is NEG
     */
    default Term unneg() {
        return this;
    }

    default boolean eventsAND(LongObjectPredicate<Term> each, long offset,
                              boolean decomposeConjDTernal, boolean decomposeXternal) {
        return each.accept(offset, this);
    }
    default boolean eventsOR(LongObjectPredicate<Term> each, long offset,
                              boolean decomposeConjDTernal, boolean decomposeXternal) {
        return each.accept(offset, this);
    }

    default void printRecursive() {
        printRecursive(System.out);
    }

    default void printRecursive(PrintStream out) {
        Terms.printRecursive(out, this);
    }

    /**
     * returns this target in a form which can identify a concept, or Null if it can't
     * generally this is equivalent to root() but for compound it includes
     * unnegation and normalization steps. this is why conceptual() and root() are
     * different.  usually 'this'
     */
    Term concept();

    /**
     * the skeleton of a target, without any temporal or other meta-assumptions
     */
    Term root();


    /** TODO make Compound only */
    default int dt() {
        return DTERNAL;
    }

    Term normalize(byte offset);

      default   Term normalize() {
        return normalize((byte) 0);
    }


    default Term replace(Map<? extends Term, Term> m) {
        return MapSubst.replace(this, m);
    }

    Term replace(Term from, Term to);

    default Term neg() {
        return TermBuilder.neg(this);
    }

    default Term negIf(boolean negate) {
        return negate ? neg() : this;
    }

    default Term anon() {
        return Anom.the(1);
    }

//    int structure();

    default boolean the() {
        return this instanceof Idempotent;
    }

    @Override
    default boolean these() {
        return the();
    }

    boolean equalsRoot(Term x);

    default boolean equalsPosOrNeg(Term t) {
        return equals(t.unneg());
    }

    default boolean equalsNeg(Term t) {
        if (this == t) {
            return false;
        } else if (t instanceof Neg) {
            return equals(t.unneg());
        } else if (this instanceof Neg) {
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

    default SortedSet<Term> eventSet() {
        assert (op() == CONJ);
        MetalTreeSet<Term> s = new MetalTreeSet();
        eventsAND((when, what) -> {
            if (what != Term.this)
                s.add(what);
            return true;
        }, 0, true, true);
        return s;
    }


    default Term eventFirst() {
        return this;
    }

    default Term eventLast() {
        return this;
    }



}

