package spacegraph.audio;


import marytts.LocalMaryInterface;
import marytts.MaryInterface;
import marytts.exceptions.SynthesisException;
import marytts.util.MaryRuntimeUtils;
import marytts.util.data.BufferedDoubleDataSource;
import marytts.util.data.audio.DDSAudioInputStream;
import marytts.util.data.audio.MaryAudioUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioInputStream;

/**
 * https://github.com/marytts/marytts/wiki/MaryInterface
 */
public class MaryTTSpeech {

    final static Logger logger = LoggerFactory.getLogger(MaryTTSpeech.class);

    final static MaryInterface marytts;


    static {

        String javaVersion = System.getProperty("java.version");
        System.setProperty("java.version", "1.9.0"); //HACK

        LocalMaryInterface m;
        try {
            try {
                MaryRuntimeUtils.ensureMaryStarted();
            } catch (Exception e) {
                e.printStackTrace();
            }

            m = new LocalMaryInterface(); //this thing has a bad version check BAD BAD BAD
            //logger.info("Speech System READY");
        } catch (Exception e) {
            e.printStackTrace();
            m = null;
        }

        System.setProperty("java.version", javaVersion);
        marytts = m;

    }

//    public static void main(String[] args) throws Exception {
//
//                + marytts.getAvailableLocales() + " languages available.");
//        System.out.println("Out of these, " + marytts.getAvailableVoices(Locale.US) + " are for US English.");
//
//
////        AudioInputStream audio = marytts.generateAudio("This is my text.");
////        MaryAudioUtils.writeWavFile(MaryAudioUtils.getSamplesAsDoubleArray(audio), "/tmp/thisIsMyText.wav", audio.getFormat());
////        MaryAudioUtils.playWavFile("/tmp/thisIsMyText.wav", 3);
//
//
//        speak("hello 1234 abc this is a sentence!!! now what?");
//
//        Thread.sleep(16 * 1000);
//    }


    /**
     * async
     */
    public static void speak(String text) {
        speak(text, null);
    }

    /**
     * async
     */
    public static void speak(String _text, @Nullable Runnable whenFinished) {
        String text = _text.trim();
        if (text.isEmpty())
            return;


        try {

            DDSoundProducer sound = speech(text);
            sound.onFinish = whenFinished;

            Audio.the().play(sound, SoundSource.center, 1f, 1f);
//                new SoundSource() {
//
//                    float now = 0;
//                    @Override
//                    public float getX(float alpha) {
//                        now += alpha;
//                        return (float) Math.sin(now);//(float) Math.random();
//                    }
//
//                    @Override
//                    public float getY(float alpha) {
//                        return (float) Math.cos(now); //(float) Math.random();
//                    }
//                },


        } catch (SynthesisException e) {
            e.printStackTrace();
        }
        if (whenFinished != null) {
            whenFinished.run();
        }

    }


    /**
     * synchronous
     */
    public static DDSoundProducer speech(String text) throws SynthesisException {


        AudioInputStream audio = marytts.generateAudio(text);
        double[] x = MaryAudioUtils.getSamplesAsDoubleArray(audio);

        DDSoundProducer sound = new DDSoundProducer(new DDSAudioInputStream(new BufferedDoubleDataSource(x), audio.getFormat()));
        return sound;


//        DDSAudioInputStream audioInputStream = new DDSAudioInputStream(new BufferedDoubleDataSource(x), audio.getFormat());


    }


}
