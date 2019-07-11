package jcog.exe.realtime;

import org.junit.jupiter.api.Tag;

@Tag("slow")
public class TimerWithBusySpinConcurrentTest extends AbstractTimerTest {

    @Override
    protected HashedWheelTimer.WaitStrategy waitStrategy() {
        return HashedWheelTimer.WaitStrategy.BusySpinWait;
    }

    @Override
    protected HashedWheelTimer.WheelModel model(long resolution, int wheels) {
        return new MetalConcurrentQueueWheelModel(wheels, 128, resolution);
    }
}
