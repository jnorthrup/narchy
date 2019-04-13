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
    private final short cause;

    public SelectorSensor(IntSupplier value, int[] values, IntFunction<Term> termizer, NAR n) {
        super(n);
        assert(values.length > 1);
        this.cause = n.newCause(id).id;
        choices = new FasterList<>(values.length);
        for (int e : values) {
            choices.add(new Signal(termizer.apply(e), cause, () -> value.getAsInt() == e ? 1f : 0f, n));
        }

    }

    @Override
    public int size() {
        return choices.size();
    }

    @Override
    public Iterator<Signal> iterator() {
        return choices.iterator();
    }
}
