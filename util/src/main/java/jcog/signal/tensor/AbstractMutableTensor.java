package jcog.signal.tensor;

public abstract class
AbstractMutableTensor extends AbstractShapedTensor implements WritableTensor {

    protected AbstractMutableTensor(int[] shape) {
        super(shape);
    }

    public void fill(float x) {
        int v = volume();
        for (int i = 0; i < v; i++)
            setAt(x, i);
    }


    @Override
    public void set(float newValue, int[] cell) {
        setAt(newValue, index(cell));
    }

    abstract public float addAt(float x, int linearCell);

}
