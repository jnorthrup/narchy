package nars.concept.sensor;

import jcog.data.list.FasterList;
import nars.NAR;
import nars.term.Term;

import java.util.Iterator;
import java.util.List;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;

public class SelectorSensor extends VectorSensor {

    final List<Signal> choices;

    public SelectorSensor(IntSupplier value, int[] values, IntFunction<Term> termizer, NAR n) {
        super(n);
        assert(values.length > 1);
        choices = new FasterList<>(values.length);
        for (int e : values) {
            choices.add(new Signal(termizer.apply(e), () -> value.getAsInt() == e ? 1f : 0f, n));
        }

    }

    @Override
    public Iterator<Signal> iterator() {
        return choices.iterator();
    }
}
