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

    private final static AtomicFloatFieldUpdater<ArrayHistogram> MASS =
            new AtomicFloatFieldUpdater(ArrayHistogram.class, "mass");

    private WritableTensor data = AtomicFloatVector.Empty;

    private volatile float rangeMin, rangeMax;
    private volatile int mass = AtomicFloatFieldUpdater.iZero;

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
        if (bins() != bins) {
            //elides subsequent data fill, the new array will be set to zero
            resize(bins);
        } else {
            data.fill(0);
        }
        range(min, max);
        return new HistogramWriter(bins, max-min);
    }

    public final class HistogramWriter {

        final int bins;
        final float rangeDelta;
        float mass = 0;

        HistogramWriter(int bins, float rangeDelta) {
            this.bins = bins;
            this.rangeDelta = rangeDelta;
        }

        public void add(float value, float weight) {
            mass += weight;
            data.addAt(weight, Util.bin((value-rangeMin)/rangeDelta, bins));
        }

        /** returns mass */
        public float commit() {
            mass(mass);
            return mass;
        }
    }

//    public void add(float value, float weight) {
//        addWithoutSettingMass(value, weight);
//        MASS.add(this, weight);
//    }

    /** mass setter */
    public final ArrayHistogram mass(float m) {
        MASS.set(this, m);
        return this;
    }


    /** TODO use the rng to generate one 64-bit or one 32-bit integer and use half of its bits for each random value needed */
    public float sample(/*FloatSupplier uniformRng*/ Random rng) {
        float rangeDelta = (rangeMax - rangeMin);

        float mass = 0;
        boolean flat;
        if (rangeDelta < ScalarValue.EPSILON)
            flat = true;
        else {
            mass = mass();
            flat = (mass <= ScalarValue.EPSILON);
        }

        WritableTensor data = this.data;

        int bins = data.volume();
        if (flat)
            return rng.nextFloat() * bins; //flat, choose random


        float f0 = rng.nextFloat();
        float f = f0 * mass;
        boolean direction = (Integer.bitCount(Float.floatToRawIntBits(f0) ) & 1) != 0; //one RNG call

        //boolean direction = rng.nextBoolean();

        int i;
        float ii;
        if (direction) {
            for (i = bins - 1; (i >= 0); ) //downward
                if ((f -= data.getAt(i--)) < 0)
                    break;
            ii = i + 0.5f;
        } else {
            for (i = 0; i < bins;) //upward
                if ((f -= data.getAt(i++)) < 0)
                    break;
            ii = i - 0.5f;
        }

        float iii = ii + (rng.nextFloat() - 0.5f);

        //TODO sub-bin interpolate?
        //randomize within the bin's proximity, naively assuming a normal PDF
        //TODO use the relative density of the adjacent bin
        return Util.unitizeSafe( iii/(bins-1) ) * rangeDelta + rangeMin;
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