package jcog.signal.tensor;

public class BufferedTensor extends BatchArrayTensor {

    protected final Tensor from;

    public BufferedTensor(Tensor from) {
        super(from.shape());
        this.from = from;
    }


    @Override public void update() {
        from.get();
        from.writeTo(data);
    }

}
