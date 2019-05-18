package spacegraph.audio;

import jcog.data.list.FasterList;
import jcog.signal.wave1d.DigitizedSignal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Signal sampled from system sound devices (via Java Media)
 */
public class AudioSource implements DigitizedSignal {

    private static final Logger logger = LoggerFactory.getLogger(AudioSource.class);
    private static final float shortRange = Short.MAX_VALUE;
    /**
     * buffer time in milliseconds
     */
//    public final IntRange bufferSize;
    public final int sampleRate;
    public final AudioFormat audioFormat;
    private final DataLine.Info dataLineInfo;
    private final int bytesPerSample;
    private final AtomicBoolean busy = new AtomicBoolean(false);
    volatile public int audioBytesRead;
    private TargetDataLine line;


    //TODO parameterize with device

    private byte[] preByteBuffer;
    private short[] preShortBuffer;

    public static List<AudioSource> all() {

        List<AudioSource> ll = new FasterList();

        Mixer.Info[] ii = AudioSystem.getMixerInfo();
        for (Mixer.Info I : ii) {
            Mixer mi = AudioSystem.getMixer(I);
            Line.Info[] mm = mi.getTargetLineInfo();
            for (Line.Info M : mm) {
                if (TargetDataLine.class.isAssignableFrom(M.getLineClass())) {
                    System.out.println(I + " " + M + " " + M.getLineClass());
                    try {
                        AudioSource ss = new AudioSource((TargetDataLine) mi.getLine(M));
                        ll.add(ss);
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return ll;
    }

    /**
     * the constructor does not call start()
     * frameRate determines buffer size and frequency that events are emitted; can also be considered a measure of latency
     */
    public AudioSource(TargetDataLine line) {

//        sampleRate = 44100;
//        bytesPerSample = 2; /* 16-bit */
//
//        this.bufferSize = new IntRange(
//                sampleRate * 4 /* ie. n seconds */,
//                sampleRate, sampleRate * 128);
//
//        audioFormat = new AudioFormat(sampleRate, 8 * bytesPerSample, 1, true, false);
//        dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat, bufferSize.intValue());
        //int numChannels = audioFormat.getChannels();
        //int audioBufferSamples = (int) Math.ceil(numChannels * bufferSize.intValue());

        this.line = line;
        this.sampleRate = (int) line.getFormat().getSampleRate();
        this.audioFormat = line.getFormat();
        this.dataLineInfo = (DataLine.Info) line.getLineInfo();
        int audioBufferSamples = line.getBufferSize();
        int numChannels = line.getFormat().getChannels();
        bytesPerSample = line.getFormat().getSampleSizeInBits()/8;

        preByteBuffer = new byte[audioBufferSamples * bytesPerSample];
        preShortBuffer = new short[audioBufferSamples];

    }

    public static void print() {
        Mixer.Info[] minfoSet = AudioSystem.getMixerInfo();

        for (Mixer.Info i : minfoSet)
            System.out.println(i);


    }

    public int sampleRate() {
        return sampleRate;
    }

    public int channelsPerSample() {
        return audioFormat.getChannels();
    }

    public final AudioSource start() throws LineUnavailableException {
        logger.info("start {} {}", line, dataLineInfo);

        line.open(audioFormat/*, line.getBufferSize()*/);
        line.start();

        //TODO
        //line.addLineListener();

        return this;


    }

    public void stop() {
        logger.info("stop {} {}", line, dataLineInfo);

        synchronized (this) {
            if (line != null) {
                line.close();
                line = null;
            }
        }
    }

    @Override
    public boolean hasNext(int atleast) {
        TargetDataLine l = this.line;
        return l != null && l.available() >= atleast;
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
                    1.0 / shortRange;
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

    public String name() {
        return line.getLineInfo().toString();
    }
}
