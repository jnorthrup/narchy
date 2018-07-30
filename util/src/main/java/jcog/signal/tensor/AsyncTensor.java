package jcog.signal.tensor;

import jcog.event.ListTopic;

public class AsyncTensor extends ListTopic<Tensor> {

    private volatile Tensor current = null;

    public AsyncTensor() {
        super();
    }

    public void commit(Tensor t) {
        emit(current = t);
    }

    public Tensor get() {
        return current;
    }
}
