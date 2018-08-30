package nars.exe;

/**
 * decides mental activity
 * this first implementation is similiar to this
 * https:
 * https:
 * <p>
 * https:
 * http:
 * https:
 * <p>
 * https:
 */
public class Focus { //extends AtomicRoulette<Causable>

//    /** note: more granularity increases the potential dynamic range
//     * (ratio of high to low prioritization). */
//    protected static final int PRI_GRANULARITY = 512;
//    protected static final int TIME_GRANULARITY_ns = 250; //0.25uS
//
//    /**
//     * how quickly the iteration demand can grow from previous (max) values
//     */
//
//    static final float workGrowthRate = 1.01f;
//
//
//    private final Exec.Revaluator revaluator;
//
//
//    private final MutableInteger concurrency;
//
//    double timesliceNS = 1;
//    public double idleTimePerCycle = 0;


//    public Focus(NAR n, Exec.Revaluator r, MutableInteger concurrency) {
//        super(32, Causable[]::new);
//
//
//        this.concurrency = concurrency;
//
//        this.revaluator = r;
//
//        n.services.change.on((xa) -> {
//            Service<NAR> x = xa.getOne();
//            if (x instanceof Causable) {
//                Causable c = (Causable) x;
//                if (xa.getTwo())
//                    add(c);
//                else
//                    remove(c);
//            }
//        });
//
//        n.services().filter(x -> x instanceof Causable).forEach(x -> add((Causable) x));
//
//        n.onCycle(this::onCycle);
//    }


//    public void onCycle(NAR nar) {
//
//        commit(() -> update(nar));
//
//
//
//
//
//
//
//
//
//    }
//
//    @Override
//    protected void onAdd(Causable causable, int slot) {
//        causable.scheduledID = slot;
//    }
//
//    final static int WINDOW = 8;
//    private final long[] committed = new long[3];
//    private final LongObjectProcedure<int[]> commiter = (timeNS, iterAndWork) -> {
//        committed[0] = timeNS;
//        committed[1] = iterAndWork[0];
//        committed[2] = iterAndWork[1];
//    };
//
//
//
//    /**
//     * absolute expectation of value
//     */
//    protected float[] value = ArrayUtils.EMPTY_FLOAT_ARRAY;
//
//    /**
//     * short history of time (in nanoseconds) spent
//     */
//    public DescriptiveStatistics[] time = null;
//    /**
//     * short history of iter spent in the corresponding times
//     */
//    public DescriptiveStatistics[] iter = null;
//
//    public DescriptiveStatistics[] workDone = null;
//    /**
//     * cache for iter.getMean() and time.getMean()
//     */
//    double[] timePerWorkMean = null;
//    int[] sliceWork = new int[0];
//
//    final AtomicBoolean updating = new AtomicBoolean(false);
//
//    final AtomicMetalBitSet singletonBusy = new AtomicMetalBitSet();
//
//    protected void update(NAR nar) {
//
//        if (!updating.compareAndSet(false, true))
//            return;
//
//        try {
//            int n = choice.size();
//            if (n == 0)
//                return;
//
//            if (sliceWork.length != n)
//                realloc(n);
//
//            revaluator.update(nar);
//
//            double jiffy = nar.loop.jiffy.floatValue();
//            double throttle = nar.loop.throttle.floatValue();
//
//
//            double timePerSlice = this.timesliceNS = nar.loop.periodNS() * (jiffy * concurrency.intValue()) * throttle;
//            idleTimePerCycle = nar.loop.periodNS() * (1-throttle);
//
//            for (int i = 0; i < n; i++) {
//                Causable c = choice.get(i);
//                if (c == null)
//                    continue;
//
//                c.can.commit(commiter);
//
//
//                long timeNS = committed[0];
//                if (timeNS > 0) {
//
//                    DescriptiveStatistics time = this.time[i];
//                    time.addValue(timeNS);
//
//                    DescriptiveStatistics iter = this.iter[i];
//                    iter.addValue(committed[1]); //unused currently.  the PID handles the conversion from work to iterations entirely within the Cause
//
//                    DescriptiveStatistics done = this.workDone[i];
//                    done.addValue(committed[2]);
//
//                    this.timePerWorkMean[i] = Math.max(TIME_GRANULARITY_ns, time.getMean()/done.getMean());
//
//                    value[i] = (c.value());
//
//                } else {
//
//                    value[i] *= 0.9f;
//                }
//
//            }
//
//
//            float[] vRange = Util.minmaxsum(value);
//            float vMin = vRange[0];
//            float vMax = vRange[1];
//            float vSum = vRange[2];
//            if (vSum < Float.MIN_NORMAL)
//                vSum = 1;
//
//            for (int i = 0; i < n; i++) {
//                double vNorm = normalize(value[i], vMin, vMax)/vSum;
//
//                //double timePerIter = timeMean[i]/Math.max(0.5f, doneMean[i]);
//                double timePerWork = timePerWorkMean[i];
//                int workLimit;
//                if (!Double.isFinite(timePerWork)) {
//                    workLimit = 1;
//                } else {
//                    workLimit = Math.max(1,
//                        //(int) Math.ceil(Math.min(doneMost * IterGrowthRate, timePerSlice / timePerIter))
//                        (int) Math.floor(timePerSlice / timePerWork * workGrowthRate)
//                    );
//                }
//
//                int pri = (int) Util.clampI((PRI_GRANULARITY * vNorm), 1, PRI_GRANULARITY);
//
//                sliceWork[i] = workLimit;
//                if (-1 == priGetAndSet(i, pri)) {
//                    singletonBusy.compareAndSet(i, true, false); //awake
//                }
//                //System.out.println(this.choice.get(i) + " " + pri + " x " + iterLimit);
//            }
//
//        } finally {
//            updating.set(false);
//        }
//    }
//
//    private void realloc(int n) {
//
//
//        time = new DescriptiveStatistics[n];
//        timePerWorkMean = new double[n];
//        iter = new DescriptiveStatistics[n];
//        workDone = new DescriptiveStatistics[n];
//
//
//        for (int i = 0; i < n; i++) {
//            time[i] = new DescriptiveStatistics(WINDOW);
//            iter[i] = new DescriptiveStatistics(WINDOW);
//            workDone[i] = new DescriptiveStatistics(WINDOW);
//        }
//
//
//        value = new float[n];
//
//        sliceWork = new int[n];
//    }


//    public boolean tryRun(int x) {
//        if (singletonBusy.get(x))
//            return false;
//
//        @Nullable Causable cx = this.choice.getSafe(x);
//        if (cx == null)
//            return false;
//
//
//
//
//        /** temporarily withold priority */
//
//        boolean singleton = cx.singleton();
//        int pri;
//        if (singleton) {
//            if (!singletonBusy.compareAndSet(x, false, true))
//                return false;
//
//            pri = priGetAndSet(x, 0);
//        } else {
//            pri = pri(x);
//        }
//
//
//
//
//
//
//
//
//        int completed = -1;
//        try {
//
//            completed = cx.run(nar, this.sliceWork[x]);
//        } finally {
//            if (singleton) {
//
//                if (completed >= 0) {
//                    priGetAndSetIfEquals(x, 0, pri);
//                    singletonBusy.clear(x); //will be uncleared next cycle
//                } else {
//                    priGetAndSet(x, -1); //sleep until next cycle
//                }
//
//
//
//            } else {
//                if (completed < 0) {
//                    priGetAndSet(x, 0);
//                }
//            }
//        }
//        return true;
//    }


}
































