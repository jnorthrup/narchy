package spacegraph.space2d.phys.fracture.materials;

import jcog.random.XoRoShiRo128PlusRandom;
import spacegraph.space2d.phys.fracture.Material;
import spacegraph.util.math.Tuple2f;
import spacegraph.util.math.v2;

import java.util.Random;

/**
 * Material, kde ohniska fragmentov su generovane rovnomerne nahodne v priestore.
 *
 * @author Marek Benovic
 */
public class Uniform extends Material {
    private static final Random r = new XoRoShiRo128PlusRandom(1);

    @Override
    public Tuple2f[] focee(Tuple2f point, Tuple2f velocity) {
        int num = 32;
        Tuple2f[] focee = new Tuple2f[num];

        float scale = 2 * m_shattering;

        for (int i = 0; i < num; ++i) {
            float x = r.nextFloat() - 0.5f; 
            float y = r.nextFloat() - 0.5f; 

            focee[i] = new v2(point.x + x * scale, point.y + y * scale);
        }
        return focee;
    }

    @Override
    public String toString() {
        return "Uniform diffusion";
    }
}