package jcog.signal.wave1d;

import jcog.exe.Loop;
import jcog.signal.tensor.ArrayTensor;
import jcog.signal.tensor.TensorTopic;

/**
 * an instance of a process of reading a signal
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
    private transient int dataPtr = 0;

    /**
     * called when next sample (buffer) frame is ready
     */
    public final TensorTopic<ArrayTensor> wave = new TensorTopic<>(new ArrayTensor(0));

    public SignalInput() {
        super();
    }

    public final SignalInput set(DigitizedSignal src, float bufferCycles) {
        int r = src.sampleRate();
        return set(src, r, Math.round(bufferCycles * r));
    }

    public synchronized SignalInput set(DigitizedSignal src, int sampleRate, int bufferSamples) {
        this.source = null;
        this.data = new float[bufferSamples];
        this.sampleRate = sampleRate;
        this.source = src; //ready
        return this;
    }

    @Override
    public boolean next() {
        DigitizedSignal source = this.source;
        if (source!=null) {
            boolean hasListeners = !wave.isEmpty();
            if (hasListeners) {
                while (source.hasNext(data.length - dataPtr)) {

                    int read = source.next(data, dataPtr, data.length - dataPtr);
                    dataPtr += read;

                    if (dataPtr == data.length) {
                        //a complete buffer
                        wave.emit(new ArrayTensor(data));
                        dataPtr = 0;
                    }
                }
            }
        }

        return true;
    }

}
