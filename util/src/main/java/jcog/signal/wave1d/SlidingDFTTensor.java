package jcog.signal.wave1d;

import jcog.math.freq.SlidingDFT;
import jcog.signal.Tensor;
import jcog.signal.tensor.ArrayTensor;

import java.util.Arrays;

public class SlidingDFTTensor extends ArrayTensor {
    final SlidingDFT dft;
    private final Tensor src;
    float[] tmp;

    public SlidingDFTTensor(Tensor wave, int fftSize) {
        super(fftSize); //todo maybe 2 separate dims, amp/phase
        this.src = wave;
        this.dft = new SlidingDFT(fftSize, 1);
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

        dft.next(tmp, 0, data);
    }
}
