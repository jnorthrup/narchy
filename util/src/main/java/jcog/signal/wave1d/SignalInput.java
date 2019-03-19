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

    public final int sampleRate;

    public DigitizedSignal source;

    /** TODO make private */
    public final float[] data;

    /**
     * called when next sample (buffer) frame is ready
     */
    public final TensorTopic<ArrayTensor> wave = new TensorTopic<>(new ArrayTensor(0));

    //TODO FloatRange bufferTime;


    //    /** uses CircularFloatBuffer internally to buffer internal calls (ex: to line.read())
//     *  TODO */
//    abstract public static class BufferedSignalReader {
//
//        /** In multiples of buffer time */
//        //public FloatRange chunkTimeMax = new FloatRange(0.5f, 0, 1);
//    }

    public SignalInput(DigitizedSignal src, int sampleRate, float bufferSeconds) {
        this(src, sampleRate, Math.round(bufferSeconds * sampleRate));
    }

    private SignalInput(DigitizedSignal src, int sampleRate, int bufferSamples) {
        this.source = src;
        this.sampleRate = sampleRate;
        data = new float[bufferSamples];
    }

    private transient int dataPtr = 0;

    @Override
    public boolean next() {
        boolean hasListeners = !wave.isEmpty();
        if (hasListeners) {
            DigitizedSignal source = this.source;
            while (source.hasNext(data.length - dataPtr)) {

                int read = source.next(data, dataPtr, data.length - dataPtr);
                dataPtr += read;
//                System.out.println(dataPtr);

                if (dataPtr == data.length) {
                    //a complete buffer
                    wave.emit(new ArrayTensor(data));
                    dataPtr = 0;
                }
            }
        }

        return true;
    }

}
