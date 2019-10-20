package spacegraph.space2d.container.time;

import jcog.Util;
import jcog.data.list.FasterList;
import jcog.math.Longerval;
import jcog.signal.Tensor;
import jcog.signal.tensor.TensorRing;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;

import java.util.List;

/** ring-buffer sequence of (evenly-timed non-overlapping?) events */
public class Timeline2DSequence implements Timeline2D.EventBuffer<Pair<Longerval,Tensor>> {

    public final TensorRing buffer;

    public Timeline2DSequence(int bufferSamples, int buffers) {
        this(new TensorRing(bufferSamples, buffers));
    }

    public Timeline2DSequence(TensorRing buffer) {
        this.buffer = buffer;
    }

    /** time to sample conversion
     * TODO
     * */
    protected static int sample(long t) {
        return Math.round(t);
        //return (int) t / bufferSize();
    }

    protected float sampleRate() {
        return bufferSize();
    }

    protected static long time(int sample) {
        return sample;
        ///return (long)(((double)sample) * bufferSize());
    }

    private int bufferSize() {
        return buffer.width;
    }

    public Tensor subTensor(int segment) {
        return buffer.viewLinear(segment*bufferSize(), (segment+1)*bufferSize());
    }


    @Override
    public Iterable<Pair<Longerval, Tensor>> events(long start, long end) {
        var v = buffer.volume();
        var sampleStart = sample(start);
        if (sampleStart > v + 1)
            return Util.emptyIterable; //starts after
        sampleStart = Math.max(0, sampleStart);
        var sampleEnd = sample(end);
        if (sampleEnd < - 1)
            return Util.emptyIterable; //ends before
        sampleEnd = Math.min(v, sampleEnd);


        List<Pair<Longerval, Tensor>> l = new FasterList();
        var w = buffer.width;
        var ss = sampleStart / w;
        for (var i = sampleStart; i < sampleEnd; ) {
            var ee = ss + w;
            l.add(Tuples.pair(new Longerval(ss, ee), subTensor(ss)));
            i += w;
            ss += w;
        }
        return l;
    }

    @Override
    public long[] range(Pair<Longerval, Tensor> event) {
        return event.getOne().toArray();
    }
}
