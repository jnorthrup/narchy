package spacegraph.audio.speech;

import jcog.Util;
import jcog.random.XoRoShiRo128PlusRandom;
import spacegraph.audio.Audio;
import spacegraph.audio.sample.SamplePlayer;
import spacegraph.audio.sample.SoundSample;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Java port of tss.js -- Tiny Speech Synthesizer in JavaScript
 * <p>
 * Original code: stan_1901 (Andrey Stephanov)
 * http://pouet.net/prod.php?which=50530
 * <p>
 * JavaScript port: losso/code red (Alexander Grupe)
 * http://heckmeck.de/demoscene/tiny-speech-synth-js/
 */
public class TinySpeech {

    public static final int SAMPLE_FREQUENCY = 44100;
    final static float PI = (float) Math.PI;
    final static float PI_2 = 2 * (float) Math.PI;

    // Auxiliary functions
    static float CutLevel(float x, float lvl) {
        if (x > lvl)
            return lvl;
        if (x < -lvl)
            return -lvl;
        return x;
    }

    static float Sawtooth(float x) {
        return (float) (0.5 - (x - Math.floor(x / PI_2) * PI_2) / PI_2);
    }

    /** HACK */
    @Deprecated static final Map g_phonemes;

    static {
        Map g;
        try {
            g = Util.jsonMapper.readValue(
                    "{           o: { f:[12,  15,  0], w:[ 10,  10,  0], len:3, amp: 6, osc:0, plosive:0 },\n" +
                            "            i: { f:[ 5,  56,  0], w:[ 10,  10,  0], len:3, amp: 3, osc:0, plosive:0 },\n" +
                            "            j: { f:[ 5,  56,  0], w:[ 10,  10,  0], len:1, amp: 3, osc:0, plosive:0 },\n" +
                            "            u: { f:[ 5,  14,  0], w:[ 10,  10,  0], len:3, amp: 3, osc:0, plosive:0 },\n" +
                            "            a: { f:[18,  30,  0], w:[ 10,  10,  0], len:3, amp:15, osc:0, plosive:0 },\n" +
                            "            e: { f:[14,  50,  0], w:[ 10,  10,  0], len:3, amp:15, osc:0, plosive:0 },\n" +
                            "            E: { f:[20,  40,  0], w:[ 10,  10,  0], len:3, amp:12, osc:0, plosive:0 },\n" +
                            "            w: { f:[ 3,  14,  0], w:[ 10,  10,  0], len:3, amp: 1, osc:0, plosive:0 },\n" +
                            "            v: { f:[ 2,  20,  0], w:[ 20,  10,  0], len:3, amp: 3, osc:0, plosive:0 },\n" +
                            "            T: { f:[ 2,  20,  0], w:[ 40,   1,  0], len:3, amp: 5, osc:0, plosive:0 },\n" +
                            "            z: { f:[ 5,  28, 80], w:[ 10,   5, 10], len:3, amp: 3, osc:0, plosive:0 },\n" +
                            "            Z: { f:[ 4,  30, 60], w:[ 50,   1,  5], len:3, amp: 5, osc:0, plosive:0 },\n" +
                            "            b: { f:[ 4,   0,  0], w:[ 10,   0,  0], len:1, amp: 2, osc:0, plosive:0 },\n" +
                            "            d: { f:[ 4,  40, 80], w:[ 10,  10, 10], len:1, amp: 2, osc:0, plosive:0 },\n" +
                            "            m: { f:[ 4,  20,  0], w:[ 10,  10,  0], len:3, amp: 2, osc:0, plosive:0 },\n" +
                            "            n: { f:[ 4,  40,  0], w:[ 10,  10,  0], len:3, amp: 2, osc:0, plosive:0 },\n" +
                            "            r: { f:[ 3,  10, 20], w:[ 30,   8,  1], len:3, amp: 3, osc:0, plosive:0 },\n" +
                            "            l: { f:[ 8,  20,  0], w:[ 10,  10,  0], len:3, amp: 5, osc:0, plosive:0 },\n" +
                            "            g: { f:[ 2,  10, 26], w:[ 15,   5,  2], len:2, amp: 1, osc:0, plosive:0 },\n" +
                            "            f: { f:[ 8,  20, 34], w:[ 10,  10, 10], len:3, amp: 4, osc:1, plosive:0 },\n" +
                            "            h: { f:[22,  26, 32], w:[ 30,  10, 30], len:1, amp:10, osc:1, plosive:0 },\n" +
                            "            s: { f:[80, 110,  0], w:[ 80,  40,  0], len:3, amp: 5, osc:1, plosive:0 },\n" +
                            "            S: { f:[20,  30,  0], w:[100, 100,  0], len:3, amp:10, osc:1, plosive:0 },\n" +
                            "            p: { f:[ 4,  10, 20], w:[  5,  10, 10], len:1, amp: 2, osc:1, plosive:1 },\n" +
                            "            t: { f:[ 4,  20, 40], w:[ 10,  20,  5], len:1, amp: 3, osc:1, plosive:1 },\n" +
                            "            k: { f:[20,  80,  0], w:[ 10,  10,  0], len:1, amp: 3, osc:1, plosive:1 } }\n"
                    , Map.class);
        } catch (IOException e) {
            g = null;
        }
        g_phonemes = g;
    }
    public static void say(String text) {
        Audio.the().play( saySample(text, 80, 1f  ) );
    }

    public static float[] _say(String text) {
        return _say(text, 80, 1f);
    }

    // Synthesizes speech and adds it to specified buffer
    public static float[] _say(String text, float f0, float speed) {

        XoRoShiRo128PlusRandom rng = new XoRoShiRo128PlusRandom(1);
        float[] buf = new float[10 * SAMPLE_FREQUENCY];
        int bufPos = 0;

        // Debug
//        int minBuf = 0, maxBuf = 0;
        // Loop through all phonemes
        int tlen = text.length();
        for (int textPos = 0; textPos < tlen; textPos++) {
            char l = text.charAt(textPos);
            // Find phoneme description
            Map p = (Map) g_phonemes.get(String.valueOf(l));
            if (p==null) {
                if (l == ' ' || l == '\n') {
                    int skip = (int) Math.floor(SAMPLE_FREQUENCY * 0.2 * speed);
                    bufPos += skip;
                }
                continue;
            }
            int v = (int)p.get("amp");
            // Generate sound
            int sl = Math.round((int)p.get("len") * (SAMPLE_FREQUENCY / 15f) * speed);
            List<Integer> pf = (List) p.get("f");
            List<Integer> pw = (List) p.get("w");
            boolean osc = p.get("osc").equals(1);
            for (int formant = 0; formant < 3; formant++) {
                float ff = pf.get(formant);
                if (ff == 0)
                    continue;

                float freq = ff * (50f / SAMPLE_FREQUENCY);

                float buf1Res = 0, buf2Res = 0;
                float q = 1.0f - pw.get(formant) * (PI * 10 / SAMPLE_FREQUENCY);
                float xp = 0;
                int thisBufPos = bufPos;

                for (int s = 0; s < sl; s++) {
                    float x = rng.nextFloat() - 0.5f;
                    if (!osc) {
                        x = Sawtooth(s * (f0 * PI_2 / SAMPLE_FREQUENCY));
                        xp = 0;
                    }
                    // Apply formant filter
                    x = (float) (x + 2f * Math.cos(PI_2 * freq) * buf1Res * q - buf2Res * q * q);
                    buf2Res = buf1Res;
                    buf1Res = x;
                    x = 0.75f * xp + x * v;
                    xp = x;
                    // Anticlick function
                    x *= CutLevel((float) (Math.sin((PI * s) / sl) * 5), 1)*10f;
                    buf[thisBufPos++] = buf[thisBufPos]/2+x;
//                    buf[thisBufPos++] = buf[thisBufPos]/2+x;
                    //buf.addAt(z);

//                    if (buf[thisBufPos - 1] < minBuf) minBuf = buf[thisBufPos - 1];
//                    if (buf[thisBufPos - 1] > maxBuf) maxBuf = buf[thisBufPos - 1];
                }
            }
            // Overlap neighbour phonemes
            bufPos += Math.round( ((3*sl/4)) + ((((int)p.get("plosive"))==1) ? (sl & 0xfffffe) : 0));
        }


        float[] aa = Arrays.copyOf(buf, bufPos);
        for (int i = 0; i < aa.length; i++) {
            aa[i] /= Short.MAX_VALUE;
        }

        return aa;
    }

    public static SamplePlayer saySample(String text, float f0, float speed) {
        return new SamplePlayer(new SoundSample(_say(text, f0, speed), SAMPLE_FREQUENCY));
    }


}
