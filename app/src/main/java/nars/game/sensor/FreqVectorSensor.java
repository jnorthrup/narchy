package nars.game.sensor;

import jcog.Util;
import jcog.data.iterator.ArrayIterator;
import jcog.math.FloatRange;
import jcog.signal.buffer.CircularFloatBuffer;
import jcog.signal.wave1d.SlidingDFT;
import nars.NAR;
import nars.game.Game;
import nars.term.Term;

import java.util.Iterator;
import java.util.function.IntFunction;

/** frequency domain representation of a waveform */
public class FreqVectorSensor extends VectorSensor {

    public final CircularFloatBuffer buf;

    final int sampleWindow;

    /**TODO abstract */
    final SlidingDFT dft;

    private final ComponentSignal[] component;
    private final int freqMax;

    float[] freqValue;
    float[] componentValue;
    private float[] inBuf = null;

    public final FloatRange center;
    public final FloatRange bandwidth;

    public FreqVectorSensor(NAR n, CircularFloatBuffer buf, int fftSize, int components, IntFunction<Term> termizer) {
        super(termizer.apply(components) /*n+1*/, n);

        this.buf = buf;

        assert(fftSize >= components*2);

        freqMax = fftSize/2;
        center = new FloatRange(freqMax/2, 0, freqMax);
        bandwidth = new FloatRange(freqMax, 0, freqMax);

        component = new ComponentSignal[components];
        componentValue = new float[components];
        freqValue = new float[fftSize];
        for (var i = 0; i < components; i++) {
            var finalI = i;
            component[i] = newComponent(termizer.apply(i), ()->componentValue[finalI]);
            freqValue[i] = 0;
        }

        dft = new SlidingDFT(fftSize, 1);
        sampleWindow = fftSize * 2; //TODO calibrate for DFT size
    }



    @Override
    public int size() {
        return component.length;
    }

    @Override
    public void accept(Game g) {

        //TODO only if buffer changed
        inBuf = buf.peekLast(inBuf, sampleWindow);

        dft.nextFreq(inBuf, 0, freqValue);

        var center = this.center.floatValue();
        var bw = this.bandwidth.floatValue();
        var fMin = Math.max(0, center - bw/2);
        var fMax = Math.min(freqMax, center + bw/2);
        var n = componentValue.length;
        var fDelta = (fMax - fMin) / n;
        var fRad = fDelta/2;
        for (var i = 0; i < n; i++) {
            var c = Util.lerp((i + 0.5f)/n, fMin, fMax);
            var a = c - fRad;
            var b = c + fRad;
            var f = Util.interpSum(freqValue, a, b)/(b-a);
            componentValue[i] = (float) f;
        }
        float intensity;
        Util.normalize(componentValue, 0, intensity = Util.max(componentValue));

        super.accept(g);
    }

    @Override
    public Iterator<ComponentSignal> iterator() {
        return ArrayIterator.iterator(component);
    }
}
