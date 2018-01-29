package org.jbox2d.fracture.materials;

import jcog.math.random.XoRoShiRo128PlusRandom;
import org.jbox2d.common.Transform;
import org.jbox2d.fracture.Material;
import spacegraph.math.Tuple2f;
import spacegraph.math.v2;

import java.util.Random;

/**
 * Material simulujuci logaritmicky rozptyl - zhustene ohniska pri dotykovom bode
 * a nizsia koncentracia vo vacsej vzdialenosti s limitou v nekonecne. Zohladneny
 * je aj kolizny vektor.
 *
 * @author Marek Benovic
 */
public class Diffusion extends Material {
    private final Random r = new XoRoShiRo128PlusRandom(1);

    @Override
    public Tuple2f[] focee(Tuple2f startPoint, Tuple2f vektor) {
        final int count = 128; //pocet
        double c = 4; // natiahnutie

        vektor = new v2((float) 1, (float) 0);

        float ln = vektor.length();
        Transform t = new Transform();
        t.set(startPoint, 0);
        t.q.c = vektor.y / ln;
        t.q.s = vektor.x / ln;

        Tuple2f[] va = new Tuple2f[count];
        for (int i = 1; i <= count; i++) {

            double a = r.nextDouble() * 2 * Math.PI;
            double d = -Math.log(r.nextDouble()) * m_shattering;

            double x = Math.sin(a) * d;
            double y = Math.cos(a) * d * c;

            Tuple2f v = new v2((float) x, (float) y);

            va[i - 1] = Transform.mul(t, v);
        }

        return va;
    }

    @Override
    public String toString() {
        return "Logaritmic diffusion";
    }
}