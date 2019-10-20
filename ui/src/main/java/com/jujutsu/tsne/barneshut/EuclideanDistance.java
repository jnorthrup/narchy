package com.jujutsu.tsne.barneshut;

public class EuclideanDistance implements Distance{

	@Deprecated public EuclideanDistance() {
	}

	@Override
	public double distance(DataPoint d1, DataPoint d2) {
		return Math.sqrt(distanceSq(d1,d2));
	}

	@Override
	public double distanceSq(DataPoint d1, DataPoint d2) {
		var dd = .0;
		var x1 = d1.getDataRef();
		var x2 = d2.getDataRef();
        for(var d = 0; d < d1._D; d++) {
			var diff = (x1[d] - x2[d]);
            dd += diff * diff;
	    }
	    return dd;
	}

}
