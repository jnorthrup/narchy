package jcog.exe.realtime;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.SAME_THREAD)
@Tag("slow") public class TimerWithYieldWait extends AbstractTimerTest {
    @Override
    protected HashedWheelTimer.WaitStrategy waitStrategy() {
        return HashedWheelTimer.WaitStrategy.YieldingWait;
    }
}

