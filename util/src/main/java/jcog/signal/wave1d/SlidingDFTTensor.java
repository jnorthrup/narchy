package jcog.signal.wave1d;

import jcog.Util;
import jcog.math.freq.SlidingDFT;
import jcog.signal.Tensor;
import jcog.signal.tensor.ArrayTensor;

import java.util.Arrays;

public class SlidingDFTTensor extends ArrayTensor {
    final SlidingDFT dft;
    private final Tensor src;
    private final boolean realOrComplex;
    float[] tmp;

    public SlidingDFTTensor(Tensor wave, int frequencies, boolean realOrComplex) {
        super(realOrComplex ? frequencies : frequencies*2); //todo maybe 2 separate dims, amp/phase
        this.src = wave;
        this.dft = new SlidingDFT(frequencies*2, 1);
        this.realOrComplex = realOrComplex;
        update();
    }

    public void update() {
        int sv = src.volume();
        if(tmp==null || tmp.length!= sv) {
            tmp = new float[sv];
        } else {
            Arrays.fill(tmp, 0);
        }

        src.writeTo(tmp);

        if (realOrComplex)
            dft.nextFreq(tmp, 0, data);
        else
            dft.next(tmp, 0, data);
    }

    /** returns the intensity */
    public float updateNormalized() {
        update();
        float max = this.maxValue();
        Util.normalize(data, 0, max);
        return max;
    }
}
