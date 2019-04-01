package jcog.signal.tensor;

import jcog.data.atomic.AtomicCycle;
import jcog.signal.Tensor;
import org.eclipse.collections.api.block.procedure.primitive.IntFloatProcedure;
import org.eclipse.collections.api.block.procedure.primitive.IntProcedure;
import org.jetbrains.annotations.Nullable;

/** TODO delegate not inherit */
public class RingTensor extends AbstractShapedTensor implements WritableTensor {
    public final int width;
    private final int num;
    private final AtomicCycle.AtomicCycleN target;
    public final WritableTensor buffer;
    private final int _volume;


    public RingTensor(int width, int history) {
        this(new ArrayTensor(width*history), width, history);
    }

    public RingTensor(WritableTensor x, int width, int history) {
        super(/*new int[] { volume,history}*/ new int[] { width * history });
        assert(x.volume() >= (width*history));
        this.buffer = x;
        this.width = width;
        this.num = history;
        this.target = new AtomicCycle.AtomicCycleN(history);
        this._volume = width * history;
    }

    @Override
    public final int volume() {
        return _volume;
    }

    @Override
    public float get(int... cell) {
        assert(cell.length==2);
        return getAtDirect(r(cell[0], cell[1]));
    }

    public int r(int c, int r) {
        return idx(0, c, num) * width + r;
    }

    @Override
    public float getAt(int linearCell) {
        return getAtDirect(idx(linearCell));
        //throw new TODO();
    }



    protected int idx(int linearCell) {
        return idx(linearCell, width * target());
    }

    protected int idx(int linearCell, int offset) {
        return idx(linearCell, offset, volume());
    }

    protected int idx(int linearCell, int offset, int v) {
        return (offset + linearCell) % v;
    }

    @Override
    public int index(int... coord) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void forEach(IntFloatProcedure each, int start, int end) {
        int v = volume();
        int offset = width * target();
        int ii = idx(offset, start, v);
        for (int i = start; i < end; i++ ) {
            each.value(i, getAtDirect(ii++));
            if (ii == v) ii = 0;
        }
    }

    @Override
    public void forEachReverse(IntFloatProcedure each, int start, int end) {
        int v = volume();
        int offset = width * target();
        for (int i = end-1; i >= start; i-- ) {
            each.value(i, getAtDirect(idx(offset, i, v)));
        }
    }

    protected float getAtDirect(int d) {
        return buffer.getAt(d);
    }

    public int target() {
        return target.getOpaque();
    }

    public void target(int nextTarget) {
        target.set(nextTarget);
    }


    @Override
    public void setAt(int linearCell, float newValue) {
        setAtDirect(newValue, idx(linearCell));
    }

    public void setAtDirect(float newValue, int i) {
        buffer.setAt(i, newValue);
    }
    public final void setSpin(float newValue) {
        setAtDirect(newValue,  targetSpin());
    }
    public void addAtDirect(float inc, int i) {
        buffer.addAt(inc, i);
    }

    public void setAt(int linearCellStart, float[] values) {

        int v = volume();
        int i = idx(linearCellStart);
        for (float x : values) {
            buffer.setAt(i++, x);
            if (i == v) i = 0;
        }
    }

    @Override
    public float addAt(float x, int linearCell) {
        return buffer.addAt(x, idx(linearCell));
    }

    public Tensor set(Tensor t) {

        t.writeTo( ((ArrayTensor) buffer).data /* HACK */, targetSpin() * width);

        return this;
    }

    public RingTensor set(float[] t) {
        setAt(target(), t);
        return this;
    }
    public RingTensor setSpin(float[] t) {
        setAt(targetSpin(), t);
        return this;
    }

    public int targetSpin() {
        return target.incrementAndGet();
    }

    /** procedure receives the index of the next target */
    public int targetSpin(IntProcedure r) {
        return target.incrementAndGet(r);
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

    public void fillAll(float x) {
        buffer.fill(x);
    }



//    @Override
//    public void fill(float x) {
//        //TODO
//    }

}
