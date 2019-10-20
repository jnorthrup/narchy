package nars.subterm;

import nars.term.Term;
import nars.term.anon.Intrin;

import static nars.term.anon.Intrin.ANOMs;

public abstract class MaskedIntrinSubterms extends ProxySubterms<IntrinSubterms> {

    protected MaskedIntrinSubterms(IntrinSubterms ref) {
        super(ref);
    }

    @Override
    public Term sub(int i) {
        short x = ref.subRaw(i);
        short ax = (int) x < 0 ? (short) -(int) x : x;
        Term y = Intrin.group((int) ax) == (int) ANOMs ? atom(((int) ax & 0xff) - 1) : Intrin._term(ax);
        return y.negIf((int) x < 0);
    }

    /** returns the facade atom for the Anom index id minus 1*/
    public abstract Term atom(int index);

    public static class SubtermsMaskedIntrinSubterms extends MaskedIntrinSubterms {
        private final Subterms mask;

        public SubtermsMaskedIntrinSubterms(IntrinSubterms skeleton, Subterms mask) {
            super(skeleton);
            this.mask = mask;
        }

        @Override
        public Term atom(int index) {
            return mask.sub(index);
        }
    }
}
