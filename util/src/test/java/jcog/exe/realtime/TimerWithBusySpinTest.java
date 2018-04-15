package jcog.exe.realtime;

public class TimerWithBusySpinTest extends AbstractTimerTest {

  @Override
  public HashedWheelTimer.WaitStrategy waitStrategy() {
    return HashedWheelTimer.WaitStrategy.BusySpinWait;
  }

}
