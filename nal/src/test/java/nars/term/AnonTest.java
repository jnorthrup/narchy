package nars.term;

import jcog.math.random.XoRoShiRo128PlusRandom;
import nars.$;
import nars.Narsese;
import nars.term.anon.Anom;
import nars.term.anon.Anon;
import nars.term.anon.AnonVector;
import nars.term.compound.CachedCompound;
import nars.term.container.ArrayTermVector;
import nars.term.container.TermVector;
import nars.term.container.TermVector1;
import nars.term.container.TermVector2;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static nars.$.$;
import static nars.Op.PROD;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AnonTest {

    @Test
    public void testAtoms() throws Narsese.NarseseException {
        assertAnon("0", "a");
        assertAnon("#1", $.varDep(1)); //unchanged
        assertAnon("0", $.the(2)); //int remaps to internal int
    }

    @Test
    public void testCompounds() throws Narsese.NarseseException {
        assertAnon("(0-->1)", "(a-->b)");

        assertAnon("(0-->#1)", "(a-->#1)");

        assertAnon("(((0-->(1,2,#1))==>(3,4)),?2)",
                "(((a-->(b,c,#2))==>(e,f)),?1)");

        assertEquals("(4..6-->x)", $("((|,4,5,6)-->x)").toString());
        assertAnon("(0-->1)", "((|,4,5,6)-->x)");

    }

    static Anon assertAnon(String expect, String test) throws Narsese.NarseseException {
        return assertAnon(expect, $(test));
    }

    static Anon assertAnon(String expect, Term x) {
        Anon a = new Anon();
        Term y = a.put(x);
        Term z = a.get(y);
        assertEquals(expect, y.toString());
        assertEquals(x, z);
        return a;
    }

    @Test public void testAnomVector() {

        Term[] x = {Anom.the(2), Anom.the(0), Anom.the(1)};

        assertEqual(new TermVector1(x[0]), new AnonVector(x[0]));
        assertEqual(new TermVector2(x[0], x[1]), new AnonVector(x[0], x[1]));
        assertEqual(new ArrayTermVector(x), new AnonVector(x));
    }

    @Test public void testMixedAnonVector() {

        Term[] x = {$.varDep(1), $.varIndep(2), $.varQuery(3), Anom.the(4)};
        Random rng = new XoRoShiRo128PlusRandom(1);
        for (int i = 0; i < 4; i++) {

            ArrayUtils.shuffle(x, rng);

            assertEqual(new TermVector1(x[0]), new AnonVector(x[0]));
            assertEqual(new TermVector2(x[0], x[1]), new AnonVector(x[0], x[1]));
            assertEqual(new ArrayTermVector(x), new AnonVector(x));
        }
    }

    static void assertEqual(TermVector v, AnonVector a) {
        assertEquals(v,a);
        assertEquals(v.toString(),a.toString());
        assertEquals(v.hashCode(), a.hashCode());
        assertEquals(v.hashCodeSubterms(), a.hashCodeSubterms());
        assertEquals(new CachedCompound(PROD, v), new CachedCompound(PROD, a));
    }

}