package jcog.pri.bag.impl;

import com.google.common.base.Joiner;
import jcog.random.XorShift128PlusRandom;
import org.apache.commons.math3.stat.Frequency;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class BaggieTest {

    @Test
    void testAddAccumulate() {
        InstrumentedBaggie<String> s = new InstrumentedBaggie();

        s.put("a", 0.1f);
        s.put("b", 0.2f);
        s.put("c", 0.3f);
        s.put("d", 0.4f);
        assertEquals(4, s.size());
        assertEquals("a", s.lowest());
        assertEquals("d", s.highest());

        s.put("a", 0.2f);
        assertEquals(4, s.size());
        assertEquals(0.3f, s.get("a"), 0.001f);
        assertEquals("b", s.lowest());

        s.put("e", 0.5f);
        assertEquals("e", s.highest());
        assertEquals("a", s.lowest());
        assertTrue(!s.contains("b"));
        assertEquals(4, s.size());
        assertEquals(1, s.evictions);

        boolean addedF = s.put("f", 0.01f); 
        assertFalse(addedF);
        assertEquals(4, s.size());
        assertEquals("e", s.highest());
        assertEquals("a", s.lowest());

        
        boolean addedAzero = s.put("a", 0f); 
        assertTrue(addedAzero);
        assertEquals(4, s.size());
        assertEquals("e", s.highest());
        assertEquals("a", s.lowest());
        

        s.clear();
        assertEquals(0, s.size());
        assertEquals(null, s.highest());
        assertEquals(null, s.lowest());
    }

    static class InstrumentedBaggie<X> extends Baggie<X> {
        int evictions = 0;
        int insertions = 0;
        int rejections = 0;
        

        InstrumentedBaggie() {
            super(4);
        }

        @Override
        public boolean put(X o, float pri) {
            if (!super.put(o, pri)) {
                rejections++;
                return false;
            }
            return true;
        }

        @Override
        public void onAdded(X o) {
            super.onAdded(o);
            insertions++;
        }

        @Override
        public void onRemoved(X o) {
            super.onRemoved(o);
            evictions++;
        }


    }

    @Test
    void testSampling() {
        int cap = 8;
        Baggie<String> s = new Baggie(cap);
        for (int i = 0; i < cap; i++) {
            s.put("x" + i, i / ((float) cap));
        }
        String t = Joiner.on(",").join(s.toList());
        assertEquals("x7=0.87498856,x6=0.7500076,x5=0.6249962,x4=0.50001526,x3=0.3750038,x2=0.24999237,x1=0.12501144,x0=0.0", t);

        Random rng = new XorShift128PlusRandom(1);
        Frequency f = new Frequency();
        final int[] num = {cap * 100};
        s.sample(rng, (x) -> {
            f.addValue(x.get());
            return num[0]-- > 0;
        });
        System.out.println(f);
        assertTrue(f.getCount("x" + (cap - 1)) > f.getCount("x0"));


        {
            
            final int[] resetNum = {cap * 100};

            
            s.sample(rng, (x) -> {
                x.set(0.5f);
                return resetNum[0]-- > 0;
            });
            s.forEach((x, p) -> {
                assertEquals(0.5f, p, 0.001f);
                return true;
            });
            assertEquals(cap, s.size()); 
            assertEquals(0.5f, s.priMax(), 0.001f);
            assertEquals(0.5f, s.priMin(), 0.001f);
        }

        {
            
            final int[] remNum = {0};
            s.sample(rng, (x) -> {
                x.set(Float.NaN); 
                remNum[0]++;
                return true;
            });
            assertEquals(cap, remNum[0]); 
            assertEquals(0, s.size()); 
            assertEquals(Float.NaN, s.priMax(), 0.001f);
            assertEquals(Float.NaN, s.priMin(), 0.001f);
        }
    }

    @Test
    void testSustainedAdd() {
        int cap = 256;
        int uniq = cap * 2;
        Baggie<String> s = new Baggie<>(cap);
        Random rng = new XorShift128PlusRandom(1);
        for (int i = 0; i < cap * 256; i++) {
            s.put("x" + i, rng.nextFloat());
        }
        assertEquals(cap, s.size());
    }
}