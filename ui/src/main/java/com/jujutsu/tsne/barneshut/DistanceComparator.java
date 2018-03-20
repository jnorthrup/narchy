package com.jujutsu.tsne.barneshut;

import java.util.Comparator;

public class DistanceComparator implements Comparator<DataPoint> {
	private final DataPoint refItem;
	private final Distance dist;
	
	DistanceComparator(DataPoint refItem) {
		this.refItem = refItem;
		this.dist = new EuclideanDistance();
	}
	
	DistanceComparator(DataPoint refItem, Distance dist) {
		this.refItem = refItem;
		this.dist = dist;
	}

	@Override
	public int compare(DataPoint o1, DataPoint o2) {
		return Double.compare(dist.distanceSq(o1, refItem), dist.distanceSq(o2, refItem));
	}
}