package spacegraph.audio.modem.chirp.transceive.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AudioFormat {
    private final AudioFormat.Encoding encoding;
    private final float sampleRate;
    private final int sampleSizeInBits;
    private final int channels;
    private final int frameSize;
    private final float frameRate;
    private final boolean bigEndian;
    private HashMap<String, Object> properties;

    public AudioFormat(AudioFormat.Encoding var1, float var2, int var3, int var4, int var5, float var6, boolean var7) {
        this.encoding = var1;
        this.sampleRate = var2;
        this.sampleSizeInBits = var3;
        this.channels = var4;
        this.frameSize = var5;
        this.frameRate = var6;
        this.bigEndian = var7;
        this.properties = null;
    }

    public AudioFormat(AudioFormat.Encoding var1, float var2, int var3, int var4, int var5, float var6, boolean var7, Map<String, Object> var8) {
        this(var1, var2, var3, var4, var5, var6, var7);
        this.properties = new HashMap(var8);
    }

    public AudioFormat(float var1, int var2, int var3, boolean var4, boolean var5) {
        this(var4 ? AudioFormat.Encoding.PCM_SIGNED : AudioFormat.Encoding.PCM_UNSIGNED, var1, var2, var3, var3 != -1 && var2 != -1 ? (var2 + 7) / 8 * var3 : -1, var1, var5);
    }

    private AudioFormat.Encoding getEncoding() {
        return this.encoding;
    }

    public float getSampleRate() {
        return this.sampleRate;
    }

    private int getSampleSizeInBits() {
        return this.sampleSizeInBits;
    }

    private int getChannels() {
        return this.channels;
    }

    public int getFrameSize() {
        return this.frameSize;
    }

    private float getFrameRate() {
        return this.frameRate;
    }

    private boolean isBigEndian() {
        return this.bigEndian;
    }

    public Map<String, Object> properties() {
        Object var1;
        if (this.properties == null) {
            var1 = new HashMap(0);
        } else {
            var1 = this.properties.clone();
        }

        return Collections.unmodifiableMap((Map) var1);
    }

    public Object getProperty(String var1) {
        return this.properties == null ? null : this.properties.get(var1);
    }

    public boolean matches(AudioFormat var1) {
        return var1.getEncoding().equals(this.getEncoding()) && (var1.getSampleRate() == -1.0F || var1.getSampleRate() == this.getSampleRate()) && var1.getSampleSizeInBits() == this.getSampleSizeInBits() && var1.getChannels() == this.getChannels() && var1.getFrameSize() == this.getFrameSize() && (var1.getFrameRate() == -1.0F || var1.getFrameRate() == this.getFrameRate()) && (var1.getSampleSizeInBits() <= 8 || var1.isBigEndian() == this.isBigEndian());
    }

    public String toString() {
        String var1 = "";
        if (this.getEncoding() != null) {
            var1 = this.getEncoding() + " ";
        }

        String var2;
        if (this.getSampleRate() == -1.0F) {
            var2 = "unknown sample rate, ";
        } else {
            var2 = this.getSampleRate() + " Hz, ";
        }

        String var3;
        if ((float) this.getSampleSizeInBits() == -1.0F) {
            var3 = "unknown bits per sample, ";
        } else {
            var3 = this.getSampleSizeInBits() + " bit, ";
        }

        String var4;
        if (this.getChannels() == 1) {
            var4 = "mono, ";
        } else if (this.getChannels() == 2) {
            var4 = "stereo, ";
        } else if (this.getChannels() == -1) {
            var4 = " unknown number of channels, ";
        } else {
            var4 = this.getChannels() + " channels, ";
        }

        String var5;
        if ((float) this.getFrameSize() == -1.0F) {
            var5 = "unknown frame size, ";
        } else {
            var5 = this.getFrameSize() + " bytes/frame, ";
        }

        String var6 = "";
        if ((double) Math.abs(this.getSampleRate() - this.getFrameRate()) > 1.0E-5D) {
            if (this.getFrameRate() == -1.0F) {
                var6 = "unknown frame rate, ";
            } else {
                var6 = this.getFrameRate() + " frames/second, ";
            }
        }

        String var7 = "";
        if ((this.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED) || this.getEncoding().equals(AudioFormat.Encoding.PCM_UNSIGNED)) && (this.getSampleSizeInBits() > 8 || this.getSampleSizeInBits() == -1)) {
            if (this.isBigEndian()) {
                var7 = "big-endian";
            } else {
                var7 = "little-endian";
            }
        }

        return var1 + var2 + var3 + var4 + var5 + var6 + var7;
    }

    public static class Encoding {
        public static final AudioFormat.Encoding ULAW = new AudioFormat.Encoding("ULAW");
        public static final AudioFormat.Encoding ALAW = new AudioFormat.Encoding("ALAW");
        static final AudioFormat.Encoding PCM_SIGNED = new AudioFormat.Encoding("PCM_SIGNED");
        static final AudioFormat.Encoding PCM_UNSIGNED = new AudioFormat.Encoding("PCM_UNSIGNED");
        private final String name;

        public Encoding(String var1) {
            this.name = var1;
        }

        public final boolean equals(Object var1) {
            if (this.toString() != null) {
                return var1 instanceof Encoding && this.toString().equals(var1.toString());
            } else {
                return var1 != null && var1.toString() == null;
            }
        }

        public final int hashCode() {
            return this.toString() == null ? 0 : this.toString().hashCode();
        }

        public final String toString() {
            return this.name;
        }
    }
}
