package nars.truth.dynamic;

import nars.Task;
import nars.task.util.TaskRegion;
import nars.truth.Truth;
import nars.truth.func.NALTruth;

abstract public class AbstractSectTruth extends AbstractDynamicTruth {




    @Override
    public final Truth truth(DynTaskify l) {
        return apply(l, truthNegComponents(), negResult());
    }

    protected abstract boolean truthNegComponents();
    protected abstract boolean negResult();

    private Truth apply(DynTaskify d, boolean negComponents, boolean negResult) {
        Truth y = null;
        for (int i = 0, dSize = d.size(); i < dSize; i++) {
            Task li = d.get(i);
            Truth x = li.truth();
            if (x == null)
                return null;

            if (negComponents ^ !d.componentPolarity.get(i))
                x = x.neg();

            if (y == null) {
                y = x;
            } else {
                y = NALTruth.Intersection.apply(y, x, null);
                if (y == null)
                    return null;
            }
        }


        return y.negIf(negResult);
    }

    @Override
    public int componentsEstimate() {
        return 8;
    }
}
