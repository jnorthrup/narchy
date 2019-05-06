package nars.term;

import nars.Op;
import nars.The;
import nars.term.anon.Intrin;
import nars.term.compound.SemiCachedUnitCompound;
import nars.term.compound.UnitCompound;
import nars.term.util.TermException;
import org.jetbrains.annotations.Nullable;

import static nars.Op.NEG;

public interface Neg extends Term { ;


    static Term neg(Term u) {

        Op uo = u.op();
        switch (uo) {
            case BOOL:
                return u.neg();

            case NEG:
                return u.unneg();

            case FRAG:
                throw new TermException("fragment can not be negated", u);

            case IMG:
                return u; //return Null;
        }

        if (u instanceof Intrin)
            return new NegIntrin(((Intrin)u).i);
        else
            return new NegCached(u);
    }

    Term sub();

    @Override
    default Term root() {
        Term x = sub(), y = x.root();
        return y != x ? y.neg() : this;
    }

    @Override
    default Term concept() {
        return sub().concept();
    }


    @Override
    @Nullable default Term normalize(byte varOffset) {
        Term x = sub();
        Term y = x instanceof Variable ? ((Variable) x).normalizedVariable((byte) (varOffset + 1)) : x.normalize(varOffset);
        if (y != x)
            return y.neg();
        return this;
    }

    @Override
    default Term neg() {
        return sub();
    }

    @Override
    default Term negIf(boolean negate) {
        return negate ? sub() : this;
    }

    @Override
    default Term unneg() {
        return sub();
    }

    @Override
    default boolean equalsNeg(Term t) {
        return sub().equals(t);
    }




    final class NegCached extends SemiCachedUnitCompound implements The, Neg {


        NegCached(Term negated) {
            super(NEG.id, negated);
        }

        @Override
        public Op op() {
            return NEG;
        }

        @Override
        public Term concept() {
            return Neg.super.concept();
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
    final class NegIntrin extends UnitCompound implements The, Neg {

        public final int sub;

        public NegIntrin(Intrin i) {
            this(i.i);
        }

        public NegIntrin(int negated) {
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
        public Term concept() {
            return this;
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
            //return t instanceof Atomic && sub().equals(t);
            return t instanceof Intrin && ((Intrin)t).i == sub;
        }

    }

    final class NegLight extends UnitCompound implements The, Neg {
        private final Term sub;

        public NegLight(Term negated) {
            this.sub = negated;
        }

        @Override
        public Term sub() {
            return sub;
        }

        @Override
        public Op op() {
            return NEG;
        }

        @Override
        public Term concept() {
            return Neg.super.concept();
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