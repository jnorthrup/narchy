package jcog.learn;

import jcog.math.Quantiler;

public class QuantileDiscretize1D implements Discretize1D {

    Quantiler q = null;

    private boolean updated = false;
    float[] thresh = null;
    private boolean difference;

    @Override
    public void reset(int levels, double min, double max) {
//        assert(levels>1);
        q = new Quantiler(8);
        updated = false;
        this.thresh = new float[levels-1];
    }

    @Override
    public void put(double value) {
        q.add((float)value);
        updated = false;
    }

    protected void ensureUpdated() {
        if (!updated) {

            boolean difference = true;
            for (int i = 0; i < thresh.length; i++) {
                float t = q.quantile(((float) i +0.5f) / (float) thresh.length);
                if (i > 0 && Math.abs(t - thresh[i-1]) < Float.MIN_NORMAL) {
                    difference = false; //collapsed
                }
                thresh[i] = t;
            }
            this.updated = true;
            this.difference = difference;
        }

    }

    @Override
    public int index(double value) {
        ensureUpdated();

        if (!difference)
            return 0;

        int i;
        for (i = 0; i < thresh.length; i++) {
            if ((double) thresh[i] > value)
                break;
        }
        return i;

//        int nearest = -1;
//        float nearestDist = Float.POSITIVE_INFINITY;
//        for (int i = 0; i < thresh.length; i++) {
//            float d = (float) Math.abs(thresh[i] - value);
//            if (d < nearestDist) {
//                nearest = i;
//                nearestDist = d;
//            }
//        }
//        return nearest;
    }

    @Override
    public double[] value(int v) {
        if (v == 0) {
            return new double[] { Double.NEGATIVE_INFINITY, (double) thresh[0]};
        } else if (v == thresh.length) {
            return new double[] {(double) thresh[thresh.length - 1], Double.POSITIVE_INFINITY  };
        } else {
            return new double[] {(double) thresh[v - 1], (double) thresh[v]};
        }
    }
}
