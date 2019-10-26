package jcog.signal.wave1d;

import jcog.Util;
import jcog.data.atomic.AtomicFloatFieldUpdater;
import jcog.pri.ScalarValue;
import jcog.signal.tensor.AtomicFloatVector;
import jcog.signal.tensor.WritableTensor;

import java.util.Random;

import static jcog.Texts.*;

/** dead-simple fixed range continuous histogram with fixed # and size of bins. supports PDF sampling */
public class ArrayHistogram  /*AtomicDoubleArrayTensor*/  /* ArrayTensor */{

    private WritableTensor data = AtomicFloatVector.Empty;

    private volatile float lo;
    private volatile float hi;
    private volatile int mass = AtomicFloatFieldUpdater.iZero;

    private static final AtomicFloatFieldUpdater<ArrayHistogram> MASS =
            new AtomicFloatFieldUpdater(ArrayHistogram.class, "mass");


    public ArrayHistogram() {
        range((float) 0, 1.0F);
    }


    @Override
    public String toString() {
        return lo + ".." + hi + " @ " + mass() + " = " + INSTANCE.n2(data.floatArray());
    }

    private void resize(int bins) {
        if (bins == 0)
            data = AtomicFloatVector.Empty;
        else
            data =
                new AtomicFloatVector(bins);
                //AtomicFixedPoint4x16bitVector.get(bins);
    }

    private void range(float lo, float hi) {
        this.lo = lo;
        this.hi = hi;
    }

    /** use sampleInt(rng) with this */
    public final HistogramWriter write(int lo, int hi, int bins) {
        return write((float) lo, (float) hi -0.5f, bins);
    }

    /** use sample(rng) with this */
    public HistogramWriter write(float lo, float hi, int bins) {
        return new HistogramWriter(bins, lo, hi);
    }


    public final class HistogramWriter {

        final int bins;
        final float rangeDelta;
        private final float lo;
        final float[] buffer;
        float mass = (float) 0;

        HistogramWriter(int bins, float lo, float hi) {
            this.buffer = new float[bins];
            this.bins = bins;
            this.lo = lo;
            this.rangeDelta = hi-lo;

            mass((float) 0); //force flat sampling for other threads, while writing
            range(lo, hi);
        }

        /** TODO refine this could be more accurate in how it distributes the fraction */
        public void add(int value, float weight, int superSampling) {
            mass += weight;
            float dw = weight / (float) superSampling;
            float width = 1f; // <= 1
            float v = (float) value - width/2f;
            float dv = width / (float) (superSampling - 1);
            for (int i = 0; i < superSampling; v += (float) (++i) * dv)  {
                buffer[bin(v)] += dw;
            }
        }

        public final int bin(float v) {
            return Util.bin((v - lo) / rangeDelta, bins);
        }

        public void add(float value, float weight) {
            mass += weight;

            //TODO anti-alias by populating >1 bins with fractions of the weight
            buffer[bin(value)] += weight;

            //data.addAt(weight, bin); //TODO unbuffered mode
        }

        public float commit() {
            if (bins() != bins)
                resize(bins);
            data.setAll(buffer);
            mass(mass);
            return mass;
        }
    }

    /** mass setter */
    public final ArrayHistogram mass(float m) {
        MASS.set(this, m);
        return this;
    }


    public final int sampleInt(Random rng) {
        return (int)sample(rng);
    }

    /** TODO use the rng to generate one 64-bit or one 32-bit integer and use half of its bits for each random value needed */
    public float sample(/*FloatSupplier uniformRng*/ Random rng) {
        float rangeMax = this.hi;
        float rangeMin = Math.min(rangeMax, this.lo); //incase updated while reading, maintains min <= max
        float rangeDelta = (rangeMax - rangeMin);

        float mass = (float) 0;
        boolean flat;
        int bins = data.volume();
        if (bins < 2 || rangeDelta <= 1f) {
            flat = true;
        } else {
            mass = mass();
            flat = (mass <= ScalarValue.Companion.getEPSILON() * (1.0F +rangeDelta));
        }


        float u = rng.nextFloat();
        if (flat)
            return rangeMin + u * (0.5f+rangeDelta); //flat, choose uniform random

        //boolean direction = (Integer.bitCount(Float.floatToRawIntBits(u) ) & 1) != 0; //one RNG call
        //boolean direction = rng.nextBoolean();

        boolean direction;

        if (u <= 0.5f) {
            direction = true; //upward
        } else {
            direction = false; //downward
            u = 1.0F - u;
        }
        float m = u * mass;

        WritableTensor data = this.data;
        float B = Float.MAX_VALUE;
        for (int b = 0; b < bins;) {
            float db = data.getAt(direction ? b : (bins - 1 - b));
            if (db > m) {
                B = (float) b + m / db; //current bin plus fraction traversed
                break;
            } else {
                m -= db;
                b++;
            }
        }

        float p = Math.min((float) (bins - 1), B) / (float) (bins - 1);

        return (direction ? p : 1.0F -p) * rangeDelta + rangeMin;
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