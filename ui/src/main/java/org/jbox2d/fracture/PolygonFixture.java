package org.jbox2d.fracture;

import org.jbox2d.dynamics.Fixture;
import org.jbox2d.fracture.util.MyList;
import spacegraph.math.Tuple2f;

/**
 * Sluzi na aplikovanie konkavnych polygonov do jbox2d enginu. Fixtury telesa,
 * ktore su produktom konvexnej dekompozicie, maju v sebe ulozenu referenciu na
 * PolygonFixture instanciu, ktory reprezentuje povodne konkavne teleso.
 * PolygonFixture zaroven obsahuje List referencii na jeho jednotlive konvexne
 * fixtures.
 *
 * @author Marek Benovic
 */
public class PolygonFixture extends Polygon {
    /**
     * Inicializuje inicializujuci prazdny polygon.
     */
    public PolygonFixture() {
        super();
    }

    /**
     * Inicializuje polygon s mnozinou vrcholov z pamatera (referencne zavisle).
     *
     * @param v
     */
    public PolygonFixture(Tuple2f[] v) {
        super(v);
    }

    /**
     * Kopirovaci konstruktor. Inicializuje jednoduchym kopirovanim referencie.
     *
     * @param pg
     */
    PolygonFixture(Polygon pg) {
        array = pg.getArray();
        count = pg.size();
    }

    /**
     * Mnozina fixture, z ktorych dany polygon pozostava (reprezentuju konvexnu
     * dekompoziciu).
     */
    public final MyList<Fixture> fixtureList = new MyList<>();
}
