package org.jbox2d.fracture.fragmentation;

import org.jbox2d.fracture.util.MyList;
import spacegraph.math.Tuple2f;

/**
 * Hrana obecneho polygonu.
 *
 * @author Marek Benovic
 */
class EdgePolygon extends AEdge {
    public EdgePolygon(Tuple2f v1, Tuple2f v2) {
        super(v1, v2);
    }

    /**
     * List prienikovych bodov, ktore sa nachadzaju na danej hrane.
     */
    public final MyList<Vec2Intersect> list = new MyList<>();
}
