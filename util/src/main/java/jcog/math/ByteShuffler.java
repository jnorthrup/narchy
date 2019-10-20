package jcog.math;

import java.util.Arrays;
import java.util.Random;

public class ByteShuffler {

    public final byte[] order;

    public ByteShuffler(int capacity) {
        this.order = new byte[capacity];
    }

    public byte[] shuffle(Random rng, int len, boolean copy) {
        assert(len >= 2 && len < 127);
        for (byte i = 0; i < ((byte)len); i++)
            order[i] = i; 

        long rndInt = 0;
        var generate = 0;
        for (var i = 0; i < len; i++) {
            if ((generate++ & 8) == 0)
                rndInt = rng.nextLong();
            else
                rndInt >>= 8;
            var j = ((int)(rndInt & 0xff)) % len;
            if (i!=j) {
                var x = order[i];
                order[i] = order[j];
                order[j] = x;
            }
        }

        return copy ? Arrays.copyOfRange(order, 0, len) : order;
    }

    public static void shuffle(Random rng, Object[] a, int from, int to) {
        var len = to - from;

        if (len == 2) {
            
            if (rng.nextBoolean()) {
                to--;
                var x = a[from];
                a[from] = a[to];
                a[to] = x;
            }
            return;
        }

        assert(len >= 2 && len < 127);

        long rndInt = 0;
        var generate = 0;
        for (var i = from; i < to; i++) {
            if ((generate++ & 8) == 0)
                rndInt = rng.nextLong();
            else
                rndInt >>= 8;
            var j = from + ((int)(rndInt & 0xff)) % len;
            if (i!=j) {
                var x = a[i];
                a[i] = a[j];
                a[j] = x;
            }
        }
    }

}
