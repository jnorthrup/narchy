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
        double ax = (double) a.x;
        double ay = (double) a.y;
        double bx = (double) b.x;
        double by = (double) b.y;
        return ax * by - bx * ay;
    }

    /**
     * @param a
     * @param b
     * @return Vrati prienik 2 hran. Pokial neexistuje, vrati null.
     */
    public static Vec2Intersect intersect(AEdge a, AEdge b) {
        v2 U = a.p2.subClone(a.p1);
        v2 V = b.p2.subClone(b.p1);
        v2 A = new v2(a.p1);
        v2 C = new v2(b.p1);
        double uv = dCross(U, V);
        if (uv == (double) 0) {
            return null; 
        }
        double k = (dCross(C, V) - dCross(A, V)) / uv;
        double o = (dCross(C, U) - dCross(A, U)) / uv;
        if (o > (double) 0 && o < 1.0 && k > (double) 0 && k < 1.0) {
            double ux = (double) U.x * k + (double) A.x;
            double uy = (double) U.y * k + (double) A.y;
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
        v2 U = p2.subClone(p1);
        v2 V = b2.subClone(b1);
        v2 A = new v2(p1);
        v2 C = new v2(b1);
        float uv = cross(U, V);
        if (uv == (float) 0)
            return false;

        float k = (cross(C, V) - cross(A, V)) / uv;
        float o = (cross(C, U) - cross(A, U)) / uv;
        return o > (float) 0 && o < 1.0F && k > (float) 0 && k < 1.0F;
    }

    /**
     * @param point
     * @return Vrati najvlizsi bod na priamke voci bodu z parametra.
     */
    public v2 kolmicovyBod(v2 point) {
        v2 U = p2.subClone(p1);
        v2 V = new v2(p1.y - p2.y, p2.x - p1.x);
        float uv = cross(U, V);
        if (uv == (float) 0) {
            return null; 
        }
        float k = (cross(point, V) - cross(p1, V)) / uv;
        if (k >= (float) 0 && k <= 1.0F) {
            U.scaled(k);
            return p1.addToNew(U);
        } else {
            double dist1 = (double) p1.distanceSq(point);
            double dist2 = (double) p2.distanceSq(point);
            return dist1 < dist2 ? p1 : p2;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof AEdge) {
            AEdge d = (AEdge) o;
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
