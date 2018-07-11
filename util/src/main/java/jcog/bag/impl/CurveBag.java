package jcog.bag.impl;

import jcog.Util;
import jcog.pri.Priority;
import jcog.pri.op.PriMerge;

import java.util.Map;
import java.util.Random;

/**
 * ArrayBag with a randomized sampling range
 */
public class CurveBag<X extends Priority> extends PriArrayBag<X> {


    public CurveBag(PriMerge mergeFunction, Map<X, X> map, int cap) {
        this(mergeFunction, map);
        setCapacity(cap);
    }


    public CurveBag(PriMerge mergeFunction, Map<X, X> map) {
        super(mergeFunction, map);
    }

    @Override
    protected int sampleStart(Random random, int size) {
        assert (size > 0);
        if (size == 1 || random == null)
            return 0;
        else {
//            float min = this.priMin();
//            float max = this.priMax();
//            float diff = max - min;
//            if (diff > Prioritized.EPSILON * size) {
                float i = random.nextFloat();

                //i = Util.lerp(diff, i /* flat */, (i * i) /* curved */);
                i = (i*i);
                int j = Util.clamp(0, Math.round(i * (size - 1)), size-1);
                return j;
//            } else {
//                return random.nextInt(size);
//            }
        }
    }

    @Override
    protected int sampleNext(Random rng, int size, int i) {

        float runLength = 3;
        float restartProb = (1f/(1+runLength));
        if (rng.nextFloat() < restartProb) {
            return sampleStart(rng, size);
        }

        if (--i >= 0)
            return i; //decrease toward high end
        else
            return sampleStart(rng, size);
    }


}