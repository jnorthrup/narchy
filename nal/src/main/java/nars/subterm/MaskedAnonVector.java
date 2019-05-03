package nars.subterm;

import nars.term.Term;
import nars.term.anon.AnonID;

import static nars.term.anon.AnonID.ANOMs;

abstract public class MaskedAnonVector extends ProxySubterms<AnonSubterms> {

    protected MaskedAnonVector(AnonSubterms ref) {
        super(ref);
    }

    @Override
    public Term sub(int i) {
        short x = ref.subRaw(i);
        short ax = x < 0 ? (short) -x : x;
        Term y;
        if (AnonID.mask(ax)== ANOMs) {
            y = atom((ax & 0xff)-1);
        } else {
            y = AnonID.termPos(ax);
        }
        return y.negIf(x < 0);
    }

    /** returns the facade atom for the Anom index id minus 1*/
    public abstract Term atom(int index);

    public static class SubtermsMaskedAnonVector extends MaskedAnonVector {
        private final Subterms mask;

        public SubtermsMaskedAnonVector(AnonSubterms skeleton, Subterms mask) {
            super(skeleton);
            this.mask = mask;
        }

        @Override
        public Term atom(int index) {
            return mask.sub(index);
        }
    }
}
