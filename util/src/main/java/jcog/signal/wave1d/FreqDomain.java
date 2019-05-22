package jcog.signal.wave1d;

import jcog.signal.Tensor;
import jcog.signal.tensor.ArrayTensor;
import jcog.signal.tensor.TensorRing;
import jcog.signal.tensor.WritableTensor;
import org.eclipse.collections.api.block.function.primitive.IntToFloatFunction;

/**
 * TODO extract RingBufferTensor to an extended impl.  this only needs to supply the next 1D vector of new freq information
 */
public class FreqDomain {
    @Deprecated
    public final WritableTensor freq;
    final SlidingDFTTensor dft;

    private ArrayTensor next;

    @Deprecated public FreqDomain(int fftSize, int history) {
        this(fftSize, history, (x)->x);
    }

    public FreqDomain(int fftSize, int history, IntToFloatFunction binFreq) {
        dft = new SlidingDFTTensor( fftSize, binFreq);
        freq = history > 1 ? new TensorRing(dft.volume(), history) : dft;

        next = new ArrayTensor(1); //empty

    }


//    final AtomicBoolean invalid = new AtomicBoolean(false);


    public Tensor apply(Tensor timeDomain) {

        dft.update(timeDomain);
//        dft.transform((i,v)->{
//            return v;
//            //return v * (i > 10 ? 1 : 0.01f);
//            //return (float) Math.exp(v);
//        });
        dft.normalize();


        return new ArrayTensor(dft.data);
    }

    public void normalize() {
        dft.normalize();
    }

    public Tensor commit() {
        if (freq!=dft)
            freq.set(dft);
        return freq;
    }


}