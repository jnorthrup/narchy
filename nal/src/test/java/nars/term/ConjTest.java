package nars.term;

import jcog.list.FasterList;
import jcog.math.random.XoRoShiRo128PlusRandom;
import nars.$;
import nars.term.compound.util.Conj;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.roaringbitmap.RoaringBitmap;

import java.util.Random;

import static nars.$.$$;
import static nars.Op.False;
import static nars.time.Tense.ETERNAL;
import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;
import static org.junit.jupiter.api.Assertions.*;

public class ConjTest {

    @Test public void testSimpleEternals() {
        Conj c = new Conj();
        c.add($.the("x"), ETERNAL);
        c.add($.the("y"), ETERNAL);
        assertEquals("(x&&y)", c.term().toString());
        assertEquals(1, c.event.size());
        assertEquals(byte[].class, c.event.get(ETERNAL).getClass());
    }

    @Test public void testSimpleEternalsNeg() {
        Conj c = new Conj();
        c.add($.the("x"), ETERNAL);
        c.add($.the("y").neg(), ETERNAL);
        assertEquals("((--,y)&&x)", c.term().toString());
    }

    @Test public void testSimpleEvents() {
        Conj c = new Conj();
        c.add($.the("x"), 1);
        c.add($.the("y"), 2);
        assertEquals("(x &&+1 y)", c.term().toString());
        assertEquals(1, c.shift());
        assertEquals(2, c.event.size());
    }
    @Test public void testRoaringBitmapNeededManyEventsAtSameTime() {
        Conj c = new Conj();
        c.add($.the("a"), 1);
        c.add($.the("b"), 1);
        c.add($.the("c"), 1);
        c.add($.the("d"), 1);
        c.add($.the("e"), 1);
        assertEquals("(&|,a,b,c,d,e)", c.term().toString());
        assertEquals(1, c.event.size());
        assertEquals(RoaringBitmap.class, c.event.get(1).getClass());
    }
    @Test public void testSimpleEventsNeg() {
        Conj c = new Conj();
        c.add($.the("x"), 1);
        c.add($.the("y").neg(), 2);
        assertEquals("(x &&+1 (--,y))", c.term().toString());
    }

    @Test public void testEventContradiction() {
        Conj c = new Conj();
        c.add($.the("x"), 1);
        assertFalse(c.add($.the("x").neg(), 1));
        assertEquals(False, c.term());
    }
    @Test public void testEventContradictionAmongNonContradictions() {
        Conj c = new Conj();
        c.add($.the("x"), 1);
        c.add($.the("y"), 1);
        c.add($.the("z"), 1);
        assertFalse(c.add($.the("x").neg(), 1));
        assertEquals(False, c.term());
    }
    @Test public void testEventContradictionAmongNonContradictionsRoaring() {
        Conj c = new Conj();
        c.add($$("(&&,a,b,c,d,e,f,g,h)"), ETERNAL);
        boolean added = c.add($.the("a").neg(), 1);
        assertEquals(False, c.term());
    }
    @Test public void testEventContradictionWithEternal() {
        Conj c = new Conj();
        c.add($.the("x"), ETERNAL);
        c.add($.the("x").neg(), 1);
        assertEquals(False, c.term());
    }

    final Random rng = new XoRoShiRo128PlusRandom(1);

    @Test
    public void testConjEventConsistency3ary() {
        for (int i = 0; i < 100; i++) {
            assertConsistentConj(3, 0, 7);
        }
    }
    @Test
    public void testConjEventConsistency4ary() {
        for (int i = 0; i < 100; i++) {
            assertConsistentConj(4, 0, 11);
        }
    }
    @Test
    public void testConjEventConsistency5ary() {
        for (int i = 0; i < 300; i++) {
            assertConsistentConj(5, 0, 17);
        }
    }

    private void assertConsistentConj(int variety, int start, int end) {
        FasterList<LongObjectPair<Term>> x = newRandomEvents(variety, start, end);

        Term y = Conj.conj(x.clone());
        FasterList<LongObjectPair<Term>> z = y.eventList();


        System.out.println(x + "\t" + y + "\t" + z);

        //for debugging to see why
        if (!x.equals(z)) {
            Term y2 = Conj.conj(x.clone());
        }

        assertEquals(x, z);
    }

    private FasterList<LongObjectPair<Term>> newRandomEvents(int variety, int start, int end) {
        FasterList<LongObjectPair<Term>> e = new FasterList<>();
        long earliest = Long.MAX_VALUE;
        for (int i = 0; i < variety; i++) {
            long at = (long) rng.nextInt(end - start) + start;
            earliest = Math.min(at, earliest);
            e.add(pair(at, $.the(String.valueOf((char)('a' + i)))));
        }
        //recreate the 'e' list shifted to start at zero
        long finalEarliest = earliest;
        e.replaceAll((x)-> pair(x.getOne()- finalEarliest, x.getTwo()));
        e.sortThisByLong(LongObjectPair::getOne);
        return e;
    }

    @Test public void testConjComplexAddRemove() {
        Term x = $$("(( ( ((_1-->_2),_3) &| (--,_4)) &| (_5 &| _6)) &&+8 ( (((_1-->_2),_3) &| (--,_4)) &| (_5 &|_6))))");
        Conj c = Conj.from(x);
        assertEquals(x, c.term());
        boolean removedLast = c.remove($$("((_1-->_2),_3)"), c.event.keysView().max());
        assertTrue(removedLast);
        assertEquals(
                "((&|,((_1-->_2),_3),(--,_4),_5,_6) &&+8 (&|,(--,_4),_5,_6))",
                c.term().toString());
        boolean removedFirst = c.remove($$("((_1-->_2),_3)"), c.event.keysView().min());
        assertTrue(removedFirst);
        assertEquals(
                "((&|,(--,_4),_5,_6) &&+8 (&|,(--,_4),_5,_6))",
                c.term().toString());

    }

    @Test
    public void testWrappingCommutiveConjunction() {

        {

            //AND, valid
            Term xEternal = $$("((((--,angX) &&+4 x) &&+10244 angX) && y)");
            assertEquals("((((--,angX) &&+4 x) &&+10244 angX)&&y)",
                    xEternal.toString());
        }
    }
    @Test @Disabled
    public void testWrappingCommutiveConjunctionX() {
        {
            //AND, valid after factoring
            Term xFactored = $$("((x&&y) &&+1 (y&&z))");
            assertEquals("((x &&+1 z)&&y)", xFactored.toString());

            //AND, contradict
            Term xAndContradict = $$("((x &&+1 x)&&--x)");
            assertEquals(False,
                    xAndContradict);

            //AND, redundant
            Term xAndRedundant = $$("((x &&+1 x)&&x)");
            assertEquals("(x &&+1 x)",
                    xAndRedundant.toString());

            //AND, redundant parallel
            Term xAndRedundantParallel = $$("(((x &| y) &| z)&&x)");
            assertEquals("(&|,x,y,z)",
                    xAndRedundantParallel.toString());

            //AND, contradiction parallel
            Term xAndContradictParallel = $$("(((x &| y) &| z)&&--x)");
            assertEquals(False,
                    xAndContradictParallel);

            //AND, contradiction parallel multiple
            Term xAndContradictParallelMultiple = $$("(&&,x,y,((x &| y) &| z))");
            assertEquals("(&|,x,y,z)",
                    xAndContradictParallelMultiple.toString());

            //AND contradiction
            Term xAndContradict2 = $$("((((--,angX) &&+4 x) &&+10244 angX) && --x)");
            assertEquals(False, xAndContradict2);

            //AND contradiction2
            Term xAndContradict3 = $$("((((--,angX) &&+4 x) &&+10244 angX) && angX)");
            assertEquals(False, xAndContradict3);

            //Ambiguous
            Term xParallel = $$("((((--,angX) &&+4 x) &&+10244 angX) &&+0 y)");
            assertEquals(False, xParallel);

        }

        {
            //ambiguous simultaneity

            Term xParallelContradiction4 = $$("((((--,angX) &&+4 x) &&+10244 angX) &&+0 angX)");
            assertEquals(False, xParallelContradiction4);
        }


        {
            Term x = $$("((((--,angX) &&+4 x) &&+10244 angX) &| angX)");
            Term y = $$("(angX &| (((--,angX) &&+4 x) &&+10244 angX))");
            assertEquals(x, y);
            //.
        }
    }

    @Disabled
    @Test
    public void testFactorFromEventSequence() {
        Term yParallel1 = $$("((((--,angX) &&+4 x) &&+10244 angX) &&+0 y)");
        String yParallel2Str = "((((--,angX)&|y) &&+4 (x&|y)) &&+10244 (angX&|y))";
        Term yParallel2 = $$(yParallel2Str);
        assertEquals(yParallel1, yParallel2);
        assertEquals(yParallel2Str, yParallel1.toString());
    }
    @Disabled
    @Test
    public void testFactorFromEventParallel() {
        Term yParallelOK = $$("(((a&&x) &| (b&&x)) &| (c&&x))");
        assertEquals("", yParallelOK.toString());
        //not: (&|,a,b,c,x)

        Term yParallelContradict = $$("((a&&x) &| (b&&--x))");
        assertEquals(False, yParallelContradict);
    }


}

