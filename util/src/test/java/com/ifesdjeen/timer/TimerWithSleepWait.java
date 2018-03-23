package com.ifesdjeen.timer;

public class TimerWithSleepWait extends AbstractTimerTest {
  @Override
  public HashedWheelTimer.WaitStrategy waitStrategy() {
    return HashedWheelTimer.WaitStrategy.SleepWait;
  }
}
