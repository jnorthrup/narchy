package spacegraph.audio;

import com.google.common.base.Joiner;
import jcog.math.FloatRange;
import jcog.signal.buffer.CircularFloatBuffer;

import javax.sound.sampled.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Signal sampled from system sound devices (via Java Media)
 */
public class AudioSource implements WaveSource {
    private final FloatRange frameRate;
    private final int sampleRate;
    private TargetDataLine line;
    private final DataLine.Info dataLineInfo;
    public final AudioFormat audioFormat;

    private final int bytesPerSample;
    public final FloatRange gain = new FloatRange(1f, 0, 32f);



    private byte[] preByteBuffer;
    private short[] preShortBuffer;
    private float[] preFloatBuffer;
    volatile public int audioBytesRead;





    public AudioSource(float frameRate) {
        this.frameRate = new FloatRange(frameRate, 1f, 40f);


        sampleRate = 22050;
        audioFormat = new AudioFormat(sampleRate, 16, 1, true, false);
        bytesPerSample = 2;

        dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);
//        System.out.println(dataLineInfo);


    }

    @Override
    public int samplesPerSecond() {
        return sampleRate;
    }

    public void printDevices() {
        
        Mixer.Info[] minfoSet = AudioSystem.getMixerInfo();
        System.out.println("Devices:\n\t" + Joiner.on("\n\t").join(minfoSet));
    }

    public static void print() {
        Mixer.Info[] minfoSet = AudioSystem.getMixerInfo();

        for (Mixer.Info i : minfoSet)
            System.out.println(i);



    }

    @Override public int channelsPerSample() {
        return audioFormat.getChannels();
    }

    @Override
    public int start() {
        
        
        try {
            
            line = (TargetDataLine) AudioSystem.getLine(dataLineInfo);

            int sampleRate = (int) audioFormat.getSampleRate();
            int numChannels = audioFormat.getChannels();

            float period = 1.0f / frameRate.floatValue();

            int audioBufferSamples = (int) Math.ceil(sampleRate * numChannels * period);


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

    @Override
    public void stop() {

    }

    private final AtomicBoolean busy = new AtomicBoolean();

    @Override
    public int next(CircularFloatBuffer buffer) {


        if (!busy.compareAndSet(false, true))
            return 0;

        try {

            int capacity = preByteBuffer.length;

            int availableBytes = Math.min(capacity, line.available());
            audioBytesRead = line.read(preByteBuffer, 0, availableBytes);
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
