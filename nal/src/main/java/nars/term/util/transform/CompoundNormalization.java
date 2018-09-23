package nars.term.util.transform;

import nars.term.Compound;
import nars.term.Term;
import nars.term.util.Image;
import org.jetbrains.annotations.Nullable;

/** procedure for Compound term Normalization */
public class CompoundNormalization extends VariableNormalization {

    private final Term root;

    public CompoundNormalization(Term root, byte varOffset) {
        super(root.vars() /* estimate */, varOffset);
        this.root = root;
    }

    @Override
    protected @Nullable Term transformNonNegCompound(Compound x) {
        if (!x.equals(root)) {
            /* if x is not the root term (ie. a subterm) */
//            Term y = Image.imageNormalize(x);
//            if (y!=x) {
//                Termed yy = transform(y);
//                if (yy != null)
//                    return yy.term();
//            }
            x = (Compound) Image.imageNormalize(x);
        }
        return super.transformNonNegCompound(x);
    }


}
