package nars.term.util.transform;

import nars.term.Compound;
import nars.term.Term;
import nars.term.Variable;
import nars.term.atom.Atomic;
import nars.term.util.Image;

import static nars.Op.INH;
import static nars.time.Tense.XTERNAL;

/** procedure for Compound target Normalization */
public final class CompoundNormalization extends VariableNormalization {

    private final Term root;

    public CompoundNormalization(Term root, byte varOffset) {
        super(root.vars() /* estimate */, varOffset);
        this.root = root;
    }

    @Override
    public Term apply(Term x) {
        if (x instanceof Compound) {
            if (x.hasVars() || x.hasAll(Image.ImageBits))
                return applyCompound((Compound)x);
        } else {
            if (x instanceof Variable) // || x instanceof ImDep)
                return applyAtomic((Atomic)x);
        }
        return x;
    }

    @Override
    public boolean preFilter(Compound x) {
        return x.hasVars() || x.hasAll(Image.ImageBits);
    }

    @Override
    protected Term applyFilteredPosCompound(Compound x) {
        /* if x is not the root target (ie. a subterm) */
        boolean hasImg = x.hasAll(Image.ImageBits);
        if (hasImg && x!=root && x.op()==INH) {
            Term y = Image._imgNormalize(x);
            if (x!=y) {
//                if (!(y instanceof Compound))
//                    return Null; //wtf
                x = (Compound) y;
                hasImg = x.hasAll(Image.ImageBits); //check if image bits remain
            }

                //hasImg = false;
//                if (y!=x) {
//                    x = y;

//                }
        }
        return hasImg || x.hasVars() ? super.applyCompound(x, null, XTERNAL) : x;
    }


}
