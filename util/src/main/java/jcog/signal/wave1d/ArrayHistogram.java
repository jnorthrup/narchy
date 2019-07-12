package jcog.signal.wave1d;

import jcog.Util;
import jcog.data.atomic.AtomicFloatFieldUpdater;
import jcog.pri.ScalarValue;
import jcog.signal.tensor.AtomicFloatVector;
import jcog.signal.tensor.WritableTensor;

import java.util.Random;

import static jcog.Texts.n2;

/** dead-simple fixed range continuous histogram with fixed # and size of bins. supports PDF sampling */
public class ArrayHistogram  /*AtomicDoubleArrayTensor*/  /* ArrayTensor */{

    private WritableTensor data = AtomicFloatVector.Empty;

    private volatile float rangeMin, rangeMax;
    private volatile int mass = AtomicFloatFieldUpdater.iZero;

    private final static AtomicFloatFieldUpdater<ArrayHistogram> MASS =
            new AtomicFloatFieldUpdater(ArrayHistogram.class, "mass");


    public ArrayHistogram() {
        range(0,1);
    }

    public ArrayHistogram(float min, float max, int bins) {
        range(min, max);
    }

    @Override
    public String toString() {
        return rangeMin + ".." + rangeMax + " @ " + mass() + " " + n2(data.floatArray());
    }

    private void resize(int bins) {
        if (bins == 0)
            data = AtomicFloatVector.Empty;
        else
            data =
                new AtomicFloatVector(bins);
                //AtomicFixedPoint4x16bitVector.get(bins);
    }

    private void range(float min, float max) {
        this.rangeMin = min;
        this.rangeMax = max;
    }

    /** note: mass is not affected in this call. you may need to call that separately */
    public HistogramWriter write(float min, float max, int bins) {
        range(0, 0); //force flat sampling for other threads, while writing
        if (bins() != bins) {
            //elides subsequent data fill, the new array will be set to zero
            resize(bins);
        } else {
            data.fill(0);
        }
        return new HistogramWriter(bins, min, max);
    }

    public final class HistogramWriter {

        final int bins;
        final float min, max, rangeDelta;
        float mass = 0;

        HistogramWriter(int bins, float min, float max) {
            this.bins = bins;
            this.rangeDelta = max-min;
            this.min = min;
            this.max = max;
        }

        public void add(float value, float weight) {
            mass += weight;
            data.addAt(weight, Util.bin((value-rangeMin)/rangeDelta, bins));
        }

        /** returns mass */
        public float commit(float mass) {
            mass(mass);
            range(min, max);
            return mass;
        }
    }

    /** mass setter */
    public final ArrayHistogram mass(float m) {
        MASS.set(this, m);
        return this;
    }


    /** TODO use the rng to generate one 64-bit or one 32-bit integer and use half of its bits for each random value needed */
    public float sample(/*FloatSupplier uniformRng*/ Random rng) {
        float rangeMax = this.rangeMax;
        float rangeMin = Math.min(rangeMax, this.rangeMin); //incase updated while reading, maintains min <= max
        float rangeDelta = (rangeMax - rangeMin);

        float mass = 0;
        boolean flat;
        if (rangeDelta <= 1f) {
            flat = true;
        } else {
            mass = mass();
            flat = (mass <= ScalarValue.EPSILON * rangeDelta);
        }

        float u = rng.nextFloat();
        if (flat)
            return rangeMin + u * rangeDelta; //flat, choose uniform random

        WritableTensor data = this.data;
        int bins = data.volume();

        float m = u * mass;
        boolean direction = (Integer.bitCount(Float.floatToRawIntBits(u) ) & 1) != 0; //one RNG call
        //boolean direction = rng.nextBoolean();

        int b;
        float ii;
        float B = Float.MAX_VALUE;
        for (b = 0; b < bins;) {
            float db = data.getAt(b);
            if (db > m) {
                B = b + m / db; //current bin plus fraction traversed
                break;
            } else {
                m -= db;
                b++;
            }
        }
        return (Math.min(bins, B)/bins) * rangeDelta + rangeMin;
    }

    public final int bins() {
        return data.volume();
    }


    public final float mass() {
        return MASS.getOpaque(this);
    }

}
//    private static int HistogramBins(int s) {
//        //TODO refine
//        int thresh = 4;
//        if (s <= thresh)
//            return s;
//        else
//            return (int)(thresh + Math.sqrt((s-thresh)));
//    }