/*
 * Copyright (C) 2015 Machine Learning Lab - University of Trieste, 
 * Italy (http:
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http:
 */
package jcog.grammar.evolve.utils;

import static jcog.Texts.*;

/**
 *
 * @author MaleLabTs
 */
public final class BasicStats {

    
    public long fp = 0L;
    public transient long fn = 0L;
    public long tp = 0L;
    public transient long tn = 0L;

    @Override
    public String toString() {
        return "accuracy=" + INSTANCE.n4(accuracy()) + ", precision=" + INSTANCE.n4(precision()) +
                ", recall=" + INSTANCE.n4(recall()) +
                ", falsePositiveRate=" + INSTANCE.n4(fpr()) + ", falseNegativeRate=" + INSTANCE.n4(fnr());
    }

    public double accuracy() {
        return ((double) (tp + tn)) / (double) (tp + tn + fp + fn);
    }

    public double precision() {
        return ((double) tp) / (double) (tp + fp);
    }

    public double recall() {
        return ((double) tp) / (double) (tp + fn);
    }
    
    public double fpr(){
        return ((double) fp) / (double) (tn + fp);
    }
    
    public double fnr(){
        return ((double) fn) / (double) (tp + fn);
    }

    /**
     * To use when number of positives cases != tp+fn (eg: text extraction)
     *
     * @param positives
     * @return
     */
    public double recall(int positives) {
        return ((double) tp) / (double) (positives);
    }

    public double trueNegativeRate() {
        return ((double) tn) / (double) (tn + fn);
    }

    public double specificity() {
        return ((double) tn) / (double) (tn + fp);
    }

    /**
     * To use when number of negative cases != tn+fp (eg: text extraction)
     *
     * @param negatives
     * @return
     */
    public double specificity(int negatives) {
        return ((double) tn) / (double) (negatives);
    }

    public double fMeasure() {
        double precision = this.precision();
        double recall = this.recall();
        return 2.0 * (precision * recall) / (precision + recall);
    }

    public BasicStats add(BasicStats stats) {
        this.fp += stats.fp;
        this.fn += stats.fn;
        this.tp += stats.tp;
        this.tn += stats.tn;
        return this;
    }
}
