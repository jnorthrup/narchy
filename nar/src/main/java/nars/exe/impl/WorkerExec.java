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


	double granularity = 6;


	/**
	 * process sub-timeslice divisor
	 * TODO auto-calculate
	 */
	private long subCycleNS;

	/**
	 * value of 1 means it shares 1/N of the current work. >1 means it will take on more proportionally more-than-fair share of work, which might reduce jitter at expense of responsive
	 */
	float workResponsibility =
		//1f;
		//1.5f;
		//1.618f;
		2f;


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

		subCycleNS = Math.round(((double)(threadWorkTimePerCycle * concurrency())) / granularity);
	}

	@Override
	protected Supplier<Worker> loop() {
		return WorkPlayLoop::new;
	}

	@Override
	public synchronized void synch() {
		if (this.exe.running() == 0) {
			in.drain(this::executeNow); //initialize
		}
	}

	//final Consumer execNow = this::executeNow;
//    final MessagePassingQueue.Consumer executeNow = WorkerExec.this::executeNow;

	private final class WorkPlayLoop implements ThreadedExec.Worker, BooleanSupplier /* kontinue */ {


		final Random rng;

		private final AtomicBoolean alive = new AtomicBoolean(true);

		private long deadline = Long.MIN_VALUE;


		WorkPlayLoop() {

			rng = new XoRoShiRo128PlusRandom((31L * System.identityHashCode(this)) + nanoTime());
		}

		@Override
		public void run() {

			do {


				long worked = work(workResponsibility);

				long cycleTimeRemain = cycleIdealNS - worked;
				if (cycleTimeRemain > 0)
					play(cycleTimeRemain);

				sleep();

			} while (alive.getOpaque());
		}

		protected long work(float responsibility) {

			long workStart = nanoTime();


			int batchSize = -1;
			Object next;
			while ((next = in.poll()) != null) {

				executeNow(next);

				if (batchSize == -1) {
					//initialization once for subsequent attempts
					int available; //estimate
					if ((available = in.size()) <= 0)
						return 0;

					batchSize = //Util.lerp(throttle,
						//available, /* all of it if low throttle. this allows most threads to remains asleep while one awake thread takes care of it all */
						Math.max(1, (int) Math.ceil(((responsibility * available) / workGranularity)));

				} else if (--batchSize == 0)
					break; //enough

			} //while (!queueSafe((available=in.size())));

			long workEnd = nanoTime();
			return workEnd - workStart;

		}

		static final double maxOverUtilization = 1.5;

		private void play(long playTime) {
			if (subCycleNS <= 0)
				return;

			AntistaticBag<How> H = nar.how;
			AntistaticBag<What> W = nar.what;

			long start = nanoTime(), until = start + playTime;

			int idle = 0;
			while (true) {
				How h = H.sample(rng);
				if (h != null && h.isOn()) {
					What w = W.sample(rng);
					if (w != null && w.isOn()) {
						if (play(w, h, until))
							idle = 0; //reset
					}
				}

				if (nanoTime() > until)
					break;

				if (idle > 0)
					Util.pauseSpin(idle++);

			}

		}

		private boolean play(What w, How h, long until) {
			boolean singleton = h.singleton();
			if (!(!singleton || h.busy.compareAndSet(false, true)))
				return false;

			float util = h._utilization;
			if (!Float.isFinite(util)) util = 1;

			long useNS = (util <= 1) ?
				Math.round(subCycleNS * (util + 1f) / 2) //less than subcycle but optimistically, more time than it might expect
				:
				Math.round(subCycleNS * (1 - (Util.min(util, maxOverUtilization) - 1))); //penalize up to a certain amount for previous over-utilization

			deadline = Math.min(until, nanoTime() + useNS);
			//try {
			h.runWhile(w, useNS, this);
			//} finally {
			if (singleton)
				h.busy.set(false);
			//}

			return true;

		}

		/**
		 * whether to continue iterating in the how when it calls this back
		 */
		@Override
		public final boolean getAsBoolean() {
			return nanoTime() < deadline;
		}

		void sleep() {
			long i = Math.round(WorkerExec.this.threadIdleTimePerCycle * (((double) concurrency()) / exe.maxThreads));
			if (i > 0) {
				Util.sleepNS(NapTimeNS);
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
