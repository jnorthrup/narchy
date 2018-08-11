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
import nars.Task;
import nars.The;
import nars.subterm.Subterms;
import nars.subterm.util.SubtermMetadataCollector;
import nars.term.anon.Anom;
import nars.term.atom.Atomic;
import nars.term.atom.Int;
import nars.term.var.NormalizedVariable;
import nars.time.Tense;
import nars.unify.Unify;
import nars.util.SoftException;
import nars.term.util.transform.MapSubst;
import nars.term.util.transform.Retemporalize;
import nars.term.util.transform.TermTransform;
import nars.term.util.transform.VariableTransform;
import org.eclipse.collections.api.block.predicate.primitive.LongObjectPredicate;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.eclipse.collections.impl.list.mutable.primitive.ByteArrayList;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
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

            if (s.subs() > 0) {
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
        return (maybeNull == null) ? Null : maybeNull;
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

    static Term forceNormalizeForBelief(Term x) {
        x = x.normalize();

        if (x.hasAny(Op.VAR_INDEP) && !Task.validTaskCompound(x, true)) {
            x = VariableTransform.indepToDepVar.transform(x);
        }

        return x;
    }

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

//    /** convenience method  do not override */
//    default boolean recurseTerms(Predicate<Term> descendFilter, Consumer<Term> each) {
//        return recurseTerms(descendFilter, (x) -> {
//            each.accept(x);
//            return true;
//        }, null);
//    }

//    /** convenience, do not override */
//    default boolean recurseTerms(Predicate<Compound> descendFilter, BiPredicate<Term,Compound> whileTrue) {
//        return recurseTerms(descendFilter, whileTrue, null);
//    }

//    /** convenience, do not override */
//    default boolean recurseTerms(BiPredicate<Term,Compound> whileTrue) {
//        return recurseTerms(x -> true, whileTrue, null);
//    }

    /** convenience, do not override */
    default void recurseTerms(BiConsumer<Term,Compound> each) {
        recurseTerms(x -> true, (sub,sup)-> {
            each.accept(sub,sup);
            return true;
        }, null);
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
        if (n == 0) return src;

        Term[] target = new Term[n];

        for (int i = 0; i < n; i++) {
            Term x = css.sub(i);
            if (path.get(depth) != i)
                target[i] = x;
            else {
                target[i] = x.subs() == 0 ? replacement : x.replaceAt(path, depth + 1, replacement);
            }

        }

        return src.op().the(src.dt(), target);
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
        return i == 0 ? this : subPath(i, subpaths.get(0));

    }

    @Nullable
    default Term subPath(ByteList path) {
        Term ptr = this;
        int s = path.size();
        for (int i = 0; i < s; i++)
            if ((ptr = ptr.subSafe(path.get(i))) == Null)
                return null;
        return ptr;
    }

    /**
     * extracts a subterm provided by the address tuple
     * returns null if specified subterm does not exist
     */
    @Nullable
    default Term subPath(byte... path) {
        int p = path.length;
        return p > 0 ? subPath(p, path) : this;
    }

    @Nullable
    default Term subPath(int subPathLen, byte... path) {
        Term ptr = this;
        for (int i = 0; i < subPathLen; i++) {
            if ((ptr = ptr.subSafe(path[i])) == Null)
                return null;
        }
        return ptr;
    }

    @Nullable
    default Term subPath(int subPathLen, ByteList path) {
        Term ptr = this;
        for (int i = 0; i < subPathLen; i++) {
            if ((ptr = ptr.subSafe(path.get(i))) == Null)
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
        return equals(y)
                ||
               (u.symmetric && y.unifyReverse(this, u));
    }

    /**
     * by default this has no effect by returning false
     */
    default boolean unifyReverse(Term x, Unify u) {
        return false;
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



    /**
     * total span across time represented by a sequence conjunction compound
     */
    default int dtRange() {
        return 0;
    }

    default boolean pathsTo(Term target, BiPredicate<ByteList, Term> receiver) {
        return pathsTo(
                x -> target.equals(x) ? x : null,
                x -> !x.impossibleSubTerm(target),
                receiver);
    }
    default boolean pathsTo(Term target, Predicate<Term> superTermFilter, BiPredicate<ByteList, Term> receiver) {
        return pathsTo(
                x -> target.equals(x) ? x : null,
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

        Term y = _y.term();
        return compareTo(y.term());
    }

    default int compareTo(Term t) {

        //if (this == y) return 0;
        if (this.equals(t)) return 0;


        int vc = Integer.compare(t.volume(), this.volume());
        if (vc != 0)
            return vc;

        Op op = this.op();
        int oc = Integer.compareUnsigned(op.id, t.op().id);
        if (oc != 0)
            return oc;


        if (this instanceof Atomic) {


            int h = Integer.compare(hashCode(), t.hashCode());
            if (h != 0)
                return h;

            if (this instanceof NormalizedVariable || this instanceof Int) {
                return 0;
            } else if (this instanceof Int.IntRange) {
                return Long.compareUnsigned(((Int.IntRange) this).hash64(), ((Int.IntRange) t).hash64());
            } else /*if (this instanceof Atomic)*/ {
                return Util.compare(
                        ((Atomic) this).bytes(),
                        ((Atomic) t).bytes()
                );
            }/* else {
                throw new UnsupportedOperationException("unimplemented comparison: " + this + ' ' + y);
            }*/


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

    /**
     * for safety, dont override this method. override evalSafe
     */

    default Term eval(Evaluation e, Function<Term, Functor> resolver, Random rng, boolean wrapBool) {
        if (!Evaluation.possiblyNeedsEval(this))
            return this;
        return Evaluation.solveAny(this, e, resolver, rng, wrapBool);
    }


    default Term eval(NAR nar, boolean wrapBool) {
        return eval(null, nar::functor, nar.random(), wrapBool);
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
        switch (m.size()) {
            case 0:
                return this;
            case 1: {
                Map.Entry<? extends Term, Term> e = m.entrySet().iterator().next();
                return replace(e.getKey(), e.getValue());
            }
            default:
                return new MapSubst(m).transform(this);
        }
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

    default void collectMetadata(SubtermMetadataCollector s) {

        int xstructure = structure();
        s.structure |= xstructure;

        if ((xstructure & VAR_PATTERN.bit) != 0)
            s.varPattern += varPattern();
        if ((xstructure & VAR_DEP.bit) != 0)
            s.varDep += varDep();
        if ((xstructure & VAR_INDEP.bit) != 0)
            s.varIndep += varIndep();
        if ((xstructure & VAR_QUERY.bit) != 0)
            s.varQuery += varQuery();

        s.vol += volume();
        s.hash = Util.hashCombine(s.hash, hashCode());
    }

    @Nullable default Term the() {
        if (this instanceof The)
            return this;
        else {
            return null;
            //throw new RuntimeException(getClass() + " does not support the()");
        }
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

    default boolean equalsNegRoot(Term t) {
        if (this == t) {
            return false;
        } else if (t.op() == NEG) {
            return equalsRoot(t.unneg());
        } else if (op() == NEG) {
            return unneg().equalsRoot(t);
        } else {
            return false;
        }
    }

    /** returns subterms transformed by the provided transform  */
    default Subterms subterms(TermTransform termTransform) {
        return subterms().transformSubs(termTransform);
    }


    /**
     * Created by me on 2/26/16.
     */
    final class InvalidTermException extends SoftException {


        private final Op op;
        private final int dt;

        private final Term[] args;

        private final String reason;


        public InvalidTermException(Op op, Term[] args, String reason) {
            this(op, DTERNAL, reason, args);
        }

        public InvalidTermException(Op op, int dt, Term[] args, String reason) {
            this(op, dt, reason, args);
        }

        public InvalidTermException(Op op, int dt, Termlike args, String reason) {
            this(op, dt, reason, args.arrayShared());
        }

        public InvalidTermException(Op op, int dt, String reason, Term... args) {
            this.op = op;
            this.dt = dt;
            this.args = args;
            this.reason = reason;
        }

//        public InvalidTermException(String s, Compound c) {
//            this(c.op(), c.dt(), c.subterms(), s);
//        }

        @NotNull
        @Override
        public String getMessage() {
            return getClass().getSimpleName() + ": " + reason + " {" +
                    op +
                    ", dt=" + dt +
                    ", args=" + Arrays.toString(args) +
                    '}';
        }

    }
}

