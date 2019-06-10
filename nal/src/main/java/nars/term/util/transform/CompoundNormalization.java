package nars.term.util.transform;

import nars.term.Compound;
import nars.term.Term;
import nars.term.util.Image;

import static nars.Op.INH;
import static nars.time.Tense.XTERNAL;

/** procedure for Compound target Normalization */
public final class CompoundNormalization extends VariableNormalization {

    private final Term root;
    final boolean imgPossible;

    /** TODO check */
    static final int MIN_IMAGE_VOL = 4;

    public CompoundNormalization(Term x, byte varOffset) {
        super(x.vars() /* estimate */, varOffset);
        this.root = x;
        this.imgPossible = x.volume() >= MIN_IMAGE_VOL && x.hasAll(Image.ImageBits);
    }


    @Override
    public boolean preFilter(Compound x) {
        return x.hasVars() || (imgPossible && x.hasAll(Image.ImageBits));
    }

    @Override
    protected Term applyFilteredPosCompound(Compound x) {
        /* if x is not the root target (ie. a subterm) */
        boolean hasImg = imgPossible && x.hasAll(Image.ImageBits);
        if (hasImg && x!=root && x.op()==INH) {
            Compound y = (Compound) Image._imgNormalize(x);
            hasImg = (x!=y) && y.hasAll(Image.ImageBits); //check if image bits remain
            x = y;
        }
        return hasImg || x.hasVars() ? super.applyCompound(x, null, XTERNAL) : x;
    }


}
