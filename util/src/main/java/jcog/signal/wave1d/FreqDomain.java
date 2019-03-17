package jcog.signal.wave1d;

import jcog.signal.buffer.CircularFloatBuffer;
import jcog.signal.tensor.ArrayTensor;
import jcog.signal.tensor.RingTensor;

import java.util.concurrent.atomic.AtomicBoolean;

/** TODO extract RingBufferTensor to an extended impl.  this only needs to supply the next 1D vector of new freq information */
public class FreqDomain {
    @Deprecated
    public final RingTensor freq;
    final SlidingDFTTensor dft;
    private final CircularFloatBuffer in;
    private final ArrayTensor inWave;

    public FreqDomain(CircularFloatBuffer in, float sampleTime, int sampleRate, int fftSize, int history) {

        this.in = in;

        this.inWave = new ArrayTensor(new float[(int) Math.ceil(sampleTime * sampleRate)]);
        dft = new SlidingDFTTensor(inWave, fftSize, true);
        freq = new RingTensor(dft.volume(), history);


    }


    final AtomicBoolean busy = new AtomicBoolean();

    public boolean update() {
        if (!busy.compareAndSet(false, true))
            return false;

        try {
            in.peekLast(inWave.data);
            dft.updateNormalized();
            freq.commit(dft);
            return true;
        } finally {
            busy.set(false);

        }
    }

}
