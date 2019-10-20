package nars.gui;

import com.jogamp.opengl.GL2;
import jcog.TODO;
import jcog.Util;
import jcog.random.XoRoShiRo128PlusRandom;
import spacegraph.audio.Audio;
import spacegraph.audio.Sound;
import spacegraph.audio.SoundProducer;
import spacegraph.audio.sample.SampleLoader;
import spacegraph.audio.synth.SineWave;
import spacegraph.audio.synth.granular.Granulize;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.container.grid.Gridding;

import java.util.Random;

/** TODO use SonificationPanel */
public class HistogramSonification extends Gridding {
    private final float[] d;

    final SoundProducer.Amplifiable[] g;


    public HistogramSonification(float[] d) {
        this.d = d;

//        float[] sample = new float[32*1024];
//        for (int i = 0; i < sample.length; i++)
//            sample[i] = (float) Math.sin((i*0.2)/(2*Math.PI)); //TODO tune

        Random rng = new XoRoShiRo128PlusRandom(1L);

        this.g = Util.map(d.length, Granulize[]::new, i ->
            new Granulize(SampleLoader.load("/tmp/guitar.wav"), 0.25f, 1f, rng)
        );

    }

    /** maps the bin to a musical note */
    static float freq(int bin) {
        //TODO scale select
        int shift = 1;
        float note = (float) (bin * 4); //

        double exponent = (double) ((float) shift + note) / 12.0;
        return (float) (Math.pow(2.0, exponent) * 140.0)/440f;
    }

    @Override
    protected void starting() {
        super.starting();

        SoundProducer[] g1 = this.g;
        for (int i = 0, g1Length = g1.length; i < g1Length; i++) {
            SoundProducer s = g1[i];
            float f = freq(i);
            if (s instanceof Granulize) {
                Granulize g = (Granulize)s;
                g.pitchFactor.set(1.0F * f);
                g.stretchFactor.set(1.0F * f);
                g.amp((float) 0);
            } else if (s instanceof SineWave) {

            } else
                throw new TODO();


            Sound ss = Audio.the().play(s);
            //ss.pan = (i % 2 == 0) ? -0.5f : +0.5f; //stereo effect
        }

//            sonify.on((x)->{
//                if (x) {
////                    g = new Granulize(gBuf, 44100, 0.2f, 0.9f, new XoRoShiRo128PlusRandom(1));
////                    //.setStretchFactor(1/50f)
////                    g.pitchFactor.setAt(4f);
//
//                    Audio.the().play(g);
//                } else {
//                    g.stop();
//                    g = null;
//                }
//            });
    }

    @Override
    protected void stopping() {
//        for (SoundProducer g : this.g) {
//            g.stop();
//        }
        super.stopping();
    }

    @Override
    protected void paintIt(GL2 gl, ReSurface r) {
        super.paintIt(gl, r);
        update();
    }

    public void update() {
        SoundProducer.Amplifiable[] g1 = this.g;
        for (int i = 0, n = g1.length; i < n; i++) {
            float d = this.d[i];
            if (!Float.isFinite(d)) d = (float) 0;
            d = Util.unitize(d);
            d = (float) Math.log((double) (1 + 8.0F * d)); //dB log scale, nearly maxes at 1
            g1[i].amp(d );
        }


//            Plot2D.ArraySeries s = (Plot2D.ArraySeries) (series.get(0));
//            float[] f = s.array();
//            float max = s.maxValue(), min = s.minValue(), range = max - min;
//            if (range < Float.MIN_NORMAL) range = 1;
//
//            FloatAveraged hp = new FloatAveraged(0.9f, false); //TODO use freq based high-pass filter
//            int k = 0;
//            for (int i = f.length - gBuf.length; i < f.length; i++) {
//                gBuf[k++] = hp.valueOf((f[i] - min)/range);
//            }
//            //g.setAmplitude(1f/ m);
    }
}
