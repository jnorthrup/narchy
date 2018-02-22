package org.jbox2d.fracture.materials;

import jcog.math.random.XoRoShiRo128PlusRandom;
import org.jbox2d.fracture.Material;
import spacegraph.math.Tuple2f;
import spacegraph.math.v2;

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
            float x = r.nextFloat() - 0.5f; //rad: -1/2..+1/2
            float y = r.nextFloat() - 0.5f; //rad: -1/2..+1/2

            focee[i] = new v2(point.x + x * scale, point.y + y * scale);
        }
        return focee;
    }

    @Override
    public String toString() {
        return "Uniform diffusion";
    }
}