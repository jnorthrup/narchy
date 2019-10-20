package spacegraph.space2d.phys.fracture.hertelmehlhorn;

import jcog.math.v2;
import spacegraph.space2d.phys.common.PlatformMathUtils;
import spacegraph.space2d.phys.fracture.Polygon;

import java.util.Arrays;

/**
 * Objekt implementujuci Hertel-Mehlhornov algoritmus. Sluzi na vypocet
 * konvexnej dekompozicie nekonvexnych polygonov zjednocovanim trojuholnikov
 * delaynay triangulacie.
 *
 * @author Marek Benovic
 */
public class SingletonHM {
    /**
     * Mnozina vyslednych fragmentov dekompozicie.
     */
    public Polygon[] dekomposition;

    private v2[] vertices;
    private int maxVerticesCount;

    private EdgeTable table;
    private NodeHM[] polygons; 
    private int count;
    private int[] polygonsVCount;

    /**
     * Inicializacia singletonu.
     */
    public SingletonHM() {
    }

    /**
     * @param list      List trojuholnikov (hodnoty su indexy vrcholov z pola vertices)
     * @param vertices  Pouzite vrcholy
     * @param maxVCount Maximalny pocet vrcholov v konvexnych utvaroch.
     *                  Zjednoti vstupnu triangulaciu do mnoziny konvexnych polygonov.
     */
    public void calculate(int[][] list, v2[] vertices, int maxVCount) {
        tableInit(list, vertices, maxVCount);
        run();
    }

    /**
     * Nastavi hashovaciu tabulku hran (len hrany, ktore maju oba trojuholniky)
     * Nastavi Nodexy a vsetky hrany
     */
    private void tableInit(int[][] list, v2[] vertices, int maxVerticesCount) {
        this.maxVerticesCount = maxVerticesCount;
        this.vertices = vertices;

        for (var ar : list) {
            var a = vertices[ar[0]];
            var b = vertices[ar[1]];
            var c = vertices[ar[2]];
            if (PlatformMathUtils.site(a, b, c) == 1) {
                var k = ar[1];
                ar[1] = ar[2];
                ar[2] = k;
            }
        }

        count = list.length;
        polygons = new NodeHM[count];
        table = new EdgeTable();
        polygonsVCount = new int[count];
        Arrays.fill(polygonsVCount, 3);

        for (var i = 0; i < list.length; ++i) {
            var tr = list[i];
            var i1 = tr[0];
            var i2 = tr[1];
            var i3 = tr[2];

            var a = new NodeHM(i1);
            var b = new NodeHM(i2);
            var c = new NodeHM(i3);

            var e = table.get(i1, i2);
            if (e == null) {
                e = new Diagonal();
                e.add(i, a, b);
                table.add(e);
            } else {
                e.add(i, a, b);
            }

            e = table.get(i2, i3);
            if (e == null) {
                e = new Diagonal();
                e.add(i, b, c);
                table.add(e);
            } else {
                e.add(i, b, c);
            }

            e = table.get(i3, i1);
            if (e == null) {
                e = new Diagonal();
                e.add(i, c, a);
                table.add(e);
            } else {
                e.add(i, c, a);
            }

            a.next = b;
            a.prev = c;
            b.next = c;
            b.prev = a;
            c.next = a;
            c.prev = b;

            polygons[i] = a; 
        }

        var edges = table.toArray(new Diagonal[0]);
        for (var e : edges) {
            if (e.i2 == -1) {
                table.remove(e);
            }
        }
    }

    /**
     * Samotny Hertel Mehlhornov algoritmus na predspracovanych datach.
     *
     * @return Vrati mnozinu disjunktnych konvexnych polygonov, ktorych zjednotenie
     * tvori povodny utvar.
     */
    private void run() {
        
        for (var i = 0; i < polygons.length; ++i) {
            var n1 = polygons[i];
            if (n1 != null) {
                var n2 = n1.next;
                var n3 = n1.prev;
                rekursion(n1, n2, i);
                rekursion(n2, n3, i);
                rekursion(n3, n1, i);
            }
        }
        

        dekomposition = new Polygon[count];

        var i = 0;
        for (var x : polygons) {
            if (x != null) {
                var it = x;
                var p = new Polygon();
                do {
                    p.add(vertices[it.index]);
                    it = it.next;
                } while (it != x);
                dekomposition[i++] = p;
            }
        }
    }

    /**
     * Rekurzivna funkcia, ktora sa zavola na trojuholniku a rekurzivne prechadza
     * na vsetkych susedov (trojuholniky), ktory neboli este spracovani a pripaja
     * ich ku konvexnemu polygonu.
     */
    private void rekursion(NodeHM n1, NodeHM n2, int triangle) {
        var i1 = n1.index;
        var i2 = n2.index;

        var e = table.get(i1, i2);
        if (e != null) { 
            if (polygonsVCount[triangle] < maxVerticesCount) {
                var opposite = (e.i1 == triangle ? e.n22 : e.n12).next;
                var i3 = opposite.index;

                if (
                        PlatformMathUtils.siteDef(vertices[i3], vertices[i2], vertices[n2.next.index]) < 1 &&
                                PlatformMathUtils.siteDef(vertices[i3], vertices[i1], vertices[n1.prev.index]) > -1
                        ) {

                    var oppositeTriangleIndex = e.i1 == triangle ? e.i2 : e.i1;
                    polygons[oppositeTriangleIndex] = null;
                    count--;
                    polygonsVCount[triangle]++;

                    opposite.next = n2;
                    opposite.prev = n1;
                    n1.next = n2.prev = opposite;

                    var right = table.get(i3, i2);
                    if (right != null) {
                        if (right.i1 == oppositeTriangleIndex) {
                            right.i1 = triangle;
                            right.n12 = n2;
                        } else {
                            right.i2 = triangle;
                            right.n22 = n2;
                        }
                        rekursion(opposite, n2, triangle);
                    }
                    var left = table.get(i3, i1);
                    if (left != null) {
                        if (left.i1 == oppositeTriangleIndex) {
                            left.i1 = triangle;
                            left.n11 = n1;
                        } else {
                            left.i2 = triangle;
                            left.n21 = n1;
                        }
                        rekursion(n1, opposite, triangle);
                    }
                }
            }

            table.remove(e); 
        }
    }
}