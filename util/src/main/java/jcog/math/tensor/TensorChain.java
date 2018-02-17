package jcog.math.tensor;

import org.eclipse.collections.api.block.procedure.primitive.IntFloatProcedure;

/** chains 2 or more tensors along the 0th dimension */
public class TensorChain extends BatchArrayTensor {

    private final Tensor[] sub;

    public static Tensor get(Tensor... t) {
        assert(t.length > 0);

        if (t.length==1)
            return t[0];

        int[] shape = t[0].shape().clone();
        shape[0] = 0;
        for (Tensor x : t) {
            int[] xs = x.shape();
            assert(xs.length == shape.length);
            for (int d = 1; d < xs.length; d++) {
                assert(xs[d] == shape[d]);
            }
            shape[0] += xs[0];
        }
        return new TensorChain(shape, t);
    }

    TensorChain(int[] shape, Tensor... sub) {
        super(shape);
        this.sub = sub;
    }

//    @Override
//    public float get(int... cell) {
//        //TODO test
//        Tensor target = sub[0];
//        int x = cell[0];
//        int i = 0;
//        int next;
//        while (x > (next = target.shape()[i++]))
//            x -= next;
//        cell[0] = x;
//        return sub[i-1].get(cell);
//    }

    @Override
    public float get(int linearCell) {
        throw new UnsupportedOperationException("TODO similar to the other get");
    }

    @Override
    public void update() {
        int[] p = { 0 };

        for (Tensor x : sub) {
            x.forEach((i,v) -> {
               data[p[0]++] = v;
            });
        }
    }


    @Override
    public void forEach(IntFloatProcedure  sequential, int start, int end) {
        assert(start == 0);
        assert(end == volume());
        final int[] p = {0};
        for (Tensor x : sub) {
            x.forEach((i,v) -> {
               sequential.value(p[0]++, v);
            });
        }
    }

}
