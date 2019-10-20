package spacegraph.audio;

import jcog.Util;
import jcog.WTF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;


public class Audio implements Runnable {
    /*
    wtf
        sudo killall -9 pulseaudio
        sudo pactl load-module module-detect
        pulseaudio -v

     */
    private static final Logger logger = LoggerFactory.getLogger(Audio.class);

    private static Audio defaultAudio;

    /**
     * the default audio system
     */
    public static synchronized Audio the() {

            if (defaultAudio == null) {
                defaultAudio = new Audio(32);
            }

        return defaultAudio;
    }


    private final SourceDataLine sdl;

    private static final int rate = 44100;

    /** TODO make dynamically reconfigurable */
    private static final int bufferSize = rate /
            20 /* = 50ms */
            //10 /* = 100ms */
    ;

    private final SoundMixer mixer;


    private final ByteBuffer soundBuffer = ByteBuffer.allocate(bufferSize * 4);
    //private final ShortBuffer soundBufferShort = soundBuffer.asShortBuffer();

    private final float[] leftBuf;
    private final float[] rightBuf;
    
    
    private boolean alive = true;



    private FileOutputStream rec;
    public Thread thread;


    public Audio(int polyphony) {

        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        for (Mixer.Info i : mixers) {
            System.out.println(i);
        }

//        Mixer m =
//                AudioSystem.getMixer(null);
                //AudioSystem.getMixer(mixers[2]);

        try {
            AudioFormat format = new AudioFormat(rate, 16, 2, true, false);
            sdl = AudioSystem.getSourceDataLine(format);
            //sdl = (SourceDataLine) AudioSystem.getMixer(null).getLine(new Line.Info(SourceDataLine.class));

            int bufferBytes = bufferSize * 2 * 2;
            sdl.open(format, bufferBytes);
            soundBuffer.order(ByteOrder.LITTLE_ENDIAN);
            sdl.start();


        } catch (LineUnavailableException e) {
            logger.error("initialize {} {}", this, e);
            thread = null;
            throw new WTF(e);

        }


        try {
            FloatControl volumeControl = (FloatControl) sdl.getControl(FloatControl.Type.MASTER_GAIN);
            volumeControl.setValue(volumeControl.getMaximum());
        } catch (IllegalArgumentException ignored) {
            System.out.println("Failed to setAt the sound volume");
        }

        this.mixer = new SoundMixer(polyphony);
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
        mixer.setSoundListener(soundSource);
    }

    public void shutDown() {
        alive = false;
    }

    public static int bufferSizeInFrames() {
        return bufferSize;
    }

    static class DefaultSource implements SoundSource {

        static final float distanceFactor = 1.0f;
        private final float balance;

        DefaultSource(SoundProducer p, float balance) {
            this.balance = balance;
        }

        @Override
        public float getY(float alpha) {
            return 0 + (1.0f /*- producer.amp()*/) * distanceFactor;
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
        return mixer.add(p, soundSource, volume, priority);
    }


    void clientTick(float alpha) {
        mixer.update(alpha);
    }

    private static final int max16 = Short.MAX_VALUE;
    private static final int min16 = -Short.MAX_VALUE;

    void tick() {
        

        
        
        mixer.read(leftBuf, rightBuf, rate);
        


        soundBuffer.clear();
        //soundBufferShort.clear();

        float gain = max16;

        byte[] ba = soundBuffer.array();


        int b = 0;
        for (int i = 0; i < bufferSize; i++) {
            short l = ((short)Util.clampSafe(leftBuf[i] * gain, min16, max16));
            //soundBufferShort.put(l);
            ba[b++] = (byte) (l & 0x00ff);
            ba[b++] = (byte) (l >> 8);
            short r = ((short)Util.clampSafe(rightBuf[i] * gain, min16, max16));
            //soundBufferShort.put(r);
            ba[b++] = (byte) (r & 0x00ff);
            ba[b++] = (byte) (r >> 8);
        }

        int bw = bufferSize * 2 * 2;
//        byte[] ba = soundBuffer.array();
        int br = sdl.write(ba, 0, bw);
        if (br!=bw)
            throw new WTF();

        if (rec != null) {
            try {
                rec.write(ba, 0, bw);
                rec.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void run() {

        int idle = 0;
        while (alive) {

            if (mixer.isEmpty()) {
                Util.pauseSpin(idle++);

            } else {
                idle = 0;
                clientTick(0);

                tick();
            }




        }
    }


}