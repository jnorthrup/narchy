package jcog.exe.realtime;

import java.util.concurrent.TimeUnit;

public abstract class AbstractTimedFuture<T> implements TimedFuture<T> {

    int rounds;

    protected AbstractTimedFuture() {

    }

    AbstractTimedFuture(int rounds) {
        this();
        this.rounds = rounds;
    }

    @Override
    public Status state() {
        return rounds-- <= 0 ? Status.READY : Status.PENDING;
    }


    @Override
    abstract public int offset(long resolution);


    @Override
    public long getDelay(TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int rounds() {
        return rounds;
    }


    @Override
    public boolean isPeriodic() {
        return false;
    }
}
