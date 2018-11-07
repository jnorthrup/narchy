package spacegraph.space2d.phys.fracture.fragmentation;

import spacegraph.space2d.phys.fracture.Polygon;
import spacegraph.util.math.v2;

/**
 * Vrchol rovinneho grafu. Sluzi na zjednocovanie fragmentov polygonu.
 *
 * @author Marek Benovic
 */
class GraphVertex {
    /**
     * Vrchol v grafe
     */
    public final v2 value;

    /**
     * Pocet polygonov, ktorych sucastou je dany vrchol
     */
    public int polygonCount;

    /**
     * Mnohosteny, ktorych je dana hrana sucastou.
     */
    public Polygon first, second;

    /**
     * Susedny vrchol cesty ohranicenia.
     */
    public GraphVertex next, prev;

    /**
     * Pomocna premenna sluziaca na vypocet.
     */
    boolean visited = false;

    /**
     * Inicializuje vrchol
     *
     * @param value
     */
    public GraphVertex(v2 value) {
        this.value = value;
        this.polygonCount = 1;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof v2) {
            v2 o2 = (v2) o;
            return value == o2;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}