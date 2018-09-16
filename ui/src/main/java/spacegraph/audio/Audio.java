package spacegraph.audio;

import jcog.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spacegraph.audio.sample.SoundSample;

import javax.sound.sampled.*;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;


public class Audio implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(Audio.class);

    private static Audio defaultAudio;

    /**
     * the default audio system
     */
    public synchronized static Audio the() {

            if (defaultAudio == null) {
                defaultAudio = new Audio(2);
            }

        return defaultAudio;
    }



    private final int bufferBytes;
    private final int maxChannels;
    private final SoundSample silentSample;
    private final SourceDataLine sdl;

    private final int rate = 44100;
    private final int bufferSize = rate / 20;

    private final ListenerMixer listenerMixer;


    private final ByteBuffer soundBuffer = ByteBuffer.allocate(bufferSize * 4);
    private final ShortBuffer soundBufferShort = soundBuffer.asShortBuffer();

    private final float[] leftBuf, rightBuf;
    
    
    private boolean alive = true;
    private float now;


    private FileOutputStream rec;
    public Thread thread;


    public Audio(int maxChannels) {

        this.maxChannels = maxChannels;
        silentSample = new SoundSample(new float[]{0}, 44100);
        Mixer mixer = AudioSystem.getMixer(null);

        bufferBytes = bufferSize * 2 * 2;


        SourceDataLine sdl;
        try {
            sdl = (SourceDataLine) mixer.getLine(new Line.Info(SourceDataLine.class));
            sdl.open(new AudioFormat(rate, 16, 2, true, false), bufferBytes);
            soundBuffer.order(ByteOrder.LITTLE_ENDIAN);
            sdl.start();


        } catch (LineUnavailableException e) {
            logger.error("initialize {} {}", this, e);
            thread = null;
            sdl = null;

        }

        this.sdl = sdl;

        try {
            FloatControl volumeControl = (FloatControl) sdl.getControl(FloatControl.Type.MASTER_GAIN);
            volumeControl.setValue(volumeControl.getMaximum());
        } catch (IllegalArgumentException ignored) {
            System.out.println("Failed to set the sound volume");
        }

        listenerMixer = new ListenerMixer(maxChannels);
        setListener(SoundSource.center);

        leftBuf = new float[bufferSize];
        rightBuf = new float[bufferSize];

        thread = new Thread(this);
        thread.setDaemon(true);
        
        thread.start();
    }


	/**
	 * Prints information about the current Mixer to System.out.
	 */
	public static void printMixerInfo() {
		Mixer.Info[] mixerinfo = AudioSystem.getMixerInfo();
		for (int i = 0; i < mixerinfo.length; i++) {
			String name = mixerinfo[i].getName();
			if (name.isEmpty())
				name = "No name";
			System.out.println((i+1) + ") " + name + " --- " + mixerinfo[i].getDescription());
			Mixer m = AudioSystem.getMixer(mixerinfo[i]);
			Line.Info[] lineinfo = m.getSourceLineInfo();
            for (Line.Info aLineinfo : lineinfo) {
                System.out.println("  - " + aLineinfo);
            }
		}
	}

    public void record(String path) throws java.io.FileNotFoundException {

        

        logger.info("recording to: {}", path);
        rec = new FileOutputStream(new File(path), false);












    }

    public void setListener(SoundSource soundSource) {
        listenerMixer.setSoundListener(soundSource);
    }

    public void shutDown() {
        alive = false;
    }

    public int bufferSizeInFrames() {
        return bufferSize;
    }

    static class DefaultSource implements SoundSource {

        private final SoundProducer producer;
        static final float distanceFactor = 1.0f;
        private final float balance;

        DefaultSource(SoundProducer p, float balance) {
            this.producer = p;
            this.balance = balance;
        }

        @Override
        public float getY(float alpha) {
            return 0 + (1.0f - producer.getAmplitude()) * distanceFactor;
        }

        @Override
        public float getX(float alpha) {
            return balance;
        }
    }


//    public void play(SoundSample sample, SoundSource soundSource, float volume, float priority) {
//        play(new SamplePlayer(sample, rate), soundSource, volume, priority);
//    }

    public <S extends SoundProducer> Sound<S> play(S p) {
	    return play(p, 1, 1, 0);
    }

    public <S extends SoundProducer> Sound<S> play(S p, float volume, float priority, float balance) {
        return play(p, new DefaultSource(p, balance), volume, priority);
    }

    public <S extends SoundProducer> Sound<S> play(S p, SoundSource soundSource, float volume, float priority) {


        return listenerMixer.addSoundProducer(p, soundSource, volume, priority);
    }


    void clientTick(float alpha) {
        listenerMixer.update(alpha);
    }

    private static final int max16 = Short.MAX_VALUE;

    void tick() {
        

        
        
        listenerMixer.read(leftBuf, rightBuf, rate);
        


        soundBufferShort.clear();

        float gain = max16;

        //byte[] ba = soundBuffer.array();


        for (int i = 0; i < bufferSize; i++) {
            short l = ((short)Util.clampSafe(leftBuf[i] * gain, -max16, max16));
            soundBufferShort.put(l);
            //ba[b++] = (byte) (l & 0x00ff);
            //ba[b++] = (byte) (l >> 8);
            short r = ((short)Util.clampSafe(rightBuf[i] * gain, -max16, max16));
            soundBufferShort.put(r);
            //ba[b++] = (byte) (r & 0x00ff);
            //ba[b++] = (byte) (r >> 8);
        }

        int bs = bufferSize * 2 * 2;
        byte[] ba = soundBuffer.array();
        sdl.write(ba, 0, bs);
        if (rec != null) {
            try {
                rec.write(ba, 0, bs);
                rec.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void run() {
        now = System.currentTimeMillis();
        int idle = 0;
        while (alive) {

            if (listenerMixer.isEmpty()) {
                Util.pauseNextIterative(idle++);
            } else {
                idle = 0;
            }

            now = System.currentTimeMillis() - now;

            clientTick(now);

            tick();

        }
    }


    public void setNow(float now) {
        this.now = now;
    }
}