package nars.exe.impl;

import jcog.Util;
import jcog.exe.Exe;
import jcog.math.FloatAveragedWindow;
import jcog.random.XoRoShiRo128PlusRandom;
import nars.NAR;
import nars.attention.What;
import nars.control.PartBag;
import nars.derive.Deriver;
import nars.derive.DeriverExecutor;
import nars.exe.NARLoop;
import org.jctools.queues.MpmcArrayQueue;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static java.lang.System.nanoTime;

public class WorkerExec extends ThreadedExec {


	/**
	 * value of 1 means a worker will attempt all of the work.  this is safest option
	 * lesser values load balance across workers at expense of avg throughput
	 */
	static final float workResponsibility = 1;
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
		return () -> new WorkPlayLoop(nar);
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


	private static final class WorkPlayLoop implements ThreadedExec.Worker {

		final Random rng;
		private final AtomicBoolean alive = new AtomicBoolean(true);
		private final NAR nar;
		private final MpmcArrayQueue in;
		long loopNS_recorded_max = 5_000_000; //initial guess at cycle per loop
		long naptime = 2_000_000;
		private transient DeriverExecutor deriver;
		@Deprecated
		private transient int concurrency;

		FloatAveragedWindow loopTime = new FloatAveragedWindow(32, 0.5f, false).mode(FloatAveragedWindow.Mode.Mean);

		WorkPlayLoop(NAR nar) {
			this.nar = nar;
			in = ((ThreadedExec) nar.exe).in;
			rng = new XoRoShiRo128PlusRandom((31L * System.identityHashCode(this)) + nanoTime());
		}

		/**
		 * updated each system dur
		 */
		private DeriverExecutor deriver() {
			DeriverExecutor d = this.deriver;
			Deriver systemDeriver = nar.exe.deriver;
			return systemDeriver != null && (d == null || d.deriver != systemDeriver) ?
				(this.deriver = new DeriverExecutor.QueueDeriverExecutor(systemDeriver)) : //deriver has changed; create new executor
				d;
		}

		@Override
		public void run() {


			//Histogram loopTime = new Histogram(30_000, loopNS_recorded_max, 3);
			//loopTime.recordValue(loopNS_recorded_max);
			loopTime.fill(loopNS_recorded_max);

			NARLoop loop = nar.loop;

			while (alive.getOpaque()) {

				concurrency = Math.max(1, nar.exe.concurrency());
				long cycleTimeNS = loop.cycleTimeNS;

				long cycleStart = System.nanoTime();

				work();

				long cycleRemaining = cycleTimeNS - (System.nanoTime() - cycleStart);
				if (cycleRemaining < 0)
					continue; //worked until next morning so start again

				boolean running = loop.isRunning();
				float throttle = running ? loop.throttle.floatValue() : 0;
				if (throttle < 1) {
					cycleRemaining = sleep(throttle, cycleTimeNS, cycleStart, cycleRemaining);
					if (cycleRemaining < 0 || !running)
						continue; //slept all day
				}

				play(cycleRemaining);
			}
		}

		protected void play(long cycleRemaining) {
			DeriverExecutor d = deriver();
			if (d == null) return;

			PartBag<What> ww = nar.what;
			int N = ww.size();
			if (N == 0) return;

			float whatGranularity = 1; //increase what slicing

			double meanLoopTimeNS = loopTime.mean(); //getMean();
			int maxWhatLoops;
			int minWhatLoops = 2;
			int loopsPlanned;
			if (loopTime.mean() < 1) {
				//TODO this means it has been measuring empty loops, add some safety limit
				loopsPlanned = maxWhatLoops = minWhatLoops;
			} else {
				loopsPlanned = (int) (cycleRemaining / meanLoopTimeNS);
				maxWhatLoops = minWhatLoops + Math.round(loopsPlanned / Math.max(1f, (N * whatGranularity) / concurrency)); //TODO tune
			}

			//StringBuilder y = new StringBuilder();

			int loopsRemain = loopsPlanned;

			long beforePlay = System.nanoTime();

			while (loopsRemain > 0) {

				What w = ww.get(rng);
				if (w == null) break;
				float p = w.priElseZero();
//					if (p < ScalarValue.EPSILON) continue; //HACK

				int loops = Math.min(loopsRemain, (int) Util.lerpSafe(p / ww.mass(), minWhatLoops, maxWhatLoops));
				loopsRemain -= loops;

				d.next(w, loops);

				//work();
			}

			long playTimeNS = System.nanoTime() - beforePlay;
			//totalPlayTimeNS += playTimeNS;
			int loopsRun = loopsPlanned - loopsRemain;
			if (loopsRun > 0)
				loopTime.next/*recordValue*/(/*Math.min(loopNS_recorded_max,*/ (((float) playTimeNS) / loopsRun));


		}

		protected long sleep(float throttle, long cycleTimeNS, long cycleStart, long cycleRemaining) {
			//sleep at most until next cycle
			long cycleSleepNS = Math.min(cycleRemaining, (int) (cycleTimeNS * (1.0 - throttle)));
			Util.sleepNSwhile(cycleSleepNS, naptime, () -> {
				work();
				return true;
			});
			long afterSleep = System.nanoTime();
			cycleRemaining = cycleTimeNS - (afterSleep - cycleStart);
			return cycleRemaining;
		}

		protected void work() {

			int batchSize = -1;
			Object next;

			while ((next = in.poll()) != null) {

				nar.exe.executeNow(next);

				if (batchSize == -1) {
					//initialization once for subsequent attempts
					int available; //estimate
					if ((available = in.size()) <= 0)
						break;

					batchSize = //Util.lerp(throttle,
						//available, /* all of it if low throttle. this allows most threads to remains asleep while one awake thread takes care of it all */
						Util.clamp((int) Math.ceil(workResponsibility * available), 1, available);

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
