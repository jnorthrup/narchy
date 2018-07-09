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
interface TSne {

	Logger logger = LoggerFactory.getLogger(TSne.class);
















    double [][] tsne(TSneConfiguration config);

	void stop();

	class R {
		double [][] P;
		double [] beta;
		double H;
	}

}
