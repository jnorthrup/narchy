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
import jcog.data.set.MetalTreeSet;
import kotlin.jvm.JvmDefault;
import nars.Idempotent;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.anon.Anom;
import nars.term.anon.IntrinAtomic;
import nars.term.atom.Atomic;
import nars.term.atom.IdempotInt;
import nars.term.atom.IdempotentBool;
import nars.term.util.builder.TermBuilder;
import nars.term.util.conj.Conj;
import nars.term.util.transform.MapSubst;
import nars.term.util.transform.TermTransform;
import nars.time.Tense;
import nars.unify.Unify;
import org.eclipse.collections.api.block.predicate.primitive.LongObjectPredicate;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.eclipse.collections.api.list.primitive.ImmutableByteList;
import org.eclipse.collections.impl.factory.primitive.ByteLists;
import org.eclipse.collections.impl.list.mutable.primitive.ByteArrayList;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
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
 * https:
 */
public interface Term extends Termlike, Termed, Comparable<Term> {

    static <X> boolean pathsTo(Term that, ByteArrayList p, Predicate<Term> descendIf, Function<Term, X> subterm, BiPredicate<ByteList, X> receiver) {
        boolean result = false;
        boolean finished = false;
        if (descendIf.test(that)) {
            if (that instanceof Compound) {
                Subterms superTerm = ((Compound) that).subtermsDirect();
                int ppp = p.size();
                int n = superTerm.subs();
                for (int i = 0; i < n; i++) {

                    p.add((byte) i);

                    Term s = superTerm.sub(i);

                    boolean kontinue = true;
                    X ss = subterm.apply(s);
                    if (ss != null) if (!receiver.test(p, ss))
                        kontinue = false;

                    
                    if (s instanceof Compound) if (!pathsTo(s, p, descendIf, subterm, receiver))
                        kontinue = false;

                    p.removeAtIndex(ppp);

                    if (!kontinue) {
                        finished = true;
                        break;
                    }
                }
                if (!finished) {
                    result = true;
                }
            }

        } else {
            result = true;
        }

        return result;
    }

    static Term nullIfNull(@Nullable Term maybeNull) {
        return (maybeNull == null) ? IdempotentBool.Null : maybeNull;
    }

    /**
     * true if there is at least some type of structure in common
     */
    static boolean commonStructure(Termlike x, Termlike y) {
        return commonStructure(x.structure(), y.structure());
    }

    static boolean commonStructure(int xStruct, int yStruct) {
        return xStruct == yStruct || ((xStruct & yStruct) != 0);
    }


    @JvmDefault default Term term() {
        return this;
    }

    Op op();

    @JvmDefault default int opBit() {
        return 1 << opID();
    }

    @JvmDefault default int opID() {
        return (int) op().id;
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
     * <p>
     * implementations are not obligated to visit in any particular order, or to repeat visit a duplicate subterm
     * for that, use recurseTermsOrdered(..)
     */
    boolean recurseTerms(Predicate<Term> inSuperCompound, Predicate<Term> whileTrue, @Nullable Compound /* Compound? */superterm);

    boolean recurseTermsOrdered(Predicate<Term> inSuperCompound, Predicate<Term> whileTrue, Compound parent);

    /**
     * TODO move to Subterms
     */
    @Deprecated
    @JvmDefault default boolean containsRecursively(Term t) {
        return containsRecursively(t, null);
    }

    /**
     * TODO move to Subterms
     */
    @Deprecated
    @JvmDefault
    default boolean containsRecursively(Term t, @Nullable Predicate<Term> inSubtermsOf) {
        return containsRecursively(t, false, inSubtermsOf);
    }


    /**
     * if root is true, the root()'s of the terms will be compared
     * TODO move to Subterms
     */
    @Deprecated
    boolean containsRecursively(Term t, boolean root, @Nullable Predicate<Term> inSubtermsOf);

    Term transform(TermTransform t);


    @JvmDefault default int hashCodeShort() {
        int h = hashCode();
        return ((h & 0xffff) ^ (h >>> 16));
    }

    @JvmDefault default boolean recurseTerms(BiFunction<Compound, Term, TermWalk> whileTrue, Compound superterm) {
        throw new TODO();
    }

    /**
     * convenience, do not override (except in Atomic)
     */
    @JvmDefault default boolean recurseTermsOrdered(Predicate<Term> whileTrue) {
        return recurseTermsOrdered(new Predicate<Term>() {
            @Override
            public boolean test(Term x) {
                return true;
            }
        }, whileTrue, null);
    }

    /**
     * whileTrue = BiPredicate<SubTerm,SuperTerm>
     * implementations are not obligated to visit in any particular order, or to repeat visit a duplicate subterm
     */
    boolean recurseTerms(Predicate<Compound> aSuperCompoundMust, BiPredicate<Term, Compound> whileTrue, @Nullable Compound superterm);

    /**
     * convenience, do not override (except in Atomic)
     */
    @JvmDefault default void recurseTerms(BiConsumer<Term, Compound> each) {
        recurseTerms(new Predicate<Compound>() {
            @Override
            public boolean test(Compound x) {
                return true;
            }
        }, new BiPredicate<Term, Compound>() {
            @Override
            public boolean test(Term sub, Compound sup) {
                each.accept(sub, sup);
                return true;
            }
        }, null);
    }

    /**
     * convenience, do not override (except in Atomic)
     */
    @JvmDefault default void recurseTerms(Consumer<Term> each) {
        recurseTerms(new Predicate<Term>() {
            @Override
            public boolean test(Term a) {
                return true;
            }
        }, new Predicate<Term>() {
            @Override
            public boolean test(Term sub) {
                each.accept(sub);
                return true;
            }
        }, null);
    }

    @Override
    Term sub(int i);

    @Override
    int subs();

    @JvmDefault default @Nullable Term replaceAt(ByteList path, Term replacement) {
        return replaceAt(path, 0, replacement);
    }

    @JvmDefault default @Nullable Term replaceAt(ByteList path, int depth, Term replacement) {
        int ps = path.size();
        if (ps == depth)
            return replacement;
        if (ps < depth)
            throw new RuntimeException("path overflow");

        Compound src = (Compound) this;
        Subterms css = src.subtermsDirect();

        int n = css.subs();

        byte which = path.get(depth);
        assert ((int) which < n);

        Term x = css.sub((int) which);
        Term y = x.replaceAt(path, depth + 1, replacement);
        if (y == x) return src; 
        else {
            Term[] target = css.arrayClone();
            target[(int) which] = y;
            return src.op().the(src.dt(), target);
        }
    }

    @JvmDefault default boolean pathsTo(Predicate<Term> selector, Predicate<Term> descendIf, BiPredicate<ByteList, Term> receiver) {
        return pathsTo(new UnaryOperator<Term>() {
            @Override
            public Term apply(Term x) {
                return selector.test(x) ? x : null;
            }
        }, descendIf, receiver);
    }
    ImmutableByteList EmptyByteList = ByteLists.immutable.empty();

    @JvmDefault default <X> boolean pathsTo(Function<Term, X> target, Predicate<Term> descendIf, BiPredicate<ByteList, X> receiver) {
        X ss = target.apply(this);
        if (ss != null && !receiver.test( EmptyByteList, ss))
            return false;

        return subs() <= 0 ||
                pathsTo(this, new ByteArrayList(0), descendIf, target, receiver);
    }

    @JvmDefault default Term commonParent(List<ByteList> subpaths) {
        int subpathsSize = subpaths.size(); 

        int shortest = Integer.MAX_VALUE;
        for (ByteList subpath : subpaths)
            shortest = Math.min(shortest, subpath.size());


        int i;
        done:
        for (i = 0; i < shortest; i++) {
            byte needs = (byte) 0;
            for (int j = 0; j < subpathsSize; j++) {
                byte pi = subpaths.get(j).get(i);
                if (j == 0) needs = pi;
                else if ((int) needs != (int) pi) break done;
            }

        }
        return i == 0 ? this : subPath(subpaths.get(0), 0, i);

    }

    @JvmDefault default @Nullable Term subPath(ByteList path) {
        int p = path.size();
        return p > 0 ? subPath(path, 0, p) : this;
    }

    /**
     * extracts a subterm provided by the address tuple
     * returns null if specified subterm does not exist
     */
    @JvmDefault default @Nullable Term subPath(byte... path) {
        int p = path.length;
        return p > 0 ? subPath(0, p, path) : this;
    }

    @JvmDefault default @Nullable Term subPath(int start, int end, byte... path) {
        Term ptr = this;
        for (int i = start; i < end; i++)
            if ((ptr = ptr.subSafe((int) path[i])) == IdempotentBool.Null)
                return null;
        return ptr;
    }

    @JvmDefault default @Nullable Term subPath(ByteList path, int start, int end) {
        Term ptr = this;
        for (int i = start; i < end; i++)
            if ((ptr = ptr.subSafe((int) path.get(i))) == IdempotentBool.Null)
                return null;
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
    @JvmDefault default boolean unify(Term y, Unify u) {
        return y instanceof Variable ? y.unify(this, u) : equals(y);
    }

    /**
     * true if the operator bit is included in the enabld bits of the provided vector
     */
    @JvmDefault default boolean isAny(int bitsetOfOperators) {
        return (opBit() & bitsetOfOperators) != 0;
    }

    void appendTo(Appendable w) throws IOException;

    @JvmDefault default StringBuilder appendTo(StringBuilder s) {
        try {
            appendTo((Appendable) s);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return s;
    }

    @JvmDefault default String structureString() {
        return String.format("%16s",
                Op.strucTerm(structure()))
                .replace(" ", "0");
    }

    @JvmDefault default boolean isNormalized() {
        return true;
    }

    /**
     * returns DTERNAL if not found
     */
    @JvmDefault default int subTimeFirst(Term x) {
        int[] time = {DTERNAL};
        subTimesWhile(x, new IntPredicate() {
            @Override
            public boolean test(int w) {
                time[0] = w; 
                return false; 
            }
        });
        return time[0];
    }




















    /**
     * returns DTERNAL if not found
     * TODO optimize traversal
     */
    @JvmDefault default int subTimeLast(Term x) {
        int[] time = {DTERNAL};
        subTimesWhile(x, new IntPredicate() {
            @Override
            public boolean test(int w) {
                time[0] = Math.max(time[0], w); 
                return true; 
            }
        });
        return time[0];
    }

    /**
     * TODO make generic Predicate<Term> selector
     * TODO move down to Compound, provide streamlined Atomic impl
     */
    @JvmDefault default boolean subTimesWhile(Term match, IntPredicate each) {
        if (equals(match))
            return each.test(0);


        if (op() == CONJ) if (Conj.isSeq(this)) {

            eventsAND(new LongObjectPredicate<Term>() {
                @Override
                public boolean accept(long when, Term what) {
                    
                    if (what.equals(match)) return each.test(Tense.occToDT(when));
                    else if (Term.this != what && what.op() == CONJ) { 
                        int subWhen = what.subTimeFirst(match);
                        
                        if (subWhen != DTERNAL) return each.test(Tense.occToDT(when + (long) subWhen));
                    }
                    return true;
                }
            }, 0L, match.op() != CONJ || match.dt() != DTERNAL, true);
            return true;
        } else if (contains(match))
            return each.test(0);
        return true;
    }

    /**
     * total span across time represented by a sequence conjunction compound
     */
    @JvmDefault default int eventRange() {
        return 0;
    }

    @JvmDefault default boolean pathsTo(Term target, BiPredicate<ByteList, Term> receiver) {
        return pathsTo(
                target::equals,
                new Predicate<Term>() {
                    @Override
                    public boolean test(Term x) {
                        return !x.impossibleSubTerm(target);
                    }
                },
                receiver);
    }

    @JvmDefault default boolean pathsTo(Term target, Predicate<Term> superTermFilter, BiPredicate<ByteList, Term> receiver) {
        return pathsTo(
                target::equals,
                new Predicate<Term>() {
                    @Override
                    public boolean test(Term x) {
                        return superTermFilter.test(x) && !x.impossibleSubTerm(target);
                    }
                },
                receiver);
    }

    @JvmDefault
    default int compareTo(Term t) {
        int result = +1;
        boolean finished = false;
        if (this != t) {
            boolean a = this instanceof Atomic, b = t instanceof Atomic;
            if (!a || b) {
                if (b && !a) {
                    result = -1;
                } else {
                    if (!b) {
                        int vc = Integer.compare(t.volume(), volume());
                        if (vc != 0) {
                            result = vc;
                            finished = true;
                        }
                    }
                    if (!finished) {
                        Op op = op();
                        int oc = Integer.compare((int) op.id, t.opID());
                        if (oc != 0) {
                            result = oc;
                        } else if (a) {

                            if (this instanceof IdempotInt /*&& t instanceof Int*/) {
                                result = Integer.compare(((IdempotInt) this).i, ((IdempotInt) t).i);
                            } else if (this instanceof IntrinAtomic && t instanceof IntrinAtomic) {
                                result = Integer.compare(hashCode(), t.hashCode());
                            } else {
                                result = Arrays.compare(
                                        ((Atomic) this).bytes(),
                                        ((Atomic) t).bytes()
                                );
                            }


                        } else {
                            

                            int c = Subterms.compare(
                                    ((Compound) this).subtermsDirect(),
                                    ((Compound) t).subtermsDirect()
                            );
                            result = c != 0 ? c : (op.temporal ? Integer.compare(dt(), t.dt()) : 0);
                        }
                    }
                }
            }

        } else {
            result = 0;
        }

        return result;
    }

    Subterms subterms();

    /**
     * unwraps negation - only changes if this term is NEG
     */
    @JvmDefault default Term unneg() {
        return this;
    }

    @JvmDefault default boolean eventsAND(LongObjectPredicate<Term> each, long offset,
                              boolean decomposeConjDTernal, boolean decomposeXternal) {
        return each.accept(offset, this);
    }

    @JvmDefault default boolean eventsOR(LongObjectPredicate<Term> each, long offset,
                             boolean decomposeConjDTernal, boolean decomposeXternal) {
        return each.accept(offset, this);
    }

    @JvmDefault default void printRecursive() {
        printRecursive(System.out);
    }

    @JvmDefault default void printRecursive(PrintStream out) {
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

    /**
     * TODO make Compound only
     */
    @JvmDefault default int dt() {
        return DTERNAL;
    }

    Term normalize(byte offset);

    @JvmDefault default Term normalize() {
        return normalize((byte) 0);
    }

    @JvmDefault default Term replace(Map<? extends Term, Term> m) {
        return MapSubst.replace(this, m);
    }

    Term replace(Term from, Term to);

    @JvmDefault default Term neg() {
        return TermBuilder.neg(this);
    }

    @JvmDefault default Term negIf(boolean negate) {
        return negate ? neg() : this;
    }

    @JvmDefault default Term anon() {
        return Anom.the(1);
    }

    @JvmDefault default boolean the() {
        return this instanceof Idempotent;
    }



    @Override
    @JvmDefault default boolean these() {
        return the();
    }

    boolean equalsRoot(Term x);

    @JvmDefault default boolean equalsPosOrNeg(Term t) {
        return equals(t.unneg());
    }

    @JvmDefault default boolean equalsNeg(Term t) {
        if (this == t) return false;
        else if (t instanceof Neg) return equals(t.unneg());
        else if (this instanceof Neg) return unneg().equals(t);
        else return false;
    }

    @JvmDefault default SortedSet<Term> eventSet() {
        assert (op() == CONJ);
        MetalTreeSet<Term> s = new MetalTreeSet();
        eventsAND(new LongObjectPredicate<Term>() {
            @Override
            public boolean accept(long when, Term what) {
                if (what != Term.this)
                    s.add(what);
                return true;
            }
        }, 0L, true, true);
        return s;
    }













    @JvmDefault default Term eventFirst() {
        return this;
    }

    @JvmDefault default Term eventLast() {
        return this;
    }

    enum TermWalk {
        Left, 
        Right, 
        Down, 
        Stop 
    }


}

