package com.jujutsu.tsne.barneshut;

@FunctionalInterface
public interface Distance {
	double distance(DataPoint d1, DataPoint d2);

	default double distanceSq(DataPoint d1, DataPoint d2) {
		double x = distance(d1, d2);
		return x*x;
	}
}
