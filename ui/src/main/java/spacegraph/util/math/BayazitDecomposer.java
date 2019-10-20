package spacegraph.util.math;


import jcog.Util;
import jcog.data.list.FasterList;
import jcog.math.v2;

import java.util.Collections;

/** <summary>
 * Convex decomposition algorithm created by Mark Bayazit (http:
 * Translated to Java by Aurelien Ribon (http:
 * For more information about this algorithm, see http:
 * </summary>
 */
class BayazitDecomposer {

	private static final float Epsilon = 1.192092896e-07f;
	private static final int MaxPolygonVertices = 8;

	public static v2 cross(v2 a, float s) {
		return new v2(s * a.y, -s * a.x);
	}

	private static v2 at(int i, FasterList<v2> vertices) {
		var s = vertices.size();
		return vertices.get(i < 0 ? s - (-i % s) : i % s);
	}

	private static FasterList<v2> copy(int i, int j, FasterList<v2> vertices) {
        while (j < i)
			j += vertices.size();

		var p = new FasterList<v2>();
        for (; i <= j; ++i) {
			p.add(at(i, vertices));
		}
		return p;
	}

	private static float getSignedArea(FasterList<v2> vect) {
        float area = 0;
		for (var i = 0; i < vect.size(); i++) {
			var j = (i + 1) % vect.size();
			var vi = vect.get(i);
			var vj = vect.get(j);
			area += vi.x * vj.y;
			area -= vi.y * vj.x;
		}
		area /= 2.0f;
		return area;
	}

	private static float getSignedArea(v2[] vect) {
        float area = 0;
		for (var i = 0; i < vect.length; i++) {
			var j = (i + 1) % vect.length;
			var vi = vect[i];
			var vj = vect[j];
			area += vi.x * vj.y;
			area -= vi.y * vj.x;
		}
		area /= 2.0f;
		return area;
	}

	private static boolean isCounterClockWise(FasterList<v2> vect) {
		
		return vect.size() < 3 || getSignedArea(vect) > 0.0f;
	}

	public static boolean isCounterClockWise(v2[] vect) {
		
		return vect.length < 3 || getSignedArea(vect) > 0.0f;
	}

	
	
	
	
	
	
	
	
	private static FasterList<FasterList<v2>> convexPartition(FasterList<v2> vertices) {
		
		
		if (!isCounterClockWise(vertices)) {
			
			Collections.reverse(vertices);
			
			
			
			
			
		}
		var list = new FasterList<FasterList<v2>>();
		var lowerInt = new v2();
		var upperInt = new v2();
		int lowerIndex = 0, upperIndex = 0;
		FasterList<v2> lowerPoly, upperPoly;
		for (var i = 0; i < vertices.size(); ++i) {
			if (reflex(i, vertices)) {
                float upperDist;
				var lowerDist = upperDist = Float.MAX_VALUE;
                for (var j = 0; j < vertices.size(); ++j) {

                    v2 p;
                    float d;
                    if (left(at(i - 1, vertices), at(i, vertices), at(j, vertices))
							&& rightOn(at(i - 1, vertices), at(i, vertices), at(j - 1, vertices))) {
						
						p = lineIntersect(at(i - 1, vertices), at(i, vertices), at(j, vertices),
								at(j - 1, vertices));
						if (right(at(i + 1, vertices), at(i, vertices), p)) {
							
							d = squareDist(at(i, vertices), p);
							if (d < lowerDist) {
								
								lowerDist = d;
								lowerInt = p;
								lowerIndex = j;
							}
						}
					}
					if (left(at(i + 1, vertices), at(i, vertices), at(j + 1, vertices))
							&& rightOn(at(i + 1, vertices), at(i, vertices), at(j, vertices))) {
						p = lineIntersect(at(i + 1, vertices), at(i, vertices), at(j, vertices),
								at(j + 1, vertices));
						if (left(at(i - 1, vertices), at(i, vertices), p)) {
							d = squareDist(at(i, vertices), p);
							if (d < upperDist) {
								upperDist = d;
								upperIndex = j;
								upperInt = p;
							}
						}
					}
				}
				
				
				if (lowerIndex == (upperIndex + 1) % vertices.size()) {
					var sp = new v2((lowerInt.x + upperInt.x) / 2,
                            (lowerInt.y + upperInt.y) / 2);
					lowerPoly = copy(i, upperIndex, vertices);
					lowerPoly.add(sp);
					upperPoly = copy(lowerIndex, i, vertices);
					upperPoly.add(sp);
				} else {
					double bestIndex = lowerIndex;
					while (upperIndex < lowerIndex)
						upperIndex += vertices.size();
                    double highestScore = 0;
                    for (var j = lowerIndex; j <= upperIndex; ++j) {
						if (canSee(i, j, vertices)) {
							double score = 1 / (squareDist(at(i, vertices), at(j, vertices)) + 1);
							if (reflex(j, vertices)) {
								if (rightOn(at(j - 1, vertices), at(j, vertices), at(i, vertices))
										&& leftOn(at(j + 1, vertices), at(j, vertices),
												at(i, vertices))) {
									score += 3;
								} else {
									score += 2;
								}
							} else {
								score += 1;
							}
							if (score > highestScore) {
								bestIndex = j;
								highestScore = score;
							}
						}
					}
					lowerPoly = copy(i, (int) bestIndex, vertices);
					upperPoly = copy((int) bestIndex, i, vertices);
				}
				list.addAll(convexPartition(lowerPoly));
				list.addAll(convexPartition(upperPoly));
				return list;
			}
		}
		
		if (vertices.size() > MaxPolygonVertices) {
			lowerPoly = copy(0, vertices.size() / 2, vertices);
			upperPoly = copy(vertices.size() / 2, 0, vertices);
			list.addAll(convexPartition(lowerPoly));
			list.addAll(convexPartition(upperPoly));
		} else
			list.add(vertices);
		
		
		
		for (var i = 0; i < list.size(); i++) {
			list.set(i, Simplify2D.collinearSimplify(list.get(i), 0));
		}
		
		for (var i = list.size() - 1; i >= 0; i--) {
			if (list.get(i).isEmpty()) list.remove(i);
		}
		return list;
	}

	private static boolean canSee(int i, int j, FasterList<v2> vertices) {
		if (reflex(i, vertices)) {
			if (leftOn(at(i, vertices), at(i - 1, vertices), at(j, vertices))
					&& rightOn(at(i, vertices), at(i + 1, vertices), at(j, vertices)))
				return false;
		} else {
			if (rightOn(at(i, vertices), at(i + 1, vertices), at(j, vertices))
					|| leftOn(at(i, vertices), at(i - 1, vertices), at(j, vertices))) return false;
		}
		if (reflex(j, vertices)) {
			if (leftOn(at(j, vertices), at(j - 1, vertices), at(i, vertices))
					&& rightOn(at(j, vertices), at(j + 1, vertices), at(i, vertices)))
				return false;
		} else {
			if (rightOn(at(j, vertices), at(j + 1, vertices), at(i, vertices))
					|| leftOn(at(j, vertices), at(j - 1, vertices), at(i, vertices))) return false;
		}
		var n = vertices.size();
		for (var k = 0; k < n; ++k) {
			if ((k + 1) % n == i || k == i || (k + 1) % n == j || k == j) {
				continue; 
			}
			var intersectionPoint = new v2();
			if (lineIntersect(at(i, vertices), at(j, vertices), at(k, vertices),
					at(k + 1, vertices), true, true)) {
				return false;
			}
		}
		return true;
	}

	private static v2 lineIntersect(v2 p1, v2 p2, v2 q1, v2 q2) {
		var i = new v2();
		var a1 = p2.y - p1.y;
		var b1 = p1.x - p2.x;
		var c1 = a1 * p1.x + b1 * p1.y;
		var a2 = q2.y - q1.y;
		var b2 = q1.x - q2.x;
		var c2 = a2 * q1.x + b2 * q1.y;
		var det = a1 * b2 - a2 * b1;
		if (!Util.equals(det, 0, Epsilon)) {
			
			i.x = (b2 * c1 - b1 * c2) / det;
			i.y = (a1 * c2 - a2 * c1) / det;
		}
		return i;
	}

	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	private static boolean lineIntersect(v2 point1, v2 point2, v2 point3,
                                         v2 point4, boolean firstIsSegment, boolean secondIsSegment) {
		var point = new v2();


		var a = point4.y - point3.y;
		var b = point2.x - point1.x;
		var c = point4.x - point3.x;
		var d = point2.y - point1.y;

		var denom = (a * b) - (c * d);
		
		if (!(denom >= -Epsilon && denom <= Epsilon)) {
			var e = point1.y - point3.y;
			var f = point1.x - point3.x;
			var oneOverDenom = 1.0f / denom;

			var ua = (c * e) - (a * f);
			ua *= oneOverDenom;
			
			if (!firstIsSegment || ua >= 0.0f && ua <= 1.0f) {

				var ub = (b * e) - (d * f);
				ub *= oneOverDenom;
				
				
				
				
				if (!secondIsSegment || ub >= 0.0f && ub <= 1.0f) {
					
					if (ua != 0f || ub != 0f) {
						
						point.x = point1.x + ua * b;
						point.y = point1.y + ua * d;
						return true;
					}
				}
			}
		}
		return false;
	}

	
	private static boolean reflex(int i, FasterList<v2> vertices) {
		return right(i, vertices);
	}

	private static boolean right(int i, FasterList<v2> vertices) {
		return right(at(i - 1, vertices), at(i, vertices), at(i + 1, vertices));
	}

	private static boolean left(v2 a, v2 b, v2 c) {
		return area(a, b, c) > 0;
	}

	private static boolean leftOn(v2 a, v2 b, v2 c) {
		return area(a, b, c) >= 0;
	}

	private static boolean right(v2 a, v2 b, v2 c) {
		return area(a, b, c) < 0;
	}

	private static boolean rightOn(v2 a, v2 b, v2 c) {
		return area(a, b, c) <= 0;
	}

	public static float area(v2 a, v2 b, v2 c) {
		return a.x * (b.y - c.y) + b.x * (c.y - a.y) + c.x * (a.y - b.y);
	}


	private static float squareDist(v2 a, v2 b) {
		var dx = b.x - a.x;
		var dy = b.y - a.y;
		return dx * dx + dy * dy;
	}
}

