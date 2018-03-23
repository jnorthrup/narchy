package com.ifesdjeen.timer;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

class OneShotTimedFuture<T> extends CompletableFuture<T> implements TimedFuture<T> {

  private final Callable<T> callable;
  protected final AtomicInteger rounds = new AtomicInteger(); // rounds is only visible to one thread
  protected volatile Status      status;

  private final long initialDelay;

  public OneShotTimedFuture(int rounds, Callable<T> callable, long initialDelay) {
    this.rounds.set(rounds);
    this.status = Status.READY;
    this.callable = callable;
    this.initialDelay = initialDelay;
  }

  @Override
  public void decrement() {
    rounds.decrementAndGet();
  }

  @Override
  public boolean ready() {
    // Check for READY here would be redundant, since if it was cancelled it'd be removed before this check
    return rounds.get() == 0;
  }

  @Override
  public void reset() {
    throw new RuntimeException("One Shot Registrations can not be rescheduled");
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    this.status = Status.CANCELLED;
    return true;
  }

  @Override
  public boolean isCancelled() {
    return status == Status.CANCELLED;
  }

  @Override
  public int getOffset() {
    throw new RuntimeException("One Shot Registration can not be rescheduled");
  }

  @Override
  public boolean isCancelAfterUse() {
    return true;
  }

  @Override
  public long getDelay(TimeUnit unit) {
    return initialDelay;
  }

  @Override
  public int rounds() {
    return rounds.get();
  }

  @Override
  public void run() {
    try {
      this.complete(callable.call());
    } catch (Exception e) {
      this.completeExceptionally(e);
    }
  }

}
