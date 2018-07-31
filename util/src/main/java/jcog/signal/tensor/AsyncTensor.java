package jcog.signal.tensor;

import jcog.event.ListTopic;
import jcog.signal.Tensor;
import org.eclipse.collections.api.block.procedure.primitive.IntFloatProcedure;

/** proxy to a (changeable) tensor referrent, and signaling topic */
public class AsyncTensor<T extends Tensor> extends ListTopic<T> implements Tensor {

    private volatile T current = null;

    public AsyncTensor() {
        super();
    }

    public AsyncTensor(T initial) {
        this();
        commit(initial);
    }

    public final void commit(T t) {
        emit(t);
    }

    @Override public final void emit(T t) {
        super.emit(current = t);
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
