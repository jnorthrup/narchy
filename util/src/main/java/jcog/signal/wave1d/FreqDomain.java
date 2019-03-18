package jcog.signal.wave1d;

import jcog.signal.Tensor;
import jcog.signal.tensor.ArrayTensor;
import jcog.signal.tensor.RingTensor;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * TODO extract RingBufferTensor to an extended impl.  this only needs to supply the next 1D vector of new freq information
 */
public class FreqDomain {
    @Deprecated
    public final RingTensor freq;
    final SlidingDFTTensor dft;
    private final SignalReading in;

    private ArrayTensor next;

    public FreqDomain(SignalReading in, int fftSize, int history) {
        this.in = in;
        dft = new SlidingDFTTensor( fftSize, true);
        freq = new RingTensor(dft.volume(), history);

        next = new ArrayTensor(1); //empty

        in.wave.on((next)->{
            this.next = next;
            invalid.set(true);
        });
    }


    final AtomicBoolean invalid = new AtomicBoolean(false);


    public Tensor next(Tensor next) {
        if (invalid.compareAndSet(true, false)) {
            try {
                dft.updateNormalized(next);
            } finally {
                invalid.set(false);
            }
        }

        return dft;
    }

    public boolean update(Tensor next) {
        freq.commit(next(next));
        return true;
    }
}