package spacegraph.audio;

import com.google.common.base.Joiner;
import jcog.Util;
import jcog.math.FloatRange;

import javax.sound.sampled.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Signal sampled from system sound devices (via Java Media)
 */
public class AudioSource implements WaveSource {
    private final FloatRange frameRate;
    private TargetDataLine line;
    private final DataLine.Info dataLineInfo;
    public final AudioFormat audioFormat;

    private final int bytesPerSample;
    public final FloatRange gain = new FloatRange(1f, 0, 32f);


    volatile private short[] samples;
    private int sampleNum;
    volatile public byte[] audioBytes;
    volatile public int audioBytesRead;





    public AudioSource(float frameRate) {
        this.frameRate = new FloatRange(frameRate, 1f, 40f);

        
        
        
        audioFormat = new AudioFormat(22050, 16, 1, true, false);
        bytesPerSample = 2;

        dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);
        System.out.println(dataLineInfo);

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

            int audioBufferSize = (int) (sampleRate * numChannels * period);

            int audioBufferSizeAllocated = Util.largestPowerOf2NoGreaterThan(audioBufferSize);
            audioBytes = new byte[audioBufferSizeAllocated * bytesPerSample];
            samples = new short[audioBufferSizeAllocated];

            line.open(audioFormat, audioBufferSize);
            line.start();

            return audioBufferSize;
        } catch (LineUnavailableException e) {
            e.printStackTrace();
            return 0;

        }


    }


    public int bufferSamples() {
        return samples.length;
    }

    @Override
    public void stop() {

    }

    private final AtomicBoolean busy = new AtomicBoolean();

    @Override
    public int next(float[] buffer) {

        short[] samples = this.samples;
        if (this.samples == null) return 0;

        if (!busy.compareAndSet(false, true))
            return 0;

        int bufferSamples = buffer.length;



        
        int avail = Math.min(bufferSamples, line.available());


        int bytesToTake = Math.min(bufferSamples * bytesPerSample, avail);
        audioBytesRead = line.read(audioBytes, avail-bytesToTake /* take the end of the buffer */,
                
                bytesToTake
        );

        
        
        
        


        
        
        ByteBuffer.wrap(audioBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(samples);

        int nSamplesRead = audioBytesRead / 2;
        int start = Math.max(0, nSamplesRead - bufferSamples);
        int end = nSamplesRead;
        int j = 0;
        short min = Short.MAX_VALUE, max = Short.MIN_VALUE;
        float gain = this.gain.floatValue() / shortRange;
        for (int i = start; i < end; i++) {
            short s = samples[i];
            if (s < min) min = s;
            if (s > max) max = s;
            buffer[j++] = s * gain;
        }
        sampleNum += j;
        Arrays.fill(buffer, end, buffer.length, 0);



        line.flush();
        busy.set(false);

        return nSamplesRead;

    }

    private static final float shortRange = ((float)Short.MAX_VALUE);

    public long sampleNum() {
        return sampleNum;
    }
}
