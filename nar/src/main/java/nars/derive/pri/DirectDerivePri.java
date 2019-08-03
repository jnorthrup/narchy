package nars.derive.pri;

import jcog.TODO;
import jcog.math.FloatRange;
import jcog.pri.ScalarValue;
import nars.Task;
import nars.derive.Derivation;
import nars.truth.Truth;

public class DirectDerivePri implements DerivePri {

    public final FloatRange gain = new FloatRange(1f, ScalarValue.EPSILON, 1f);

    @Override
    public float pri(Task t, Derivation d) {
        return d.parentPri() * gain.floatValue();
    }

    @Override
    public float prePri(float priBase, Truth concTruth) {
        throw new TODO();
    }


}
