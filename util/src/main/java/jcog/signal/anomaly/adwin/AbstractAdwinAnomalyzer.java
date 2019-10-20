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
        var keepTotal = histogram.sum();
        var keepVariance = histogram.sum();
        var keepSize = histogram.size();

        double cutTotal = 0;
        double cutVariance = 0;
        var cutSize = 0;

        var cutPointsChecked = 0;
        for (var bucket : iterable) {
            var bucketTotal = bucket.sum();
            var bucketVariance = bucket.variance();
            double bucketSize = bucket.size();
            var bucketMean = bucket.mean();

            keepTotal -= bucketTotal;
            keepVariance -= bucketVariance + keepSize * bucketSize * Math.pow(keepTotal / keepSize - bucketMean, 2) / (keepSize + bucketSize);
            keepSize -= bucketSize;

            cutTotal += bucketTotal;
            if (cutSize > 0)
                cutVariance += bucketVariance + cutSize * bucketSize * Math.pow(cutTotal / cutSize - bucketMean, 2) / (cutSize + bucketSize);
            cutSize += bucketSize;


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
        var absMeanDifference = Math.abs(keepTotal / keepSize - cutTotal / cutSize);
        var dd = Math.log(2.0 * Math.log(histogram.size()) / delta);
        var m = 1.0 / (keepSize - minKeepSize + 3) + 1.0 / (cutSize - minCutSize + 3);
        var epsilon = Math.sqrt(2.0 * m * (histogram.variance() / histogram.size()) * dd) + 2.0 / 3.0 * dd * m;
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

            var tryToFindCut = true;
            var cutFound = false;
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
