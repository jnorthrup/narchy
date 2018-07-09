package spacegraph.space2d.phys.fracture.fragmentation;

import spacegraph.util.math.Tuple2f;
import spacegraph.util.math.v2;

import static spacegraph.space2d.phys.common.Vec2.cross;

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
    Tuple2f p1, p2;

    /**
     * Inicializuje vrcholy hrany
     *
     * @param p1
     * @param p2
     */
    AEdge(Tuple2f p1, Tuple2f p2) {
        this.p1 = p1;
        this.p2 = p2;
    }

    /**
     * @param a
     * @param b
     * @return Vektorovy sucin
     */
    private static double dCross(Tuple2f a, Tuple2f b) {
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
        Tuple2f U = a.p2.sub(a.p1);
        Tuple2f V = b.p2.sub(b.p1);
        Tuple2f A = new v2(a.p1);
        Tuple2f C = new v2(b.p1);
        double uv = dCross(U, V); 
        if (uv == 0) {
            return null; 
        }
        double k = (dCross(C, V) - dCross(A, V)) / uv;
        double o = (dCross(C, U) - dCross(A, U)) / uv;
        if (o > 0 && o < 1 && k > 0 && k < 1) {
            double ux = U.x * k + A.x;
            double uy = U.y * k + A.y;
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
    public boolean intersectAre(Tuple2f b1, Tuple2f b2) {
        Tuple2f U = p2.sub(p1);
        Tuple2f V = b2.sub(b1);
        Tuple2f A = new v2(p1);
        Tuple2f C = new v2(b1);
        float uv = cross(U, V);
        if (uv == 0)
            return false; 

        float k = (cross(C, V) - cross(A, V)) / uv;
        float o = (cross(C, U) - cross(A, U)) / uv;
        return o > 0 && o < 1 && k > 0 && k < 1;
    }

    /**
     * @param point
     * @return Vrati najvlizsi bod na priamke voci bodu z parametra.
     */
    public Tuple2f kolmicovyBod(Tuple2f point) {
        Tuple2f U = p2.sub(p1);
        Tuple2f V = new v2(p1.y - p2.y, p2.x - p1.x);
        float uv = cross(U, V);
        if (uv == 0) {
            return null; 
        }
        float k = (cross(point, V) - cross(p1, V)) / uv;
        if (k >= 0 && k <= 1) {
            U.scaled(k);
            return p1.add(U);
        } else {
            double dist1 = (double) p1.distanceSq(point);
            double dist2 = (double) p2.distanceSq(point);
            return dist1 < dist2 ? p1 : p2;
        }
    }

    @Override
    public boolean equals(Object o) {
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
