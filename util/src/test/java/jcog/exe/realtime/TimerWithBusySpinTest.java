package jcog.exe.realtime;

public class TimerWithBusySpinTest extends AbstractTimerTest {

  @Override
  protected HashedWheelTimer.WaitStrategy waitStrategy() {
    return HashedWheelTimer.WaitStrategy.BusySpinWait;
  }

}
