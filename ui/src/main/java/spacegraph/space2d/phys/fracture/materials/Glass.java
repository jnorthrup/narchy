package spacegraph.space2d.phys.fracture.materials;

import jcog.math.random.XoRoShiRo128PlusRandom;
import spacegraph.space2d.phys.common.Transform;
import spacegraph.space2d.phys.fracture.Material;
import spacegraph.util.math.Tuple2f;
import spacegraph.util.math.v2;

import java.util.Random;

/**
 * Material simulujuci sklo. Ohniska su generovane nahodne v n-kruzniciach s
 * malou odchylkou.
 *
 * @author Marek Benovic
 */
public class Glass extends Material {
    private final Random r = new XoRoShiRo128PlusRandom(1);

    /**
     * Pocet prvkov kruznice.
     */
    private final int levels = 4;

    /**
     * Pocet prvkov v jednej kruznici.
     */
    private final int count = 30;

    /**
     * Konstruktor inicializujuci sklo.
     */
    public Glass() {
        m_shattering = 1.0f;
        m_radius = 4f;
    }

    @Override
    public Tuple2f[] focee(Tuple2f startPoint, Tuple2f vektor) {
        Transform t = new Transform();
        t.set(startPoint, 0);

        int allCount = count * levels;

        Tuple2f[] va = new Tuple2f[allCount];
        for (int l = 0; l < levels; l++) {
            for (int c = 0; c < count; c++) {
                int i = l * count + c;

                double u = r.nextDouble() * Math.PI * 2; 
                double deficit = (r.nextDouble() - 0.5) * m_shattering / 20;
                double r = (l + 1) * m_shattering + deficit;

                double x = Math.sin(u) * r;
                double y = Math.cos(u) * r;

                Tuple2f v = new v2((float) x, (float) y);

                va[i] = Transform.mul(t, v);
            }
        }

        return va;
    }

    @Override
    public String toString() {
        return "Glass";
    }
}