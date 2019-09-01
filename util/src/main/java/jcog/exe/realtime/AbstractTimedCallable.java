package jcog.exe.realtime;

import jcog.WTF;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static jcog.Util.sleepMS;

abstract public class AbstractTimedCallable<X> extends AbstractTimedFuture<X> {
    static final int DEFAULT_TIMEOUT_POLL_PERIOD_MS = 2;

    private final Callable<X> callable;
    private volatile Object result = this;
    protected /*volatile*/ int status = PENDING;

    protected AbstractTimedCallable(int rounds, Callable<X> callable) {
        super(rounds);
        this.callable = callable;
    }

    @Override
    public boolean isCancelled() {
        return status == CANCELLED;
    }


    @Override
    public boolean cancel(boolean mayInterruptIfRunning /* ignored */) {
        this.status = CANCELLED;
        return true;
    }


    @Override
    public boolean isDone() {
        return result!=this;
    }

    @Override
    public void run() {
        try {

            this.result = callable.call();

        } catch (Exception e) {
            result = e;
        }
    }

    @Override
    public X get() {
        Object r = result;
        return r == this ? null : (X) r;
    }

    @Override
    public X get(long timeout, TimeUnit unit) {
        return poll(this, timeout, unit);
    }

    public static  <X> X poll(Future<X> f, long timeout, TimeUnit unit) {
        X r;

        long deadline = System.nanoTime() + unit.toNanos(timeout);

        do {

            try {
                r = f.get();
            } catch (InterruptedException e) {
                break;
            } catch (ExecutionException e) {
                throw new WTF(e);
            }

            if (r != null)
                return r;
            else {
                //TODO abstract sleep strategy
                sleepMS(DEFAULT_TIMEOUT_POLL_PERIOD_MS);
                //Util.pauseNextIterative( );
            }

        } while (System.nanoTime() < deadline);

        return null;
    }

}
