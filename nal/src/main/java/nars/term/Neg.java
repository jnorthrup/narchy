package nars.term;

import jcog.Skill;
import nars.Op;
import nars.Idempotent;
import nars.term.anon.Intrin;
import nars.term.atom.Atomic;
import nars.term.compound.SemiCachedUnitCompound;
import nars.term.compound.UnitCompound;
import org.jetbrains.annotations.Nullable;

import static nars.Op.NEG;

@Skill("Negativity_bias") public interface Neg extends Term { ;


    Term sub();

    @Override
    default boolean equalsPosOrNeg(Term t) {
        return t instanceof Neg ? equals(t) : equalsNeg(t);
    }

    @Override
    default Term root() {
        Term x = sub(), y = x.root();
        return y != x ? y.neg() : this;
    }

    @Override
    default @Nullable Term normalize(byte varOffset) {
        Term x = sub();
        Term y = x instanceof Variable ? ((Variable) x).normalizedVariable((byte) (varOffset + 1)) : x.normalize(varOffset);
        return y != x ? y.neg() : this;
    }

    @Override
    default Term neg() {
        return sub();
    }

    @Override
    default Term unneg() {
        return sub();
    }

    @Override
    default boolean equalsNeg(Term t) {
        return sub().equals(t);
    }




    final class NegCached extends SemiCachedUnitCompound implements Idempotent, Neg {


        public NegCached(Term negated) {
            super(NEG.id, negated);
        }

        @Override
        public Op op() {
            return NEG;
        }
        @Override
        public int opID() {
            return NEG.id;
        }

        @Override
        public Term root() {
            return Neg.super.root();
        }

        @Override
        public Term normalize(byte varOffset) {
            return Neg.super.normalize(varOffset);
        }
    }

    /** TODO refine */
    final class NegIntrin extends UnitCompound implements Idempotent, Neg {

        public final short sub;

        public NegIntrin(Atomic i) {
            this(i.intrin());
        }

        public NegIntrin(short negated) {
            //assert(negated!=0);
            this.sub = negated;

        }

        @Override
        public Term sub() {
            return Intrin.term(sub);
        }

        @Override
        public Op op() {
            return NEG;
        }

        @Override
        public int opID() {
            return NEG.id;
        }

        @Override
        public Term root() {
            return this;
        }

        @Override
        public Term normalize(byte varOffset) {
            switch (Intrin.group(sub)) {
                case Intrin.VARDEPs:
                case Intrin.VARINDEPs:
                case Intrin.VARPATTERNs:
                case Intrin.VARQUERYs:
                    return Neg.super.normalize(varOffset);
            }
            return this;
        }

        @Override
        public final boolean equalsNeg(Term t) {
            if (t instanceof Atomic) {
                int i = ((Atomic)t).intrin();
                if (i!=0)
                    return sub==i;
            }
            return false;
        }

    }

    final class NegLight extends UnitCompound implements Idempotent, Neg {
        private final Term sub;

        public NegLight(Term negated) {
            this.sub = negated;
        }

//        /** warning: promiscuous reference re-sharing implementation */
//        @Override public boolean equals(@Nullable Object that) {
//            if (this == that)
//                return true;
//            if (Compound.equals(this, that, false)) {
//                Term x = ((Compound)that).sub(0);
//                if (x!=sub && System.identityHashCode(x) < System.identityHashCode(sub)) {
//                    sub = x;
//                }
//                return true;
//            }
//            return false;
//        }

        @Override
        public Term sub() {
            return sub;
        }

        @Override
        public Op op() {
            return NEG;
        }
        @Override
        public int opID() {
            return NEG.id;
        }

        @Override
        public Term root() {
            return Neg.super.root();
        }

        @Override
        public Term normalize(byte varOffset) {
            return Neg.super.normalize(varOffset);
        }
    }
}