package nars.concept.scalar;

import jcog.Util;
import jcog.math.FloatSupplier;
import jcog.util.ArrayIterator;
import nars.$;
import nars.NAR;
import nars.term.Term;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.function.IntFunction;

/** calculates a set of derived scalars from an input scalar */
public class FilteredScalar extends DemultiplexedScalar {

    final Filter[] window;

//    public FilteredScalar(FloatSupplier input, @Nullable Term id, NAR nar) {
//        super(input, id, nar);
//    }

    public FilteredScalar(@Nullable Term id, FloatSupplier input, int filters, IntFunction<Filter> windowBuilder, NAR nar) {
        super(input, id, nar);
        this.window = Util.map(0, filters, windowBuilder, Filter[]::new);

        for (Scalar s : window)
            nar.on(s);

        nar.on(this);
    }

    public static FilteredScalar filter(@Nullable Term id,
                                       FloatSupplier input,
                                       NAR nar,
                                       FloatToFloatFunction... filters) {
        return new FilteredScalar(id, input, filters.length,
                (f) -> new Filter(f == 0 ? id : $.p(id, $.the("f" + f)),
                input, filters[f], nar), nar);
    }

    @Override
    public Iterator<Scalar> iterator() {
        return ArrayIterator.get(window);
    }

    static class Filter extends Scalar {

        //TODO
        //public float belief; //relative priority of generated beliefs
        //public float goal; //relative priority of generated goals

        Filter(Term id, FloatSupplier input, FloatToFloatFunction f, NAR nar) {
            super(id, nar,
                    () -> f.valueOf(input.asFloat()));

        }
    }
}
