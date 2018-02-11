package jcog.util;

import java.util.concurrent.atomic.AtomicInteger;

public class CountDownThenRun extends AtomicInteger {

    private volatile Runnable onFinish = null;

    public void set(int count, Runnable onFinish) {
        assert(count > 0);
        getAndUpdate(before->{
            assert(before==0);
            this.onFinish = onFinish;
            return count;
        });
    }

    public void countDown() {
        if (decrementAndGet() == 0) {
            onFinish.run();
        }
    }

}
