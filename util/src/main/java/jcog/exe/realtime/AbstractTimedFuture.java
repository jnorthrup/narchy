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
        if (rounds--<=0) {
            return Status.READY;
        }
        return Status.PENDING;
    }

    @Override
    public void reset(long resolution, int wheels) {
        throw new RuntimeException("One Shot Registrations can not be rescheduled");
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
