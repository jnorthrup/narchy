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
package jcog.signal.anomaly;

import jcog.TODO;

/**
 * Anomaly Analysis - an instance of an analysis of potential anomalies in a changing signal
 *
 * TODO make this a view to a signal stream that is asynchronously updated and transparently iterates the source
 * at specific time intervals
 *
 * and synchronous sub-view with synchronously polled event reporting system
 *
 */
public class Anomalysis {

    private final double predicted;

    private final AnomalyLevel level;

    private final AnomalyThresholds thresholds;

    /** TODO */
    public interface Anomalyzer {

    }

    public Anomalysis(double predicted, AnomalyLevel level, AnomalyThresholds thresh) {
        this.predicted = predicted;
        this.level = level;
        this.thresholds = thresh;
    }

    /** trainer provided meta-classification of the classification, for interactive or online training purposes */
    public void feedback(double accuracy, double utility /* .... */) {
        throw new TODO();
    }

    /**
     * Weak and strong thresholds to support both one- and two-tailed tests.
     */
    public static class AnomalyThresholds {
        private final double upperStrong, upperWeak, lowerStrong, lowerWeak;

        public AnomalyThresholds(double upperStrong, double upperWeak, double lowerStrong, double lowerWeak) {
            this.upperStrong = upperStrong;
            this.upperWeak = upperWeak;
            this.lowerStrong = lowerStrong;
            this.lowerWeak = lowerWeak;
        }

        //
    //    @JsonCreator
    //    public AnomalyThresholds(
    //            @JsonProperty("upperStrong") Double upperStrong,
    //            @JsonProperty("upperWeak") Double upperWeak,
    //            @JsonProperty("lowerWeak") Double lowerWeak,
    //            @JsonProperty("lowerStrong") Double lowerStrong) {
    //
    //        isFalse(upperStrong == null && upperWeak == null && lowerWeak == null && lowerStrong == null,
    //                "At least one of the thresholds must be not null");
    //
    //        if (upperStrong != null) {
    //            isTrue(upperWeak == null || upperStrong >= upperWeak, String.format("Required: upperStrong (%f) >= upperWeak (%f)", upperStrong, upperWeak));
    //            isTrue(lowerWeak == null || upperStrong >= lowerWeak, String.format("Required: upperStrong (%f) >= lowerWeak (%f)", upperStrong, lowerWeak));
    //            isTrue(lowerStrong == null || upperStrong >= lowerStrong, String.format("Required: upperStrong (%f) >= lowerStrong (%f)", upperStrong, lowerStrong));
    //        }
    //        if (upperWeak != null) {
    //            isTrue(lowerWeak == null || upperWeak >= lowerWeak, String.format("Required: upperWeak (%f) >= lowerWeak (%f)", upperWeak, lowerWeak));
    //            isTrue(lowerStrong == null || upperWeak >= lowerStrong, String.format("Required: upperWeak (%f) >= lowerStrong (%f)", upperWeak, lowerStrong));
    //        }
    //        if (lowerWeak != null) {
    //            isTrue(lowerStrong == null || lowerWeak >= lowerStrong, String.format("Required: lowerWeak (%f) >= lowerStrong (%f)", lowerWeak, lowerStrong));
    //        }
    //
    //        this.upperStrong = upperStrong;
    //        this.upperWeak = upperWeak;
    //        this.lowerStrong = lowerStrong;
    //        this.lowerWeak = lowerWeak;
    //    }

        public AnomalyLevel classify(double value) {
            if (upperStrong==upperStrong && value >= upperStrong) {
                return AnomalyLevel.STRONG;
            } else if (upperWeak==upperWeak && value >= upperWeak) {
                return AnomalyLevel.WEAK;
            } else if (lowerStrong==lowerStrong & value <= lowerStrong) {
                return AnomalyLevel.STRONG;
            } else if (lowerWeak==lowerWeak && value <= lowerWeak) {
                return AnomalyLevel.WEAK;
            } else {
                return AnomalyLevel.NORMAL;
            }
        }
    }

    /**
     * Anomaly Type enum.
     */
    public enum AnomalyType {

        /**
         * Left tail. Generate alerts below the threshold.
         */
        LEFT_TAILED,

        /**
         * Right tail. Generate alerts above the threshold.
         */
        RIGHT_TAILED,

        /**
         * Both tails. Includes both left and right tails.
         */
        TWO_TAILED

    }

    /**
     * Anomaly level enum.
     */
    public enum AnomalyLevel {

        /**
         * Normal data point (not an anomaly).
         */
        NORMAL,

        /**
         * Weak outlier.
         */
        WEAK,

        /**
         * Strong outlier.
         */
        STRONG,

        /**
         * No classification because the model is warming up.
         */
        MODEL_WARMUP,

        /**
         * Unknown outlier. Should be used when we are not sure about the anomaly level. e.g. during the warm up period.
         */
        UNKNOWN
    }
}
