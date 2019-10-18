package jcog.tree.rtree;

import jcog.tree.rtree.rect.RectDouble;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SplitTest {

        /**
     * Adds many random entries to trees of different types and confirms that
     * no entries are lost during insert/split.
     */
    @Test
    void randomEntryTest() {

        int entryCount = 32*1024;

        final RectDouble[] rects = RTree2DTest.generateRandomRects(entryCount);

        for (Spatialization.DefaultSplits s : Spatialization.DefaultSplits.values()) {
            /*for (int min : new int[]{2, 3, 4})*/ {
                for (int max : new int[]{2, 3, 8}) /*for (int max : new int[]{min, min+1, 8})*/ {

                    int min = max;
                    int TOTAL = Math.round(max/8f * rects.length);
                    if (TOTAL%2==1) TOTAL++;

                    assert(TOTAL<=entryCount); 
                    assert(TOTAL%2==0); 
                    int HALF = TOTAL/2;


                    final RTree<RectDouble> t = RTree2DTest.createRect2DTree(s, max);
                    int i = 0;
                    for (int i1 = 0; i1 < HALF; i1++) {
                        RectDouble r = rects[i1];
                        boolean added = t.add(r);
                        if (!added) {
                            t.add(r); 
                            fail("");
                        }
                        assertTrue(added);
                        assertEquals(++i, t.size());
                        

                        boolean tryAddingAgainToTestForNonMutation = t.add(r);
                        if (tryAddingAgainToTestForNonMutation) {
                            t.add(r); 
                            fail("");
                        }
                        assertFalse(tryAddingAgainToTestForNonMutation, i + "==?" + t.size()); 
                        assertEquals(i, t.size()); 
                        
                    }


                    assertEquals(HALF, t.size());

                    assertEquals(HALF, t.stats().print(System.out).size());


                    for (int k = 0; k < HALF; k++) {
                        assertFalse(t.add(rects[k])); 
                    }

                    for (int k = 0; k < HALF; k++) {
                        RectDouble a = rects[k];
                        RectDouble b = rects[k + HALF];
                        assertNotEquals(a,b);

                        assertFalse(t.contains(b));
                        assertTrue(t.contains(a));
                        t.replace(a, b);




                        assertFalse(t.contains(a));
                        assertTrue(t.contains(b));

                        assertEquals(HALF, t.size()); 

                    }

                    
                    assertEquals(HALF, t.size());

                    assertEquals(HALF, t.stats().size());

                    for (int k = 0; k < HALF; k++) {
                        assertTrue(t.add(rects[k])); 
                    }


                    assertEquals(TOTAL, t.size());
                    assertEquals(TOTAL, t.stats().size());

                    final int[] andCount = {0};
                    assertTrue(t.root().AND(x -> {
                        andCount[0]++;
                        return true;
                    }));
                    assertEquals(TOTAL, andCount[0]);

                    final int[] orCount = {0};
                    assertFalse(t.OR(x -> {
                        orCount[0]++;
                        return false;
                    }));
                    assertEquals(TOTAL, orCount[0]);

                    final int[] eachCount= {0};
                    t.forEach(x -> eachCount[0]++);
                    assertEquals(TOTAL, eachCount[0]);
                }
            }
        }
    }

}
