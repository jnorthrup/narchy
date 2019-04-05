package nars.concept.sensor;

import jcog.data.NumberX;
import jcog.data.atomic.AtomicFloat;
import jcog.math.FloatSupplier;
import nars.NAR;
import nars.agent.Game;
import nars.term.Term;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatFloatToObjectFunction;

/** accepts a scalar input that is decomposed into components represented via multiple concepts */
abstract public class DemultiplexedScalarSensor extends VectorSensor implements FloatSupplier {

    public final NumberX value = new AtomicFloat();
    public final FloatSupplier input;
    public final FloatFloatToObjectFunction<Truth> truther;
    public final Term term;

    public DemultiplexedScalarSensor(FloatSupplier input, Term root, FloatFloatToObjectFunction<Truth> truther, NAR n) {
        super(n);
        this.term = root;
        this.truther = truther;
        this.input = input;
    }

    public void updatePrevNow(long prev, long now, Game g) {

        if (input!=null)
            value.set(input.asFloat());

        super.updatePrevNow(prev, now, g);
    }


    @Override
    public float asFloat() {
        return value.floatValue();
    }

}
