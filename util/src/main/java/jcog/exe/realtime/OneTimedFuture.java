package jcog.exe.realtime;

import java.util.concurrent.Callable;

public class OneTimedFuture<T> extends AbstractTimedCallable<T> {

    private final int firstFireOffset;

    public OneTimedFuture(int firstFireOffset, int rounds, Callable<T> callable) {
        super(rounds, callable);
        this.firstFireOffset = firstFireOffset;
    }

    @Override
    public final int offset(long resolution) {
        return firstFireOffset;
    }
}
