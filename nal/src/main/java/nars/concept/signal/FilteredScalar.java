package nars.concept.signal;

import jcog.Util;
import jcog.math.FloatSupplier;
import jcog.util.ArrayIterator;
import nars.NAR;
import nars.term.Term;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import org.eclipse.collections.api.tuple.Pair;

import java.util.Iterator;

import static nars.Op.CONJ;

/** calculates a set of derived scalars from an input scalar */
public class FilteredScalar extends DemultiplexedScalar {

    public final Filter[] filter;

    public FilteredScalar(FloatSupplier input, NAR nar, Pair<Term,FloatToFloatFunction>... filters) {
        super(input,
                
                CONJ.the
                    (Util.map(Pair::getOne, Term[]::new, filters)), nar);

        this.filter = new Filter[filters.length];

        int j = 0;
        for (Pair<Term,FloatToFloatFunction> p : filters) {
            filter[j++] = new Filter(p.getOne(), input, p.getTwo(), nar);
        }

        for (Signal s : filter)
            nar.on(s);

        nar.on(this);
    }












    @Override
    public Iterator<Signal> iterator() {
        return ArrayIterator.get(filter);
    }

    /** TODO use Scalar */
    @Deprecated public static class Filter extends Signal {

        Filter(Term id, FloatSupplier input, FloatToFloatFunction f, NAR nar) {
            super(id,
                () -> f.valueOf(input.asFloat()),
                nar);
        }
    }
}
