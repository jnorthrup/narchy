package nars.game.sensor;

import jcog.data.NumberX;
import jcog.data.atomic.AtomicFloat;
import jcog.math.FloatSupplier;
import nars.NAR;
import nars.game.Game;
import nars.term.Term;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatFloatToObjectFunction;

/** accepts a scalar input that is decomposed into components represented via multiple concepts */
abstract public class DemultiplexedScalarSensor extends VectorSensor implements FloatSupplier {

    public final NumberX value = new AtomicFloat();
    public FloatSupplier input;
    public final FloatFloatToObjectFunction<Truth> truther;
    public final Term term;

    protected DemultiplexedScalarSensor(FloatSupplier input, Term root, FloatFloatToObjectFunction<Truth> truther, NAR n) {
        super(root, n);
        this.term = root;
        this.truther = truther;
        this.input = input;
    }

    @Override public void accept(Game g) {
        value.set(input.asFloat());
        super.accept(g);
    }


    @Override
    public float asFloat() {
        return value.floatValue();
    }

}
