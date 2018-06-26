//package spacegraph.audio;
//
//
//import marytts.LocalMaryInterface;
//import marytts.MaryInterface;
//import marytts.exceptions.SynthesisException;
//import marytts.util.data.BufferedDoubleDataSource;
//import marytts.util.data.audio.DDSAudioInputStream;
//import marytts.util.data.audio.MaryAudioUtils;
//import org.jetbrains.annotations.Nullable;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import javax.sound.sampled.*;
//import java.io.IOException;
//import java.util.concurrent.ForkJoinPool;
//
///**
// * https:
// */
//public class MaryTTSpeech {
//
//    final static Logger logger = LoggerFactory.getLogger(MaryTTSpeech.class);
//
//    final static MaryInterface marytts;
//
//
//    static {
//
//        String javaVersion = System.getProperty("java.version");
//        System.setProperty("java.version", "1.9.0");
//
//        LocalMaryInterface m;
//        try {
//
//
//
//
//
//
//            m = new LocalMaryInterface();
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            m = null;
//        }
//
//        System.setProperty("java.version", javaVersion);
//        marytts = m;
//
//    }
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//    /**
//     * async
//     */
//    public static void speak(String text) {
//        speak(text, null);
//    }
//
//    /**
//     * async
//     */
//    public static void speak(String _text, @Nullable Runnable whenFinished) {
//        String text = _text.trim();
//        if (text.isEmpty())
//            return;
//
//
//        try {
//
//            DDSoundProducer sound = speech(text);
//            sound.onFinish = whenFinished;
//
//            Audio.the().play(sound, SoundSource.center, 1f, 1f);
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//        } catch (SynthesisException e) {
//            e.printStackTrace();
//        }
//        if (whenFinished != null) {
//            whenFinished.run();
//        }
//
//    }
//
//
//    /**
//     * synchronous
//     */
//    public static DDSoundProducer speech(String text) throws SynthesisException {
//
//
//        AudioInputStream audio = marytts.generateAudio(text);
//        double[] x = MaryAudioUtils.getSamplesAsDoubleArray(audio);
//
//        DDSoundProducer sound = new DDSoundProducer(new DDSAudioInputStream(new BufferedDoubleDataSource(x), audio.getFormat()));
//        return sound;
//
//
//
//
//
//    }
//
//
//    public static class DDSoundProducer implements SoundProducer {
//
//        private final DDSAudioInputStream in;
//        private Line line;
//        int pos = 0;
//        AudioFormat format;
//        byte[] bytes;
//        int samplesPerFrame;
//        private boolean alive = true;
//        @Nullable
//        public Runnable onFinish;
//
//
//
//
//
//
//
//
//        public DDSoundProducer(DDSAudioInputStream i) {
//            format = i.getFormat();
//            DataLine.Info info = new DataLine.Info(Clip.class, format);
//
//            this.in = i;
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//        }
//
//
//        public int read(float[] buffer) {
//            return read(buffer, 0, buffer.length);
//        }
//
//
//        public int read(float[] buffer, int start, int count) {
//
//            if ((bytes == null) || ((bytes.length * 2) < count)) {
//                bytes = new byte[count * 2];
//            }
//            int bytesRead = 0;
//
//            try {
//
//                int available = in.available();
//                if (available > 0) {
//                    bytesRead = in.read(bytes, start, Math.min(available, bytes.length));
//                    if (bytesRead == 0) {
//                        alive = false;
//                    }
//                } else {
//                    alive = false;
//                }
//            } catch (IOException e) {
//                logger.error("read {} {}", in, e);
//                alive = false;
//                return 0;
//            }
//
//
//            int bi = 0;
//            byte[] b = this.bytes;
//            for (int i = 0; i < bytesRead / 2; i++) {
//                int sample = b[bi++] & 0x00FF;
//                sample = sample + (this.bytes[bi++] << 8);
//                buffer[i] = sample / 32767.0f;
//            }
//            return bytesRead / 4;
//        }
//
//        @Override
//        public float read(float[] buf, int readRate) {
//            pos += read(buf, 0, buf.length);
//            return 1;
//        }
//
//        @Override
//        public void skip(int samplesToSkip, int readRate) {
//
//            float step = format.getSampleRate() / readRate;
//            pos += step * samplesToSkip;
//
//
//
//
//        }
//
//        @Override
//        public boolean isLive() {
//
//            if (!alive) {
//                synchronized (in) {
//                    @Nullable Runnable r = this.onFinish;
//                    this.onFinish = null;
//                    if (r!=null)
//                        ForkJoinPool.commonPool().execute(r);
//                }
//            }
//
//            return alive;
//        }
//
//        @Override
//        public void stop() {
//            alive = false;
//        }
//    }
//}
