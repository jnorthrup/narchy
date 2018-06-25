package jcog.exe.realtime;

import org.junit.jupiter.api.Tag;

@Tag("slow") public class TimerWithSleepWait extends AbstractTimerTest {
  @Override
  protected HashedWheelTimer.WaitStrategy waitStrategy() {
    return HashedWheelTimer.WaitStrategy.SleepWait;
  }
}
