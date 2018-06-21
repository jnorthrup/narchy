package jcog.math;

import jcog.math.random.XoRoShiRo128PlusRandom;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ByteShufflerTest {

    @Test
    void testByteShuffler() {
        ByteShuffler b = new ByteShuffler(16);
        Random rng = new XoRoShiRo128PlusRandom(1);
        for (int i = 2; i < 5; i ++)
            testPermutes(b, rng, i);
    }

    private void testPermutes(ByteShuffler b, Random rng, int len) {
        int permutations = (int) org.apache.commons.math3.util.CombinatoricsUtils.factorial(len);
        int iterates = permutations * 12 /* to be sure */;
        TreeSet<String> combos = new TreeSet<>();
        for (int i = 0; i < iterates; i++) {
            byte[] order = b.shuffle(rng, len, true);
            assertEquals(len, order.length );
            combos.add( Arrays.toString(order) );
        }
        
        assertEquals(permutations, combos.size());
    }

}