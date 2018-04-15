package jcog.exe.realtime;

public class TimerWithYieldWait extends AbstractTimerTest {
    @Override
    public HashedWheelTimer.WaitStrategy waitStrategy() {
        return HashedWheelTimer.WaitStrategy.YieldingWait;
    }
}

