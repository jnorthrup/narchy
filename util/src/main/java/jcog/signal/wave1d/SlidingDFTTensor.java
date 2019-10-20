package jcog.signal.wave1d;

import jcog.Util;
import jcog.signal.Tensor;
import jcog.signal.tensor.ArrayTensor;
import org.eclipse.collections.api.block.function.primitive.IntFloatToFloatFunction;

import java.util.Arrays;

public class SlidingDFTTensor extends ArrayTensor {
    final SlidingDFT dft;

    //private final boolean realOrComplex;
    float[] tmp;

    public SlidingDFTTensor(int fftSize) {
        super(fftSize); //todo maybe 2 separate dims, amp/phase
        this.dft = new SlidingDFT(fftSize, 1);
        //this.realOrComplex = realOrComplex;
    }

    public void update(Tensor src) {
        int sv = src.volume();
        if(tmp==null || tmp.length!= sv) {
            tmp = new float[sv];
        } else {
            Arrays.fill(tmp, (float) 0);
        }

        src.writeTo(tmp);

        //if (realOrComplex)
            dft.nextFreq(tmp, 0, data);
//        else
//            dft.next(tmp, 0, data);
    }

    /** returns the intensity */
    public void updateNormalized(Tensor src) {
        update(src);
        normalize();
    }

    public void normalize() {
        float[] minmax = Util.minmax(data);
        if (!(minmax[1] - minmax[0] < Float.MIN_NORMAL)) {
            Util.normalize(data, minmax[0], minmax[1]);
        } else {
            Arrays.fill(data, (float) 0);
        }
    }

    /** multiplicative filter, by freq index */
    public void transform(IntFloatToFloatFunction f) {
        for (int i = 0; i < data.length; i++) {
            data[i] *= f.valueOf(i, data[i]);
        }
    }
}
