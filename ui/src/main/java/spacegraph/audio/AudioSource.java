package spacegraph.audio;

import jcog.math.FloatRange;
import jcog.math.IntRange;
import jcog.signal.buffer.CircularFloatBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Signal sampled from system sound devices (via Java Media)
 */
public class AudioSource {

    /** buffer time in milliseconds */
    public final IntRange bufferSize;

    private final int sampleRate;
    private TargetDataLine line;
    private final DataLine.Info dataLineInfo;
    public final AudioFormat audioFormat;

    private final int bytesPerSample;

    public final FloatRange gain = new FloatRange(1f, 0, 128f);

    private static final Logger logger = LoggerFactory.getLogger(AudioSource.class);

    private byte[] preByteBuffer;
    private short[] preShortBuffer;
    private float[] preFloatBuffer;
    volatile public int audioBytesRead;





    /** frameRate determines buffer size and frequency that events are emitted; can also be considered a measure of latency */
    public AudioSource() {

        sampleRate = 22050;
        bytesPerSample = 2;

        this.bufferSize = new IntRange(sampleRate/16, 1, sampleRate * 2);

        audioFormat = new AudioFormat(sampleRate, 8*bytesPerSample, 1, true, false);
        dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat, bufferSize.intValue());
    }

    public int samplesPerSecond() {
        return sampleRate;
    }


    public static void print() {
        Mixer.Info[] minfoSet = AudioSystem.getMixerInfo();

        for (Mixer.Info i : minfoSet)
            System.out.println(i);



    }

    public int channelsPerSample() {
        return audioFormat.getChannels();
    }

    public int start() {
        
        
        try {
            
            line = (TargetDataLine) AudioSystem.getLine(dataLineInfo);
            logger.info("open {} {}", dataLineInfo);

            int sampleRate = (int) audioFormat.getSampleRate();
            int numChannels = audioFormat.getChannels();


            int audioBufferSamples = (int) Math.ceil(numChannels * bufferSize.intValue());


            preByteBuffer = new byte[audioBufferSamples * bytesPerSample];
            preShortBuffer = new short[audioBufferSamples];
            preFloatBuffer = new float[audioBufferSamples];


            line.open(audioFormat, audioBufferSamples);
            line.start();



            return audioBufferSamples;
        } catch (LineUnavailableException e) {
            e.printStackTrace();
            return 0;

        }

    }

    public void stop() {

    }

    private final AtomicBoolean busy = new AtomicBoolean(false);

    public int next(CircularFloatBuffer buffer) {


        if (!busy.compareAndSet(false, true))
            return 0;

        try {

            int capacity = preByteBuffer.length;
            //int availableBytes = Math.min(capacity, line.available());
            audioBytesRead = line.read(preByteBuffer, 0, capacity);
            if(audioBytesRead==0)
                return 0;
            int nSamplesRead = audioBytesRead / 2;

            ByteBuffer.wrap(preByteBuffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(preShortBuffer);


            int start = 0;
            int end = nSamplesRead;
            int j = 0;
            short min = Short.MAX_VALUE, max = Short.MIN_VALUE;
            float gain = this.gain.floatValue() / shortRange;
            for (int i = start; i < end; i++) {
                short s = preShortBuffer[i];
                if (s < min) min = s;
                if (s > max) max = s;

                preFloatBuffer[j++] = s * gain;
            }
            buffer.flush(j);
            buffer.write(preFloatBuffer, j);
            //Arrays.fill(buffer, end, buffer.length, 0);

            line.flush();
            return nSamplesRead;

        } finally {
            busy.set(false);
        }


    }

    private static final float shortRange = Short.MAX_VALUE;

}
