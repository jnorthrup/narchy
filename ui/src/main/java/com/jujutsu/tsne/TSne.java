package com.jujutsu.tsne;

import com.jujutsu.tsne.barneshut.TSneConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
*
* Author: Leif Jonsson (leif.jonsson@gmail.com)
* 
* This is a Java implementation of van der Maaten and Hintons t-sne 
* dimensionality reduction technique that is particularly well suited 
* for the visualization of high-dimensional datasets
*
*/
public interface TSne {

	public static Logger logger = LoggerFactory.getLogger(TSne.class);

//    static TSneConfiguration buildConfig(double[][] xin, int outputDims, int initial_dims,
//                                         double perplexity, int max_iter, boolean use_pca, double theta, boolean silent, boolean printError) {
//        return new TSneConfig(xin, outputDims, initial_dims, perplexity, max_iter, use_pca, theta, silent, printError);
//    }
//
//    static TSneConfiguration buildConfig(double[][] xin, int outputDims, int initial_dims,
//                                         double perplexity, int max_iter, boolean use_pca, double theta, boolean silent) {
//        return new TSneConfig(xin, outputDims, initial_dims, perplexity, max_iter, use_pca, theta, silent, true);
//    }
//
//    static TSneConfiguration buildConfig(double[][] xin, int outputDims, int initial_dims,
//                                         double perplexity, int max_iter) {
//        return new TSneConfig(xin, outputDims, initial_dims, perplexity, max_iter, true, 0.5, false, true);
//    }

    double [][] tsne(TSneConfiguration config);

	void stop();

	class R {
		double [][] P;
		double [] beta;
		double H;
	}

}
