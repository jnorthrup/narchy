package com.jujutsu.tsne.barneshut;

import static java.lang.Math.min;
import static java.lang.Math.sqrt;

public class DataPoint {
	
	private final int _ind;
	double [] _x;
	final int _D;
	
	public DataPoint() {
        _D = 1;
        _ind = -1;
    }

	public DataPoint(int D, int ind, double [] x) {
		_D = D;
		_ind = ind;
		_x = x.clone();
	}
	
	@Override
	public String toString() {
		StringBuilder xStr = new StringBuilder();
		for (int i = 0; i < min(20,_x.length); i++) {
			xStr.append(_x[i]).append(", ");
		}
		return xStr.append("DataPoint (index=").append(_ind).append(", Dim=").append(_D).append(", point=").append(xStr).append(')').toString();
	}

	public int index() { return _ind; }
	int dimensionality() { return _D; }
	double x(int d) { return _x[d]; }
	
	public double euclidean_distance( DataPoint t1 ) {
	    double dd = .0;
	    double [] x1 = t1._x;
	    double [] x2 = _x;
	    double diff;
	    for(int d = 0; d < t1._D; d++) {
	        diff = (x1[d] - x2[d]);
	        dd += diff * diff;
	    }
	    return sqrt(dd);
	}
	
	public static double euclidean_distance( DataPoint t1, DataPoint t2 ) {
	    double dd = .0;
	    double [] x1 = t1._x;
	    double [] x2 = t2._x;
	    double diff;
	    for(int d = 0; d < t1._D; d++) {
	        diff = (x1[d] - x2[d]);
	        dd += diff * diff;
	    }
	    return sqrt(dd);
	}
}
