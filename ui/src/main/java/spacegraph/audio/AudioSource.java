package spacegraph.audio;

import jcog.math.IntRange;
import jcog.signal.wave1d.DigitizedSignal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Signal sampled from system sound devices (via Java Media)
 */
public class AudioSource implements DigitizedSignal {

    /** buffer time in milliseconds */
    public final IntRange bufferSize;

    public final int sampleRate;
    private TargetDataLine line;
    private final DataLine.Info dataLineInfo;
    public final AudioFormat audioFormat;



    private final int bytesPerSample;



    private static final Logger logger = LoggerFactory.getLogger(AudioSource.class);

    private byte[] preByteBuffer;
    private short[] preShortBuffer;

    volatile public int audioBytesRead;


    //TODO parameterize with device
    /**
     * the constructor does not call start()
     * frameRate determines buffer size and frequency that events are emitted; can also be considered a measure of latency */
    public AudioSource() {

        sampleRate = 44100;
        bytesPerSample = 2; /* 16-bit */

        this.bufferSize = new IntRange(
                sampleRate * 4 /* ie. n seconds */,
                sampleRate, sampleRate * 128);

        audioFormat = new AudioFormat(sampleRate, 8*bytesPerSample, 1, true, false);
        dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat, bufferSize.intValue());
    }


    public int sampleRate() {
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

    public final AudioSource start() {

        try {

            logger.info("start {} {}", this, dataLineInfo);

            line = (TargetDataLine) AudioSystem.getLine(dataLineInfo);

            int numChannels = audioFormat.getChannels();

            int audioBufferSamples = (int) Math.ceil(numChannels * bufferSize.intValue());

            preByteBuffer = new byte[audioBufferSamples * bytesPerSample];
            preShortBuffer = new short[audioBufferSamples];

            line.open(audioFormat, audioBufferSamples);
            line.start();

            //TODO
            //line.addLineListener();

            return this;

        } catch (LineUnavailableException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void stop() {
        line.close();
        logger.info("stopped {} {} {}", this, dataLineInfo);
    }

    private final AtomicBoolean busy = new AtomicBoolean(false);

    @Override
    public boolean hasNext(int atleast) {
        return line.available() >= atleast;
    }

    @Override
    public int next(float[] target, int targetIndex, int capacitySamples) {

        if (!busy.compareAndSet(false, true))
            return 0;

        try {

//            do {


                int availableBytes = line.available();

                //logger.trace
                //System.out.println(available + "/" + capacity + " @ " + line.getMicrosecondPosition());

//                if (available > capacity) {
//                    int excess = available - capacity;
//
//                    while (excess % bytesPerSample != 0) {
//                        excess--; //pad
//                    }
//                    if (excess > 0) {
//                        if (excess <= preByteBuffer.length) {
//                            //small skip. just read more
//                            line.read(preByteBuffer, 0, excess);
//                            System.err.println(this + " buffer skip: available=" + available + " capacity=" + capacity);
//                            available -= excess;
//                        } else {
//                            System.err.println(this + " buffer skip flush: available=" + available + " capacity=" + capacity);
//                            line.flush();
//                            return 0;
//                        }
//                    }
//                }
                int toRead = Math.min(capacitySamples * bytesPerSample, availableBytes);
                if (toRead < availableBytes) {
                    while (toRead % bytesPerSample != 0)
                        toRead++;
                } else {
                    while (toRead % bytesPerSample != 0)
                        toRead--;
                }

                //int availableBytes = Math.min(capacity, line.available());
                audioBytesRead = line.read(preByteBuffer, 0, toRead);
                if (audioBytesRead == 0)
                    return 0;
                int nSamplesRead = audioBytesRead / 2;

                ByteBuffer.wrap(preByteBuffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(preShortBuffer);

                int start = 0;
                int end = nSamplesRead;
                int j = 0;
//                short min = Short.MAX_VALUE, max = Short.MIN_VALUE;
                double gain =
                        1.0/shortRange;
                        //this.gain.floatValue() / shortRange;
                for (int i = start; i < end; ) {
                    short s = preShortBuffer[i++];
//                    if (s < min) min = s;
//                    if (s > max) max = s;
                    target[j++] = (float) (s * gain); //compute in double for exra precision
                }

                return nSamplesRead;

//            } while (line.available() > 0);

        } finally {
            busy.set(false);
        }


    }

    private static final float shortRange = Short.MAX_VALUE;

    public String name() {
        return line.getLineInfo().toString();
    }
}
