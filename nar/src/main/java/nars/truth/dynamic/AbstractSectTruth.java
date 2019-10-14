package nars.truth.dynamic;

import nars.truth.MutableTruth;
import nars.truth.Truth;
import nars.truth.func.NALTruth;

abstract public class AbstractSectTruth extends AbstractDynamicTruth {




    @Override
    public final Truth truth(DynTaskify l) {
        return apply(l, truthNegComponents(), negResult());
    }

    protected abstract boolean truthNegComponents();
    protected abstract boolean negResult();

    private static Truth apply(DynTaskify d, boolean negComponents, boolean negResult) {
        MutableTruth y = null;
        for (int i = 0, dSize = d.size(); i < dSize; i++) {
            Truth x = d.truth(i);
//            if (x == null)
//                return null;

            if (negComponents ^ !d.componentPolarity.get(i))
                x = x.neg();

            if (y == null) {
                y = new MutableTruth(x);
            } else {
                Truth yy = NALTruth.Intersection.apply(y, x, null);
                if (yy == null)
                    return null;
                y.set(yy);
            }
        }


        return y.negateThisIf(negResult);
    }

    @Override
    public int componentsEstimate() {
        return 8;
    }
}
