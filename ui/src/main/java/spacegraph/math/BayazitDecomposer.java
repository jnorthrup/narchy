package spacegraph.math;

//Taken from BayazitDecomposer.java (Physics Body Editor):
//http://code.google.com/p/box2d-editor/source/browse/editor/src/aurelienribon/bodyeditor/maths/earclipping/bayazit/BayazitDecomposer.java
//Taken from BayazitDecomposer.cs (FarseerPhysics.Common.Decomposition.BayazitDecomposer)
//at http://farseerphysics.codeplex.com


import jcog.Util;
import jcog.list.FasterList;

import java.util.Collections;

/** <summary>
 * Convex decomposition algorithm created by Mark Bayazit (http://mnbayazit.com/)
 * Translated to Java by Aurelien Ribon (http://aurelienribon.com/) in his Physics Body Editor.
 * For more information about this algorithm, see http://mnbayazit.com/406/bayazit
 * </summary>
 */
public class BayazitDecomposer {

	public static final float Epsilon = 1.192092896e-07f;
	public static final int MaxPolygonVertices = 8;

	public static v2 cross(v2 a, float s) {
		return new v2(s * a.y, -s * a.x);
	}

	private static v2 at(int i, FasterList<v2> vertices) {
		int s = vertices.size();
		return vertices.get(i < 0 ? s - (-i % s) : i % s);
	}

	private static FasterList<v2> copy(int i, int j, FasterList<v2> vertices) {
		FasterList<v2> p = new FasterList<v2>();
		while (j < i)
			j += vertices.size();
		// p.reserve(j - i + 1);
		for (; i <= j; ++i) {
			p.add(at(i, vertices));
		}
		return p;
	}

	public static float getSignedArea(FasterList<v2> vect) {
		int i;
		float area = 0;
		for (i = 0; i < vect.size(); i++) {
			int j = (i + 1) % vect.size();
			v2 vi = vect.get(i);
			v2 vj = vect.get(j);
			area += vi.x * vj.y;
			area -= vi.y * vj.x;
		}
		area /= 2.0f;
		return area;
	}

	public static float getSignedArea(v2[] vect) {
		int i;
		float area = 0;
		for (i = 0; i < vect.length; i++) {
			int j = (i + 1) % vect.length;
			v2 vi = vect[i];
			v2 vj = vect[j];
			area += vi.x * vj.y;
			area -= vi.y * vj.x;
		}
		area /= 2.0f;
		return area;
	}

	public static boolean isCounterClockWise(FasterList<v2> vect) {
		// We just return true for lines
		return vect.size() < 3 ? true : getSignedArea(vect) > 0.0f;
	}

	public static boolean isCounterClockWise(v2[] vect) {
		// We just return true for lines
		return vect.length < 3 ? true : getSignedArea(vect) > 0.0f;
	}

	// / <summary>
	// / Decompose the polygon into several smaller non-concave polygon.
	// / If the polygon is already convex, it will return the original polygon,
	// unless it is over Settings.MaxPolygonVertices.
	// / Precondition: Counter Clockwise polygon
	// / </summary>
	// / <param name="vertices"></param>
	// / <returns></returns>
	public static FasterList<FasterList<v2>> convexPartition(FasterList<v2> vertices) {
		// We force it to CCW as it is a precondition in this algorithm.
		// vertices.ForceCounterClockWise();
		if (!isCounterClockWise(vertices)) {
			// Collections.reverse(vertices);
			Collections.reverse(vertices);
			// FasterList<v2> reversed = new FasterList<v2>(vertices.size());
			// for (int i = vertices.size() - 1; i <= 0; i--) {
			// reversed.add(vertices.get(i));
			// }
			// vertices = reversed;
		}
		FasterList<FasterList<v2>> list = new FasterList<FasterList<v2>>();
		float d, lowerDist, upperDist;
		v2 p;
		v2 lowerInt = new v2();
		v2 upperInt = new v2(); // intersection points
		int lowerIndex = 0, upperIndex = 0;
		FasterList<v2> lowerPoly, upperPoly;
		for (int i = 0; i < vertices.size(); ++i) {
			if (reflex(i, vertices)) {
				lowerDist = upperDist = Float.MAX_VALUE; // std::numeric_limits<qreal>::max();
				for (int j = 0; j < vertices.size(); ++j) {
					// if line intersects with an edge
					if (left(at(i - 1, vertices), at(i, vertices), at(j, vertices))
							&& rightOn(at(i - 1, vertices), at(i, vertices), at(j - 1, vertices))) {
						// find the point of intersection
						p = lineIntersect(at(i - 1, vertices), at(i, vertices), at(j, vertices),
								at(j - 1, vertices));
						if (right(at(i + 1, vertices), at(i, vertices), p)) {
							// make sure it's inside the poly
							d = squareDist(at(i, vertices), p);
							if (d < lowerDist) {
								// keep only the closest intersection
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
				// if there are no vertices to connect to, choose a point in the
				// middle
				if (lowerIndex == (upperIndex + 1) % vertices.size()) {
					v2 sp = new v2((lowerInt.x + upperInt.x) / 2,
							(lowerInt.y + upperInt.y) / 2);
					lowerPoly = copy(i, upperIndex, vertices);
					lowerPoly.add(sp);
					upperPoly = copy(lowerIndex, i, vertices);
					upperPoly.add(sp);
				} else {
					double highestScore = 0, bestIndex = lowerIndex;
					while (upperIndex < lowerIndex)
						upperIndex += vertices.size();
					for (int j = lowerIndex; j <= upperIndex; ++j) {
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
		// polygon is already convex
		if (vertices.size() > MaxPolygonVertices) {
			lowerPoly = copy(0, vertices.size() / 2, vertices);
			upperPoly = copy(vertices.size() / 2, 0, vertices);
			list.addAll(convexPartition(lowerPoly));
			list.addAll(convexPartition(upperPoly));
		} else
			list.add(vertices);
		// The polygons are not guaranteed to be with collinear points. We
		// remove
		// them to be sure.
		for (int i = 0; i < list.size(); i++) {
			list.set(i, Simplify2D.collinearSimplify(list.get(i), 0));
		}
		// Remove empty vertice collections
		for (int i = list.size() - 1; i >= 0; i--) {
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
		int n = vertices.size();
		for (int k = 0; k < n; ++k) {
			if ((k + 1) % n == i || k == i || (k + 1) % n == j || k == j) {
				continue; // ignore incident edges
			}
			v2 intersectionPoint = new v2();
			if (lineIntersect(at(i, vertices), at(j, vertices), at(k, vertices),
					at(k + 1, vertices), true, true, intersectionPoint)) {
				return false;
			}
		}
		return true;
	}

	public static v2 lineIntersect(v2 p1, v2 p2, v2 q1, v2 q2) {
		v2 i = new v2();
		float a1 = p2.y - p1.y;
		float b1 = p1.x - p2.x;
		float c1 = a1 * p1.x + b1 * p1.y;
		float a2 = q2.y - q1.y;
		float b2 = q1.x - q2.x;
		float c2 = a2 * q1.x + b2 * q1.y;
		float det = a1 * b2 - a2 * b1;
		if (!Util.equals(det, (float) 0, Epsilon)) {
			// lines are not parallel
			i.x = (b2 * c1 - b1 * c2) / det;
			i.y = (a1 * c2 - a2 * c1) / det;
		}
		return i;
	}

	// / <summary>
	// / This method detects if two line segments (or lines) intersect,
	// / and, if so, the point of intersection. Use the
	// <paramname="firstIsSegment"/> and
	// / <paramname="secondIsSegment"/> parameters to set whether the
	// intersection point
	// / must be on the first and second line segments. Setting these
	// / both to true means you are doing a line-segment to line-segment
	// / intersection. Setting one of them to true means you are doing a
	// / line to line-segment intersection test, and so on.
	// / Note: If two line segments are coincident, then
	// / no intersection is detected (there are actually
	// / infinite intersection points).
	// / Author: Jeremy Bell
	// / </summary>
	// / <param name="point1">The first point of the first line segment.</param>
	// / <param name="point2">The second point of the first line
	// segment.</param>
	// / <param name="point3">The first point of the second line
	// segment.</param>
	// / <param name="point4">The second point of the second line
	// segment.</param>
	// / <param name="point">This is set to the intersection
	// / point if an intersection is detected.</param>
	// / <param name="firstIsSegment">Set this to true to require that the
	// / intersection point be on the first line segment.</param>
	// / <param name="secondIsSegment">Set this to true to require that the
	// / intersection point be on the second line segment.</param>
	// / <returns>True if an intersection is detected, false
	// otherwise.</returns>
	public static boolean lineIntersect(v2 point1, v2 point2, v2 point3,
			v2 point4, boolean firstIsSegment, boolean secondIsSegment, v2 point) {
		point = new v2();
		// these are reused later.
		// each lettered sub-calculation is used twice, except
		// for b and d, which are used 3 times
		float a = point4.y - point3.y;
		float b = point2.x - point1.x;
		float c = point4.x - point3.x;
		float d = point2.y - point1.y;
		// denominator to solution of linear system
		float denom = (a * b) - (c * d);
		// if denominator is 0, then lines are parallel
		if (!(denom >= -Epsilon && denom <= Epsilon)) {
			float e = point1.y - point3.y;
			float f = point1.x - point3.x;
			float oneOverDenom = 1.0f / denom;
			// numerator of first equation
			float ua = (c * e) - (a * f);
			ua *= oneOverDenom;
			// check if intersection point of the two lines is on line segment 1
			if (!firstIsSegment || ua >= 0.0f && ua <= 1.0f) {
				// numerator of second equation
				float ub = (b * e) - (d * f);
				ub *= oneOverDenom;
				// check if intersection point of the two lines is on line
				// segment 2
				// means the line segments intersect, since we know it is on
				// segment 1 as well.
				if (!secondIsSegment || ub >= 0.0f && ub <= 1.0f) {
					// check if they are coincident (no collision in this case)
					if (ua != 0f || ub != 0f) {
						// There is an intersection
						point.x = point1.x + ua * b;
						point.y = point1.y + ua * d;
						return true;
					}
				}
			}
		}
		return false;
	}

	// precondition: ccw
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
		float dx = b.x - a.x;
		float dy = b.y - a.y;
		return dx * dx + dy * dy;
	}
}

