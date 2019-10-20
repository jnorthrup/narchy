package spacegraph.space2d.phys.fracture.fragmentation;

import jcog.math.v2;

import static jcog.math.v2.cross;


/**
 * Hrana polygonu voronoi diagramu - sluzi na spracovanie prienikov polygonu
 * a voronoi rozdelenia.
 *
 * @author Marek Benovic
 */
abstract class AEdge {
    /**
     * Vrchol hrany.
     */
    v2 p1;
    v2 p2;

    /**
     * Inicializuje vrcholy hrany
     *
     * @param p1
     * @param p2
     */
    AEdge(v2 p1, v2 p2) {
        this.p1 = p1;
        this.p2 = p2;
    }

    /**
     * @param a
     * @param b
     * @return Vektorovy sucin
     */
    private static double dCross(v2 a, v2 b) {
        double ax = a.x;
        double ay = a.y;
        double bx = b.x;
        double by = b.y;
        return ax * by - bx * ay;
    }

    /**
     * @param a
     * @param b
     * @return Vrati prienik 2 hran. Pokial neexistuje, vrati null.
     */
    public static Vec2Intersect intersect(AEdge a, AEdge b) {
        var U = a.p2.subClone(a.p1);
        var V = b.p2.subClone(b.p1);
        var A = new v2(a.p1);
        var C = new v2(b.p1);
        var uv = dCross(U, V);
        if (uv == 0) {
            return null; 
        }
        var k = (dCross(C, V) - dCross(A, V)) / uv;
        var o = (dCross(C, U) - dCross(A, U)) / uv;
        if (o > 0 && o < 1 && k > 0 && k < 1) {
            var ux = U.x * k + A.x;
            var uy = U.y * k + A.y;
            A.set((float) ux, (float) uy);
            return new Vec2Intersect(A, k);
        } else {
            return null;
        }
    }

    /**
     * @param b1
     * @param b2
     * @return Vrati true, ak sa hrany pretinaju.
     */
    public boolean intersectAre(v2 b1, v2 b2) {
        var U = p2.subClone(p1);
        var V = b2.subClone(b1);
        var A = new v2(p1);
        var C = new v2(b1);
        var uv = cross(U, V);
        if (uv == 0)
            return false;

        var k = (cross(C, V) - cross(A, V)) / uv;
        var o = (cross(C, U) - cross(A, U)) / uv;
        return o > 0 && o < 1 && k > 0 && k < 1;
    }

    /**
     * @param point
     * @return Vrati najvlizsi bod na priamke voci bodu z parametra.
     */
    public v2 kolmicovyBod(v2 point) {
        var U = p2.subClone(p1);
        var V = new v2(p1.y - p2.y, p2.x - p1.x);
        var uv = cross(U, V);
        if (uv == 0) {
            return null; 
        }
        var k = (cross(point, V) - cross(p1, V)) / uv;
        if (k >= 0 && k <= 1) {
            U.scaled(k);
            return p1.addToNew(U);
        } else {
            double dist1 = p1.distanceSq(point);
            double dist2 = p2.distanceSq(point);
            return dist1 < dist2 ? p1 : p2;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof AEdge) {
            var d = (AEdge) o;
            return (d.p1 == p1 && d.p2 == p2) || (d.p1 == p2 && d.p2 == p1);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return p1.hashCode() ^ p2.hashCode();
    }

    @Override
    public String toString() {
        return "[" + p1 + "]-[" + p2 + ']';
    }
}
