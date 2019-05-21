package spacegraph.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.TargetDataLine;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Signal sampled from system sound devices (via Java Media)
 * assume that internal timing refers to durations of ms (milliseconds)
 */
public class AudioSourcePCM16 extends AudioSource {

    private static final float shortRange = Short.MAX_VALUE;


    //TODO parameterize with device

    private final short[] preShortBuffer;

    /**
     * the constructor does not call start()
     * frameRate determines buffer size and frequency that events are emitted; can also be considered a measure of latency
     *
     * line may be already open or not.
     */
    public AudioSourcePCM16(TargetDataLine line) {
        super(line);

        assert(line.getFormat().getEncoding()== AudioFormat.Encoding.PCM_SIGNED);
        assert(line.getFormat().getSampleSizeInBits() == 16);
//        assert(line.getFormat().getChannels() == 1);


        int audioBufferSamples = line.getBufferSize();
        preShortBuffer = new short[audioBufferSamples];

    }



    @Override protected void decode(float[] target, int nSamplesRead) {
        ByteBuffer.wrap(preByteBuffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(preShortBuffer);

        int start = 0;
        int end = nSamplesRead;
        int j = 0;
//                short min = Short.MAX_VALUE, max = Short.MIN_VALUE;
        double gain =
                1.0 / shortRange;
        //this.gain.floatValue() / shortRange;
        for (int i = start; i < end; ) {
            short s = preShortBuffer[i++];
//                    if (s < min) min = s;
//                    if (s > max) max = s;
            target[j++] = (float) (s * gain); //compute in double for exra precision
        }

    }

}
