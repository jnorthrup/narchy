package spacegraph.audio.sample;


import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public enum SampleLoader
{
    ;

    private static final Logger logger = LoggerFactory.getLogger(SampleLoader.class);

    /**
     * Loads a sample from an url
     */
    private static SoundSample load(InputStream isis) throws UnsupportedAudioFileException, IOException
    {

        var d = rip(isis);
        var ais = AudioSystem.getAudioInputStream(new ByteArrayInputStream(d));
        
        return buildSample(rip(ais), ais.getFormat());
    }

    public static SoundSample load(String path)  {
        try {
            return load(new FileInputStream(path));
        } catch (UnsupportedAudioFileException | IOException e) {
            logger.warn(e.getMessage());
        }
        return null;
    }


    /**
     * Rips the entire contents of an inputstream into a byte array
     */
    private static byte[] rip(InputStream in) throws IOException {
        return in.readAllBytes();











    }

    /**
     * Reorganizes audio sample data into the intenal sonar format
     */
    private static SoundSample buildSample(byte[] b, AudioFormat af) throws UnsupportedAudioFileException
    {

        var channels = af.getChannels();
        var sampleSize = af.getSampleSizeInBits();
        var rate = af.getFrameRate();
        var signed = af.getEncoding() == AudioFormat.Encoding.PCM_SIGNED;

        
        if (channels != 1) throw new UnsupportedAudioFileException("Only mono samples are supported");
        if (!(sampleSize == 8 || sampleSize == 16 || sampleSize == 32)) throw new UnsupportedAudioFileException("Unsupported sample size");
        if (!(af.getEncoding() == AudioFormat.Encoding.PCM_UNSIGNED || af.getEncoding() == AudioFormat.Encoding.PCM_SIGNED)) throw new UnsupportedAudioFileException("Unsupported encoding");


        var bb = ByteBuffer.wrap(b);
        bb.order(af.isBigEndian() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);

        var s = b.length / (sampleSize / 8);
        var buf = new float[s];
        
        switch (sampleSize) {
            case 8:
                if (signed) {
                    for (var i = 0; i < s; i++)
                        buf[i] = bb.get() / (float) 0x80;
                } else {
                    for (var i = 0; i < s; i++)
                        buf[i] = ((bb.get() & 0xFF) - 0x80) / (float) 0x80;
                }
                break;
            case 16:
                if (signed) {
                    for (var i = 0; i < s; i++)
                        buf[i] = bb.getShort() / (float) 0x8000;
                } else {
                    for (var i = 0; i < s; i++)
                        buf[i] = ((bb.getShort() & 0xFFFF) - 0x8000) / (float) 0x8000;
                }
                break;
            case 32:
                if (signed) {
                    for (var i = 0; i < s; i++)
                        buf[i] = bb.getInt() / (float) 0x80000000;
                } else {
                    
                    for (var i = 0; i < s; i++)
                        buf[i] = ((bb.getInt() & 0xFFFFFFFFL) - 0x80000000L) / (float) 0x80000000;
                }
                break;
        }
        
        
        return new SoundSample(buf, rate);
    }

    /** digitize provided function at sample rate (ex: 44.1kh) */
    public static SoundSample digitize(FloatToFloatFunction f, int sampleRate, int duration) {

        var samples = duration * sampleRate;
        var ss = new SoundSample(new float[samples], sampleRate);
        var b = ss.buf;
        float t = 0, dt = 1.0f / sampleRate;
        for (var i = 0; i < samples; i++) {
            b[i] = f.valueOf(t);
            t+=dt;
        }

        return ss;
    }
}