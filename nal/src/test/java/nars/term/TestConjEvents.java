package nars.term;

import jcog.list.FasterList;
import jcog.math.random.XoRoShiRo128PlusRandom;
import nars.$;
import nars.Op;
import nars.term.compound.util.ConjEvents;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static nars.Op.False;
import static nars.time.Tense.ETERNAL;
import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class TestConjEvents {

    @Test public void testSimpleEternals() {
        ConjEvents c = new ConjEvents();
        c.add($.the("x"), ETERNAL);
        c.add($.the("y"), ETERNAL);
        assertEquals("(x&&y)", c.term().toString());
        assertEquals(1, c.event.size());
    }
    @Test public void testSimpleEternalsNeg() {
        ConjEvents c = new ConjEvents();
        c.add($.the("x"), ETERNAL);
        c.add($.the("y").neg(), ETERNAL);
        assertEquals("((--,y)&&x)", c.term().toString());
    }

    @Test public void testSimpleEvents() {
        ConjEvents c = new ConjEvents();
        c.add($.the("x"), 1);
        c.add($.the("y"), 2);
        assertEquals("(x &&+1 y)", c.term().toString());
        assertEquals(1, c.shift());
        assertEquals(2, c.event.size());
    }
    @Test public void testSimpleEventsNeg() {
        ConjEvents c = new ConjEvents();
        c.add($.the("x"), 1);
        c.add($.the("y").neg(), 2);
        assertEquals("(x &&+1 (--,y))", c.term().toString());
    }

    @Test public void testEventContradiction() {
        ConjEvents c = new ConjEvents();
        c.add($.the("x"), 1);
        assertFalse(c.add($.the("x").neg(), 1));
        assertEquals(False, c.term());
    }

    @Test public void testEventContradictionWithEternal() {
        ConjEvents c = new ConjEvents();
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

        Term y = Op.conj(x.clone());
        FasterList<LongObjectPair<Term>> z = y.eventList();


        System.out.println(x + "\t" + y + "\t" + z);

        //for debugging to see why
        if (!x.equals(z)) {
            Term y2 = Op.conj(x.clone());
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


    @Test
    public void testWrappingCommutiveConjunction() {

        {

            //AND, valid
            Term xEternal = $.$safe("((((--,angX) &&+4 x) &&+10244 angX) && y)");
            assertEquals("((((--,angX) &&+4 x) &&+10244 angX)&&y)",
                    xEternal.toString());

            //AND, valid after factoring
            Term xFactored = $.$safe("((x&&y) &&+1 (y&&z))");
            assertEquals("((x &&+1 z)&&y)", xFactored.toString());

            //AND, contradict
            Term xAndContradict = $.$safe("((x &&+1 x)&&--x)");
            assertEquals(False,
                    xAndContradict);

            //AND, redundant
            Term xAndRedundant = $.$safe("((x &&+1 x)&&x)");
            assertEquals("(x &&+1 x)",
                    xAndRedundant.toString());

            //AND, redundant parallel
            Term xAndRedundantParallel = $.$safe("(((x &| y) &| z)&&x)");
            assertEquals("(&|,x,y,z)",
                    xAndRedundantParallel.toString());

            //AND, contradiction parallel
            Term xAndContradictParallel = $.$safe("(((x &| y) &| z)&&--x)");
            assertEquals(False,
                    xAndContradictParallel);

            //AND, contradiction parallel multiple
            Term xAndContradictParallelMultiple = $.$safe("(&&,x,y,((x &| y) &| z))");
            assertEquals("(&|,x,y,z)",
                    xAndContradictParallelMultiple.toString());

            //AND contradiction
            Term xAndContradict2 = $.$safe("((((--,angX) &&+4 x) &&+10244 angX) && --x)");
            assertEquals(False, xAndContradict2);

            //AND contradiction2
            Term xAndContradict3 = $.$safe("((((--,angX) &&+4 x) &&+10244 angX) && angX)");
            assertEquals(False, xAndContradict3);

            //Ambiguous
            Term xParallel = $.$safe("((((--,angX) &&+4 x) &&+10244 angX) &&+0 y)");
            assertEquals(False, xParallel);

        }

        {
            //ambiguous simultaneity

            Term xParallelContradiction4 = $.$safe("((((--,angX) &&+4 x) &&+10244 angX) &&+0 angX)");
            assertEquals(False, xParallelContradiction4);
        }


        {
            Term x = $.$safe("((((--,angX) &&+4 x) &&+10244 angX) &| angX)");
            Term y = $.$safe("(angX &| (((--,angX) &&+4 x) &&+10244 angX))");
            assertEquals(x, y);
            //.
        }
    }

    @Test
    public void testFactorFromEventSequence() {
        Term yParallel1 = $.$safe("((((--,angX) &&+4 x) &&+10244 angX) &&+0 y)");
        String yParallel2Str = "((((--,angX)&|y) &&+4 (x&|y)) &&+10244 (angX&|y))";
        Term yParallel2 = $.$safe(yParallel2Str);
        assertEquals(yParallel1, yParallel2);
        assertEquals(yParallel2Str, yParallel1.toString());
    }
    @Test
    public void testFactorFromEventParallel() {
        Term yParallelOK = $.$safe("(((a&&x) &| (b&&x)) &| (c&&x))");
        assertEquals("", yParallelOK.toString());
        //not: (&|,a,b,c,x)

        Term yParallelContradict = $.$safe("((a&&x) &| (b&&--x))");
        assertEquals(False, yParallelContradict);
    }


}

