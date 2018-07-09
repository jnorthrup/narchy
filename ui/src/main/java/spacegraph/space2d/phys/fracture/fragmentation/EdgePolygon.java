package spacegraph.space2d.phys.fracture.fragmentation;

import spacegraph.space2d.phys.fracture.util.MyList;
import spacegraph.util.math.Tuple2f;

import java.util.List;

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
    public final List<Vec2Intersect> list = new MyList<>();
}
