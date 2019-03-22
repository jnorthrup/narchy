package jcog.signal.wave1d;

import jcog.Util;
import jcog.data.atomic.AtomicFloatFieldUpdater;
import jcog.signal.tensor.AtomicFloatArray;

import java.util.Random;

/** dead-simple fixed range continuous histogram with fixed # and size of bins. supports PDF sampling */
public class ArrayHistogram extends AtomicFloatArray /*AtomicDoubleArrayTensor*/  /* ArrayTensor */{

    private final static AtomicFloatFieldUpdater<ArrayHistogram> MASS =
            new AtomicFloatFieldUpdater(ArrayHistogram.class, "mass");

    public volatile float rangeMin;
    public volatile float rangeMax;
    private volatile float rangeDelta;

    //TODO use field updater
    public volatile float mass = 0;


    public ArrayHistogram(float min, float max, int bins) {
        super(bins);
        range(min, max);
    }

    private void range(float min, float max) {
        if (Util.equals(max, min)) {
            min -= Float.MIN_NORMAL;
            max += Float.MIN_NORMAL;
        }
        this.rangeMin = min;
        this.rangeMax = max;
        this.rangeDelta = (rangeMax - rangeMin);
    }

    public ArrayHistogram clear(float min, float max, int bins) {
        if (bins() != bins)
            return new ArrayHistogram(min, max, bins);
        else {
            range(min, max);
            clear();
            return this;
        }
    }

    public void clear() {
        mass = 0;
        fill(0);
    }

    public void add(float value, float weight) {
        addWithoutSettingMass(value, weight);
        MASS.add(this, weight);
    }

    public void addWithoutSettingMass(float value, float weight) {
        int bin = Util.bin((value-rangeMin)/rangeDelta, bins());
        addAt(weight, bin);
    }

    /** TODO use the rng to generate one 64-bit or one 32-bit integer and use half of its bits for each random value needed */
    public float sample(/*FloatSupplier uniformRng*/ Random rng) {


        int n = bins();

        float mass = MASS.getOpaque(this);
        float rangeDelta = this.rangeDelta;
        float rangeMin = this.rangeMin;
        if (mass < Float.MIN_NORMAL || rangeDelta < Float.MIN_NORMAL) {
            return rng.nextFloat()*n; //flat, choose random
        }


        float f0 = rng.nextFloat();
        float f = f0 * mass;
        boolean direction = (Float.floatToRawIntBits(f0) & 1) != 0; //one RNG call

        //boolean direction = rng.nextBoolean();

        int i;
        float ii;
        if (direction) {
            for (i = n - 1; (i >= 0); ) //downward
                if ((f -= getAt(i--)) < 0)
                    break;
            ii = i + 0.5f;
        } else {
            for (i = 0; i < n; ) //upward
                if ((f -= getAt(i++)) < 0)
                    break;
            ii = i - 0.5f;
        }

        float iii = ii + (rng.nextFloat() - 0.5f);

        //TODO sub-bin interpolate?
        //randomize within the bin's proximity, naively assuming a normal PDF
        //TODO use the relative density of the adjacent bin
        return Util.unitizeSafe( iii/(n-1) ) * rangeDelta + rangeMin;
    }

    public final int bins() {
        return volume();
    }

}
