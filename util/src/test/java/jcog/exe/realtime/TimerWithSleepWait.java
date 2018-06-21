package jcog.exe.realtime;

public class TimerWithSleepWait extends AbstractTimerTest {
  @Override
  protected HashedWheelTimer.WaitStrategy waitStrategy() {
    return HashedWheelTimer.WaitStrategy.SleepWait;
  }
}
