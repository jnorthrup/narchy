package nars.term.compound;

import jcog.Util;
import jcog.data.byt.DynBytes;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.util.builder.TermBuilder;
import org.eclipse.collections.api.block.function.primitive.ByteFunction0;
import org.eclipse.collections.api.block.function.primitive.IntObjectToIntFunction;
import org.eclipse.collections.api.tuple.primitive.ObjectBytePair;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectByteHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.List;

import static nars.Op.EmptyTermArray;
import static nars.time.Tense.DTERNAL;

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

    @Override
    public String toString() {
        return Compound.toString(this);
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
        } else {
            return Compound.equals(this, that,false);
        }
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
    }


}
