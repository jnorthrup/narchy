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
 * along with Open-NARS.  If not, see <http://www.gnu.org/licenses/>.
 */
package nars.term;


import com.google.common.io.ByteArrayDataOutput;
import jcog.Util;
import jcog.list.FasterList;
import nars.*;
import nars.op.mental.AliasConcept;
import nars.subterm.Neg;
import nars.subterm.Subterms;
import nars.subterm.TermVector;
import nars.subterm.util.TermMetadata;
import nars.term.anon.Anom;
import nars.term.atom.Atomic;
import nars.term.atom.Int;
import nars.term.var.NormalizedVariable;
import nars.term.var.Variable;
import nars.unify.Unify;
import nars.util.SoftException;
import nars.util.term.transform.MapSubst;
import nars.util.term.transform.Retemporalize;
import nars.util.term.transform.Subst;
import nars.util.term.transform.TermTransform;
import nars.util.time.Tense;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.block.function.primitive.IntObjectToIntFunction;
import org.eclipse.collections.api.block.predicate.primitive.LongObjectPredicate;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.eclipse.collections.api.list.primitive.ImmutableByteList;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.eclipse.collections.impl.factory.primitive.ByteLists;
import org.eclipse.collections.impl.list.mutable.primitive.ByteArrayList;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static nars.Op.*;
import static nars.util.time.Tense.DTERNAL;
import static nars.util.time.Tense.XTERNAL;


public interface Term extends Termed, Comparable<Termed> {


    //@NotNull public static final int[] ZeroIntArray = new int[0];
    Term[] EmptyArray = new Term[0];
    ImmutableByteList EmptyByteList = ByteLists.immutable.empty();

    @Override
    default Term term() {
        return this;
    }


    /*@NotNull*/
    @Override
    Op op();

    @Override
    int volume();

    @Override
    int complexity();

//    @Override
//    int varPattern();
//
//    @Override
//    int varQuery();
//
//    @Override
//    int varIndep();
//
//    @Override
//    int varDep();

    @Override
    int structure();

    @Override
    boolean contains(Term t);

    boolean containsRoot(Term t);

    void append(ByteArrayDataOutput out);

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
    default boolean recurseTerms(Predicate<Term> descendFilter, Predicate<Term> whileTrue, Term parent) {
        return whileTrue.test(this);
    }

    @Override
    default int intifyRecurse(IntObjectToIntFunction<Term> reduce, int v) {
        return reduce.intValueOf(v, this);
    }

    //    default boolean recurseTerms(Predicate<Term> parentsMust, Predicate<Term> whileTrue) {
//        return recurseTerms(parentsMust, whileTrue, this);
//    }

    /**
     * whether this term is or contains, as subterms, any temporal terms
     */
    boolean isTemporal();

    /**
     * whether this term contains any XTERNAL relations
     */
    default boolean hasXternal() {
        if (dt() == XTERNAL) return true;

        Subterms xs = subterms();
        return xs.isTemporal() && xs.OR(Term::hasXternal);
    }

    /**
     * returns an int[] path to the first occurrence of the specified subterm
     *
     * @return null if not a subterm, an empty int[] array if equal to this term, or a non-empty int[] array specifying subterm paths to reach it
     */
    @Nullable
    default byte[] pathTo(/*@NotNull*/ Term subterm) {
        if (subterm.equals(this)) return ArrayUtils.EMPTY_BYTE_ARRAY;
        //if (!containsRecursively(subterm)) return null;
        return
                this instanceof Compound && !impossibleSubTerm(subterm) ?
                        pathTo(new ByteArrayList(0), this.subterms(), subterm) : null;
    }

    @Nullable default Term transform(TermTransform t) {
        Termed y = t.transformAtomic(this);
        return y == null ? null : y.term();
    }

    @Nullable
    default Term transform(/*@NotNull*/ ByteList path, Term replacement) {
        return transform(path, 0, replacement);
    }


    @Nullable
    default Term transform(/*@NotNull*/ ByteList path, int depth, Term replacement) {
        final Term src = this;
        int ps = path.size();
        if (ps == depth)
            return replacement;
        if (ps < depth)
            throw new RuntimeException("path overflow");

        if (!(src instanceof Compound))
            return src; //path wont continue inside an atom

        Compound csrc = (Compound) src;
        Subterms css = csrc.subterms();

        int n = css.subs();
        if (n == 0) return src;

        Term[] target = new Term[n];

        for (int i = 0; i < n; i++) {
            Term x = css.sub(i);
            if (path.get(depth) != i)
                //unchanged subtree
                target[i] = x;
            else {
                //replacement is in this subtree
                target[i] = x.subs() == 0 ? replacement : x.transform(path, depth + 1, replacement);
            }

        }

        return csrc.op().the(csrc.dt(), target);
    }

    default <X> boolean pathsTo(/*@NotNull*/ Function<Term, X> target, Predicate<Term> descendIf, /*@NotNull*/ BiPredicate<ByteList, X> receiver) {
        X ss = target.apply(this);
        if (ss != null) {
            if (!receiver.test(EmptyByteList, ss))
                return false;
        }
        if (this.subs() > 0) {
            return pathsTo(this, new ByteArrayList(0), descendIf, target, receiver);
        } else {
            return true;
        }
    }

    @Nullable
    static byte[] pathTo(/*@NotNull*/ ByteArrayList p, Subterms superTerm, /*@NotNull*/ Term target) {

        int n = superTerm.subs();
        for (int i = 0; i < n; i++) {
            Term s = superTerm.sub(i);
            if (s.equals(target)) {
                p.add((byte) i);
                return p.toArray();
            }
            if (s instanceof Compound && !s.impossibleSubTerm(target)) {
                byte[] pt = pathTo(p, s.subterms(), target);
                if (pt != null) {
                    p.add((byte) i);
                    return pt;
                }

            }
        }

        return null;
    }

    static <X> boolean pathsTo(Term that, /*@NotNull*/ ByteArrayList p, Predicate<Term> descendIf,/*@NotNull*/ Function<Term, X> subterm, BiPredicate<ByteList, X> receiver) {
        if (!descendIf.test(that))
            return true;

        Subterms superTerm = that.subterms();

        int ppp = p.size();

        int n = superTerm.subs();
        for (int i = 0; i < n; i++) {

            p.add((byte) i); //push

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

            p.removeAtIndex(ppp); //pop

            if (!kontinue)
                return false;
        }

        return true;
    }


    @Nullable
    default Term commonParent(List<ByteList> subpaths) {
        int subpathsSize = subpaths.size();
        assert (subpathsSize > 1);

        int shortest = Integer.MAX_VALUE;
        for (int i = 0, subpathsSize1 = subpaths.size(); i < subpathsSize1; i++) {
            shortest = Math.min(shortest, subpaths.get(i).size());
        }

        //find longest common prefix
        int i;
        done:
        for (i = 0; i < shortest; i++) {
            byte needs = 0;
            for (int j = 0; j < subpathsSize; j++) {
                ByteList p = subpaths.get(j);
                byte pi = p.get(i);
                if (j == 0) {
                    needs = pi;
                } else if (needs != pi) {
                    break done; //first mismatch, done
                } //else: continue downwards
            }
            //all matched, proceed downward to the next layer
        }
        return i == 0 ? this : subPath(i, subpaths.get(0));

    }

    @Nullable
    default Term subPath(/*@NotNull*/ ByteList path) {
        Term ptr = this;
        int s = path.size();
        for (int i = 0; i < s; i++)
            if ((ptr = ptr.sub(path.get(i))) == Null)
                return Null;
        return ptr;
    }

    /**
     * extracts a subterm provided by the address tuple
     * returns null if specified subterm does not exist
     */
    @Nullable
    default Term subPath(/*@NotNull*/ byte... path) {
        return subPath(path.length, path);
    }

    @Nullable
    default Term subPath(int n, /*@NotNull*/ byte... path) {
        Term ptr = this;
        for (byte b : path) {
            if ((ptr = ptr.sub(b)) == Null)
                return Null;
        }
        return ptr;
    }

    @Nullable
    default Term subPath(int n, /*@NotNull*/ ByteList path) {
        Term ptr = this;
        for (int i = 0; i < n; i++) {
            if ((ptr = ptr.sub(path.get(i))) == Null)
                return Null;
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
     * equlity has already been tested prior to calling this
     *
     * @param y       another term
     * @param ignored the unification context
     * @return whether unification succeeded
     */
    default boolean unify(Term y, Unify u) {
//        if (((this instanceof Atomic) || !isCommutative() || u.constant(this, y)) && this.equals(y)) {
//            return true; //only assume unification via equality if not a compound, since
        if (equals(y) && (!isCommutative() || u.constant(this, y))) {
            return true; //only assume unification via equality if not a compound, since
        } else if (y instanceof Variable && !(this instanceof Variable) && u.varSymmetric && u.matchType(y.op())) {
            return y.unify(this, u); //reverse
        } else if (y instanceof AliasConcept.AliasAtom) {
            return unify(((AliasConcept.AliasAtom) y).target, u); //dereference alias
        } else {
            return false;
        }
    }


    /**
     * true if the operator bit is included in the enabld bits of the provided vector
     */
    default boolean isAny(int bitsetOfOperators) {
        int s = op().bit;
        return (bitsetOfOperators & s) > 0;
    }

//    /** for multiple Op comparsions, use Op.or to produce an int and call isAny(int vector) */
//    default boolean isA(/*@NotNull*/ Op otherOp) {
//        return op() == otherOp;
//    }


//    default boolean hasAll(int structuralVector) {
//        final int s = structure();
//        return (s & structuralVector) == s;
//    }
//


    void append(Appendable w) throws IOException;

//    default public void append(Writer w, boolean pretty) throws IOException {
//        //try {
//            name().append(w, pretty);
////        } catch (IOException e) {
////            e.printStackTrace();
////        }
//    }

    //    default public StringBuilder toStringBuilder(boolean pretty) {
//        return name().toStringBuilder(pretty);
//    }

//    @Deprecated
//    String toString();
//    default public String toString(boolean pretty) {
//        return toStringBuilder(pretty).toString();
//    }


    default String structureString() {
        return String.format("%16s",
                Integer.toBinaryString(structure()))
                .replace(" ", "0");
    }


    @Override
    default boolean isNormalized() {
        return true;
    }


    /**
     * computes the first occuring event's time relative to the start of a conjunction
     *
     * @param x subterm which must be present
     */
    default int subTime(/*@NotNull*/ Term x) {

        int d = subTimeSafe(x);
        if (d != DTERNAL)
            return d;

        throw new RuntimeException(x + " not contained by " + this);
    }

    /**
     * computes the first occuring event's time relative to the start of the
     * temporal term
     *
     * @param dt the current offset in the search
     * @return DTERNAL if the subterm was not found
     */
    default int subTimeSafe(/*@NotNull*/ Term x, int after) {
        return equals(x) ? 0 : DTERNAL;
    }

    default int subTimeSafe(/*@NotNull*/ Term x) {
        return subTimeSafe(x, 0);
    }


    /**
     * total span across time represented by a sequence conjunction compound
     */
    default int dtRange() {
        return 0;
    }


//    default boolean equalsIgnoringVariables(@NotNull Term other, boolean requireSameTime) {
//        return (this instanceof Variable) || (other instanceof Variable) || equals(other);
//    }


//    default public boolean hasAll(final Op... op) {
//        //TODO
//    }
//    default public boolean hasAny(final Op... op) {
//        //TODO
//    }


    default ByteList structureKey() {
        return structureKey(new ByteArrayList(volume() * 2 /* estimate */));
    }


    default ByteList structureKey(/*@NotNull*/ ByteArrayList appendTo) {
        appendTo.add(op().id);
        return appendTo;
    }


    default List<ByteList> pathsTo(Term target, int minLengthOfPathToReturn) {

        if (impossibleSubTerm(target))
            return List.of();

        List<ByteList> list = $.newArrayList(0);
        pathsTo(target, minLengthOfPathToReturn > 0 ?
                (l, t) -> {
                    if (l.size() >= minLengthOfPathToReturn)
                        list.add(l);
                    return true;
                }
                :
                (l, t) -> {
                    //simpler version when min=0
                    list.add(l.toImmutable());
                    return true;
                }
        );
        return list;
    }

    default boolean pathsTo(/*@NotNull*/ Term target, /*@NotNull*/ BiPredicate<ByteList, Term> receiver) {
        return pathsTo(
                x -> target.equals(x) ? x : null,
                x -> !x.impossibleSubTerm(target),
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
    default int compareTo(/*@NotNull*/ Termed _y) {
        if (this == _y) return 0;

        Term y = _y.term();
        if (this == y) return 0;

//        //order first by volume. this is important for conjunctions which rely on volume-dependent ordering for balancing
//        //left should be heavier
//        //compareTo semantics state that a -1 value means left is less than right. we want the opposite
        int vc = Integer.compare(y.volume(), this.volume());
        if (vc != 0)
            return vc;

        int oc = Integer.compare(this.opX(), y.opX());
        if (oc != 0)
            return oc;


        if (this instanceof Atomic) {

            //assert (y instanceof Atomic) : "because volume should have been determined to be equal";
            int h = Integer.compare(hashCode(), y.hashCode());
            if (h != 0)
                return h;

            if (this instanceof NormalizedVariable || this instanceof Int) { //includes Anom (extends Int)
                return 0; //hashcode was all that needed compared
            } else if (this instanceof Int.IntRange) {
                return Long.compareUnsigned(((Int.IntRange) this).hash64(), ((Int.IntRange) y).hash64());
            } else if (this instanceof Atomic) {
                return Util.compare(
                        ((Atomic) this).bytes(),
                        ((Atomic) y).bytes()
                );
            } else {
                throw new UnsupportedOperationException("unimplemented comparison: " + this + ' ' + y);
            }


        } else {

            int c = Subterms.compare(subterms(), y.subterms());
            return c != 0 ? c : Integer.compare(dt(), y.dt());
        }
    }

    @Override
    default Subterms subterms() {
        return TermVector.Empty;
    }


    /**
     * unwraps any negation superterm
     */
    /*@NotNull*/
    @Override
    default Term unneg() {
        return this;
    }

    /**
     * for safety, dont override this method. override evalSafe
     */
    /*@NotNull*/
    default Term eval(Evaluation.TermContext context) {
        return evalSafe(context, null, 0, Param.MAX_EVAL_RECURSION);
    }
    default Term eval(NAR nar) {
        return eval(nar.concepts.functors);
    }
    /*@NotNull*/

    /**
     *
     * @param context
     * @param whichSubterm current subterm index being evaluated
     * @param remain recursion limit (valid until decreases to zero)
     * @return
     */
    default Term evalSafe(Evaluation.TermContext context, Op supertermOp, int whichSubterm, int remain) {
        return /*remain <= 0 ? Null : */
                context.applyTermIfPossible(this, supertermOp, whichSubterm);
    }


    /**
     * includes itself in the count unless it's a CONJ sequence in which case it becomes the sum of the subterms event counts
     */
    default int eventCount() {
        return 1;
    }


    /* collects any contained events */
    @Deprecated default void events(Consumer<LongObjectPair<Term>> events) {
        eventsWhile((w, t) -> {
            events.accept(PrimitiveTuples.pair(w, t));
            return true; //continue
        }, 0);
    }

//    default MutableSet<LongObjectPair<Term>> eventSet(long offset) {
//        UnifiedSet<LongObjectPair<Term>> events = new UnifiedSet<>(1);
//        eventsWhile((w, t) -> {
//            events.add(PrimitiveTuples.pair(w, t));
//            return true; //continue
//        }, offset);
//        events.trimToSize();
//        return events;
//    }

    default FasterList<LongObjectPair<Term>> eventList(long offset, int dtDither) {
        return eventList(offset, dtDither, true, false);
    }
    /** sorted by time; decomposes inner parallel conj
     * TODO make sorting optional
     * */
    default FasterList<LongObjectPair<Term>> eventList(long offset, int dtDither, boolean decomposeParallel, boolean decomposeEternal) {
        FasterList<LongObjectPair<Term>> events = new FasterList(2);
        eventsWhile((w, t) -> {
            events.add(PrimitiveTuples.pair(
                    (dtDither > 1) ? Tense.dither(w, dtDither) : w,
                    t));
            return true; //continue
        }, offset, decomposeParallel, decomposeEternal, false, 0);
        if (events.size() > 1) {
            events.sortThisByLong(LongObjectPair::getOne);
        }
        return events;
    }

//    default LongObjectHashMap<Term> eventMap(long offset) {
//        LongObjectHashMap<Term> events = new LongObjectHashMap();
//        eventsWhile((w, t) -> {
//            Term existed = events.put(w, t);
//            if (existed != null) {
//                events.put(w, CONJ.the(0, existed, t));
//            }
//            return true;
//        }, offset);
//        return events;
//    }

    /**
     * event list, sorted by time
     * sorted by time; decomposes inner parallel conj
     */
    /* final */ default FasterList<LongObjectPair<Term>> eventList() {
        return eventList(0, 1);
    }



    /* final */ default boolean eventsWhile(LongObjectPredicate<Term> whileEachEvent, long dt) {
        return eventsWhile(whileEachEvent, dt, true, false, false, 0);
    }

    default boolean eventsWhile(LongObjectPredicate<Term> whileEachEvent, long dt,
                                boolean decomposeConjParallel, boolean decomposeConjDTernal, boolean decomposeXternal, int level) {
        return whileEachEvent.accept(dt, this);
    }

    default void printRecursive() {
        printRecursive(System.out);
    }

    default void printRecursive(@NotNull PrintStream out) {
        Terms.printRecursive(out, this);
    }

    static Term nullIfNull(@Nullable Term maybeNull) {
        return (maybeNull == null) ? Null : maybeNull;
    }

//    /** https://google.github.io/guava/releases/snapshot/api/docs/com/google/common/collect/TreeTraverser.html */
//    default TreeTraverser<Term> termverse() {
//        return TreeTraverser.using(x -> x instanceof Compound ? ((Compound)x).subterms() : Collections.emptyList());
//    }

    /**
     * opX function
     */
    public static int opX(Op o, short subOp) {
        return (o.id << 16) | (subOp);
    }

    /**
     * for convenience, delegates to the byte function
     */
    @Deprecated static int opX(/*@NotNull*/ Op o, int subOp) {
        assert(subOp < Short.MAX_VALUE-1);
        return opX(o, (short) subOp);
    }


    default Term dt(int dt) {
//        if (dt!=DTERNAL)
//            throw new UnsupportedOperationException("temporality not supported");
        return this;
    }

//    /**
//     * return null if none, cheaper than using an empty iterator
//     */
//    @Nullable
//    default Set<Variable> varsUnique(@Nullable Op type/*, Set<Term> exceptIfHere*/) {
//        int num = vars(type);
//        if (num == 0)
//            return null;
//
//        //must check all in case of repeats
//        MutableSet<Variable> u = new UnifiedSet(num);
//        final int[] remain = {num};
//
//        recurseTerms(parent -> vars(type) > 0,
//                (sub) -> {
//                    if (sub instanceof Variable && (type == null || sub.op() == type)) {
//                        //if (!unlessHere.contains(sub))
//                        u.add((Variable) sub);
//                        remain[0]--;
//                    }
//                    return (remain[0] > 0);
//                });
//        return u.isEmpty() ? null : u;
//    }

    /**
     * returns this term in a form which can identify a concept, or Null if it can't
     * generally this is equivalent to root() but for compound it includes
     * unnegation and normalization steps. this is why conceptual() and root() are
     * different
     */
    default Term concept() {
        //throw new UnsupportedOperationException();
        return this;
    }

    /**
     * the skeleton of a term, without any temporal or other meta-assumptions
     */
    default Term root() {
        return this;
    }

    default boolean equalsRoot(Term x) {
        return equals(x);
    }


    default int dt() {
        return DTERNAL;
    }

    default Term normalize(byte offset) {
        return this; //no change
    }

    @Nullable
    default Term normalize() {
        return normalize((byte) 0);
    }


    @Nullable
    default Term replace(/*@NotNull*/ Map<Term, Term> m) {
        Subst s = MapSubst.the(m);
        return s != null ? transform(s) : this;
    }

    default Term replace(Term from, Term to) {
        if (from.equals(to))
            return this;
        else
            return equals(from) ? to : (impossibleSubTerm(from) ? this : transform(new MapSubst.MapSubst1(from, to)));
    }

    default Term neg() {
        return Neg.the(this); //the DTERNAL gets it directly to it
    }

    default Term negIf(boolean negate) {
        return negate ? neg() : this;
    }

    @Nullable
    Term temporalize(Retemporalize r);


    default Term anon() {
        return Anom.the(1);
    }


    default void collectMetadata(TermMetadata.SubtermMetadataCollector s) {
        s.varPattern += varPattern();

        int xstructure = structure();
        if ((xstructure & VAR_DEP.bit) > 0)
            s.varDep += varDep();
        if ((xstructure & VAR_INDEP.bit) > 0)
            s.varIndep += varIndep();
        if ((xstructure & VAR_QUERY.bit) > 0)
            s.varQuery += varQuery();
        s.structure |= xstructure;

        s.vol += volume();
        s.hash = Util.hashCombine(s.hash, hashCode());
    }

    default Term the() {
        if (this instanceof The)
            return this;
        else
            throw new RuntimeException(getClass() + " needs to impl the()");
    }

    default boolean equalsNeg(Term t) {
        if (this == t) {
            return false;
        } else if (t.op() == NEG) {
            return equals(t.unneg());
        } else {
            return hasAny(NEG) && equals(t.neg());
        }
    }

    /**
     * Created by me on 2/26/16.
     */
    final class InvalidTermException extends SoftException {

        @NotNull private final Op op;
        private final int dt;
        @NotNull private final Term[] args;
        @NotNull private final String reason;


        public InvalidTermException(/*@NotNull*/ Op op, @NotNull Term[] args, @NotNull String reason) {
            this(op, DTERNAL, reason, args);
        }

        public InvalidTermException(/*@NotNull*/ Op op, int dt, @NotNull Term[] args, @NotNull String reason) {
            this(op, dt, reason, args);
        }

        public InvalidTermException(/*@NotNull*/ Op op, int dt, @NotNull Subterms args, @NotNull String reason) {
            this(op, dt, reason, args.arrayShared());
        }

        public InvalidTermException(/*@NotNull*/ Op op, int dt, @NotNull String reason, @NotNull Term... args) {
            this.op = op;
            this.dt = dt;
            this.args = args;
            this.reason = reason;
        }

        public InvalidTermException(String s, @NotNull Compound c) {
            this(c.op(), c.dt(), c.subterms(), s);
        }

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

