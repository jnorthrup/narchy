package nars.exe.impl;

import jcog.Util;
import jcog.exe.Exe;
import jcog.random.XoRoShiRo128PlusRandom;
import nars.attention.AntistaticBag;
import nars.attention.What;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static java.lang.System.nanoTime;

public class WorkerExec extends ThreadedExec {


	/**
	 * min # of whats per cycle, in total across all threads
	 */
	double granularity = 4;
	/**
	 * value of 1 means it shares 1/N of the current work. >1 means it will take on more proportionally more-than-fair share of work, which might reduce jitter at expense of responsive
	 */
	float workResponsibility =
		Util.PHIf * 2;
		//1f;
		//1.5f;
		//2f;
	/**
	 * process sub-timeslice divisor
	 * TODO auto-calculate
	 */
	private long subCycleNS;


	public WorkerExec(int threads) {
		super(threads);
	}

	public WorkerExec(int threads, boolean affinity) {
		super(threads, affinity);


		Exe.setExecutor(this);
	}

	@Override
	protected void update() {

		super.update();

		subCycleNS = (long) ((threadWorkTimePerCycle * concurrency()) / granularity);
	}

	@Override
	protected Supplier<Worker> loop() {
		return WorkPlayLoop::new;
	}

	@Override
	public synchronized void synch() {
		if (this.exe.size() == 0) {
			in.drain(this::executeNow); //initialize
		}
	}

	private final class WorkPlayLoop implements ThreadedExec.Worker {


		static final double maxOverUtilization = 1.5;
		final Random rng;
		private final AtomicBoolean alive = new AtomicBoolean(true);


		WorkPlayLoop() {

			rng = new XoRoShiRo128PlusRandom((31L * System.identityHashCode(this)) + nanoTime());
		}

		@Override
		public void run() {

			while (alive.getOpaque()) {

				long workStart = nanoTime();

				work();

				long workEnd = nanoTime();

				long worked = workEnd - workStart;

				long playTime = threadWorkTimePerCycle - worked;
				if (playTime > 0 && subCycleNS > 0)
					play(workEnd, playTime);

				sleep();
			}
		}

		protected void work() {

			int batchSize = -1;
			Object next;
			while ((next = in.poll()) != null) {

				executeNow(next);

				if (batchSize == -1) {
					//initialization once for subsequent attempts
					int available; //estimate
					if ((available = in.size()) <= 0)
						break;

					batchSize = //Util.lerp(throttle,
						//available, /* all of it if low throttle. this allows most threads to remains asleep while one awake thread takes care of it all */
						Util.clamp((int) Math.ceil(workResponsibility/concurrency() * available), 1, available);

				} else if (--batchSize == 0)
					break; //enough

			}


		}

		private void play(long playStart, long playTime) {

			AntistaticBag<What> W = nar.what;

			Object[] ww = W.items();
			int n = W.size();
			n = Math.min(ww.length, n); //safety
			if (n == 0)
				return;

			/** granularity=1 means no additional timeslice 'redundancy', >1 means finer timeslices for improved fairness at expense of throughput and overhead */
			float whatGranularity = 1;
			/** global concurrency, indicates a factor to inflate the time that this thread can visit each what */
			int concurrency = concurrency();


			int idle = 0;
			long end = playStart + playTime;
			float mass = W.mass();
			while (true) {

				What w = (What) ww[rng.nextInt(n)];
				if (!w.isOn()) w = null;

				long now = nanoTime();
				if (now >= end)
					break;

				if (w != null) {

					float p = w.priElseZero();
					if (p > Float.MIN_NORMAL) {
						double priNormalized = n > 1 && mass > Float.MIN_NORMAL ?
							Util.unitize(p / mass) :
							1f / n;

						long useNS = Math.round(playTime * priNormalized * concurrency / whatGranularity);

						w.next(now, Math.min(end-now, useNS));
					}

					idle = 0; //reset

				} else {
					Util.pauseSpin(++idle);
				}

			}

		}




		/** TODO improve and interleave naps with play */
		void sleep() {
			long i = WorkerExec.this.threadIdleTimePerCycle;
			if (i > 0) {
				Util.sleepNS(i);
				//Util.sleepNSwhile(i, NapTimeNS, () -> queueSafe());
			}
		}

		@Override
		public void close() {
			if (alive.compareAndSet(true, false)) {
//                //execute remaining tasks in callee's thread
//                schedule.removeIf(x -> {
//                    if (x != null)
//                        executeNow(x);
//                    return true;
//                });
			}
		}
	}

}
