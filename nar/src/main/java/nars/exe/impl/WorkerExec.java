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
	static final float workResponsibility = 1.0F;
	//1f;
	//1.5f;
	//2f;


	public WorkerExec(final int threads) {
		super(threads);
	}

	public WorkerExec(final int threads, final boolean affinity) {
		super(threads, affinity);

		Exe.setExecutor(this);
	}


	@Override
	protected Supplier<Worker> loop() {
		return ()->new WorkPlayLoop(nar);
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

		long loopNS_recorded_max = 5_000_000L; //initial guess at cycle per loop
		long naptime = 2_000_000L;


		final Random rng;
		private final AtomicBoolean alive = new AtomicBoolean(true);
		private final NAR nar;
		private final MpmcArrayQueue in;
		private transient DeriverExecutor deriver;
		@Deprecated
		private transient int concurrency;

		WorkPlayLoop(final NAR nar) {
			this.nar = nar;
			in = ((ThreadedExec)nar.exe).in;
			rng = new XoRoShiRo128PlusRandom((31L * (long) System.identityHashCode(this)) + nanoTime());
		}

		/**
		 * updated each system dur
		 */
		private DeriverExecutor deriver() {
            final DeriverExecutor d = this.deriver;
            final Deriver systemDeriver = nar.exe.deriver;
			return systemDeriver != null && (d == null || d.deriver != systemDeriver) ?
				(this.deriver = new DeriverExecutor.QueueDeriverExecutor(systemDeriver)) : //deriver has changed; create new executor
				d;
		}

		@Override
		public void run() {


			//Histogram loopTime = new Histogram(30_000, loopNS_recorded_max, 3);
			//loopTime.recordValue(loopNS_recorded_max);
            final FloatAveragedWindow loopTime = new FloatAveragedWindow(8, 0.5f, false).mode(FloatAveragedWindow.Mode.Mean);
			loopTime.fill((float) loopNS_recorded_max);

            final NARLoop loop = nar.loop;

			while (alive.getOpaque()) {

				concurrency = Math.max(1,nar.exe.concurrency());
                final long cycleTimeNS = loop.cycleTimeNS;

                final long cycleStart = System.nanoTime();

				work();

                long cycleRemaining = cycleTimeNS - (System.nanoTime() - cycleStart);
				if (cycleRemaining < 0L)
					continue; //worked until next morning so start again

                final boolean running = loop.isRunning();
                final float throttle = running ? loop.throttle.floatValue() : (float) 0;
				if (throttle < 1.0F) {
					cycleRemaining = sleep(throttle, cycleTimeNS, cycleStart, cycleRemaining);
					if (cycleRemaining < 0L || !running)
						continue; //slept all day
				}

                final DeriverExecutor d = deriver();
				if (d == null) continue;

                final PartBag<What> ww = nar.what;
                final int N = ww.size();
				if (N == 0) continue;

				final double meanLoopTimeNS = loopTime.mean(); //getMean();
				final int maxWhatLoops;
                final int minWhatLoops = 2;
				final int loopsPlanned;
				if (loopTime.mean() < 1.0) {
					//TODO this means it has been measuring empty loops, add some safety limit
					loopsPlanned = maxWhatLoops = minWhatLoops;
				} else {
					loopsPlanned = (int) ((double) cycleRemaining / meanLoopTimeNS);
					//increase what slicing
					final float whatGranularity = 1.0F;
					maxWhatLoops = minWhatLoops + Math.round((float) loopsPlanned / Math.max(1f, ((float) N * whatGranularity) / (float) concurrency)); //TODO tune
				}

				//StringBuilder y = new StringBuilder();

                int loopsRemain = loopsPlanned;

                final long beforePlay = System.nanoTime();

				while (loopsRemain > 0) {

                    final What w = ww.get(rng);
					if (w == null)  break;
                    final float p = w.priElseZero();
//					if (p < ScalarValue.EPSILON) continue; //HACK

                    final int loops = Math.min(loopsRemain, (int) Util.lerpSafe(p / ww.mass(), (float) minWhatLoops, (float) maxWhatLoops));
					loopsRemain -= loops;

					d.next(w, loops);

					//work();
				}

                final long playTimeNS = System.nanoTime() - beforePlay;
				//totalPlayTimeNS += playTimeNS;
                final int loopsRun = loopsPlanned - loopsRemain;
				if (loopsRun > 0)
					loopTime.next/*recordValue*/(/*Math.min(loopNS_recorded_max,*/ (((float)playTimeNS)/ (float) loopsRun));

			}
		}

		public long sleep(final float throttle, final long cycleTimeNS, final long cycleStart, long cycleRemaining) {
			//sleep at most until next cycle
            final long cycleSleepNS = Math.min(cycleRemaining, (long) (int) ((double) cycleTimeNS * (1.0 - (double) throttle)));
			Util.sleepNSwhile(cycleSleepNS, naptime, ()->{
				work();
				return true;
			});
            final long afterSleep = System.nanoTime();
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
					final int available; //estimate
					if ((available = in.size()) <= 0)
						break;

					batchSize = //Util.lerp(throttle,
						//available, /* all of it if low throttle. this allows most threads to remains asleep while one awake thread takes care of it all */
						Util.clamp((int) Math.ceil((double) (workResponsibility * (float) available)), 1, available);

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
