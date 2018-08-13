package nars.concept.sensor;

import jcog.Util;
import jcog.math.FloatSupplier;
import nars.NAR;
import nars.term.Term;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatFloatToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import org.eclipse.collections.api.tuple.Pair;

import java.util.Iterator;
import java.util.List;

import static nars.Op.CONJ;

/**
 * calculates a set of derived scalars from an input scalar
 */
public class FilteredScalar extends DemultiplexedScalar {

    public final List<Signal> components;

    public FilteredScalar(FloatSupplier input, FloatFloatToObjectFunction<Truth> truther, NAR nar, Pair<Term, FloatToFloatFunction>... filters) {
        super(input,

                CONJ.the
                        (Util.map(Pair::getOne, Term[]::new, filters)), nar, truther);

        Filter[] filter = new Filter[filters.length];

        int j = 0;
        for (Pair<Term, FloatToFloatFunction> p : filters) {
            filter[j++] = new Filter(p.getOne(), input, p.getTwo(), nar);
        }

        for (Signal s : filter)
            nar.on(s);

        nar.on(this);

        this.components = List.of(filter);
    }


    @Override
    public Iterator<Signal> iterator() {
        return components.iterator();
    }

    /**
     * TODO use Scalar
     */
    @Deprecated
    public static class Filter extends Signal {

        Filter(Term id, FloatSupplier input, FloatToFloatFunction f, NAR nar) {
            super(id,
                    () -> f.valueOf(input.asFloat()),
                    nar);
        }
    }
}
