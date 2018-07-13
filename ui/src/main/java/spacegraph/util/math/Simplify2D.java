package spacegraph.util.math;

import jcog.data.bit.MetalBitSet;
import jcog.data.list.FasterList;

import java.security.InvalidParameterException;

public class Simplify2D {
	
	

	
	
	
	
	
	
	public static FasterList<v2> collinearSimplify(FasterList<v2> vertices, float collinearityTolerance) {
		
		int n = vertices.size();
		if (n < 3) return vertices;
		FasterList<v2> simplified = new FasterList<>();
		for (int i = 0; i < n; i++) {
			int prevId = i - 1;
			if (prevId < 0) prevId = n - 1;
			int nextId = i + 1;
			if (nextId >= n) nextId = 0;
			v2 prev = vertices.get(prevId);
			v2 current = vertices.get(i);
			v2 next = vertices.get(nextId);
			
			if (collinear(prev, current, next, collinearityTolerance))
				continue;
			simplified.add(current);
		}
		return simplified;
	}

	public static boolean collinear(v2 a, v2 b, v2 c, float tolerance) {
		return floatInRange(BayazitDecomposer.area(a, b, c), -tolerance, tolerance);
	}
	public static float collinearity(v2 a, v2 b, v2 c) {
		return Math.abs(BayazitDecomposer.area(a, b, c));
	}

	private static boolean floatInRange(float value, float min, float max) {
		return (value >= min && value <= max);
	}

	
	
	
	
	
	
	public static FasterList<v2> collinearSimplify(FasterList<v2> vertices) {
		return collinearSimplify(vertices, 0);
	}

	
	
	
	
	
	
	
	
	public static FasterList<v2> douglasPeuckerSimplify(FasterList<v2> vertices, float distanceTolerance) {
		
		int n = vertices.size();
		MetalBitSet usePt = MetalBitSet.bits(n);
		usePt.setAll();
		simplifySection(usePt, distanceTolerance, vertices, 0, n - 1);
		FasterList<v2> result = new FasterList<>();
		for (int i = 0; i < n; i++)
			if (usePt.get(i))
				result.add(vertices.get(i));
		return result;
	}

	private static void simplifySection(MetalBitSet usePt, float _distanceTolerance, FasterList<v2> vertices, int i, int j) {
		if ((i + 1) == j) return;
		v2 A = vertices.get(i);
		v2 B = vertices.get(j);
		double maxDistance = -1.0;
		int maxIndex = i;
		for (int k = i + 1; k < j; k++) {
			double distance = distancePointLine(vertices.get(k), A, B);
			if (distance > maxDistance) {
				maxDistance = distance;
				maxIndex = k;
			}
		}
		if (maxDistance <= _distanceTolerance)
			for (int k = i + 1; k < j; k++)
				usePt.clear(k);
		else {
			simplifySection(usePt, _distanceTolerance, vertices, i, maxIndex);
			simplifySection(usePt, _distanceTolerance, vertices, maxIndex, j);
		}
	}

	private static double distancePointPoint(v2 p, v2 p2) {
		double dx = p.x - p2.x;
		double dy = p.y - p2.x;
		return Math.sqrt(dx * dx + dy * dy);
	}

	private static double distancePointLine(v2 p, v2 A, v2 B) {
		
		if (A.x == B.x && A.y == B.y) return distancePointPoint(p, A);
		
		
		/*
		 * (1) AC dot AB r = --------- ||AB||^2 r has the following meaning: r=0
		 * Point = A r=1 Point = B r<0 Point is on the backward extension of AB
		 * r>1 Point is on the forward extension of AB 0<r<1 Point is interior
		 * to AB
		 */
		double r = ((p.x - A.x) * (B.x - A.x) + (p.y - A.y) * (B.y - A.y))
				/ ((B.x - A.x) * (B.x - A.x) + (B.y - A.y) * (B.y - A.y));
		if (r <= 0.0) return distancePointPoint(p, A);
		if (r >= 1.0) return distancePointPoint(p, B);
		/*
		 * (2) (Ay-Cy)(Bx-Ax)-(Ax-Cx)(By-Ay) s = -----------------------------
		 * Curve^2 Then the distance from C to Point = |s|*Curve.
		 */
		double s = ((A.y - p.y) * (B.x - A.x) - (A.x - p.x) * (B.y - A.y))
				/ ((B.x - A.x) * (B.x - A.x) + (B.y - A.y) * (B.y - A.y));
		return Math.abs(s) * Math.sqrt(((B.x - A.x) * (B.x - A.x) + (B.y - A.y) * (B.y - A.y)));
	}

	
	public static FasterList<v2> reduceByArea(FasterList<v2> vertices, float areaTolerance) {
		if (vertices.size() <= 3) return vertices;
		if (areaTolerance < 0) {
			throw new InvalidParameterException(
					"areaTolerance: must be equal to or greater then zero.");
		}
		FasterList<v2> result = new FasterList<>();
		v2 v1, v2, v3;
		float old1, old2, new1;
		v1 = vertices.get(vertices.size() - 2);
		v2 = vertices.get(vertices.size() - 1);
		areaTolerance *= 2;
		for (int index = 0; index < vertices.size(); ++index, v2 = v3) {
			if (index == vertices.size() - 1) {
				if (result.isEmpty()) {
					throw new InvalidParameterException("areaTolerance: The tolerance is too high!");
				}
				v3 = result.get(0);
			} else {
				v3 = vertices.get(index);
			}
			old1 = cross(v1, v2);
			old2 = cross(v2, v3);
			new1 = cross(v1, v3);
			if (Math.abs(new1 - (old1 + old2)) > areaTolerance) {
				result.add(v2);
				v1 = v2;
			}
		}
		return result;
	}

	private static Float cross(v2 a, v2 b) {
		return a.x * b.y - a.y * b.x;
	}

	
	
	
	
	
	
	public static void mergeParallelEdges(FasterList<v2> vertices, float tolerance) {
		if (vertices.size() <= 3) return; 
										
		boolean[] mergeMe = new boolean[vertices.size()];
		int newNVertices = vertices.size();
		
		for (int i = 0; i < vertices.size(); ++i) {
			int lower = (i == 0) ? (vertices.size() - 1) : (i - 1);
			int middle = i;
			int upper = (i == vertices.size() - 1) ? (0) : (i + 1);
			float dx0 = vertices.get(middle).x - vertices.get(lower).x;
			float dy0 = vertices.get(middle).y - vertices.get(lower).y;
			float dx1 = vertices.get(upper).y - vertices.get(middle).x;
			float dy1 = vertices.get(upper).y - vertices.get(middle).y;
			float norm0 = (float) Math.sqrt(dx0 * dx0 + dy0 * dy0);
			float norm1 = (float) Math.sqrt(dx1 * dx1 + dy1 * dy1);
			if (!(norm0 > 0.0f && norm1 > 0.0f) && newNVertices > 3) {
				
				mergeMe[i] = true;
				--newNVertices;
			}
			dx0 /= norm0;
			dy0 /= norm0;
			dx1 /= norm1;
			dy1 /= norm1;
			float cross = dx0 * dy1 - dx1 * dy0;
			float dot = dx0 * dx1 + dy0 * dy1;
			if (Math.abs(cross) < tolerance && dot > 0 && newNVertices > 3) {
				mergeMe[i] = true;
				--newNVertices;
			} else
				mergeMe[i] = false;
		}
		if (newNVertices == vertices.size() || newNVertices == 0) return;
		int currIndex = 0;
		
		FasterList<v2> oldVertices = new FasterList<>(vertices);
		vertices.clear();
		for (int i = 0; i < oldVertices.size(); ++i) {
			if (mergeMe[i] || newNVertices == 0 || currIndex == newNVertices) continue;
			
			vertices.add(oldVertices.get(i));
			++currIndex;
		}
	}

	
	
	
	
	
	
	public static FasterList<v2> mergeIdenticalPoints(FasterList<v2> vertices) {
		FasterList<v2> results = new FasterList<>();
        for (v2 vOriginal : vertices) {
            boolean alreadyExists = false;
            for (v2 v : results) {
                if (vOriginal.equals(v)) {
                    alreadyExists = true;
                    break;
                }
            }
            if (!alreadyExists) results.add(vOriginal);
        }
		return results;
	}

	
	
	
	
	
	
	
	public static FasterList<v2> reduceByDistance(FasterList<v2> vertices, final float distance) {
		
		if (vertices.size() < 3) return vertices;
		float distSq = distance * distance;
		FasterList<v2> simplified = new FasterList<>();
		for (int i = 0; i < vertices.size(); i++) {
			v2 current = vertices.get(i);
			int ii = i + 1;
			if (ii >= vertices.size()) ii = 0;
			v2 next = vertices.get(ii);
			v2 diff = new v2(next.x - current.x, next.y - current.y);
			
			if (diff.lengthSquared() <= distSq) continue;
			simplified.add(current);
		}
		return simplified;
	}

	
	
	
	
	
	
	public static FasterList<v2> reduceByNth(FasterList<v2> vertices, int nth) {
		
		if (vertices.size() < 3) return vertices;
		if (nth == 0) return vertices;
		FasterList<v2> result = new FasterList<>(vertices.size());
		for (int i = 0; i < vertices.size(); i++) {
			if (i % nth == 0) continue;
			result.add(vertices.get(i));
		}
		return result;
	}
}
