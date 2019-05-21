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
 * assume that internal timing refers to durations of ms (milliseconds)
 */
public class AudioSource implements DigitizedSignal {

    private static final Logger logger = LoggerFactory.getLogger(AudioSource.class);
    private static final float shortRange = Short.MAX_VALUE;


    private final int bytesPerSample;
    private final AtomicBoolean busy = new AtomicBoolean(false);
    volatile public int audioBytesRead;
    private final TargetDataLine line;


    //TODO parameterize with device

    private final byte[] preByteBuffer;
    private final short[] preShortBuffer;

    /** system ms time at start */
    private long _start;

    public static List<AudioSource> all() {

        List<AudioSource> ll = new FasterList();

        Mixer.Info[] ii = AudioSystem.getMixerInfo();
        for (Mixer.Info I : ii) {
            Mixer mi = AudioSystem.getMixer(I);
            Line.Info[] mm = mi.getTargetLineInfo();
            for (Line.Info M : mm) {
                if (TargetDataLine.class.isAssignableFrom(M.getLineClass())) {
                    //System.out.println(I + " " + M + " " + M.getLineClass());
                    try {
                        TargetDataLine lm = (TargetDataLine) mi.getLine(M);


                        lm.open(); //attempts to open, will fail if it can't

                        AudioSource ss = new AudioSource(lm);
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
     *
     * line may be already open or not.
     */
    public AudioSource(TargetDataLine line) {

        this.line = line;

        int audioBufferSamples = line.getBufferSize();
        int numChannels = line.getFormat().getChannels();
        bytesPerSample = numChannels * line.getFormat().getSampleSizeInBits()/8;

        preByteBuffer = new byte[audioBufferSamples * bytesPerSample];
        preShortBuffer = new short[audioBufferSamples];

    }

    public static void print() {
        Mixer.Info[] minfoSet = AudioSystem.getMixerInfo();

        for (Mixer.Info i : minfoSet)
            System.out.println(i);
    }

    public int sampleRate() {
        return (int) line.getFormat().getSampleRate();
    }

    @Override
    public long time() {
        return _start + Math.round(line.getMicrosecondPosition()/1000.0);
    }

    public int channelsPerSample() {
        return line.getFormat().getChannels();
    }

    public final AudioSource start() throws LineUnavailableException {
        logger.info("start {} {}", line, line.getLineInfo());

        synchronized (this) {
            if (!line.isOpen()) {
                line.open();
                //line.open(audioFormat/*, line.getBufferSize()*/);
            }

            this._start = System.currentTimeMillis();
            line.start();
        }

        //TODO
        //line.addLineListener();

        return this;


    }

    public void stop() {
        logger.info("stop {} {}", line, line.getLineInfo());

        synchronized (this) {
            if (line != null) {
                line.stop();
                line.close();
            }
        }
    }

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
            int toDrain = availableBytes - toRead;
            if (toDrain > 0) {
                //drain excess TODO optional
                line.read(new byte[toDrain], 0, toDrain); //HACK TODO use line fast forward method if exist otherwise shared buffer
            }

            //pad to bytes per sample
            if (toRead < availableBytes) {
                while (toRead % bytesPerSample != 0) toRead++;
            } else {
                while (toRead % bytesPerSample != 0) toRead--;
            }

            //int availableBytes = Math.min(capacity, line.available());
            audioBytesRead = line.read(preByteBuffer, 0, toRead);
            if (audioBytesRead == 0)
                return 0;
            int nSamplesRead = audioBytesRead / bytesPerSample;

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
