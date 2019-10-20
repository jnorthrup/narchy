package nars.term.util.transform;

import nars.term.Compound;
import nars.term.Term;
import nars.term.util.Image;

import static nars.Op.INH;

/** procedure for Compound target Normalization */
public final class CompoundNormalization extends VariableNormalization {

    private final Term root;
    private final boolean imgPossible;

    /** TODO check */
    private static final int MIN_IMAGE_VOL = 4;

    public CompoundNormalization(Term x, byte varOffset) {
        super(x.vars() /* estimate */, (int) varOffset);
        this.root = x;
        this.imgPossible = x.volume() >= MIN_IMAGE_VOL && x.hasAll(Image.ImageBits);
    }


    @Override public boolean preFilter(Compound x) {
        return super.preFilter(x) || (imgPossible && x.hasAll(Image.ImageBits));
    }

    @Override
    public Term applyPosCompound(Compound x) {
        /* if x is not the root target (ie. a subterm) */
        boolean hasImg = imgPossible && x.hasAll(Image.ImageBits);
        if (hasImg && x!=root && x.opID()== (int) INH.id) {
            Compound y = (Compound) Image._imgNormalize(x);
            if (x!=y) {
                x = y;
                hasImg = x.hasAll(Image.ImageBits); //check if image bits remain
            }
        }
        return hasImg || x.hasVars() ? super.applyPosCompound(x) : x;
    }


}
