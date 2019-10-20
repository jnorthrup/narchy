package spacegraph.util.math;

import jcog.data.bit.MetalBitSet;
import jcog.data.list.FasterList;
import jcog.math.v2;

import java.security.InvalidParameterException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Simplify2D {
	
	

	
	
	
	
	
	
	public static FasterList<v2> collinearSimplify(FasterList<v2> vertices, float collinearityTolerance) {

		var n = vertices.size();
		if (n < 3) return vertices;
		var simplified = new FasterList<v2>();
		for (var i = 0; i < n; i++) {
			var prevId = i - 1;
			if (prevId < 0) prevId = n - 1;
			var nextId = i + 1;
			if (nextId >= n) nextId = 0;
			var prev = vertices.get(prevId);
			var current = vertices.get(i);
			var next = vertices.get(nextId);
			
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

		var n = vertices.size();
		var usePt = MetalBitSet.bits(n);
		usePt.setAll();
		simplifySection(usePt, distanceTolerance, vertices, 0, n - 1);
		var result = IntStream.range(0, n).filter(usePt::get).mapToObj(vertices::get).collect(Collectors.toCollection(FasterList::new));
		return result;
	}

	private static void simplifySection(MetalBitSet usePt, float _distanceTolerance, FasterList<v2> vertices, int i, int j) {
		if ((i + 1) == j) return;
		var A = vertices.get(i);
		var B = vertices.get(j);
		var maxDistance = -1.0;
		var maxIndex = i;
		for (var k = i + 1; k < j; k++) {
			var distance = distancePointLine(vertices.get(k), A, B);
			if (distance > maxDistance) {
				maxDistance = distance;
				maxIndex = k;
			}
		}
		if (maxDistance <= _distanceTolerance)
			for (var k = i + 1; k < j; k++)
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
		var v1 = vertices.get(vertices.size() - 2);
		var v2 = vertices.get(vertices.size() - 1);
		areaTolerance *= 2;
        v2 v3;
		var result = new FasterList<jcog.math.v2>();
        for (var index = 0; index < vertices.size(); ++index, v2 = v3) {
			if (index == vertices.size() - 1) {
				if (result.isEmpty()) {
					throw new InvalidParameterException("areaTolerance: The tolerance is too high!");
				}
				v3 = result.get(0);
			} else {
				v3 = vertices.get(index);
			}
            float old1 = cross(v1, v2);
            float old2 = cross(v2, v3);
            float new1 = cross(v1, v3);
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

		var mergeMe = new boolean[vertices.size()];
		var newNVertices = vertices.size();
		
		for (var i = 0; i < vertices.size(); ++i) {
			var lower = (i == 0) ? (vertices.size() - 1) : (i - 1);
			var middle = i;
			var upper = (i == vertices.size() - 1) ? (0) : (i + 1);
			var dx0 = vertices.get(middle).x - vertices.get(lower).x;
			var dy0 = vertices.get(middle).y - vertices.get(lower).y;
			var dx1 = vertices.get(upper).y - vertices.get(middle).x;
			var dy1 = vertices.get(upper).y - vertices.get(middle).y;
			var norm0 = (float) Math.sqrt(dx0 * dx0 + dy0 * dy0);
			var norm1 = (float) Math.sqrt(dx1 * dx1 + dy1 * dy1);
			if (!(norm0 > 0.0f && norm1 > 0.0f) && newNVertices > 3) {
				
				mergeMe[i] = true;
				--newNVertices;
			}
			dx0 /= norm0;
			dy0 /= norm0;
			dx1 /= norm1;
			dy1 /= norm1;
			var cross = dx0 * dy1 - dx1 * dy0;
			var dot = dx0 * dx1 + dy0 * dy1;
			if (Math.abs(cross) < tolerance && dot > 0 && newNVertices > 3) {
				mergeMe[i] = true;
				--newNVertices;
			} else
				mergeMe[i] = false;
		}
		if (newNVertices == vertices.size() || newNVertices == 0) return;

		var oldVertices = new FasterList<v2>(vertices);
		vertices.clear();
		var currIndex = 0;
        for (var i = 0; i < oldVertices.size(); ++i) {
			if (mergeMe[i] || currIndex == newNVertices) continue;
			
			vertices.add(oldVertices.get(i));
			++currIndex;
		}
	}

	
	
	
	
	
	
	public static FasterList<v2> mergeIdenticalPoints(FasterList<v2> vertices) {
		var results = new FasterList<v2>();
        for (var vOriginal : vertices) {
			var alreadyExists = results.stream().anyMatch(vOriginal::equals);
			if (!alreadyExists) results.add(vOriginal);
        }
		return results;
	}

	
	
	
	
	
	
	
	public static FasterList<v2> reduceByDistance(FasterList<v2> vertices, float distance) {
		
		if (vertices.size() < 3) return vertices;
		var distSq = distance * distance;
		var simplified = new FasterList<v2>();
		for (var i = 0; i < vertices.size(); i++) {
			var current = vertices.get(i);
			var ii = i + 1;
			if (ii >= vertices.size()) ii = 0;
			var next = vertices.get(ii);
			var diff = new v2(next.x - current.x, next.y - current.y);
			
			if (diff.lengthSquared() <= distSq) continue;
			simplified.add(current);
		}
		return simplified;
	}

	
	
	
	
	
	
	public static FasterList<v2> reduceByNth(FasterList<v2> vertices, int nth) {
		
		if (vertices.size() < 3) return vertices;
		if (nth == 0) return vertices;
		var result = new FasterList<v2>(vertices.size());
		for (var i = 0; i < vertices.size(); i++) {
			if (i % nth == 0) continue;
			result.add(vertices.get(i));
		}
		return result;
	}
}
