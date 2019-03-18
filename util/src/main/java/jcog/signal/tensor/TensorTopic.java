package jcog.signal.tensor;

import jcog.event.ListTopic;
import jcog.signal.Tensor;
import org.eclipse.collections.api.block.procedure.primitive.IntFloatProcedure;

import java.util.function.Supplier;

/** proxy to a (changeable) tensor referrent, and signaling topic */
public class TensorTopic<T extends Tensor> extends ListTopic<T> implements Tensor {

    private volatile T current = null;

    public TensorTopic() {
        super();
    }

    public TensorTopic(T initial) {
        this();
        emit(initial);
    }


    @Override public final void emit(T x) {
        super.emit(current = x);
    }

    public T get() {
        return current;
    }


    @Override
    public float getAt(int linearCell) {
        return current.getAt(linearCell);
    }

    @Override
    public float get(int... cell) {
        return current.get(cell);
    }

    @Override
    public int volume() {
        return current.volume();
    }

    @Override
    public void forEach(IntFloatProcedure sequential, int start, int end) {
        current.forEach(sequential, start, end);
    }

    @Override
    public float[] snapshot() {
        return current.snapshot();
    }

    @Override
    public int[] shape() {
        return current.shape();
    }

    @Override
    public int[] stride() {
        return current.stride();
    }
}
