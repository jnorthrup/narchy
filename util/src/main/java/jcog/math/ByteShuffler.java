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
        for (byte i = (byte) 0; (int) i < (int) ((byte) len); i++)
            order[(int) i] = i;

        long rndInt = 0L;
        int generate = 0;
        for (int i = 0; i < len; i++) {
            if ((generate++ & 8) == 0)
                rndInt = rng.nextLong();
            else
                rndInt >>= 8L;
            int j = ((int)(rndInt & 0xffL)) % len;
            if (i!=j) {
                byte x = order[i];
                order[i] = order[j];
                order[j] = x;
            }
        }

        return copy ? Arrays.copyOfRange(order, 0, len) : order;
    }

    public static void shuffle(Random rng, Object[] a, int from, int to) {
        int len = to - from;

        if (len == 2) {
            
            if (rng.nextBoolean()) {
                to--;
                Object x = a[from];
                a[from] = a[to];
                a[to] = x;
            }
            return;
        }

        assert(len >= 2 && len < 127);

        long rndInt = 0L;
        int generate = 0;
        for (int i = from; i < to; i++) {
            if ((generate++ & 8) == 0)
                rndInt = rng.nextLong();
            else
                rndInt >>= 8L;
            int j = from + ((int)(rndInt & 0xffL)) % len;
            if (i!=j) {
                Object x = a[i];
                a[i] = a[j];
                a[j] = x;
            }
        }
    }

}
