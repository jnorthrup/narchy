package jcog.signal.tensor;

import jcog.signal.Tensor;
import org.eclipse.collections.api.block.procedure.primitive.IntFloatProcedure;

public class RingBufferTensor extends ArrayTensor {
    private final Tensor t;
    private final int segment;
    private final int num;
    int target;

    public static Tensor get(Tensor t, int history) {
        if (history == 1) return t;
        return new RingBufferTensor(t, history);
    }

    RingBufferTensor(Tensor t, int history) {
        super(t.volume() * history);
        this.t = t;
        this.segment = t.volume();
        this.num = history;
    }

    @Override
    public float get(int... cell) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public int index(int... coord) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void forEach(IntFloatProcedure each, int start, int end) {
        assert(start == 0);
        assert(end==volume());
        int k = 0;
        for (int i = 0; i < num; i++) {
            int ts = target * segment;
            for (int j = 0; j < segment; j++) {
                each.value(k++, data[ts++]);
            }
            if (++target == num) target = 0;
        }
    }


    @Override
    public float[] snapshot() {
        t.get();
        t.writeTo(data, target * segment);
        if (++target == num) target = 0;
        return data;
    }
}
