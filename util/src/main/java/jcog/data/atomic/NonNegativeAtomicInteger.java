package jcog.data.atomic;

import java.util.concurrent.atomic.AtomicInteger;

public class NonNegativeAtomicInteger extends AtomicInteger {

    public int getAndIncrementNonNegative() {
        return getAndUpdate(x -> {
            int y = x + 1;
            if (y < 0)
                y = 0;
            return y;
        });
    }

}
