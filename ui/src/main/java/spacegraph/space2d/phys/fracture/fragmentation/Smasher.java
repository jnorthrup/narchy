package spacegraph.space2d.phys.fracture.fragmentation;

import jcog.data.list.FasterList;
import jcog.math.v2;
import spacegraph.space2d.phys.callbacks.ContactImpulse;
import spacegraph.space2d.phys.collision.WorldManifold;
import spacegraph.space2d.phys.common.PlatformMathUtils;
import spacegraph.space2d.phys.dynamics.Dynamics2D;
import spacegraph.space2d.phys.dynamics.Fixture;
import spacegraph.space2d.phys.dynamics.contacts.Contact;
import spacegraph.space2d.phys.fracture.Fracture;
import spacegraph.space2d.phys.fracture.Fragment;
import spacegraph.space2d.phys.fracture.Material;
import spacegraph.space2d.phys.fracture.Polygon;
import spacegraph.space2d.phys.fracture.util.HashTabulka;
import spacegraph.space2d.phys.fracture.util.MyList;
import spacegraph.space2d.phys.fracture.voronoi.SingletonVD;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static spacegraph.space2d.phys.dynamics.BodyType.DYNAMIC;

/**
 * Hlavny objekt, ktory robi prienik voronoi diagramu generovany ohniskami
 * a polygonu predanom v parametri.
 *
 * @author Marek Benovic
 */
public class Smasher {

    private final SingletonVD factory = new SingletonVD();

    /**
     * Mnozina vyslednych fragmentov.
     */
    public Polygon[] fragments;

    private final HashTabulka<Fracture> fractures = new HashTabulka<>();

    private v2[] focee;
    private Polygon p;

    private float[] constant;
    private float[] multiple;

    private final HashTabulka<EdgeDiagram> table = new HashTabulka<>();


    /**
     * Vrati prienik voronoi diagramu a polygonu.
     *
     * @param focee
     * @param p            Kopia polygonu, moze byt modifikovana
     * @param contactPoint Bod dotyku
     * @param ic           Funkcionalny interface, ktory definuje, ci fragment patri,
     *                     alebo nepatri do mnoziny ulomkov
     */
    public void calculate(Polygon p, v2[] focee, v2 contactPoint, IContains ic) {
        this.focee = focee;
        this.p = p;

        var list = getVoronoi();

        List<EdgePolygon> polygonEdgesList = new FasterList<>();
        var polygonEdges = new HashTabulka<EdgePolygon>();


        var count = p.size();
        for (var i = 1; i <= count; i++) {
            var p1 = p.get(i - 1);
            var p2 = p.get(i == count ? 0 : i);
            var e = new EdgePolygon(p1, p2);
            polygonEdges.add(e);
            polygonEdgesList.add(e);
        }


        var diagramEdges = new HashTabulka<EdgeDiagram>();
        for (var pp : list) {
            count = pp.size();
            for (var i = 1; i <= count; i++) {
                var p1 = pp.get(i - 1);
                var p2 = pp.get(i == count ? 0 : i);

                var e = new EdgeDiagram(p1, p2);
                var alternative = diagramEdges.get(e);
                if (alternative == null) {
                    diagramEdges.add(e);
                    e.d1 = pp;
                } else {
                    alternative.d2 = pp;
                }
            }
        }

        AEdge[][] allEdges = {
                diagramEdges.toArray(new AEdge[0]),
                polygonEdges.toArray(new AEdge[0])
        };

        diagramEdges.clear();
        polygonEdges.clear();

        List<EVec2> vectorList = new FasterList<>();

        for (var array : allEdges) {
            for (var e : array) {
                var v1 = new EVec2(e.p1);
                var v2 = new EVec2(e.p2);
                v1.e = e;
                v2.e = e;
                if (v1.p.y < v2.p.y) {
                    v1.start = true;
                } else {
                    v2.start = true;
                }
                vectorList.add(v1);
                vectorList.add(v2);
            }
        }

        var vectors = vectorList.toArray(new EVec2[0]);

        Arrays.sort(vectors);


        for (var e : vectors) {
            if (e.e instanceof EdgeDiagram) {
                if (e.start) {
                    var ex = (EdgeDiagram) e.e;
                    diagramEdges.add(ex);


                    for (var px : polygonEdges) {
                        process(px, ex);
                    }

                } else {
                    diagramEdges.remove(e.e);
                }
            } else {
                if (e.start) {
                    var px = (EdgePolygon) e.e;
                    polygonEdges.add(px);

                    for (var ex : diagramEdges) {
                        process(px, ex);
                    }


                } else {
                    polygonEdges.remove(e.e);
                }
            }
        }

        for (var pol : list) {
            pol.resort();
            var pn = pol.size();
            for (var i = 0; i < pn; i++) {
                var v = pol.get(i);
                if (v instanceof Vec2Intersect) {
                    var vi = (Vec2Intersect) v;
                    if (vi.p1 == pol) {
                        vi.i1 = i;
                    } else {
                        vi.i2 = i;
                    }
                }
            }
        }

        var polygonAll = new Polygon();


        for (var ex : polygonEdgesList) {
            polygonAll.add(ex.p1);
            ex.list.sort(c);
            polygonAll.add(ex.list);
        }

        for (var i = 0; i < polygonAll.size(); i++) {
            var v = polygonAll.get(i);
            if (v instanceof Vec2Intersect) {
                ((Vec2Intersect) v).index = i;
            }
        }


        precalc_values();

        var allIntersections = new MyList<Fragment>();
        for (var ppp : list) {
            var intsc = getIntersections(ppp, polygonAll);
            if (intsc == null) {
                fragments = new Polygon[]{p};
                return;
            }
            allIntersections.addAll(intsc);
        }

        table.clear();


        for (var f : allIntersections) {
            for (var i = 0; i < f.size(); ++i) {
                var v1 = f.get(i);
                var v2 = f.cycleGet(i + 1);
                var e = new EdgeDiagram(v1, v2);
                var e2 = table.get(e);
                if (e2 != null) {
                    e = e2;
                    e.d2 = f;
                } else {
                    e.d1 = f;
                    table.add(e);
                }
            }
        }


        double[] distance = {Double.MAX_VALUE};
        Fragment[] startPolygon = {null};
        v2[] kolmicovyBod = {null};
        var allEdgesPolygon = new MyList<EdgeDiagram>();


        for (var edgeDiagram : table) {
            if (edgeDiagram.d2 == null) {
                var vv = edgeDiagram.kolmicovyBod(contactPoint);
                double newDistance = contactPoint.distanceSq(vv);
                if (newDistance <= distance[0]) {
                    distance[0] = newDistance;
                    kolmicovyBod[0] = vv;
                    startPolygon[0] = edgeDiagram.d1;
                }
                allEdgesPolygon.add(edgeDiagram);
            }
        }

        var ppx = new MyList<Fragment>();
        ppx.add(startPolygon[0]);
        var epx = new EdgeDiagram(null, null);
        startPolygon[0].visited = true;

        var vysledneFragmenty = new HashTabulka<Fragment>();
        while (!ppx.isEmpty()) {
            var px = ppx.get(0);
            vysledneFragmenty.add(px);

            for (var i = 0; i < px.size(); ++i) {
                var v1 = px.get(i);
                var v2 = px.cycleGet(i + 1);
                epx.p1 = v1;
                epx.p2 = v2;
                var ep = table.get(epx);
                var opposite = ep.d1 == px ? ep.d2 : ep.d1;

                if (opposite != null && !opposite.visited) {
                    var centroid = opposite.centroid();
                    opposite.visited = true;
                    if (ic.contains(centroid)) {
                        var intersection = allEdgesPolygon.stream().anyMatch(edge -> edge.d1 != startPolygon[0] && edge.d2 != startPolygon[0] && edge.intersectAre(centroid, kolmicovyBod[0]));


                        if (!intersection) {
                            ppx.add(opposite);
                        }
                    }
                }

            }

            ppx.removeAt(0);
        }

        var fragmentsArray = vysledneFragmenty.toArray(new Fragment[0]);
        var fragmentsBody = allIntersections.stream().filter(fx -> !vysledneFragmenty.contains(fx)).collect(Collectors.toCollection(MyList::new));

        var result = zjednotenie(fragmentsBody);

        result.add(fragmentsArray);
        fragments = new Polygon[result.size()];
        result.addToArray(fragments);
    }

    public void addFracture(Fracture fracture) {


        var f = fractures.get(fracture);
        if (f != null) {
            if (f.normalImpulse < fracture.normalImpulse) {
                fractures.remove(f);
                fractures.add(fracture);
            }
        } else {
            fractures.add(fracture);
        }
    }

    public boolean isFractured(Fixture fx) {
        return fractures.contains(fx);
    }

    /**
     * Detekuje, ci dany kontakt vytvara frakturu
     *
     * @param contact
     * @param impulse
     * @param w
     */
    public void init(Contact contact, ContactImpulse impulse) {
        var f1 = contact.aFixture;
        var f2 = contact.bFixture;

//        if (f1.getBody().getType()!=DYNAMIC && f2.getBody().getType()!=DYNAMIC) {
//            contact.setEnabled(false);
//            Arrays.fill(impulse.normalImpulses, 0);
//            Arrays.fill(impulse.tangentImpulses, 0);
//            return;
//        }

        var impulses = impulse.normalImpulses;
        for (var i = 0; i < impulse.count; ++i) {

            var iml = impulses[i];


            tryFracture(f1, f2, iml, contact, i);
            tryFracture(f2, f1, iml, contact, i);
        }
    }


    private void tryFracture(Fixture f1, Fixture f2, float iml, Contact contact, int i) {


        var m = f1.material;
        if (m != null && m.m_rigidity < iml) {
            f1.body.m_fractureTransformUpdate = f2.body.m_fractureTransformUpdate = false;
            if (f1.body.m_massArea >= Material.MASS_DESTRUCTABLE_MIN) {
                var wm = new WorldManifold();
                contact.getWorldManifold(wm);
                addFracture(new Fracture(f1, f2, m, contact, iml, new v2(wm.points[i])));
            } else if (f1.body.type != DYNAMIC) {
                addFracture(new Fracture(f1, f2, m, null, 0, null));
            }
        }


    }

    private static final Comparator<Vec2Intersect> c = (o1, o2) -> {
        var v1 = o1;
        var v2 = o2;
        return Double.compare(v1.k, v2.k);
    };

    /**
     * @param p1 Fragment 1
     * @param p2 Fragment 2
     * @return Vrati list polygonov, ktore su prienikmi polygonov z parametra.
     */
    private List<Fragment> getIntersections(Fragment p1, Polygon p2) {
        Vec2Intersect firstV = null;

        for (var v : p1) {
            if (v instanceof Vec2Intersect) {
                firstV = (Vec2Intersect) v;

                var p2Next = p2.cycleGet(firstV.index + 1);
                var p1Back = p1.cycleGet((firstV.p1 == p1 ? firstV.i1 : firstV.i2) + 1);

                if (PlatformMathUtils.siteDef(p1Back, firstV, p2Next) >= 0) {
                    break;
                }
            }
        }

        List<Fragment> polygonList = new ArrayList<>();
        if (firstV == null) {
            if (pointInPolygon(p1.get(0))) {
                polygonList.add(p1);
            } else if (p1.inside(p2.get(0))) {
                return null;
            }
            return polygonList;
        }

        v2 start = firstV;

        var iterationPolygon = p2;
        var index = firstV.index;

        var idemPoKonvexnom = false;
        cyklus:
        for (var exI = 0; ; ) {
            var prienik = new Fragment();
            v2 iterator;
            do {
                exI++;
                if (exI >= 10000) {
                    throw new RuntimeException();
                }

                iterator = iterationPolygon.cycleGet(++index);
                if (iterator instanceof Vec2Intersect) {
                    var intersect = (Vec2Intersect) iterator;
                    prienik.add(intersect.vec2);
                    intersect.visited = true;
                    idemPoKonvexnom = !idemPoKonvexnom;
                    iterationPolygon = idemPoKonvexnom ? p1 : p2;
                    index = idemPoKonvexnom ? intersect.p1 == p1 ? intersect.i1 : intersect.i2 : intersect.index;
                } else {
                    prienik.add(iterator);
                }
            } while (iterator != firstV);
            polygonList.add(prienik);

            iterationPolygon = p1;
            index = iterationPolygon == firstV.p1 ? firstV.i1 : firstV.i2;
            idemPoKonvexnom = true;
            for (; ; ) {
                iterator = iterationPolygon.cycleGet(++index);
                if (iterator == start) {
                    break cyklus;
                }
                if (iterator instanceof Vec2Intersect) {
                    firstV = (Vec2Intersect) iterator;
                    if (!firstV.visited) {
                        break;
                    }
                }

                exI++;
                if (exI >= 10000) {
                    throw new RuntimeException();
                }

            }
        }
        for (var v : p1) {
            if (v instanceof Vec2Intersect) {
                var vi = (Vec2Intersect) v;
                vi.visited = false;
            }
        }
        return polygonList;
    }

    /**
     * @return Vygeneruje list fragmentov voronoi diagramu na zaklade vstupnych
     * ohnisk z clenskej premennej focee.
     */
    private List<Fragment> getVoronoi() {
        var min = new v2(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        var max = new v2(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);

        for (var v : p) {
            min = v2.min(min, v);
            max = v2.max(max, v);
        }
        for (var v : focee) {
            min = v2.min(min, v);
            max = v2.max(max, v);
        }

        var deficit = new v2(1, 1);
        min.subbed(deficit);
        max.added(deficit);

        factory.calculateVoronoi(focee, min, max);

        List<Fragment> fragmentList = new FasterList<>(focee.length);

        var bound = factory.pCount;
        var pp = IntStream.range(0, bound).mapToObj(i1 -> new v2(factory.points[i1])).toArray(v2[]::new);

        for (var i = 0; i < focee.length; i++) {

            var n = factory.vCount[i];
            var ppx = factory.voronoi[i];

            var f = new Fragment(n);
            for (var j = 0; j < n; ++j) {
                f.add(pp[ppx[j]]);
            }
            f.focus = focee[i];
            fragmentList.add(f);
        }

        return fragmentList;
    }

    /**
     * Vezme polygony a vrati ich zjednotenie. Plygony su navzajom disjunknte
     * avsak dotykaju sa bodmi hranami, ktore maju referencnu zavislost.
     *
     * @param polygony
     * @return Vrati List zjednotenych polygonov.
     */
    private static MyList<Polygon> zjednotenie(MyList<Fragment> polygony) {
        var graf = new HashTabulka<GraphVertex>();
        for (Polygon p : polygony) {
            for (var i = 1; i <= p.size(); ++i) {
                var v = p.cycleGet(i);
                var vertex = graf.get(v);
                if (vertex == null) {
                    vertex = new GraphVertex(v);
                    graf.add(vertex);
                    vertex.first = p;
                } else {
                    vertex.polygonCount++;
                    vertex.second = p;
                }
            }
        }

        for (Polygon p : polygony) {
            for (var i = 0; i < p.size(); ++i) {
                var v1 = graf.get(p.get(i));
                var v2 = graf.get(p.cycleGet(i + 1));
                if (v1.polygonCount == 1 || v2.polygonCount == 1 || (v1.polygonCount <= 2 && v2.polygonCount <= 2 && !((v1.first == v2.first && v1.second == v2.second) || (v1.first == v2.second && v1.second == v2.first)))) {
                    v1.next = v2;
                    v2.prev = v1;
                }
            }
        }

        var vysledok = new MyList<Polygon>();

        var arr = graf.toArray(new GraphVertex[0]);
        for (var v : arr) {
            if (v.next != null && !v.visited) {
                var p = new Polygon();
                for (var iterator = v; !iterator.visited; iterator = iterator.next) {
                    if (PlatformMathUtils.siteDef(iterator.next.value, iterator.value, iterator.prev.value) != 0) {
                        p.add(iterator.value);
                    }
                    iterator.visited = true;
                }
                vysledok.add(p);
            }
        }

        return vysledok;
    }

    /**
     * Najde prienik 2 hran a spracuje vysledky.
     *
     * @param a Hrana polygonu
     * @param b Hrana vo voronoi diagrame
     */
    private static void process(EdgePolygon a, EdgeDiagram b) {
        var p = AEdge.intersect(a, b);
        if (p != null) {
            p.p1 = b.d1;
            p.p2 = b.d2;
            b.d1.add(p);
            b.d2.add(p);
            a.list.add(p);
        }
    }

    /**
     * Predpocita hodnoty pre zistovanie prieniku polygonu s bodmi
     */
    private void precalc_values() {
        var n = p.size();
        multiple = new float[n];
        constant = new float[n];
        var j = n - 1;
        for (var i = 0; i < n; i++) {
            var vi = p.get(i);
            var vj = p.get(j);
            multiple[i] = (vj.x - vi.x) / (vj.y - vi.y);
            constant[i] = vi.x - vi.y * multiple[i];
            j = i;
        }
    }

    /**
     * @param v
     * @return Vrati true, pokial sa vrchol nachadza v polygone. Treba mat
     * predpocitane hodnoty primarneho polygonu metodou precalc_values().
     */
    private boolean pointInPolygon(v2 v) {
        var x = v.x;
        var y = v.y;
        var n = p.size();
        var j = n - 1;
        var b = false;
        for (var i = 0; i < n; i++) {
            var vi = p.get(i);
            var vj = p.get(j);
            if ((vi.y < y && vj.y >= y || vj.y < y && vi.y >= y) && y * multiple[i] + constant[i] < x) {
                b = !b;
            }
            j = i;
        }
        return b;
    }

    public void update(Dynamics2D dyn, float dt) {
        fractures.forEach(f ->
            f.smash(this, dt, dyn)
        );
        fractures.clear();
    }
}