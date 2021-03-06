package jcog.exe;

import jcog.util.MutableInt;

import java.util.function.LongConsumer;

/** synchronously triggered executor but executes only if elapsed time exceeds adjustable threshold.
 * milliseconds
 * */
public class Every {

    public static Every Never = new Every((LongConsumer)null, Integer.MAX_VALUE) {
        @Override
        public void next() {
            
        }
    };

    public final MutableInt periodMS = new MutableInt();


    private final LongConsumer each;
    long last = System.currentTimeMillis();

    public Every(Runnable r, int initialPeriodMS) {
        this(new LongConsumer() {
            @Override
            public void accept(long x) {
                r.run();
            }
        }, initialPeriodMS);
    }

    public Every(LongConsumer r, int initialPeriodMS) {
        this.each = r;
        this.periodMS.set(initialPeriodMS);
    }

    public void next() {
        long now = System.currentTimeMillis();
        long delta = now - last;
        if (delta > (long) periodMS.intValue()) {
            last = now;
            each.accept(delta);
        }
    }
}
