package jcog.event;

import jcog.util.CountDownThenRun;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * arraylist implementation, thread safe.  creates an array copy on each update
 * for fastest possible iteration during emitted events.
 */
public class ListTopic<V> extends jcog.data.list.FastCoWList<Consumer<V>> implements Topic<V> {

    final CountDownThenRun busy = new CountDownThenRun();

    public ListTopic() {
        this(8);
    }

    public ListTopic(int capacity) {
        super(capacity, Consumer[]::new);
    }

    @Override
    public void emit(V x) {
        final Consumer[] cc = this.array();

        for (Consumer c: cc)
            c.accept(x);

    }



    @Override
    public void emitAsync(V x, Executor executorService) {
        final Consumer[] cc = this.array();
        if (cc != null) {
            for (Consumer c: cc)
                executorService.execute(() -> c.accept(x));
        }
    }

    @Override
    public void emitAsyncAndWait(V x, Executor executorService) throws InterruptedException {
        final Consumer[] cc = this.array();
        if (cc != null) {
            int n = cc.length;
            switch (n) {
                case 0:
                    return;
                default:
                    CountDownLatch l = new CountDownLatch(n);

                    for (Consumer c: cc) {
                        executorService.execute(() -> {
                            try {
                                c.accept(x);
                            } finally {
                                l.countDown();
                            }

                        });
                    }
                    l.await();
                    break;
            }
        }
    }

    @Override
    public void emitAsync(V x, Executor exe, Runnable onFinish) {
        final Consumer[] cc = this.array();
        int n = cc != null ? cc.length : 0;
        switch (n) {
            case 0:
                return;
            case 1: {
                Consumer cc0 = cc[0];
                exe.execute(() -> {
                    try {
                        cc0.accept(x);
                    } finally {
                        onFinish.run();
                    }
                });
                break;
            }
            default: {
                busy.reset(n, onFinish);

                for (Consumer c: cc)
                    exe.execute(busy.run(c, x));
                break;
            }
        }
    }

    @Override
    public void enable(Consumer<V> o) {
        assert (o != null);
        add(o);
    }

    @Override
    public void disable(Consumer<V> o) {
        assert (o != null);
        remove(o);
    }


}