package jcog.bag.impl;

import jcog.Util;
import jcog.pri.Prioritized;
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
        assert(size > 0);
        if (size == 1 || random==null)
            return 0;
        else {
            float min = this.priMin();
            float max = this.priMax();
            float diff = max - min;
            if (diff > Prioritized.EPSILON * size) {
                float i = random.nextFloat(); 
                
                i = Util.lerp(diff, i /* flat */, (i*i) /* curved */);
                int j = (int) /*Math.floor*/(i * (size-0.5f));
                if (j >= size) j = size-1;
                else if (j < 0) j = 0;
                return j;
            } else {
                return random.nextInt(size);
            }
        }
    }

    @Override
    protected int sampleNext(Random rng, int size, int i) {
        return sampleStart(rng, size);
    }

    






















































































































    






















}