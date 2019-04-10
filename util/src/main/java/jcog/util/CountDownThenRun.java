package jcog.util;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/** usage: call reset().. then call your runnables/consumers with run(...)
 * TODO this is probably the same as CountedCompleter
 * */
public class CountDownThenRun extends AtomicInteger {

    private volatile Runnable onFinish = null;

    public void reset(int count, Runnable onFinish) {
        if (count < 1)
            throw new RuntimeException("count > 0");
        if (!compareAndSet(0, count))
            throw new RuntimeException(this + " not ready");
        this.onFinish = onFinish;
    }

    private void countDown() {
        if (decrementAndGet() == 0) {
            Runnable of = onFinish;
            onFinish = null;
            of.run();
        }
    }

    /*
    TODO
    public Runnable run(Runnable r) {
        return new MyRunnable(r);
    }
    */

    public Runnable run(Consumer c, Object x) {
        return new MyConsumerRunnable(c, x);
    }

    private class MyConsumerRunnable implements Runnable {
        private final Consumer c;
        private final Object x;

        public MyConsumerRunnable(Consumer c, Object x) {
            this.c = c;
            this.x = x;
        }

        @Override
        public String toString() {
            return c.toString();
        }

        @Override
        public void run() {
            try {
                c.accept(x);
            } finally {
                CountDownThenRun.this.countDown();
            }
        }
    }
}
