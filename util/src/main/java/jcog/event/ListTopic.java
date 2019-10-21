package jcog.event;

import jcog.util.CountDownThenRun;
import org.eclipse.collections.api.block.procedure.Procedure2;

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
        forEachWith(Consumer::accept, x);
//        final Consumer[] cc = this.array();
//        for (Consumer c: cc)
//            c.accept(x);
    }



    @Override
    public void emitAsync(V x, Executor executorService) {
//        final Consumer[] cc = this.array();
//
//            for (Consumer c: cc)
//                executorService.execute(() -> c.accept(x));
        forEachWith(new Procedure2<Consumer<V>, V>() {
            @Override
            public void value(Consumer<V> c, V X) {
                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        c.accept(X);
                    }
                });
            }
        }, x);
    }

    @Override
    public void emitAsyncAndWait(V x, Executor executorService) throws InterruptedException {
        Consumer[] cc = this.array();
        if (cc != null) {
            int n = cc.length;
            if (n != 0) {
                CountDownLatch l = new CountDownLatch(n);

                for (Consumer c : cc) {
                    executorService.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                c.accept(x);
                            } finally {
                                l.countDown();
                            }

                        }
                    });
                }
                l.await();
            } else {
                return;
            }
        }
    }

    @Override
    public void emitAsync(V x, Executor exe, Runnable onFinish) {
        Consumer[] cc = this.array();
        int n = cc != null ? cc.length : 0;
        switch (n) {
            case 0:
                return;
            case 1: {
                Consumer cc0 = cc[0];
                exe.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            cc0.accept(x);
                        } finally {
                            onFinish.run();
                        }
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
    public void start(Consumer<V> o) {
        //assert (o != null);
        add(o);
    }

    @Override
    public void stop(Consumer<V> o) {
        //assert (o != null);
        remove(o);
    }


}