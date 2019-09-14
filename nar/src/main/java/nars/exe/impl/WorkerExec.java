package nars.exe.impl;

import jcog.Util;
import jcog.exe.Exe;
import jcog.random.XoRoShiRo128PlusRandom;
import nars.attention.AntistaticBag;
import nars.attention.What;
import nars.control.How;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import static java.lang.System.nanoTime;

public class WorkerExec extends ThreadedExec {


	/**
	 * min # of play's per cycle, in total across all threads
	 */
	double granularity = 16;
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
		nar.how.commit(null);
		nar.what.commit(null);

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

	private final class WorkPlayLoop implements ThreadedExec.Worker, BooleanSupplier /* kontinue */ {


		static final double maxOverUtilization = 1.5;
		final Random rng;
		private final AtomicBoolean alive = new AtomicBoolean(true);
		private long deadline = Long.MIN_VALUE;

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

				long cycleTimeRemain = threadWorkTimePerCycle - worked;
				if (cycleTimeRemain > 0 && subCycleNS > 0)
					play(workEnd + cycleTimeRemain);

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

		private void play(long end) {

			AntistaticBag<How> H = nar.how;
			AntistaticBag<What> W = nar.what;

			int idle = 0;
			while (true) {

				What w;
				How h = H.sample(rng);
				if (h != null && h.isOn()) {
					w = W.sample(rng);
					if (!w.isOn()) w = null;
				} else
					w = null;

				long now = nanoTime();
				if (now >= end)
					break;

				if (w != null) {
					play(w, h, now, end);
					idle = 0; //reset
				} else {
					Util.pauseSpin(++idle);
				}

			}

		}

		private void play(What w, How h, long now, long end) {

			float util = h._utilization;
			if (!Float.isFinite(util)) util = 1;

			long useNS =
				(long) (subCycleNS * ((util <= 1) ?
					((util + 1f) / 2) //less than subcycle but optimistically, more time than it might expect
					:
					((1 - (Util.min(util, maxOverUtilization) - 1))))); //penalize up to a certain amount for previous over-utilization

			deadline = Math.min(end, now + useNS);

			h.runWhile(w, useNS, this);
		}

		/**
		 * whether to continue iterating in the how when it calls this back
		 */
		@Override
		public final boolean getAsBoolean() {
			return nanoTime() < deadline;
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
