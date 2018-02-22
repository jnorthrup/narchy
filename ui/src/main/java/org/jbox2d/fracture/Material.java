package org.jbox2d.fracture;

import jcog.Util;
import org.jbox2d.fracture.fragmentation.Smasher;
import org.jbox2d.fracture.materials.Diffusion;
import org.jbox2d.fracture.materials.Glass;
import org.jbox2d.fracture.materials.Uniform;
import spacegraph.math.Tuple2f;

import static org.jbox2d.common.Settings.EPSILON;

/**
 * Material telesa
 *
 * @author Marek Benovic
 */
public abstract class Material {


    /**
     * Najmensi ulomok, ktory je mozne triestit - aby sa zabranilo rekurzivnemu triesteniu.
     */
    public static final float MINMASSDESCTRUCTION = 0.005f;


    /**
     * Po destrukcii kruhu je kruh transformovany na regular polygon s danym poctom vrcholov.
     */
    public static final int CIRCLEVERTICES = 32;

    /**
     * Objekty musia mat najdlhsiu hranu (radius) vacsiu ako dany limit, vacsi obsah
     * a taktiez mass / radius.
     */
    public static final double MINFRAGMENTSIZE = 0.01;

    /**
     * Material polárneho logaritmického rozptylu
     */
    public static final Material DIFFUSION = new Diffusion();

    /**
     * Materiál rovnomerného rozptylu
     */
    public static final Material UNIFORM = new Uniform();

    /**
     * Sklo
     */
    public static final Material GLASS = new Glass();

    /**
     * Od akeho limitu tangentInertia sa zacne objekt triestit.
     */
    public float m_rigidity = 64.0f;

    /**
     * Na ake drobne kusky sa objekt zvykne triestit (minimalne). Sluzi pre
     * material na urcovanie, do akej vzdialenosti budu fragmenty sucastou
     * povodneho telesa a ktore sa odstiepia. Polomer na ^2
     */
    public float m_shattering = 4.0f;

    /**
     * Polomer kruhu, z ktoreho sa rataju fragmenty (fragmenty mimo kruhu su
     * zjednocovane do povodneho telesa)
     */
    public float m_radius = 2.0f;

//    /**
//     * Aky material sa definuje na fragmenty. this - fragmenty preberaju material
//     * od povodneho predka, null - ziadne rekurzivne triestenie. Pomocou inych
//     * referencii sa da dobre definovat napr. cihlova stena.
//     */
//    public final Material m_fragments = this;

    /**
     * Abstraktna funkcia urcujuca sposob triesenia.
     *
     * @param contactVector
     * @param contactPoint
     * @return Vrati ohniska v ktorych sa bude teleso triestit.
     */
    protected abstract Tuple2f[] focee(Tuple2f contactPoint, Tuple2f contactVector);

    /**
     * Fragmentacia telesa.
     *
     * @param p             Teleso
     * @param localVel        Vektor ratajuc aj jeho velkost - ta urcuje rychlost.
     * @param localPos     Lokalny bod narazu na danom telese.
     * @param normalImpulse Intenzita kolizie
     * @return Vrati pole Polygonov, na ktore bude dany polygon rozdeleny
     */
    protected Polygon[] split(Smasher geom, Polygon p, Tuple2f localPos, Tuple2f localVel, float normalImpulse) {
        Tuple2f[] foceeArray = focee(localPos, localVel);

        //inverzna tranformacia
        float ln = localVel.length();

        //definicia filtru posobnosti
        float r = m_radius; // polomer
        float rr = r * r;

        float c = 2;

        float dd = Util.sqr(Math.max(ln * c, r));

        if (ln > EPSILON) {

            float sin = -localVel.x / ln;
            float cos = -localVel.y / ln;

            geom.calculate(p, foceeArray, localPos, point -> {
                float x = localPos.x - point.x;
                float y = localPos.y - point.y;

                x = cos * x + -sin * y;
                y = sin * x + cos * y;

                float xx = x * x;
                float yy = y * y;
                return (y < 0 && (xx + yy < rr)) || (y > 0 && (xx / rr + yy / dd < 1));
            });
        }

        return geom.fragments;
    }
}
