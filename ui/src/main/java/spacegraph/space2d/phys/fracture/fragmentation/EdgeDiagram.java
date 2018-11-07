package spacegraph.space2d.phys.fracture.fragmentation;

import spacegraph.space2d.phys.fracture.Fragment;
import spacegraph.util.math.v2;

/**
 * Hrana Voronoi diagramu.
 *
 * @author Marek Benovic
 */
class EdgeDiagram extends AEdge {
    public EdgeDiagram(v2 v1, v2 v2) {
        super(v1, v2);
    }

    /**
     * Fragmenty, ktore ohranicuje dana hrana.
     */
    public Fragment d1, d2;
}
