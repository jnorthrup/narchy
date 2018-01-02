package spacegraph.audio;

import com.google.common.base.Joiner;
import jcog.Util;
import jcog.math.FloatParam;

import javax.sound.sampled.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Signal sampled from system sound devices (via Java Media)
 */
public class AudioSource implements WaveSource {
    public final FloatParam frameRate;
    public final int device;
    private TargetDataLine line;
    public final Mixer mixer;
    public final DataLine.Info dataLineInfo;
    public final AudioFormat audioFormat;

    private byte[] audioBytes;
    private short[] samples;
    private final int bytesPerSample;
    public final FloatParam gain = new FloatParam(1f, 0, 32f);

//    private short sampleMin, sampleMax;



    public AudioSource(int device, float frameRate) {
        this.device = device;
        this.frameRate = new FloatParam(frameRate, 1f, 40f);

        // Pick a format...
        // NOTE: It is better to enumerate the formats that the system supports,
        // because getLine() can error out with any particular format...
        audioFormat = new AudioFormat(22050, 16, 1, true, false);
        bytesPerSample = 2;



        dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);
        System.out.println(dataLineInfo);

        Mixer.Info[] minfoSet = AudioSystem.getMixerInfo();
        mixer = AudioSystem.getMixer(minfoSet[device]);
    }

    public void printDevices() {
        // Get TargetDataLine with that format
        Mixer.Info[] minfoSet = AudioSystem.getMixerInfo();
        System.out.println("Devices:\n\t" + Joiner.on("\n\t").join(minfoSet));
    }
    public static void print() {
        Mixer.Info[] minfoSet = AudioSystem.getMixerInfo();

        for (Mixer.Info i : minfoSet)
            System.out.println(i);
//        System.out.println(mixer);
//        System.out.println(mixer.getMixerInfo());
//        System.out.println(Arrays.toString(mixer.getControls()));
    }

    @Override public int channelsPerSample() {
        return audioFormat.getChannels();
    }

    @Override
    public int start() {
        // Open and start capturing audio
        // It's possible to have more control over the chosen audio device with this line:
        try {
            //line = (TargetDataLine) mixer.getLine(dataLineInfo);
            line = (TargetDataLine) AudioSystem.getLine(dataLineInfo);
            line.open(audioFormat);
            line.start();


            int sampleRate = (int) audioFormat.getSampleRate();
            int numChannels = audioFormat.getChannels();

            float period = 1.0f / frameRate.floatValue();

            int audioBufferSize = (int) (sampleRate * numChannels * period);

            int audioBufferSizeAllocated = Util.largestPowerOf2NoGreaterThan(audioBufferSize);
            audioBytes = new byte[audioBufferSizeAllocated * bytesPerSample];
            samples = new short[audioBufferSizeAllocated];

            return audioBufferSize;
        } catch (LineUnavailableException e) {
            e.printStackTrace();
            return 0;

        }


    }

    @Override
    public void stop() {

    }

    final AtomicBoolean busy = new AtomicBoolean();

    @Override
    public int next(float[] buffer) {

        short[] samples = this.samples;
        if (this.samples == null) return 0;

        if (!busy.compareAndSet(false, true))
            return 0;

        int bufferSamples = buffer.length;



        // Read from the line... non-blocking
        int avail = Math.min(bufferSamples, line.available());


        int bytesToTake = Math.min(bufferSamples * bytesPerSample, avail);
        int nBytesRead = line.read(audioBytes, avail-bytesToTake /* take the end of the buffer */,
                //bufferSamples*2
                bytesToTake
        );

        // Since we specified 16 bits in the AudioFormat,
        // we need to convert our read byte[] to short[]
        // (see source from FFmpegFrameRecorder.recordSamples for AV_SAMPLE_FMT_S16)
        // Let's initialize our short[] array


        // Let's wrap our short[] into a ShortBuffer and
        // pass it to recordSamples
        ByteBuffer.wrap(audioBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(samples);

        int nSamplesRead = nBytesRead / 2;
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
        Arrays.fill(buffer, end, buffer.length, 0);
//        this.sampleMin = min;
//        this.sampleMax = max;

        line.flush();
        busy.set(false);

        return nSamplesRead;

    }

    static final float shortRange = ((float)Short.MAX_VALUE);//-((float)Short.MIN_VALUE)-1f)/2f;
}
