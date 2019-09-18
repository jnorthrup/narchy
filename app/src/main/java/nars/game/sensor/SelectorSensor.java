package nars.game.sensor;

import jcog.data.list.FasterList;
import nars.NAR;
import nars.term.Term;

import java.util.Iterator;
import java.util.List;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;

public class SelectorSensor extends VectorSensor {

    final List<ComponentSignal> choices;
    private final short[] cause;

    public SelectorSensor(IntSupplier value, int[] values, IntFunction<Term> termizer, NAR n) {
        super(n);
        assert(values.length > 1);
        this.cause = new short[] { n.newCause(id).id } ;
        choices = new FasterList<>(values.length);
        for (int e : values) {
            choices.add(newComponent(
                termizer.apply(e),
                () -> value.getAsInt() == e ? 1f : 0f)
            );
        }

    }

    @Override
    public int size() {
        return choices.size();
    }

    @Override
    public Iterator<ComponentSignal> iterator() {
        return choices.iterator();
    }
}
