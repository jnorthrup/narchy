package spacegraph.audio;

import jcog.event.ListTopic;
import jcog.event.Topic;
import jcog.exe.Loop;
import jcog.signal.buffer.CircularFloatBuffer;
import jcog.signal.tensor.ArrayTensor;
import jcog.signal.tensor.AsyncTensor;

/**
 * acts as a short-term memory and event dispatcher in reaction to a live wave input stream
 *
 * TODO enable/disable switch
 * TODO correct ringbuffer w/ notify of buffer change interval https://github.com/waynetam/CircularBuffer
 * <p>
 * Created by me on 10/28/15.
 */
public class AudioBuffer extends Loop {

    private AudioSource source;

    public final CircularFloatBuffer buffer;

    /**
     * called when next sample (buffer) frame is ready
     */
    public final Topic<AudioBuffer> frame = new ListTopic<>();

    public final AsyncTensor<ArrayTensor> wave = new AsyncTensor<>(new ArrayTensor(0));



    /**
     * buffer time in seconds
     */
    public AudioBuffer(AudioSource source, float bufferTime) {

        //TODO move into setSource and recreate in case the source sample rate is different, the buffer can be resized etc
        buffer = new CircularFloatBuffer(1);

        setSource(source, bufferTime);
    }

    private void setSource(AudioSource source, float bufferTime) {
        synchronized (this) {

            buffer.setCapacity((int)(source.samplesPerSecond() * bufferTime));
            buffer.rewind();

            if (this.source != null) {
                if (this.source == source)
                    return; //no change

                this.source.stop();
                this.source = null;
            }

            this.source = source;

            if (this.source != null) {
                this.source.start();
            }



//                if (samples == null || samples.length != audioBufferSize) {
//                    samples = new float[audioBufferSize]; //Util.largestPowerOf2NoGreaterThan(audioBufferSize)];
//                    nextSamples = new float[audioBufferSize]; //Util.largestPowerOf2NoGreaterThan(audioBufferSize)];
//                }
        }
    }


    @Override
    public boolean next() {

        int read = source.next(buffer);
        if (read == 0)
            return true;

        frame.emit(this);

        if (!wave.isEmpty())
            wave.commit(new ArrayTensor(buffer, buffer.size() - read, read));

        return true;
    }


    public final AudioSource source() {
        return source;
    }
}
