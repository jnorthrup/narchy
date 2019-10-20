package spacegraph.space2d.phys.fracture.materials;

import jcog.math.v2;
import jcog.random.XoRoShiRo128PlusRandom;
import spacegraph.space2d.phys.common.Transform;
import spacegraph.space2d.phys.fracture.Material;

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
    public v2[] focee(v2 startPoint, v2 vektor) {


        var ln = vektor.length();
        var t = new Transform();
        t.set(startPoint, 0);
        t.c = vektor.y / ln;
        t.s = vektor.x / ln;

        final var count = 128;
        var va = new v2[count];
        double c = 4;
        for (var i = 1; i <= count; i++) {

            var a = r.nextFloat() * 2 * Math.PI;
            var d = -Math.log(r.nextFloat()) * m_shattering;

            var x = Math.sin(a) * d;
            var y = Math.cos(a) * d * c;

            var v = new v2((float) x, (float) y);

            va[i - 1] = Transform.mul(t, v);
        }

        return va;
    }

    @Override
    public String toString() {
        return "Logaritmic diffusion";
    }
}