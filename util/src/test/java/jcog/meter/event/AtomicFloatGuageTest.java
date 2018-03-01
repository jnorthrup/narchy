package jcog.meter.event;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AtomicFloatGuageTest {

    @Test
    public void test1() {
        AtomicFloatGuage x = new AtomicFloatGuage("x");
        x.accept(1);
        x.accept(5);
        x.accept(9);
        x.commit();
        assertEquals(15, x.getSum());
        assertEquals(5, x.getMean());

        x.accept(1.5f);
        assertEquals(1.5f, x.commitSum());

        x.accept(10f);
        x.accept(20f);
        assertEquals(15, x.commitMean());
    }

}