package jcog.signal.wave1d;

import jcog.signal.buffer.CircularFloatBuffer;
import jcog.signal.tensor.ArrayTensor;
import jcog.signal.tensor.RingBufferTensor;

import java.util.concurrent.atomic.AtomicBoolean;

/** TODO extract RingBufferTensor to an extended impl.  this only needs to supply the next 1D vector of new freq information */
public class Spectrogram {
    @Deprecated
    public final RingBufferTensor freq;
    final SlidingDFTTensor dft;
    private final CircularFloatBuffer in;
    private final ArrayTensor inWave;

    public Spectrogram(CircularFloatBuffer in, float sampleTime, int sampleRate, int fftSize, int history) {

        this.in = in;

        this.inWave = new ArrayTensor(new float[(int) Math.ceil(sampleTime * sampleRate)]);
        dft = new SlidingDFTTensor(inWave, fftSize, true);
        freq = (RingBufferTensor) RingBufferTensor.get(dft, history);


    }


    final AtomicBoolean busy = new AtomicBoolean();

    public boolean update() {
        if (!busy.compareAndSet(false, true))
            return false;

        try {
            in.peekLast(inWave.data);
            dft.updateNormalized();
            freq.snapshot();
            return true;
        } finally {
            busy.set(false);

        }
    }

}
