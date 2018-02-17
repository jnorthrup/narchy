package net.beadsproject.beads.core.io;

import jcog.TODO;
import net.beadsproject.beads.core.*;
import spacegraph.audio.SoundProducer;

import javax.sound.sampled.*;

public class UGenOutput extends AudioIO implements SoundProducer {

    /**
     * The default system buffer size.
     */

//	/** The mixer. */
//	private Mixer mixer;
//
//	/** The source data line. */
//	private SourceDataLine sourceDataLine;
//
    /**
     * The system buffer size in frames.
     */
    private int systemBufferSizeInFrames;

    /**
     * The current byte buffer.
     */
    private int channels;

    public UGenOutput() {

    }


//	/**
//	 * Gets the JavaSound mixer being used by this AudioContext.
//	 *
//	 * @return the requested mixer.
//	 */
//	private void getDefaultMixerIfNotAlreadyChosen() {
//		if(mixer == null) {
//			selectMixer(0);
//		}
//	}
//
//	/**
//	 * Presents a choice of mixers on the commandline.
//	 */
//	public void chooseMixerCommandLine() {
//		System.out.println("Choose a mixer...");
//		printMixerInfo();
//		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
//		try {
//			selectMixer(Integer.parseInt(br.readLine()) - 1);
//		} catch(Exception e) {
//			e.printStackTrace();
//		}
//	}
//
//	/**
//	 * Select a mixer by index.
//	 *
//	 * @param i the index of the selected mixer.
//	 */
//	public void selectMixer(int i) {
//		Mixer.Info[] mixerinfo = AudioSystem.getMixerInfo();
//		mixer = AudioSystem.getMixer(mixerinfo[i]);
//		if(mixer != null) {
//			System.out.print("JavaSoundAudioIO: Chosen mixer is ");
//			System.out.println(mixer.getMixerInfo().getName() + ".");
//		} else {
//			System.out.println("JavaSoundAudioIO: Failed to get mixer.");
//		}
//	}
//

    /**
     * Starts the audio system running.
     */
    @Override
    protected boolean start() {
        AudioContext context = getContext();
        IOAudioFormat ioAudioFormat = getContext().getAudioFormat();
        AudioFormat audioFormat =
                new AudioFormat(ioAudioFormat.sampleRate, ioAudioFormat.bitDepth, ioAudioFormat.outputs, ioAudioFormat.signed, ioAudioFormat.bigEndian);

        this.channels = audioFormat.getChannels();
//		getDefaultMixerIfNotAlreadyChosen();
//		if (mixer == null) {
//			return false;
//		}
//		DataLine.Info info = new DataLine.Info(SourceDataLine.class,
//				audioFormat);
//		try {
//			sourceDataLine = (SourceDataLine) mixer.getLine(info);
//			if (systemBufferSizeInFrames < 0)
//				sourceDataLine.open(audioFormat);
//			else
//				sourceDataLine.open(audioFormat, systemBufferSizeInFrames
//						* audioFormat.getFrameSize());
//		} catch (LineUnavailableException ex) {
//			System.out
//					.println(getClass().getName() + " : Error getting line\n");
//		}

        return true;
    }


    @Override
    protected UGen getAudioInput(int[] channels) {
        //TODO not properly implemented, this does not respond to channels arg.
        IOAudioFormat ioAudioFormat = getContext().getAudioFormat();
        AudioFormat audioFormat =
                new AudioFormat(ioAudioFormat.sampleRate, ioAudioFormat.bitDepth, ioAudioFormat.inputs, ioAudioFormat.signed, ioAudioFormat.bigEndian);
        return new JavaSoundRTInput(getContext(), audioFormat);
    }

    @Override
    public float read(float[] buf, int readRate) {

        int samples = buf.length;
        context.setBufferSize(samples);

        update(); // this propagates update call to context

        int c = 0;
        for (int i = 0; i < samples; i++) {
            //for (int j = 0; j < channels; j++)
            int j = 0;
            float vi = context.out.getValue(j, i);
            buf[c++] = vi;
        }

        return 1f;

//        AudioUtils.floatToByte(bbuf, interleavedOutput,
//                audioFormat.isBigEndian());
//        sourceDataLine.write(bbuf, 0, bbuf.length);
    }

    @Override
    public void skip(int samplesToSkip, int readRate) {
        throw new TODO();
    }

    @Override
    public boolean isLive() {
        return context.isRunning();
    }

    @Override
    public void stop() {
        context.stop();
    }

    /**
     * JavaSoundRTInput gathers audio from the JavaSound audio input device.
     *
     * @beads.category input
     */
    private static class JavaSoundRTInput extends UGen {

        /**
         * The audio format.
         */
        private final AudioFormat audioFormat;

        /**
         * The target data line.
         */
        private TargetDataLine targetDataLine;

        /**
         * Flag to tell whether JavaSound has been initialised.
         */
        private boolean javaSoundInitialized;

        private float[] interleavedSamples;
        private byte[] bbuf;

        /**
         * Instantiates a new RTInput.
         *
         * @param context     the AudioContext.
         * @param audioFormat the AudioFormat.
         */
        JavaSoundRTInput(AudioContext context, AudioFormat audioFormat) {
            super(context, audioFormat.getChannels());
            this.audioFormat = audioFormat;
            javaSoundInitialized = false;
        }

        /**
         * Set up JavaSound. Requires that JavaSound has been set up in AudioContext.
         */
        public void initJavaSound() {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
            try {
                int inputBufferSize = 5000;
                targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
                targetDataLine.open(audioFormat, inputBufferSize);
                if (targetDataLine == null) System.out.println("no line");
                else
                    System.out.println("CHOSEN INPUT: " + targetDataLine.getLineInfo() + ", buffer size in bytes: " + inputBufferSize);
            } catch (LineUnavailableException ex) {
                System.out.println(getClass().getName() + " : Error getting line\n");
            }
            targetDataLine.start();
            javaSoundInitialized = true;
            interleavedSamples = new float[bufferSize * audioFormat.getChannels()];
            bbuf = new byte[bufferSize * audioFormat.getFrameSize()];
        }


        /* (non-Javadoc)
         * @see com.olliebown.beads.core.UGen#calculateBuffer()
         */
        @Override
        public void gen() {
            if (!javaSoundInitialized) {
                initJavaSound();
            }
            targetDataLine.read(bbuf, 0, bbuf.length);
            AudioUtils.byteToFloat(interleavedSamples, bbuf, audioFormat.isBigEndian());
            AudioUtils.deinterleave(interleavedSamples, audioFormat.getChannels(), bufferSize, bufOut);
        }


    }


}
