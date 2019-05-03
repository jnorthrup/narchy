package nars.term;

import nars.NAL;
import nars.Op;
import nars.The;
import nars.term.anon.AnonID;
import nars.term.atom.Atomic;
import nars.term.compound.SemiCachedUnitCompound;
import nars.term.compound.UnitCompound;
import nars.term.util.TermException;
import org.jetbrains.annotations.Nullable;

import static nars.Op.NEG;
import static nars.term.atom.Bool.Null;

public enum Neg { ;

    public static Term neg(Term u) {


        Op uo = u.op();
        switch (uo) {
            case ATOM:

                break;
            case BOOL:
                return u.neg();
            case NEG:
                return u.unneg();

            case FRAG:
                if (NAL.DEBUG)
                    throw new TermException("fragment can not be negated", u);
                return Null;

            case IMG:
                return u; //return Null;
        }

        if (u instanceof AnonID)
            return new NegAnonID(((AnonID)u).i);

        return new NegCached(u);
    }



    private static final class NegCached extends SemiCachedUnitCompound implements The {


        NegCached(Term negated) {
            super(NEG.id, negated);
        }


        @Override
        public Op op() {
            return NEG;
        }


        @Override
        public Term root() {
            Term x = unneg(), y = x.root();
            return y != x ? y.neg() : this;
        }

        @Override
        public Term concept() {
            return unneg().concept();
        }


        @Override
        public @Nullable Term normalize(byte varOffset) {
            Term x = unneg();
            Term y = x instanceof Variable ? ((Variable) x).normalizedVariable((byte) (varOffset + 1)) : x.normalize(varOffset);
            if (y != x)
                return y.neg();
            return this;
        }

        @Override
        public Term neg() {
            return sub;
        }

        @Override
        public Term negIf(boolean negate) {
            return negate ? sub : this;
        }

        @Override
        public Term unneg() {
            return sub;
        }


        @Override
        public final boolean equalsNeg(Term t) {
            return sub.equals(t);
        }

    }

    /** TODO refine */
    public static final class NegAnonID extends UnitCompound implements The {

        private final int sub;

        NegAnonID(int negated) {
            this.sub = negated;
        }

        @Override
        public Term sub() {
            return AnonID.term(sub);
        }

        @Override
        public Op op() {
            return NEG;
        }

        @Override
        public Term root() {
            return this;
        }

        @Override
        public Term concept() {
            return unneg().concept();
        }

        @Override
        public @Nullable Term normalize(byte varOffset) {
            Term x = unneg();
            Term y = x instanceof Variable ? ((Variable) x).normalizedVariable((byte) (varOffset + 1)) : x.normalize(varOffset);
            if (y != x)
                return y.neg();
            return this;
        }

        @Override
        public Term neg() {
            return sub();
        }

        @Override
        public Term negIf(boolean negate) {
            return negate ? sub() : this;
        }

        @Override
        public Term unneg() {
            return sub();
        }

        @Override
        public final boolean equalsNeg(Term t) {
            return t instanceof Atomic && sub().equals(t);
        }

    }

    public final class NegLight extends UnitCompound implements The {
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
        public Term root() {
            Term x = unneg(), y = x.root();
            return y != x ? y.neg() : this;
        }

        @Override
        public Term concept() {
            return unneg().concept();
        }


        @Override
        public @Nullable Term normalize(byte varOffset) {
            Term x = unneg();
            Term y = x instanceof Variable ? ((Variable) x).normalizedVariable((byte) (varOffset + 1)) : x.normalize(varOffset);
            if (y != x)
                return y.neg();
            return this;
        }

        @Override
        public Term neg() {
            return sub;
        }

        @Override
        public Term negIf(boolean negate) {
            return negate ? sub : this;
        }

        @Override
        public Term unneg() {
            return sub;
        }


        @Override
        public final boolean equalsNeg(Term t) {
            return sub.equals(t);
        }

    }
}