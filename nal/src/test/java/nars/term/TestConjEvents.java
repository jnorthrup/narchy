package nars.term;

import jcog.list.FasterList;
import jcog.math.random.XoRoShiRo128PlusRandom;
import nars.$;
import nars.Op;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestConjEvents {

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
}

