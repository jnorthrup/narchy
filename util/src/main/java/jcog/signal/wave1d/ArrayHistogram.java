package jcog.signal.wave1d;

import jcog.Util;
import jcog.signal.tensor.ArrayTensor;

import java.util.Arrays;
import java.util.Random;

/** dead-simple fixed range continuous histogram with fixed # and size of bins. supports PDF sampling */
public class ArrayHistogram extends ArrayTensor {

    public float rangeMin;
    public float rangeMax;

    public float mass = 0;

    public ArrayHistogram(float min, float max, int bins) {
        super(bins);
        this.rangeMin = min;
        this.rangeMax = max;
    }

    public ArrayHistogram clear(float min, float max, int bins) {
        if (bins() != bins)
            return new ArrayHistogram(min, max, bins);
        else {
            this.rangeMin = min;
            this.rangeMax = max;
            clear();
            return this;
        }
    }

    public void clear() {
        Arrays.fill(data, 0);
        mass = 0;
    }

    public void add(float value, float weight) {
        int bin = Util.bin(Util.unitize((value-rangeMin)/(rangeMax-rangeMin)), bins());
        data[bin] += weight;
        mass += weight;
    }

    /** TODO use the rng to generate one 64-bit or one 32-bit integer and use half of its bits for each random value needed */
    public float sample(/*FloatSupplier uniformRng*/ Random rng) {

        float f = rng.nextFloat() * mass;
        int n = bins();
        int i;
        //for (i = 0; i < n && f > 0; i++) {
        for (i = n-1; (i >= 0); ) {
            f -= data[i];
            i--;
            if (f < 0)
                break;

        }
        //TODO sub-bin interpolate?
        //randomize within the bin's proximity, naively assuming a normal PDF
        //TODO use the relative density of the adjacent bin
        return Util.unitize((((i+0.5f) /(n-1)) + (rng.nextFloat() - 0.5f))) * (rangeMax - rangeMin) + rangeMin;
    }

    public final int bins() {
        return shape[0];
    }

}
