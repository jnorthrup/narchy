package nars.truth.dynamic;

import nars.NAR;
import nars.Task;
import nars.task.util.TaskRegion;
import nars.truth.Truth;
import nars.truth.func.NALTruth;
import org.jetbrains.annotations.Nullable;

abstract public class AbstractSectTruth extends AbstractDynamicTruth {




    @Override
    public final Truth truth(DynTaskify l) {
        return apply(l, truthNegComponents(), negResult());
    }

    protected abstract boolean truthNegComponents();
    protected abstract boolean negResult();

    @Nullable
    private Truth apply(DynTaskify d, boolean negComponents, boolean negResult) {
        Truth y = null;
        final NAR nar = d.nar;
        for (int i = 0, dSize = d.size(); i < dSize; i++) {
            TaskRegion li = d.get(i);
            Truth x = (((Task) li)).truth();
            if (x == null)
                return null;

            if (negComponents ^ !d.componentPolarity.get(i))
                x = x.neg();

            if (y == null) {
                y = x;
            } else {
                y = NALTruth.Intersection.apply(y, x, nar);
                if (y == null)
                    return null;
            }
        }


        return y.negIf(negResult);
    }

}
