package org.jbox2d.fracture.fragmentation;

import spacegraph.math.Tuple2f;

/**
 * Objekt sluziaci na rychle hladanie prienikovych bodov voronoi diagramu a
 * polygonu. Reprezentuje zaciatocny/koncovy bod usecky - udalost algoritmu zametania.
 *
 * @author Marek Benovic
 */
class EVec2 implements Comparable<EVec2> {
    /**
     * Hrana
     */
    public AEdge e;

    /**
     * Bod hrany
     */
    public final Tuple2f p;

    /**
     * True, pokial je dany bod zaciatocny, false ak je koncovy.
     */
    public boolean start;

    /**
     * Inicializuje bod
     *
     * @param p
     */
    public EVec2(Tuple2f p) {
        this.p = p;
    }

    @Override
    public int compareTo(EVec2 o) {
        Tuple2f l = o.p;
        return p.y > l.y ? 1 : p.y == l.y ? (o.start == start ? 0 : (!start ? 1 : -1)) : -1;
    }
}
