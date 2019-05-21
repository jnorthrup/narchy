package jcog.signal.wave1d;

import jcog.exe.Loop;
import jcog.signal.tensor.ArrayTensor;
import jcog.signal.tensor.TensorTopic;

/**
 * buffering process for signal input
 * acts as a short-term memory and event dispatcher in reaction to a live-input 1D wave
 *
 * TODO enable/disable switch
 * TODO correct ringbuffer w/ notify of buffer change interval https://github.com/waynetam/CircularBuffer
 */
public class SignalInput extends Loop {

    public int sampleRate;

    volatile public DigitizedSignal source;

    /** TODO make private */
    public float[] data;
    private transient volatile int dataPtr = 0;

    /**
     * called when next sample (buffer) frame is ready
     */
    public final TensorTopic<ArrayTensor> wave = new TensorTopic<>(new ArrayTensor(0));
    private long s, e;

    public SignalInput() {
        super();
    }

    public final SignalInput set(DigitizedSignal src, float bufferSeconds) {

        this.s = this.e = src.time();

        int r = src.sampleRate();
        return set(src, r, Math.round(bufferSeconds * r));
    }

    public synchronized SignalInput set(DigitizedSignal src, int sampleRate, int bufferSamples) {
        this.source = null;
        this.data = new float[bufferSamples];
        this.sampleRate = sampleRate;
        this.source = src; //ready
        return this;
    }

    public static class RealTimeTensor extends ArrayTensor {

        public final long start, end;

        public RealTimeTensor(float[] oneD, long start, long end) {
            super(oneD);
            this.start = start; this.end = end;
        }
    }

    @Override
    public boolean next() {
        DigitizedSignal source = this.source;
        if (source!=null) {
            boolean hasListeners = !wave.isEmpty();
            if (hasListeners) {
                while (source.hasNext(data.length - dataPtr)) {


                    int read = source.next(data, dataPtr, data.length - dataPtr);

                    dataPtr += read; assert(dataPtr <= data.length);

                    this.e = Math.round(dataPtr/((double)sampleRate)*1000) + this.s;


                    if (dataPtr == data.length) {
                        //a complete buffer

                        wave.emit(new RealTimeTensor(data.clone(), s, e-1));
                        dataPtr = 0;

                        //if (dataPtr==0)
                            this.s = source.time();
                    }
                }
            }
        }

        return true;
    }

}
