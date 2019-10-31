package nars.term.compound;

import com.google.common.collect.Streams;
import jcog.Util;
import jcog.data.bit.MetalBitSet;
import jcog.data.byt.DynBytes;
import jcog.data.list.FasterList;
import jcog.data.set.ArrayUnenforcedSortedSet;
import jcog.data.set.MetalTreeSet;
import jcog.decide.Roulette;
import nars.Op;
import nars.subterm.*;
import nars.subterm.util.TermMetadata;
import nars.term.*;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.util.builder.TermBuilder;
import nars.term.var.ellipsis.Fragment;
import org.eclipse.collections.api.block.function.primitive.ByteFunction0;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.function.primitive.IntObjectToIntFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntObjectPredicate;
import org.eclipse.collections.api.block.predicate.primitive.ObjectIntPredicate;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.primitive.ObjectBytePair;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectByteHashMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import static nars.Op.*;
import static nars.subterm.Subterms.*;
import static nars.time.Tense.*;

/**
 * Annotates a GenericCompound with cached data to accelerate pattern matching
 * TODO not finished yet
 */
public abstract class FastCompound implements SameSubtermsCompound /* The */ {

    private static final Op[] ov = Op.values();

    private static final int MAX_LAYERS = 8;


    public static class FastCompoundInstancedAtoms extends FastCompound {
        private final Term[] atoms;

        FastCompoundInstancedAtoms(Term[] atoms, byte[] shadow, int structure, int hash, byte volume, boolean normalized) {
            super(shadow, structure, hash, volume, normalized);
            this.atoms = atoms;
        }

        @Override
        protected int atomCount() {
            return atoms.length;
        }

        @Override
        protected boolean containsAtomic(Atomic x) {
            if (!hasAny(x.op()))
                return false;
            return Arrays.asList(atoms).contains(x);
        }

        @Override
        protected Term atom(byte id) {
            return atoms[id];
        }
    }


    private final byte[] shadow;

    private final int hash;
    private final byte volume;
    protected final int structure;

    FastCompound(byte[] shadow, int structure, int hash, byte volume, boolean normalized) {
        this.shadow = shadow;
        this.hash = hash;
        this.volume = volume;
        this.structure = structure;
    }


    @Override
    public int volume() {
        return volume;
    }

    @Override
    public int structure() {
        return structure;
    }

    public static FastCompound get(Compound x) {
        if (x instanceof FastCompound)
            return ((FastCompound) x);

        return get(x.op(), x.subs(), x.subterms());
    }

    public static FastCompound get(Op o, List<Term> subterms) {
        return get(o, subterms.size(), subterms);
    }

    public static FastCompound get(Op o, int subs, Iterable<Term> subterms) {

        ObjectByteHashMap<Term> atoms = new ObjectByteHashMap();

        DynBytes shadow = new DynBytes(256);


        shadow.writeUnsignedByte(o.ordinal());
        shadow.writeUnsignedByte(subs);
        byte[] numAtoms = {0};
        ByteFunction0 nextUniqueAtom = () -> numAtoms[0]++;
        int structure = o.bit, hashCode = 1;
        byte volume = 1;

        for (Term x : subterms) {
            x.recurseTermsOrdered(child -> {
                shadow.writeUnsignedByte((byte) child.op().ordinal());
                if (child.op().atomic) {
                    int aid = atoms.getIfAbsentPut(child, nextUniqueAtom);
                    shadow.writeUnsignedByte((byte) aid);
                } else {
                    shadow.writeUnsignedByte(child.subs());

                }
                return true;
            });
            structure |= x.structure();
            hashCode = Util.hashCombine(hashCode, x.hashCode());
            volume += x.volume();
        }

        hashCode = Util.hashCombine(hashCode, o.id);

        assert (volume < 127);


        Term[] a = new Term[atoms.size()];
        for (ObjectBytePair<Term> p : atoms.keyValuesView()) {
            a[p.getTwo()] = p.getOne();
        }
        boolean normalized = false;

        return new FastCompoundInstancedAtoms(a, shadow.toByteArray(), structure, hashCode, volume, normalized);
    }

    public static void print() {
        System.out.println();
    }

    //TODO verify with other impl's there may be something missing here
//    @Override
//    public boolean containsRecursively(Term t, boolean root, Predicate<Term> inSubtermsOf) {
//        if (t instanceof Atomic) {
//            return containsAtomic((Atomic) t);
//        } else {
//            return SameSubtermsCompound.super.containsRecursively(t, root, inSubtermsOf);
//        }
//    }

    @Override
    public Term sub(int i) {
        return subterms().sub(i); //HACK TODO slow
    }

    protected abstract boolean containsAtomic(Atomic t);

    @Override
    public Op op() {
        return ov[shadow[0]];
    }

    @Override
    public int subs() {
        return shadow[1];
    }

    @Override
    @Deprecated
    public Subterms subterms() {
        return new SubtermView(this, 0);
    }

    @FunctionalInterface
    public interface ByteIntPredicate {
        boolean test(byte a, int b);
    }


    public byte subtermCountAt(int at) {
        return shadow[at + 1];
    }

    /**
     * seeks and returns the offset of the ith subterm
     */
    private int subtermOffsetAt(int subterm, int at) {
        if (subterm == 0) {

            return at + 2;
        }

        int[] o = new int[1];
        subtermOffsets(at, (sub, offset) -> {
            if (sub == subterm) {
                o[0] = offset;
                return false;
            }
            return true;
        });
        assert (o[0] != 0);
        return o[0];
    }

    private void subtermOffsets(int at, ByteIntPredicate each) {
        byte[] shadow = this.shadow;

        assert (!ov[shadow[at]].atomic);

        byte subterms = shadow[at + 1];
        if (subterms == 0)
            return;

        byte[] stack = new byte[MAX_LAYERS];
        stack[0] = subterms;

        at += 2;

        byte depth = 0;
        for (byte i = 0; i < subterms; ) {
            if (depth == 0) {
                if (!each.test(i, at) || i == subterms - 1)
                    return;
            }

            byte op = shadow[at++];


            if (ov[op].atomic) {
                at++;
            } else {
                stack[++depth] = shadow[at++];
            }


            if (--stack[depth] == 0)
                depth--;
            if (depth == 0)
                i++;
        }

    }


    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public int dt() {
        return DTERNAL;
    }

    /**
     * subterm, or sub-subterm, etc.
     */
    public Term term(int offset) {
        Op opAtSub = ov[shadow[offset]];
		return opAtSub.atomic ? atom(shadow[offset + 1]) : TermBuilder.newCompound(opAtSub,
			Op.terms.subterms(new SubtermView(this, offset).toArray(EmptyTermArray))

		);
    }

    protected abstract Term atom(byte id);

    public Term sub(byte i, int containerOffset) {

        return term(subtermOffsetAt(i, containerOffset));

    }


    @Override
    public boolean equals(@Nullable Object that) {
        if (this == that) return true;

        if (!(that instanceof Term) || hash != that.hashCode())
            return false;

        if (that instanceof FastCompound) {
            FastCompound f = (FastCompound) that;
            int aa = atomCount();
            if (aa == f.atomCount()) {
                if (Arrays.equals(shadow, f.shadow)) {
                    for (byte i = 0; i < aa; i++)
                        if (!atom(i).equals(f.atom(i)))
                            return false;
                    return true;
                }
            }
        } /*else {
            return Compound.equals(this, that,false);
        }*/
        return false;
    }

    protected abstract int atomCount();

    private static class SubtermView extends AbstractList<Term> implements Subterms {
        private final FastCompound c;

        private int offset = 0;
        int _hash = 0;

        SubtermView(FastCompound terms, int offset) {
            this.c = terms;
            if (offset != 0)
                go(offset);
        }


        @Override
        public Term get(int index) {
            return sub(index);
        }

        @Override
        public boolean equals(Object obj) {
            return
                    (this == obj)
                            ||
                            (obj instanceof Subterms)
                                    && hashCodeSubterms() == ((Subterms) obj).hashCodeSubterms()
                                    && equalTerms(((Subterms) obj));
        }

        @Override
        public int intifyShallow(int v, IntObjectToIntFunction<Term> reduce) {
            int o = offset;
            int[] vv = {v};
            c.subtermOffsets(o, (subterm, at) -> {
                Term t = c.term(at);

                vv[0] = reduce.intValueOf(vv[0], t);
                return true;
            });
            return vv[0];
        }

        @Override
        public int hashCode() {

            int h = _hash;
			return h == 0 ? (_hash = intifyShallow(1, (i, t) -> Util.hashCombine(i, t.hashCode()))) : h;
        }


        public SubtermView go(int offset) {
            int o = this.offset;
            if (o != offset) {
                this.offset = offset;
                _hash = 0;
            }
            return this;
        }

        @Override
        public Term sub(int i) {
            return c.sub((byte) i, offset);
        }

        @Override
        public int subs() {
            int offset = this.offset;
            byte[] s = c.shadow;
            Op op = ov[s[offset]];
			return op.atomic ? 0 : s[offset + 1];
        }

        @Override
        public String toString() {
            return Subterms.toString(this);
        }

        @Override
        public int size() {
            return subs();
        }

        @Override
        public boolean hasXternal() {
            return hasAny(Op.Temporal) && OR(Term::hasXternal);
        }

        @Override
        public Predicate<Term> containing() {
            switch (subs()) {
                case 0:
                    return (x) -> false;
                case 1:
                    return sub(0)::equals;
                default:
                    return this::contains;
            }
        }

        @Override
        public boolean contains(Term x) {
            return indexOf(x) != -1;
        }

        @Override
        public boolean containsInstance(Term t) {
            return ORwith((i, T) -> i == T, t);
        }

        @Override
        @Nullable
        public Term subSub(byte[] path) {
            return subSub(0, path.length, path);
        }

        @Override
        @Nullable
        public Term subSub(int start, int end, byte[] path) {
            Termlike ptr = this;
            for (int i = start; i < end; i++) {
                if ((ptr = ptr.subSafe(path[i])) == Bool.Null)
                    return null;
            }
            return ptr != this ? (Term) ptr : null;
        }

        @Override
        @Nullable
        public Term subSubUnsafe(int start, int end, byte[] path) {
            Termlike ptr = this;
            for (int i = start; i < end; )
                ptr = ptr.sub(path[i++]);
            return (Term) ptr;
        }

        @Override
        public boolean containsAll(Subterms ofThese) {
            return this.equals(ofThese) || ofThese.AND(this::contains);
        }

        @Override
        public boolean containsAny(Subterms ofThese) {
            return this.equals(ofThese) || ofThese.OR(this::contains);
        }

        @Override
        public <X> X[] array(Function<Term, X> map, IntFunction<X[]> arrayizer) {
            int s = subs();
            X[] xx = arrayizer.apply(s);
            for (int i = 0; i < s; i++)
                xx[i] = map.apply(sub(i));
            return xx;
        }

        @Override
        public int subEventRange(int i) {
            return sub(i).eventRange();
        }

        @Override
        @Nullable
        public Term subRoulette(FloatFunction<Term> subValue, Random rng) {
            int s = subs();
            switch (s) {
                case 0:
                    return null;
                case 1:
                    return sub(0);
                default:
                    return sub(Roulette.selectRoulette(s, i -> subValue.floatValueOf(sub(i)), rng));
            }
        }

        @Override
        @Nullable
        public Term sub(Random rng) {
            int s = subs();
            switch (s) {
                case 0:
                    return null;
                case 1:
                    return sub(0);
                default:
                    return sub(rng.nextInt(s));
            }
        }

        @Override
        public Subterms remove(Term event) {
            Term[] t = removing(event);
            return (t == null || t.length == subs()) ? this : Op.terms.subterms(t);
        }

        @Override
        public int structure() {
            return intifyShallow(0, (s, x) -> s | x.structure());
        }

        @Override
        public void forEachI(ObjectIntProcedure<Term> t) {
            int s = subs();
            for (int i = 0; i < s; i++)
                t.value(sub(i), i);
        }

        @Override
        public <X> void forEachWith(BiConsumer<Term, X> t, X argConst) {
            int s = subs();
            for (int i = 0; i < s; i++)
                t.accept(sub(i), argConst);
        }

        @Override
        public Subterms commuted() {
            if (subs() <= 1) return this;
            Term[] x = arrayShared();
            Term[] y = Terms.commute(x);
            return x == y ? this : new TermList(y);
        }

        @Override
        public boolean isSorted() {
            int s = subs();
            if (s < 2) return true;
            Term p = sub(0);
            for (int i = 1; i < s; i++) {
                Term n = sub(i);
                if (p.compareTo(n) > 0)
                    return false;
                p = n;
            }
            return true;
        }

        @Override
        public boolean isCommuted() {
            int s = subs();
            if (s < 2) return true;
            Term p = sub(0);
            for (int i = 1; i < s; i++) {
                Term n = sub(i);
                if (p.compareTo(n) >= 0)
                    return false;
                p = n;
            }
            return true;
        }

        @Override
        public Term subSafe(int i) {
            return sub(i, Bool.Null);
        }

        @Override
        public Term sub(int i, Term ifOutOfBounds) {
            return i >= subs() ? ifOutOfBounds : sub(i);
        }

        @Override
        public int height() {
            return subs() == 0 ? 1 : 1 + max(Term::height);
        }

        @Override
        public int volume() {
            return 1 + sum(Term::volume);
        }

        @Override
        public int complexity() {
            return 1 + sum(Term::complexity);
        }

        @Override
        public int sum(ToIntFunction<Term> value) {
    //        int x = 0;
    //        int s = subs();
    //        for (int i = 0; i < s; i++)
    //            x += value.applyAsInt(sub(i));
    //
    //        return x;
            return intifyShallow(0, (x, t) -> x + value.applyAsInt(t));
        }

        @Override
        public int max(ToIntFunction<Term> value) {
            return intifyShallow(Integer.MIN_VALUE, (x, t) -> Math.max(value.applyAsInt(t), x));
        }

        @Override
        public int intifyRecurse(int _v, IntObjectToIntFunction<Term> reduce) {
            return intifyShallow(_v, (v, s) -> s.intifyRecurse(v, reduce));
        }

        @Override
        public float voluplexity() {
            return (complexity() + volume()) / 2f;
        }

        @Override
        public boolean hasAll(int structuralVector) {
            return Op.has(structure(), structuralVector, true);
        }

        @Override
        public boolean hasAny(int structuralVector) {
            return Op.has(structure(), structuralVector, false);
        }

        @Override
        public /* final */ boolean hasAny(/*@NotNull*/ Op op) {
            return hasAny(op.bit);
        }

        @Override
        public /* final */ boolean hasAllAny(/*@NotNull*/ int all, int any) {
            int s = structure();
            return Op.has(s, all, true) && Op.has(s, any, false);
        }

        @Override
        public boolean hasVarIndep() {
            return hasAny(Op.VAR_INDEP.bit);
        }

        @Override
        public boolean hasVarDep() {
            return hasAny(Op.VAR_DEP.bit);
        }

        @Override
        public boolean hasVarQuery() {
            return hasAny(Op.VAR_QUERY.bit);
        }

        @Override
        public boolean hasVarPattern() {
            return hasAny(Op.VAR_PATTERN.bit);
        }

        @Override
        public boolean impossibleSubVolume(int otherTermVolume) {
            return otherTermVolume > volume() - subs();
        }

        @Override
        public int vars() {
            return hasVars() ? sum(Term::vars) : 0;
        }

        @Override
        public boolean hasVars() {
            return hasAny(VAR_INDEP.bit | VAR_DEP.bit | VAR_QUERY.bit | VAR_PATTERN.bit);
        }

        @Override
        public int varDep() {
            return sum(Term::varDep);
        }

        @Override
        public int varIndep() {
            return sum(Term::varIndep);
        }

        @Override
        public int varQuery() {
            return sum(Term::varQuery);
        }

        @Override
        public int varPattern() {
            return sum(Term::varPattern);
        }

        @Override
        public int structureSurface() {
            return intifyShallow(0, (s, x) -> s | x.opBit());
        }

        @Override
        public int addAllTo(Term[] t, int offset) {
            int s = subs();
            for (int i = 0; i < s; )
                t[offset++] = sub(i++);
            return s;
        }

        @Override
        public boolean subEquals(int i, /*@NotNull*/ Term x) {
            return subs() > i && sub(i).equals(x);
        }

        @Override
        public /*@NotNull*/ SortedSet<Term> toSetSorted() {
            SortedSet<Term> u = new MetalTreeSet();
            addAllTo(u);
            return u;
        }

        @Override
        @SuppressWarnings("LambdaUnfriendlyMethodOverload")
        public /*@NotNull*/ SortedSet<Term> toSetSorted(UnaryOperator<Term> map) {
            MetalTreeSet<Term> u = new MetalTreeSet();
            addAllTo(u);
            return u;
        }

        @Override
        public /*@NotNull*/ SortedSet<Term> toSetSorted(Predicate<Term> t) {
            int s = subs();
            switch (s) {
                case 1:
                    Term the = sub(0);
                    return t.test(the) ? ArrayUnenforcedSortedSet.the(the) : ArrayUnenforcedSortedSet.empty;
                case 2:
                    Term a = sub(0), b = sub(1);
                    boolean aok = t.test(a), bok = t.test(b);
                    if (aok && bok) {
                        return ArrayUnenforcedSortedSet.the(a, b);
                    } else if (!aok && !bok) {
                        return ArrayUnenforcedSortedSet.empty;
                    } else if (!bok) {
                        return ArrayUnenforcedSortedSet.the(a);
                    } else
                        return ArrayUnenforcedSortedSet.the(b);
                default:
                    List<Term> u = new FasterList<>(s);
                    for (Term x : this) {
                        if (t.test(x)) u.add(x);
                    }
                    int us = u.size();
                    if (us == s) {
                        if (false)
                            return ArrayUnenforcedSortedSet.the(this.arrayShared());
                    }
                    switch (us) {
                        case 0:
                            return ArrayUnenforcedSortedSet.empty;
                        case 1:
                            return ArrayUnenforcedSortedSet.the(u.get(0));
                        case 2:
                            return ArrayUnenforcedSortedSet.the(u.get(0), u.get(1));
                        default:
                            return ArrayUnenforcedSortedSet.the(u.toArray(Op.EmptyTermArray));
                    }
            }

        }

        @Override
        public Term[] arrayShared() {
            return arrayClone();
        }

        @Override
        public Term[] arrayClone() {
            int s = subs();
            return s == 0 ? Op.EmptyTermArray : arrayClone(new Term[s], 0, s);
        }

        @Override
        public Term[] arrayClone(Term[] target) {
            return arrayClone(target, 0, subs());
        }

        @Override
        public Term[] arrayClone(Term[] target, int from, int to) {
            for (int i = from, j = 0; i < to; i++, j++)
                target[j] = this.sub(i);

            return target;
        }

        @Override
        public /*@NotNull*/ TermList toList() {
            return new TermList(this);
        }

        @Override
        public /*@NotNull*/ MutableSet<Term> toSet() {
            int s = subs();
            UnifiedSet<Term> u = new UnifiedSet(s, 0.99f);
            if (s > 0)
                addAllTo(u);
            return u;
        }

        @Override
        @Nullable
        public <C extends Collection<Term>> C collect(Predicate<Term> ifTrue, C c) {
            int s = subs();
            //UnifiedSet<Term> u = null;
            for (int i = 0; i < s; i++) {
                /*@NotNull*/
                Term x = sub(i);
                if (ifTrue.test(x)) {
//                    if (c == null)
//                        c = new UnifiedSet<>((s - i) * 2);
                    c.add(x);
                }
            }

            return c;
        }

        @Override
        public void setNormalized() {

        }

        @Override
        public boolean isNormalized() {
            return TermMetadata.normalized(this);
        }

        /*@NotNull*/
        @Override
        public Set<Term> recurseSubtermsToSet(Op _onlyType) {
            int onlyType = _onlyType.bit;
            if (!hasAny(onlyType))
                return Sets.mutable.empty();

            Set<Term> t = new HashSet(volume());


            recurseTerms(
                tt -> tt.hasAny(onlyType),
                tt -> {
                    if (tt.opBit() == onlyType)
                        t.add(tt);
                    return true;
                }, null);
            return t;
        }

        /*@NotNull*/
        @Override
        public boolean recurseSubtermsToSet(int inStructure, /*@NotNull*/ Collection<Term> t, boolean untilAddedORwhileNotRemoved) {
            boolean[] r = {false};
            Predicate<Term> selector = s -> {

                if (!untilAddedORwhileNotRemoved && r[0])
                    return false;

                if (s.hasAny(inStructure)) {
                    r[0] |= (untilAddedORwhileNotRemoved) ? t.add(s) : t.remove(s);
                }

                return true;
            };


            recurseTerms(
                (inStructure != -1) ?
                    p -> p.hasAny(inStructure)
                    :
                    any -> true,
                selector, null);


            return r[0];
        }

        @Override
        public boolean containsRecursively(/*@NotNull*/ Term x, boolean root, Predicate<Term> subTermOf) {

            if (!impossibleSubTerm(x)) {
                int s = subs();
                Term prev = null;
                for (int i = 0; i < s; i++) {
                    Term ii = sub(i);
                    if (different(prev, ii)) {
                        if (ii == x ||
                            ((root ? ii.equalsRoot(x) : ii.equals(x)) || ii.containsRecursively(x, root, subTermOf)))
                            return true;
                        prev = ii;
                    }
                }
            }
            return false;
        }

        @Override
        public boolean equalTerms(/*@NotNull*/ Subterms c) {
            int s = subs();
            if (s != c.subs())
                return false;
for (int i = 0; i < s; i++) {
if (!sub(i).equals(c.sub(i)))
return false;
}
return true;
        }

        @Override
        public void addAllTo(Collection target) {
forEach(target::add);
        }

        @Override
        public void addAllTo(FasterList target) {
            target.ensureCapacity(subs());
forEach(target::addFast);
        }

        @Override
        public /* final */ boolean impossibleSubStructure(int structure) {
            //return !hasAll(structure);
            return !Op.has(subStructure(), structure, true);
        }

        @Override
        public int subStructure() {
            assert (!(this instanceof Compound));
            return structure();
        }

        @Override
        public Term[] terms(/*@NotNull*/ IntObjectPredicate<Term> filter) {
            TermList l = null;
            int s = subs();
            for (int i = 0; i < s; i++) {
                Term t = sub(i);
                if (filter.accept(i, t)) {
                    if (l == null)
                        l = new TermList(subs() - i);
                    l.add(t);
                }
            }
            return l == null ? Op.EmptyTermArray : l.arrayKeep();
        }

        @Override
        public void forEach(Consumer<? super Term> action, int start, int stop) {
            if (start < 0 || stop > subs())
                throw new ArrayIndexOutOfBoundsException();

            for (int i = start; i < stop; i++)
                action.accept(sub(i));
        }

        @Override
        public boolean subIs(int i, Op o) {
            return sub(i).opID() == o.id;
        }

        @Override
        public int count(Predicate<Term> match) {
            return intifyShallow(0, (c, sub) -> match.test(sub) ? c + 1 : c);
        }

        @Override
        public boolean countEquals(Predicate<Term> match, int n) {
            int s = subs();
            if (n > s) return false; //impossible
            int c = 0;
            for (int i = 0; i < s; i++) {
                if (match.test(sub(i))) {
                    c++;
                    if (c > n)
                        return false;
                }
            }
            return c == n;
        }

        @Override
        public int count(Op matchingOp) {
            int matchingOpID = matchingOp.id;
            return count(x -> x.opID() == matchingOpID);
        }

        @Override
        public boolean subIsOrOOB(int i, Op o) {
            Term x = sub(i, null);
            return x != null && x.opID() == o.id;
        }

        @Override
        public /* final */ int indexOf(/*@NotNull*/ Term t) {
            return indexOf(t, -1);
        }

        @Override
        public int indexOf(/*@NotNull*/ Term t, int after) {
            return indexOf(t::equals, after);
        }

        @Override
        @Nullable
        public Term subFirst(Predicate<Term> match) {
            int i = indexOf(match);
            return i != -1 ? sub(i) : null;
        }

        @Override
        public boolean impossibleSubTerm(Termlike target) {
            return impossibleSubVolume(target.volume()) || impossibleSubStructure(target.structure());
        }

        @Override
        public boolean impossibleSubTerm(int structure, int volume) {
            return impossibleSubStructure(structure) || impossibleSubVolume(volume);
        }

        @Override
        public Stream<Term> subStream() {
            return Streams.stream(this);
        }

        @Override
        public int hashCodeSubterms() {
            return Subterms.hash(this);
        }

        @Override
        public MetalBitSet indicesOfBits(Predicate<Term> match) {
            int n = subs();
            MetalBitSet m = MetalBitSet.bits(n);
            for (int i = 0; i < n; i++) {
                if (match.test(sub(i)))
                    m.set(i);
            }
            return m;
        }

        @Override
        public Term[] subsIncluding(Predicate<Term> toKeep) {
            return subsIncExc(indicesOfBits(toKeep), true);
        }

        @Override
        public Term[] subsIncluding(MetalBitSet toKeep) {
            return subsIncExc(toKeep, true);
        }

        @Override
        public Term[] removing(MetalBitSet toRemove) {
            return subsIncExc(toRemove, false);
        }

        @Override
        @Nullable
        public Term[] subsIncExc(MetalBitSet s, boolean includeOrExclude) {

            int c = s.cardinality();

            if (c == 0) {
//            if (!includeOrExclude)
//                throw new UnsupportedOperationException("should not reach here");
                return includeOrExclude ? Op.EmptyTermArray : arrayShared();
            }

            int size = subs();
            assert (c <= size) : "bitset has extra bits setAt beyond the range of subterms";

            if (includeOrExclude) {
                if (c == size) return arrayShared();
                if (c == 1) return new Term[]{sub(s.first(true))};
            } else {
                if (c == size) return EmptyTermArray;
                if (c == 1) return removing(s.first(true));
            }


            int newSize = includeOrExclude ? c : size - c;
            Term[] t = new Term[newSize];
            int j = 0;
            for (int i = 0; i < size; i++) {
                if (s.get(i) == includeOrExclude)
                    t[j++] = sub(i);
            }
            return t;
        }

        /*@NotNull*/
        @Override
        public Term[] subRangeArray(int from, int to) {
            int n = subs();
            if (from == Integer.MIN_VALUE) from = 0;
            if (to == Integer.MAX_VALUE) to = n;

            if (from == 0 && to == n) {
                return arrayShared();

            } else {

                int s = to - from;
                if (s == 0)
                    return EmptyTermArray;
                else {
                    Term[] l = new Term[s];
                    int y = from;
                    for (int i = 0; i < s; i++)
                        l[i] = sub(y++);
                    return l;
                }
            }
        }

        @Override
        public int indexOf(/*@NotNull*/ Predicate<Term> p) {
            return indexOf(p, -1);
        }

        @Override
        public int indexOf(/*@NotNull*/ Predicate<Term> p, int after) {
            int s = subs();
            Term prev = null;
            for (int i = after + 1; i < s; i++) {
                Term next = sub(i);
                if (different(prev, next)) {
                    if (p.test(next))
                        return i;
                    prev = next;
                }
            }
            return -1;
        }

        @Override
        public boolean AND(/*@NotNull*/ Predicate<Term> p) {
            int s = subs();
            Term prev = null;
            for (int i = 0; i < s; i++) {
                Term next = sub(i);
                if (different(prev, next)) {
                    if (!p.test(next))
                        return false;
                    prev = next;
                }
            }
            return true;
        }

        @Override
        public boolean OR(/*@NotNull*/ Predicate<Term> p) {
            int s = subs();
            Term prev = null;
            for (int i = 0; i < s; i++) {
                Term next = sub(i);
                if (different(prev, next)) {
                    if (p.test(next))
                        return true;
                    prev = next;
                }
            }
            return false;
        }

        @Override
        public boolean ANDi(/*@NotNull*/ ObjectIntPredicate<Term> p) {
            int s = subs();
for (int i = 0; i < s; i++)
if (!p.accept(sub(i), i))
return false;
return true;
        }

        @Override
        public boolean ORi(/*@NotNull*/ ObjectIntPredicate<Term> p) {
            int s = subs();
for (int i = 0; i < s; i++)
if (p.accept(sub(i), i))
return true;
return false;
        }

        @Override
        public <X> boolean ORwith(/*@NotNull*/ BiPredicate<Term, X> p, X param) {
            int s = subs();
            Term prev = null;
            for (int i = 0; i < s; i++) {
                Term next = sub(i);
                if (different(prev, next)) {
                    if (p.test(next, param))
                        return true;
                    prev = next;
                }
            }
            return false;
        }

        @Override
        public <X> boolean ANDwith(/*@NotNull*/ BiPredicate<Term, X> p, X param) {
            int s = subs();
            Term prev = null;
            for (int i = 0; i < s; i++) {
                Term next = sub(i);
                if (different(prev, next)) {
                    if (!p.test(next, param))
                        return false;
                    prev = next;
                }
            }
            return true;
        }

        @Override
        public <X> boolean ANDwithOrdered(/*@NotNull*/ BiPredicate<Term, X> p, X param) {
            int s = subs();
for (int i = 0; i < s; i++) {
if (!p.test(sub(i), param))
return false;
}
return true;
        }

        @Override
        public boolean ANDrecurse(Predicate<Term> p) {
            int s = subs();
            Term prev = null;
            for (int i = 0; i < s; i++) {
                Term next = sub(i);
                if (different(prev, next)) {
                    if (!p.test(next) || (next instanceof Compound && !((Compound) next).ANDrecurse(p)))
                        return false;
                    prev = next;
                }
            }
            return true;
        }

        @Override
        public boolean ORrecurse(Predicate<Term> p) {
            int s = subs();
            Term prev = null;
            for (int i = 0; i < s; i++) {
                Term next = sub(i);
                if (different(prev, next)) {
                    if (p.test(next) || (next instanceof Compound && ((Compound) next).ORrecurse(p)))
                        return true;
                    prev = next;
                }
            }
            return false;
        }

        @Override
        public boolean recurseTerms(Predicate<Term> inSuperCompound, Predicate<Term> whileTrue, Compound parent) {
            return AND(s -> s.recurseTerms(inSuperCompound, whileTrue, parent));
        }

        @Override
        public boolean recurseTerms(Predicate<Compound> aSuperCompoundMust, BiPredicate<Term, Compound> whileTrue, Compound parent) {
            return AND(s -> s.recurseTerms(aSuperCompoundMust, whileTrue, parent));
        }

        @Override
        public boolean recurseTermsOrdered(Predicate<Term> inSuperCompound, Predicate<Term> whileTrue, Compound parent) {
int s = subs();
for (int i = 0; i < s; i++)
if (!sub(i).recurseTermsOrdered(inSuperCompound, whileTrue, parent))
return false;
return true;
        }

        @Override
        public Subterms reversed() {
            return RemappedSubterms.reverse(this);
        }

        @Override
        public Term[] removing(int index) {
            int s = subs();
            Term[] x = new Term[s - 1];
            int k = 0;
            for (int j = 0; j < s; j++) {
                if (j != index)
                    x[k++] = sub(j);
            }
            return x;

            //return ArrayUtils.remove(arrayShared(), Term[]::new, i);
        }

        @Override
        public int hashWith(Op op) {
            return hashWith(op.id);
        }

        @Override
        public int hashWith(/*byte*/int op) {
            return Util.hashCombine(this.hashCodeSubterms(), op);
        }

        @Override
        @Nullable
        public Term[] removing(Term x) {
            MetalBitSet toRemove = indicesOfBits(x::equals);
            return toRemove.cardinality() == 0 ? null : removing(toRemove);
        }

        @Override
        public Subterms transformSub(int which, UnaryOperator<Term> f) {
            Term x = sub(which);
            Term y = f.apply(x);
            //if (x == y)
            if (x.equals(y))
                return this;

            Term[] yy = arrayClone();
            yy[which] = y;
            return Op.terms.subterms(yy);
        }

        @Override
        @Nullable
        public Subterms transformSubs(UnaryOperator<Term> f, Op superOp) {

            TermList y = null;

            int s = subs();


            for (int i = 0; i < s; i++) {

                Term xi = sub(i);

                Term yi = f.apply(xi);

                //if (yi instanceof Bool) {
                if (yi == Bool.Null)
                    return null; //short-circuit
                //}

                if (yi instanceof Fragment) {

                    Subterms yy = yi.subterms();
                    if (s == 1) {
                        return (yy.subs() == 0) ?
                            EmptySubterms //the empty ellipsis is the only subterm
                            :
                            yy.transformSubs(f, superOp); //it is only this ellipsis match so inline it by transforming directly and returning it (tail-call)
                    } else {
                        y = Subterms.transformSubInline(yy, f, y, s, i);
                        if (y == null)
                            return null;
                    }


                } else {


                    if (y == null) {
                        if (differentlyTransformed(xi, yi) /* special */)
                            y = new DisposableTermList(s, i);
//                    else Util.nop(); ///why
                    }

                    if (y != null)
                        y.addFast(yi);
                }
            }

            return y != null ? y.commit(this) : this;
        }

        @Override
        public boolean these() {
            return AND(Term::the);
        }

        @Override
        public boolean containsPosOrNeg(Term x) {
            //TODO optimize
            return contains(x) || containsNeg(x);
        }

        @Override
        public boolean containsNeg(Term x) {
            return x instanceof Neg ? contains(x.unneg()) : hasAny(NEG) && !impossibleSubTerm(x.structure() | NEG.bit, x.volume() + 1)
                &&
                ORwith((z, xx) -> z instanceof Neg && xx.equals(z.unneg()), x);
        }
    }


}
