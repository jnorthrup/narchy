package spacegraph.space2d.phys.fracture;

import jcog.math.v2;
import spacegraph.space2d.phys.common.PlatformMathUtils;

import java.util.Arrays;

/**
 * Polygon pre voronoi diagram. Funguje ako ArrayList 2D bodov typu Point2D,
 * ktory potom zotriedim podla ohniska na konvexny polygon. V pripade potreby si
 * moze uzivatel dodefinovat dalsie funkcie - oddedit od polygonu a nadefinovat
 * si funkcie na vypocet obsahu, alebo taziska. Jedna sa o specificky pripad polygonu.
 * Reprezentuje uz konkretny ulomok povodneho telesa.
 *
 * @author Marek Benovic
 * @version 1.0
 */
public class Fragment extends Polygon {
    /**
     * Inicializuje prazdny fragment
     */
    public Fragment() {
    }
    public Fragment(int sides) {
        super(sides);
    }










    /**
     * Ohnisko fragmentu
     */
    public v2 focus;

    /**
     * Pomocna premenna pre vypocet do geometry kniznice.
     */
    public boolean visited;

    /**
     * Zotriedi konvexny polygon podla bodu focus na zaklade uhlov jednotlivych
     * vrcholov
     */
    public void resort() {
        var size = size();
        var comparer = new double[10];
        var count = 0;
        for (var i1 = 0; i1 < size; i1++) {
            var angle = PlatformMathUtils.angle(get(i1), focus);
            if (comparer.length == count) comparer = Arrays.copyOf(comparer, count * 2);
            comparer[count++] = angle;
        }
        comparer = Arrays.copyOfRange(comparer, 0, count);
        for (var i = 0; i != size; ++i) {
            var maxIndex = i;
            for (var j = i + 1; j != size; ++j) {
                if (comparer[j] < comparer[maxIndex]) {
                    maxIndex = j;
                }
            }
            var swap = comparer[i];
            comparer[i] = comparer[maxIndex];
            comparer[maxIndex] = swap;
            swap(i, maxIndex);
        }
    }
















    /**
     * Vymeni 2 vrcholy polygonu
     *
     * @param i
     * @param j
     */
    private void swap(int i, int j) {
        var a = this.vertices;
        var item = a[i];
        a[i] = a[j];
        a[j] = item;
    }
}
