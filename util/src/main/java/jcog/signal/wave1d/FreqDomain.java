package jcog.signal.wave1d;

import jcog.signal.Tensor;
import jcog.signal.tensor.ArrayTensor;
import jcog.signal.tensor.TensorRing;
import jcog.signal.tensor.WritableTensor;

/**
 * TODO extract RingBufferTensor to an extended impl.  this only needs to supply the next 1D vector of new freq information
 */
public class FreqDomain {
    @Deprecated
    public final WritableTensor freq;
    final SlidingDFTTensor dft;


    public FreqDomain(int fftSize, int history) {
        dft = new SlidingDFTTensor( fftSize);
        freq = history > 1 ? new TensorRing(dft.volume(), history) : new ArrayTensor(dft.volume());

        var next = new ArrayTensor(1); //empty

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

        return commit();

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