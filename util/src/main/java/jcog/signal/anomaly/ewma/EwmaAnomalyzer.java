/*
 * Copyright 2018-2019 Expedia Group, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jcog.signal.anomaly.ewma;

import jcog.signal.anomaly.Anomalysis;

import static java.lang.Math.sqrt;

// TODO Add model warmup param and anomaly level. See e.g. CUSUM, Individuals, PEWMA. [WLW]

/**
 * <p>
 * Anomaly detector based on the exponential weighted moving average (EWMA) chart, a type of control chart used in
 * statistical quality control. This is an online algorithm, meaning that it updates the thresholds incrementally as new
 * data comes in.
 * </p>
 * <p>
 * EWMA is also called "Single Exponential Smoothing", "Simple Exponential Smoothing" or "Basic Exponential Smoothing".
 * </p>
 * <p>
 * It takes a little while before the internal mean and variance estimates converge to something that makes sense. As a
 * rule of thumb, feed the detector 10 data points or so before using it for actual anomaly detection.
 * </p>
 *
 * @see <a href="https://en.wikipedia.org/wiki/EWMA_chart">EWMA chart</a>
 * @see <a href="https://en.wikipedia.org/wiki/Moving_average#Exponentially_weighted_moving_variance_and_standard_deviation">Exponentially weighted moving average and standard deviation</a>
 * @see <a href="https://www.itl.nist.gov/div898/handbook/pmc/section3/pmc324.htm">EWMA Control Charts</a>
 */
public final class EwmaAnomalyzer {

    /**
     * Mean estimate.
     */
    private double mean = 0.0;

    /**
     * Variance estimate.
     */
    private double variance = 0.0;

    final EwmaParams params = new EwmaParams();

    public EwmaAnomalyzer() {
        this.mean = params.initMeanEstimate;
    }


    public Anomalysis classify(long when, double what) {
        //notNull(metricData, "metricData can't be null");

        //val params = getParams();

        double stdDev = sqrt(this.variance);
        double weakDelta = params.weakSigmas * stdDev;
        double strongDelta = params.strongSigmas * stdDev;

        Anomalysis.AnomalyThresholds thresholds = new Anomalysis.AnomalyThresholds(
                this.mean + strongDelta,
                this.mean + weakDelta,
                this.mean - weakDelta,
                this.mean - strongDelta
        );

        updateEstimates(what);

        Anomalysis.AnomalyLevel level = thresholds.classify(what);

        return new Anomalysis(mean, level, thresholds);
    }

    private void updateEstimates(double value) {

        // https://en.wikipedia.org/wiki/Moving_average#Exponentially_weighted_moving_variance_and_standard_deviation
        // http://people.ds.cam.ac.uk/fanf2/hermes/doc/antiforgery/stats.pdf
        double diff = value - this.mean;
        double alpha = params.alpha;
        double incr = alpha * diff;
        this.mean += incr;

        // Welford's algorithm for computing the variance online
        // https://en.wikipedia.org/wiki/Algorithms_for_calculating_variance#Online_algorithm
        // https://www.johndcook.com/blog/2008/09/26/comparing-three-methods-of-computing-standard-deviation/
        this.variance = (1.0 - alpha) * (this.variance + diff * incr);
    }

    public static final class EwmaParams  {

        /**
         * Smoothing param. Somewhat misnamed because higher values lead to less smoothing, but it's called the
         * smoothing parameter in the literature.
         */
        private static final double alpha = 0.15;

        /**
         * Weak threshold sigmas.
         */
        private static final double weakSigmas = 3.0;

        /**
         * Strong threshold sigmas.
         */
        private static final double strongSigmas = 4.0;

        /**
         * Initial mean estimate.
         */
        private static final double initMeanEstimate = 0.0;

//        public void validate() {
//            assert(0.0 <= alpha && alpha <= 1.0): "Required: alpha in the range [0, 1]";
//            assert(weakSigmas > 0.0): "Required: weakSigmas > 0.0";
//            assert(strongSigmas > weakSigmas): "Required: strongSigmas > weakSigmas";
//        }
    }
}
