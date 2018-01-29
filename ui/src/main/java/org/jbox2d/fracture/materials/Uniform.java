package org.jbox2d.fracture.materials;

import jcog.math.random.XoRoShiRo128PlusRandom;
import org.jbox2d.fracture.Material;
import org.jbox2d.fracture.util.MyList;
import spacegraph.math.Tuple2f;
import spacegraph.math.v2;

import java.util.Random;

/**
 * Material, kde ohniska fragmentov su generovane rovnomerne nahodne v priestore.
 *
 * @author Marek Benovic
 */
public class Uniform extends Material {
    private final Random r = new XoRoShiRo128PlusRandom(1);

    @Override
    public Tuple2f[] focee(Tuple2f bodNarazu, Tuple2f vektorNarazu) {
        MyList<Tuple2f> focee = new MyList<>();
        float rad = m_shattering / 4;
        for (int i = 0; i < 100; ++i) {
            float x = r.nextFloat() - 0.5f;
            float y = r.nextFloat() - 0.5f;

            x = x * rad * 10;
            y = y * rad * 10;

            Tuple2f v = new v2(x, y);

            v.added(bodNarazu);

            focee.add(v);
        }
        Tuple2f[] foceeArray = new Tuple2f[focee.size()];
        focee.addToArray(foceeArray);
        return foceeArray;
    }

    @Override
    public String toString() {
        return "Uniform diffusion";
    }
}