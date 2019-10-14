package nars.exe.impl;

import jcog.Util;
import jcog.exe.Exe;
import jcog.random.XoRoShiRo128PlusRandom;
import nars.attention.What;
import nars.derive.Deriver;
import nars.derive.DeriverExecutor;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static java.lang.System.nanoTime;

public class WorkerExec extends ThreadedExec {


	/**
	 * value of 1 means it shares 1/N of the current work. >1 means it will take on more proportionally more-than-fair share of work, which might reduce jitter at expense of responsive
	 */
	float workResponsibility =
		Util.PHIf * 2;
	//1f;
	//1.5f;
	//2f;


	public WorkerExec(int threads) {
		super(threads);
	}

	public WorkerExec(int threads, boolean affinity) {
		super(threads, affinity);

		Exe.setExecutor(this);
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

//	final private FloatAveragedWindow queueSize = new FloatAveragedWindow(3,0.75f, 0).mode(FloatAveragedWindow.Mode.Mean);

//	@Override
//	protected float workDemand() {
//		double cautionSensitivity = 0.75; //0..1
//
//		int QS = in.size();
//		float QSavg = queueSize.valueOf(QS);
//
//		float demand = (float) Math.pow(QSavg / in.capacity(), 1 - cautionSensitivity);
//
//		//System.out.println(QS + "\t" + QSavg + " => " + demand);
//
//		return demand;
//	}


	private final class WorkPlayLoop implements ThreadedExec.Worker {


		final Random rng;
		private final AtomicBoolean alive = new AtomicBoolean(true);
		float updateDurs = 1;
		private transient DeriverExecutor.QueueDeriverExecutor dExe;
		private transient long nextUpdate;

		WorkPlayLoop() {
			rng = new XoRoShiRo128PlusRandom((31L * System.identityHashCode(this)) + nanoTime());
			nextUpdate = nar.time();
		}

		/**
		 * updated each system dur
		 */
		private void update() {
			long now = nar.time();
			if (now < nextUpdate) return;

			nextUpdate = now +
					1; //per-cycle
					//Math.round(updateDurs * nar.dur()); //per-dur

			if ((dExe = deriver()) != null)
				dExe.cycle();
		}

		@Override
		public void run() {

			while (alive.getOpaque()) {

				try {

					work(); //HACK

					update();

					What W = schedule.get(rng);

					long t = subCycleNS;
					if (W == null) {
						Util.sleepNS(t);
					} else {
						DeriverExecutor dExe = this.dExe;
						if (dExe == null) continue;

						dExe.next(W);

						long deadline = nanoTime() + t;

						//int cycles = 0;
						do {

							dExe.next();
						//		cycles++;

						} while (nanoTime() < deadline);
						//System.out.println(dExe.d.what + " " + cycles + " cyc");
					}

				} catch (Throwable t) {
					t.printStackTrace();
				}

			}
		}

		@Nullable
		DeriverExecutor.QueueDeriverExecutor deriver() {
			Deriver systemDeriver = WorkerExec.this.deriver;
			if (systemDeriver == null)
				return null;
			DeriverExecutor.QueueDeriverExecutor dExe = this.dExe;
			if (dExe == null || dExe.deriver != systemDeriver) {
				//deriver has changed; create new executor
				dExe = new DeriverExecutor.QueueDeriverExecutor(systemDeriver);
			}
			return dExe;
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
						Util.clamp((int) Math.ceil(workResponsibility / concurrency() * available), 1, available);

				} else if (--batchSize == 0)
					break; //enough

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


//	private final class WorkPlayLoop0 implements ThreadedExec.Worker {
//
//
//		final Random rng;
//		private final AtomicBoolean alive = new AtomicBoolean(true);
//
//		private DeriverExecutor.QueueDeriverExecutor dExe;
//
//		WorkPlayLoop0() {
//			rng = new XoRoShiRo128PlusRandom((31L * System.identityHashCode(this)) + nanoTime());
//		}
//
//
//		@Override
//		public void run() {
//
//			while (alive.getOpaque()) {
//
//				long workStart = nanoTime();
//
//				work();
//
//				long workEnd = nanoTime();
//
//				long worked = workEnd - workStart;
//
//				long playTime = threadWorkTimePerCycle - worked;
//				if (playTime > 0)
//					play(workEnd, playTime);
//
//				sleep();
//			}
//		}
//
//		protected void work() {
//
//			int batchSize = -1;
//			Object next;
//			while ((next = in.poll()) != null) {
//
//				executeNow(next);
//
//				if (batchSize == -1) {
//					//initialization once for subsequent attempts
//					int available; //estimate
//					if ((available = in.size()) <= 0)
//						break;
//
//					batchSize = //Util.lerp(throttle,
//						//available, /* all of it if low throttle. this allows most threads to remains asleep while one awake thread takes care of it all */
//						Util.clamp((int) Math.ceil(workResponsibility/concurrency() * available), 1, available);
//
//				} else if (--batchSize == 0)
//					break; //enough
//
//			}
//
//
//		}
//
//		private void play(long playStart, long playTime) {
//
//			DeriverExecutor.QueueDeriverExecutor dExe = this.dExe;
//			if (dExe == null)
//				return; //no deriver
//			Deriver systemDeriver = WorkerExec.this.deriver;
//			if (dExe.deriver!= systemDeriver) {
//				//deriver has changed; create new executor
//				dExe = new DeriverExecutor.QueueDeriverExecutor(systemDeriver);
//			}
//
//			AntistaticBag<What> W = nar.what;
//
//			Object[] ww = W.items();
//			int n = W.size();
//			n = Math.min(ww.length, n); //safety
//			if (n == 0)
//				return;
//
//			/** granularity=1 means no additional timeslice 'redundancy', >1 means finer timeslices for improved fairness at expense of throughput and overhead */
//			float whatGranularity = 1;
//			/** global concurrency, indicates a factor to inflate the time that this thread can visit each what */
//			int concurrency = concurrency();
//
//			dExe.nextCycle();
//
//			int idle = 0;
//			long end = playStart + playTime;
//			float mass = W.mass();
//			while (true) {
//
//				What w = (What) ww[n>1 ? rng.nextInt(n) : 0];
//				if (!w.isOn()) w = null;
//
//				long now = nanoTime();
//				if (now >= end)
//					break;
//
//				if (w != null) {
//
//					float p = w.priElseZero();
//					if (p > Float.MIN_NORMAL) {
//						double priNormalized = n > 1 && mass > Float.MIN_NORMAL ?
//							Util.unitize(p / mass) :
//							1f / n;
//
//						long useNS = Math.round(playTime * priNormalized * concurrency / whatGranularity);
//
//						dExe.next(w, now, Math.min(end-now, useNS));
//					}
//
//					idle = 0; //reset
//
//				} else {
//					Util.pauseSpin(++idle);
//				}
//
//			}
//
//		}
//
//
//
//
//		/** TODO improve and interleave naps with play */
//		void sleep() {
//			long i = WorkerExec.this.threadIdleTimePerCycle;
//			if (i > 0) {
//				Util.sleepNS(i);
//				//Util.sleepNSwhile(i, NapTimeNS, () -> queueSafe());
//			}
//		}
//
//		@Override
//		public void close() {
//			if (alive.compareAndSet(true, false)) {
////                //execute remaining tasks in callee's thread
////                schedule.removeIf(x -> {
////                    if (x != null)
////                        executeNow(x);
////                    return true;
////                });
//			}
//		}
//	}

}
