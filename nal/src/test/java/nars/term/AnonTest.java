package nars.term;

import com.google.common.collect.Iterators;
import jcog.math.random.XoRoShiRo128PlusRandom;
import nars.$;
import nars.Narsese;
import nars.The;
import nars.term.anon.Anom;
import nars.term.anon.Anon;
import nars.term.anon.AnonVector;
import nars.term.compound.CachedCompound;
import nars.term.sub.ArrayTermVector;
import nars.term.sub.TermVector;
import nars.term.sub.TermVector1;
import nars.term.sub.TermVector2;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static nars.$.$;
import static nars.Op.PROD;
import static org.junit.jupiter.api.Assertions.*;

public class AnonTest {

    @Test
    public void testAtoms() throws Narsese.NarseseException {
        assertAnon("_0", "a");
        assertAnon("#1", $.varDep(1)); //unchanged
        assertAnon("_0", $.the(2)); //int remaps to internal int

        assertNotEquals(Anom.the(0), $.the(0));
        assertNotEquals(Anom.the(2), $.the(2));
    }

    @Test
    public void testCompounds() throws Narsese.NarseseException {
        assertAnon("(_0-->_1)", "(a-->b)");

        assertAnon("(_0-->#1)", "(a-->#1)");

        assertAnon("(((_0-->(_1,_2,#1))==>(_3,_4)),?2)",
                "(((a-->(b,c,#2))==>(e,f)),?1)");

    }

    @Test @Disabled
    public void testIntRange() throws Narsese.NarseseException {
        assertEquals("(4..6-->x)", $("((|,4,5,6)-->x)").toString());
        assertAnon("(_0-->_1)", "((|,4,5,6)-->x)");
    }

    @Test public void testCompounds2() throws Narsese.NarseseException {
        //TODO check that this is correct (includes a impl in conj reduction):
        String xs = "(((($1-->tetris) ==>-1422 (happy-->$1)) &&+105 (--,(((isRow,(8,true),true)~(checkScore,()))-->tetris))) &&+7 ((--,(((isRow,(8,true),true)~(checkScore,()))-->tetris)) &&+74 ((act,0,true)-->#2)))";
        String ys = "(((($2-->_4) &&+105 (--,(((_0,(_1,_2),_2)~(_3,()))-->_4))) &&+7 ((--,(((_0,(_1,_2),_2)~(_3,()))-->_4)) &&+81 ((_5,_6,_2)-->#1))) ==>-1832 (_7-->$2))";
        Term x = $(xs);
        Term y = x.anon();
        assertEquals(ys, y.toString());
    }



    @Test public void testAnomVector() {

        Term[] x = {Anom.the(2), Anom.the(0), Anom.the(1)};

        assertEqual(new TermVector1(x[0]), new AnonVector(x[0]));
        assertEqual(new TermVector2(x[0], x[1]), new AnonVector(x[0], x[1]));
        assertEqual(new ArrayTermVector(x), new AnonVector(x));
    }

    @Test public void testAnomVectorNegations() {

        Term[] x = {Anom.the(2), Anom.the(0), Anom.the(1).neg()};

        AnonVector av = new AnonVector(x);
        assertEquals(new ArrayTermVector(x).toString(), av.toString());
        assertEqual(new ArrayTermVector(x), av);

        assertTrue(av.contains(x[2]));
        assertFalse(av.contains(x[2].neg()));
        assertTrue(av.containsRecursively(x[2]));
        assertTrue(av.containsRecursively(x[2].neg()), ()->av + " containsRecursively " + x[2].neg());


        //assertTrue(The.subterms(x) instanceof AnonVector );
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

    static void assertEqual(TermVector v, AnonVector a) {
        assertEquals(v,a);
        assertEquals(v.toString(),a.toString());
        assertEquals(v.hashCode(), a.hashCode());
        assertEquals(v.hashCodeSubterms(), a.hashCodeSubterms());
        assertTrue(Iterators.elementsEqual(v.iterator(), a.iterator()));
        assertEquals(new CachedCompound(PROD, v), new CachedCompound(PROD, a));
    }

}