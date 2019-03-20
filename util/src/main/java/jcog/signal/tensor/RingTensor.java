package jcog.signal.tensor;

import jcog.data.atomic.AtomicCycle;
import jcog.signal.Tensor;
import org.eclipse.collections.api.block.procedure.primitive.IntFloatProcedure;
import org.eclipse.collections.api.block.procedure.primitive.IntProcedure;
import org.jetbrains.annotations.Nullable;

/** TODO delegate not inherit */
public class RingTensor extends AbstractShapedTensor implements WritableTensor {
    public final int segment;
    private final int num;
    private final AtomicCycle.AtomicCycleN target;
    private final WritableTensor buffer;


    public RingTensor(int volume, int history) {
        this(new ArrayTensor(volume*history), volume, history);
    }

    public RingTensor(WritableTensor x, int volume, int history) {
        super(/*new int[] { volume,history}*/ new int[] { volume * history });
        assert(x.volume() >= (volume*history));
        this.buffer = x;
        this.segment = volume;
        this.num = history;
        this.target = new AtomicCycle.AtomicCycleN(history);
    }

    @Override
    public float get(int... cell) {
        //assert(cell.length==2);
        return getAt(r(cell[0], cell[1]));
    }

    public int r(int c, int r) {
        return ((c + target()))%num*segment + r;
    }

    @Override
    public float getAt(int linearCell) {
        return buffer.getAt(linearCell);
    }

    @Override
    public int index(int... coord) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void forEach(IntFloatProcedure each, int start, int end) {
//        assert(start == 0);
//        assert(end==volume());
//
//        int target = target();
//        for (int i = 0; i < num; i++) {
//            int ts = target * segment;
//            buffer.forEach((x,y)->each.value(i, y), ts, ts + segment);
//
//            if (++target == num) target = 0;
//        }
        int v = volume();
        int offset = segment * target();
        for (int i = start; i < end; i++ ) {
            each.value(i, getAt((i + offset)%v));
        }
    }

    public RingTensor commit(float[] t) {
        buffer.setAt(t, spin()*segment);
        return this;
    }

    public int target() {
        return target.getOpaque();
    }

    public Tensor commit(Tensor t) {

        t.writeTo( ((ArrayTensor) buffer).data /* HACK */, spin() * segment);

        return this;
    }

    public int spin() {
        return target.getAndIncrement();
    }

    /** procedure receives the index of the next target */
    public int spin(IntProcedure r) {
        return target.getAndIncrement(r);
    }


    public final float[] snapshot() {
        return snapshot(null);
    }

    public final float[] snapshot(@Nullable float[] output) {
        int v = volume();
        if (output==null || output.length != v)
            output = new float[v];
        writeTo(output);
        return output;
    }


    @Override
    public void setAt(float newValue, int linearCell) {
        buffer.setAt(newValue, linearCell);
    }

    @Override
    public void fill(float x) {
        buffer.fill(x);
    }

}
