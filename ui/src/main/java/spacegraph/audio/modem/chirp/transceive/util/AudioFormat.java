package spacegraph.audio.modem.chirp.transceive.util;

import java.util.*;

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
        return Optional.ofNullable(this.properties).map(stringObjectHashMap -> stringObjectHashMap.get(var1)).orElse(null);
    }

    public boolean matches(AudioFormat var1) {
        return Objects.equals(var1.encoding, this.encoding) && (var1.sampleRate == -1.0F || var1.sampleRate == this.sampleRate) && var1.sampleSizeInBits == this.sampleSizeInBits && var1.channels == this.channels && var1.frameSize == this.frameSize && (var1.frameRate == -1.0F || var1.frameRate == this.frameRate) && (var1.sampleSizeInBits <= 8 || var1.bigEndian == this.bigEndian);
    }

    public String toString() {
        var var1 = "";
        if (this.encoding != null) {
            var1 = this.encoding + " ";
        }

        String var2;
        if (this.sampleRate == -1.0F) {
            var2 = "unknown sample rate, ";
        } else {
            var2 = this.sampleRate + " Hz, ";
        }

        String var3;
        if ((float) this.sampleSizeInBits == -1.0F) {
            var3 = "unknown bits per sample, ";
        } else {
            var3 = this.sampleSizeInBits + " bit, ";
        }

        String var4;
        switch (this.channels) {
            case 1:
                var4 = "mono, ";
                break;
            case 2:
                var4 = "stereo, ";
                break;
            case -1:
                var4 = " unknown number of channels, ";
                break;
            default:
                var4 = this.channels + " channels, ";
                break;
        }

        String var5;
        if ((float) this.frameSize == -1.0F) {
            var5 = "unknown frame size, ";
        } else {
            var5 = this.frameSize + " bytes/frame, ";
        }

        var var6 = "";
        if ((double) Math.abs(this.sampleRate - this.frameRate) > 1.0E-5D) {
            if (this.frameRate == -1.0F) {
                var6 = "unknown frame rate, ";
            } else {
                var6 = this.frameRate + " frames/second, ";
            }
        }

        var var7 = "";
        if ((this.encoding.equals(AudioFormat.Encoding.PCM_SIGNED) || this.encoding.equals(AudioFormat.Encoding.PCM_UNSIGNED)) && (this.sampleSizeInBits > 8 || this.sampleSizeInBits == -1)) {
            if (this.bigEndian) {
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
