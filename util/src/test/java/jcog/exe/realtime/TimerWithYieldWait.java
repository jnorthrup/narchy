package jcog.exe.realtime;

public class TimerWithYieldWait extends AbstractTimerTest {
    @Override
    protected HashedWheelTimer.WaitStrategy waitStrategy() {
        return HashedWheelTimer.WaitStrategy.YieldingWait;
    }
}

