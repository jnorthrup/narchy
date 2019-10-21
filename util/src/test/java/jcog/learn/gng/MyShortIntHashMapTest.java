package jcog.learn.gng;

import jcog.learn.gng.impl.MyShortIntHashMap;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.procedure.primitive.ShortIntProcedure;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by me on 5/25/16.
 */
class MyShortIntHashMapTest {

    @Test
    void testAddToValuesAndFilter() {
        MyShortIntHashMap m = new MyShortIntHashMap();
        for (int c = 0; c < 4; c++) {
            for (int i = 0; i < 100; i++) {
                m.put((short) (Math.random() * 1000), 1);
            }

            m.addToValues(4);

            m.forEachKeyValue(new ShortIntProcedure() {
                @Override
                public void value(short k, int v) {
                    assertEquals(5, v);
                }
            });

            m.filter(new IntPredicate() {
                @Override
                public boolean accept(int v) {
                    return v == 0;
                }
            });

            assertTrue(m.isEmpty());
        }
    }

}