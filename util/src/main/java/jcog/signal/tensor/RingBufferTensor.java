package jcog.signal.tensor;

import jcog.signal.Tensor;
import org.eclipse.collections.api.block.procedure.primitive.IntFloatProcedure;

public class RingBufferTensor extends ArrayTensor {
    public final int segment;
    private final int num;
    int target;


    public RingBufferTensor(int volume, int history) {
        super(volume * history);
        this.segment = volume;
        this.num = history;
    }

    @Override
    public float get(int... cell) {
        return data[((cell[0] + target))%num*segment + cell[1]];
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
        int target = this.target;
        for (int i = 0; i < num; i++) {
            int ts = target * segment;
            for (int j = 0; j < segment; j++) {
                each.value(k++, data[ts++]);
            }
            if (++target == num) target = 0;
        }
    }

    public RingBufferTensor commit(float[] t) {
        return commit(new ArrayTensor(t));
    }

    public RingBufferTensor commit(Tensor t) {
        //synchronized (data) {
        t.writeTo(data, target * segment);
        if (++target == num) target = 0;

        //}
        return this;
    }

    public float[] snapshot() {
        return snapshot(new float[volume()]);
    }

    public float[] snapshot(float[] output) {
        if (output==null || output.length != volume())
            output = new float[volume()];
        writeTo(output);
        return output;
    }

    public float[] commitToArray(Tensor t) {
        return commit(t).snapshot();
    }
}
