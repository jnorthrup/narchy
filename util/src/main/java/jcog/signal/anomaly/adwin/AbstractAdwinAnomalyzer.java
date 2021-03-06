package jcog.signal.anomaly.adwin;

/**
 * This is the foundation of our reimplementation of the core ADWIN algorithm.
 * It basically contains the checkHistogramForCut which receives a {@link AdwinHisto} and detects concept drifts on it.
 * It is implemented by {@link SingleThreadAdwinModel} or {@link HalfCutCheckThreadExecutorADWINImpl}
 */
 abstract class AbstractAdwinAnomalyzer {
    private final double delta;
    private final int minKeepSize;
    private final int minCutSize;

    public AbstractAdwinAnomalyzer(double delta) {
        this.delta = delta;
        this.minKeepSize = 7;
        this.minCutSize = 7;
    }

    /**
     * Detects concept drifts on a given {@link AdwinHisto}
     * @param histogram
     * @param iterable
     * @param numCutPointsToCheck
     * @return true if concept drift was found
     */
    public boolean checkHistogramForCut(AdwinHisto histogram, Iterable<Bucket> iterable, int numCutPointsToCheck) {
        double keepTotal = histogram.sum();
        double keepVariance = histogram.sum();
        int keepSize = histogram.size();

        double cutTotal = (double) 0;
        double cutVariance = (double) 0;
        int cutSize = 0;

        int cutPointsChecked = 0;
        for (Bucket bucket : iterable) {
            double bucketTotal = bucket.sum();
            double bucketVariance = bucket.variance();
            double bucketSize = (double) bucket.size();
            double bucketMean = bucket.mean();

            keepTotal -= bucketTotal;
            keepVariance -= bucketVariance + (double) keepSize * bucketSize * Math.pow(keepTotal / (double) keepSize - bucketMean, 2.0) / ((double) keepSize + bucketSize);
            keepSize = (int) ((double) keepSize - bucketSize);

            cutTotal += bucketTotal;
            if (cutSize > 0)
                cutVariance += bucketVariance + (double) cutSize * bucketSize * Math.pow(cutTotal / (double) cutSize - bucketMean, 2.0) / ((double) cutSize + bucketSize);
            cutSize = (int) ((double) cutSize + bucketSize);


            cutPointsChecked++;

            if (keepSize >= minKeepSize && cutSize >= minCutSize && isCutPoint(histogram, keepTotal, keepVariance, keepSize, cutTotal, cutVariance, cutSize)) {
                return true;
            } else if (keepSize < minKeepSize) {
                return false;
            } else if (cutPointsChecked == numCutPointsToCheck) {
                return false;
            }
        }
        return false;
    }

    private boolean isCutPoint(AdwinHisto histogram, double keepTotal, double keepVariance, int keepSize, double cutTotal, double cutVariance, int cutSize) {
        double absMeanDifference = Math.abs(keepTotal / (double) keepSize - cutTotal / (double) cutSize);
        double dd = Math.log(2.0 * Math.log((double) histogram.size()) / delta);
        double m = 1.0 / (double) (keepSize - minKeepSize + 3) + 1.0 / (double) (cutSize - minCutSize + 3);
        double epsilon = Math.sqrt(2.0 * m * (histogram.variance() / (double) histogram.size()) * dd) + 2.0 / 3.0 * dd * m;
        return absMeanDifference > epsilon;
    }

    public abstract boolean execute(AdwinHisto histogram);
    public abstract void terminate();

    /**
     * This is the serial implementation of ADWIN.
     * It basically executes a full cut detection in the main thread.
     */
    public static class SingleThreadAdwinModel extends AbstractAdwinAnomalyzer {

        public SingleThreadAdwinModel(double delta) {
            super(delta);
        }


        @Override
        public boolean execute(AdwinHisto histogram) {

            boolean tryToFindCut = true;
            boolean cutFound = false;
            while (tryToFindCut) {
                tryToFindCut = false;
                if (checkHistogramForCut(histogram,
                        histogram.reverseBucketIterable(),
     histogram.buckets() - 1)) {
                    histogram.removeBuckets(1);
                    tryToFindCut = true;
                    cutFound = true;
                }
            }
            return cutFound;
        }


        @Override
        public void terminate() {}

    }
}
