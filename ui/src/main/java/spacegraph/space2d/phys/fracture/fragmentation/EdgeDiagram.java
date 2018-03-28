package spacegraph.space2d.phys.fracture.fragmentation;

import spacegraph.space2d.phys.fracture.Fragment;
import spacegraph.util.math.Tuple2f;

/**
 * Hrana Voronoi diagramu.
 *
 * @author Marek Benovic
 */
class EdgeDiagram extends AEdge {
    public EdgeDiagram(Tuple2f v1, Tuple2f v2) {
        super(v1, v2);
    }

    /**
     * Fragmenty, ktore ohranicuje dana hrana.
     */
    public Fragment d1, d2;
}
