package org.jbox2d.fracture.fragmentation;

import org.jbox2d.fracture.Fragment;
import spacegraph.math.Tuple2f;

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
