package nars.term.util.transform;

import nars.term.Compound;
import nars.term.Term;
import nars.term.Variable;
import nars.term.atom.Atomic;
import nars.term.util.Image;
import org.jetbrains.annotations.Nullable;

import static nars.Op.INH;

/** procedure for Compound term Normalization */
public final class CompoundNormalization extends VariableNormalization {

    private final Term root;

    public CompoundNormalization(Term root, byte varOffset) {
        super(root.vars() /* estimate */, varOffset);
        this.root = root;
    }

    @Override
    public Term transform(Term x) {
        if (x instanceof Compound) {
            if (x.hasVars() || x.hasAll(Image.ImageBits))
                return transformCompound((Compound)x);
        } else {
            if (x instanceof Variable) // || x instanceof ImDep)
                return transformAtomic((Atomic)x);
        }
        return x;
    }

    @Override
    protected @Nullable Term transformNonNegCompound(Compound x) {
        /* if x is not the root term (ie. a subterm) */
        boolean hasImg = x.hasAll(Image.ImageBits);
        if (hasImg && x!=root && x.op()==INH) {
            Term y = Image.normalize(x);
            if (x!=y) {
                if (!(y instanceof Compound))
                    return y;
                x = (Compound) y;
                hasImg = x.hasAll(Image.ImageBits); //check if image bits remain
            }

                //hasImg = false;
//                if (y!=x) {
//                    x = y;

//                }
        }
        return hasImg || x.hasVars() ? x.transform(this) : x;
    }


}
