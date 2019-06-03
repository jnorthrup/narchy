package nars.table.temporal;

import jcog.WTF;
import jcog.data.list.FasterList;
import jcog.math.LongInterval;
import jcog.sort.FloatRank;
import jcog.sort.Top;
import jcog.tree.rtree.*;
import jcog.tree.rtree.split.LinearSplitLeaf;
import nars.NAL;
import nars.NAR;
import nars.Task;
import nars.control.op.Remember;
import nars.task.ProxyTask;
import nars.task.util.Answer;
import nars.task.util.Revision;
import nars.task.util.TaskRegion;
import nars.task.util.TimeRange;
import nars.time.Tense;
import nars.truth.Truth;
import nars.truth.polation.TruthProjection;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.lang.Float.NEGATIVE_INFINITY;

public class RTreeBeliefTable extends ConcurrentRTree<TaskRegion> implements TemporalBeliefTable {

    private static final float PRESENT_AND_FUTURE_BOOST_BELIEF =
            1.0f;
    //1.5f;
    private static final float PRESENT_AND_FUTURE_BOOST_GOAL =
            1.0f;
    //2f;

    private static final int MAX_TASKS_PER_LEAF = 3;

    /**
     * TODO tune
     */
    private static final Split SPLIT =
            new LinearSplitLeaf();
//              new QuadraticSplitLeaf();
//            new AxialSplitLeaf();  //AXIAL SPLIT IS PROBABLY BAD FOR THIS UNLESS A LEAF ENDS UP BEING SPLIT IN A CERTAIN WAY


    protected int capacity;


    public RTreeBeliefTable() {
        super(new RTree<>(RTreeBeliefModel.the));
    }

    /**
     * immediately returns false if space removed at least one as a result of the scan, ie. by removing
     * an encountered deleted task.
     */
    private static boolean findEvictable(Space<TaskRegion> tree, Node<TaskRegion> next, /*@Nullable Top<TaskRegion> closest, */Top<Task> weakest, Consumer<Leaf<TaskRegion>> mergeableLeaf) {
        if (next instanceof Leaf) {

            Leaf l = (Leaf) next;
            Object[] data = l.data;
            short s = l.size;
            for (int i = 0; i < s; i++) {
                Task x = (Task) data[i];
                if (x.isDeleted()) {
                    boolean removed = tree.remove(x);
                    assert (removed);
                    return false;
                }

                weakest.accept(x);
            }

            if (s >= 2)
                mergeableLeaf.accept(l);

        } else {
            return ((Branch<TaskRegion>) next).ANDlocal((bb) -> findEvictable(tree, bb, /*closest, */weakest, mergeableLeaf));
        }

        return true;
    }

    /**
     * TODO use the same heuristics as task strength
     */
    private static FloatRank<TaskRegion> regionWeakness(long now, float futureFactor, float dur) {


        float pastDiscount = 1.0f - (futureFactor - 1.0f);

        return (TaskRegion r, float min) -> {

            float y =
                    //(float)Math.log(1+r.meanTimeTo(now));
                    (1 + r.maxTimeTo(now)) / dur;

            if (y < min)
                return Float.NaN;

            float conf =
                    //(((float) r.coord(2, false)) + ((float) r.coord(2, true))) / 2;
                    Math.max(NAL.truth.TRUTH_EPSILON, r.confMin());

            y = y * (1 - conf);
            if (y < min)
                return Float.NaN;

            if (pastDiscount != 1 && r.end() < now)
                y *= pastDiscount;

            return (float) y;

//            long regionTimeDist = r.midTimeTo(when);
//
//            float timeDist = (regionTimeDist) / ((float) perceptDur);
//
//
//            float evi =
//                    c2wSafe((float) r.coord(true, 2));
//
//
//            float antivalue = 1f / (1f + evi);
//
//            if (PRESENT_AND_FUTURE_BOOST != 1 && r.end() >= when - perceptDur)
//                antivalue /= PRESENT_AND_FUTURE_BOOST;
//
//
//            return (float) ((antivalue) * (1 + timeDist));
        };
    }

    /**
     * returns true if at least one net task has been removed from the table.
     */
    /*@NotNull*/
    private static boolean compress(Space<TaskRegion> tree, FloatFunction<Task> taskStrength, FloatRank<TaskRegion> leafRegionWeakness, Remember remember) {

        Top<Leaf<TaskRegion>> mergeableLeaf = new Top<>((L, min1) ->
                leafRegionWeakness.rank((TaskRegion) L.bounds(), min1));

        Top<Task> weakest = new Top<>((t, min) -> -taskStrength.floatValueOf(t));

        return !findEvictable(tree, tree.root(), /*closest, */weakest, mergeableLeaf)
                ||
                mergeOrDelete(tree, weakest, mergeableLeaf, taskStrength, remember);
    }

    private static boolean mergeOrDelete(Space<TaskRegion> treeRW,
                                         Top<Task> weakest,
                                         Top<Leaf<TaskRegion>> mergeableLeaf,
                                         FloatFunction<Task> taskStrength,
                                         Remember r) {

        Task W = weakest.the;

        float valueEvictWeakest = -taskStrength.floatValueOf(W);

        double valueMergeLeaf = NEGATIVE_INFINITY;
        Pair<Task, TruthProjection> AB;
        if (!mergeableLeaf.isEmpty()) {
            Leaf<TaskRegion> ab = mergeableLeaf.get();
            AB = Revision.merge(r.nar, true, 2, Arrays.copyOf(ab.data, ab.size)); //HACK type adaptation
            if (AB != null) {
                float mergeValue = taskStrength.floatValueOf(AB.getOne());
                double mergeCost = AB.getTwo().sumOfFloat(t ->
                    (!t.valid()) ? 0 : taskStrength.floatValueOf(t.task()) //0 for a task not included in revision
                );
                valueMergeLeaf = (float) (+mergeValue - mergeCost);
            }
        } else {
            AB = null;
        }

        if (valueMergeLeaf > valueEvictWeakest) {
            //merge leaf
            Task m = AB.getOne();
            TruthProjection merge = AB.getTwo();
            TemporalBeliefTable.budget(merge, m);
            merge.forEachTask(treeRW::remove);
            if (treeRW.add(m)) {
                r.remember(m);
            } //else: possibly already contained the merger?
            return true;
        } else {
            //evict weakest
            if (treeRW.remove(W)) {
                r.forget(W);
                return true;
            } else {

//            Spatialization model = ((RTree) treeRW).model;
//            HyperRegion ww = model.bounds(W);
//            List<Node<TaskRegion>> containingNodes = treeRW.root().streamNodesRecursively().filter((Node n) -> {
//                return n.contains(W, ww, model);
//            }).sorted(Comparators.byFloatFunction( w -> (float)w.bounds().cost())).collect(toList());
//            if (!containingNodes.isEmpty()) {
//
//                HyperRegion tb = treeRW.root().bounds();
//                System.out.println("tree bounds=" + tb);
//                for (Node n : containingNodes) {
//                    System.out.println("\t" + n + " bounds contained: " + tb.contains(n.bounds()));
//                }
//                treeRW.remove(W);
//            }
                throw new
                        WTF();
                //UnsupportedOperationException();
            }
        }

    }

    @Override
    public final boolean isEmpty() {
        return super.isEmpty();
    }

    @Override
    public final int taskCount() {
        return size();
    }


    @Override
    public final void match(Answer a) {

        HyperIterator.iterate(this,
                a::temporalDistanceFn,
                x -> a.tryAccept((Task) x)
        );
    }

    @Override
    public void setTaskCapacity(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public void remember(Remember r) {

        if (r.input.isEternal())
            return;

        if (capacity == 0)
            return;

        /** buffer removal handling until outside of the locked section */


        Task input;
        if (r.input instanceof ProxyTask) {
            //dont store TaskProxy's
            input = ((ProxyTask) r.input).the();
            if (input == null)
                throw new WTF();
        } else {
            input = r.input;
        }

//        if (r.input instanceof SpecialTruthAndOccurrenceTask) {
//            //dont do this for SpecialTermTask coming from Image belief table
//            input = ((TaskProxy) r.input).the();
//        } else if (r.input instanceof SpecialTermTask) {
//            //ex: Image belief table proxy
//            input = ((TaskProxy) r.input).the();
//        } else {
//            input = r.input;
//        }


        /** TODO only enter write lock after deciding insertion is necessary (not merged with existing)
         *    subclass RInsertion to RConcurrentInsertion, storing Stamped Lock lock value along with it */
        TaskInsertion insertion = write((Function<Space<TaskRegion>,TaskInsertion>) (treeRW -> {
            TaskInsertion ii = (TaskInsertion) treeRW.insert(input);
            if (ii.added()) {
                ensureCapacity(treeRW, input.isBelief() /* else Goal*/, r);
            }
            return ii;
        }));

        Task existing = (Task) insertion.mergedWith;
        if (existing != null && existing != input) {
            r.merge(existing);
            onReject(input);
        } else {
            if (!input.isDeleted()) {
                r.remember(input);
                onRemember(input);
            } else {
                r.forget(input);
                onReject(input);
            }
        }


    }

    protected void onReject(Task input) {
        /* optional: implement in subclasses */
    }

    protected void onRemember(Task input) {
        /* optional: implement in subclasses */

    }

    private boolean ensureCapacity(Space<TaskRegion> treeRW, boolean beliefOrGoal, Remember remember) {

        FloatFunction<Task> taskStrength = null;
        FloatRank<TaskRegion> leafRegionWeakness = null;
        int e = 0, cap;
        long atStart;
        while (treeRW.size() > (cap = capacity)) {
            if (taskStrength == null) {
                atStart = remember.nar.time();
                int tableDur = Tense.occToDT(tableDur(atStart));
                int dur =
                        //nar.dur();
                        tableDur;
                taskStrength = taskStrength(beliefOrGoal, atStart, dur, tableDur);
                leafRegionWeakness = regionWeakness(atStart, beliefOrGoal ? PRESENT_AND_FUTURE_BOOST_BELIEF : PRESENT_AND_FUTURE_BOOST_GOAL, tableDur);
            }

            if (!compress(treeRW,  /** only limit by inputRegion on first iter */
                    taskStrength, leafRegionWeakness,
                    remember))
                return false;

            e++;
            assert (e < cap);
        }

        return true;
    }

    /**
     * decides the value of keeping a task, used in compression decision
     */
    protected FloatRank<Task> taskStrength(boolean beliefOrGoal, long now, int narDur, int tableDur) {
        //return FloatRank.the(t->t.evi(now, dur));
        return Answer.temporalTaskStrength(now - narDur / 2, now + narDur / 2, tableDur);

//        return Answer.taskStrengthWithFutureBoost(now, now - dur,
//                beliefOrGoal ? PRESENT_AND_FUTURE_BOOST_BELIEF : PRESENT_AND_FUTURE_BOOST_GOAL,
//                dur
//        );
    }

    /**
     * this is the range as a radius surrounding present moment and optionally further subdivided
     * to represent half inside the super-duration, half outside the super-duration
     */
    @Override
    public long tableDur(long now) {
        TaskRegion root = bounds();
        if (root == null)
            return 1;
        else {
            //return 1 + (root == null ? 0 : root.range() / NAL.TEMPORAL_BELIEF_TABLE_DUR_DIVISOR);
            return 1 + Math.max(Math.abs(now - root.start()), Math.abs(now - root.end())) / NAL.TEMPORAL_BELIEF_TABLE_DUR_DIVISOR;
        }
    }


    @Override
    public Stream<? extends Task> taskStream() {
        return stream().map(TaskRegion::task);
    }

    @Override
    public Task[] taskArray() {
        int s = size();
        if (s == 0) {
            return Task.EmptyArray;
        } else {
            FasterList<Task> l = new FasterList(s);
            forEachTask(l::add);
            return l.toArrayRecycled(Task[]::new);
        }
    }

    @Override
    public void clear() {
        write(t -> {
            if (!t.isEmpty()) {
                t.forEach(r -> ((Task) r).delete());
                t.clear();
            }
        });
    }

    @Override
    public void whileEach(Predicate<? super Task> each) {
        intersectsWhile(root().bounds(), TaskRegion.asTask(each));
    }

    @Override
    public void whileEach(long minT, long maxT, Predicate<? super Task> each) {
        intersectsWhile(new TimeRange(minT, maxT), TaskRegion.asTask(each));
    }

    @Override
    public void forEachTask(long minT, long maxT, Consumer<? super Task> x) {
        if (minT == Long.MIN_VALUE && maxT == Long.MAX_VALUE) {
            forEach(TaskRegion.asTask(x));
        } else {
            whileEach(minT, maxT, (t) -> {
                x.accept(t);
                return true;
            });
        }
    }

    @Override
    public void forEachTask(Consumer<? super Task> each) {
        forEach(t -> each.accept((Task) t));
    }

    @Override
    public boolean removeTask(Task x, boolean delete) {
        if (x.isEternal())
            return false;

        if (remove(x)) {
            if (delete)
                x.delete();
            return true;
        }
        return false;
    }

    public void print(PrintStream out) {
        forEachTask(t -> out.println(t.toString(true)));
        stats().print(out);
    }

    public int capacity() {
        return capacity;
    }

    /**
     * bounds of the entire table
     */
    @Nullable
    public TaskRegion bounds() {
        return (TaskRegion) root().bounds();
    }

    private static final class RTreeBeliefModel extends Spatialization<TaskRegion> {

        static final Spatialization<TaskRegion> the = new RTreeBeliefModel();

        private RTreeBeliefModel() {
            super((t -> t), RTreeBeliefTable.SPLIT,
                    RTreeBeliefTable.MAX_TASKS_PER_LEAF);
        }

        @Override
        public RInsertion<TaskRegion> insertion(TaskRegion t) {
            return new TaskInsertion(t);
        }

        @Override
        public final TaskRegion bounds(TaskRegion taskRegion) {
            return taskRegion;
        }

        @Override
        public Leaf<TaskRegion> newLeaf(int capacity) {
            return new Leaf<>(new TaskRegion[capacity]);
        }

        @Nullable
        @Override
        public TaskRegion merge(TaskRegion existing, TaskRegion incoming) {

            Task ee = (Task) existing;
            Task ii = (Task) incoming;

            Task m = null;
            if (ee.equals(ii))
                m = ee;
            else {
                if (Arrays.equals(ee.stamp(), ii.stamp())) {
                    Truth et = ee.truth(), it = ii.truth();

                    if (et.equals(it)) {
                        if (ee.term().equals(ii.term())) {
                            if (ee.containsRaw((LongInterval) ii)) {
                                m = ee;
                            } else if (ii.containsRaw((LongInterval) ee)) {
                                m = ii;
                            }
                        }
                    }

                    //does this produce inconsistent results because conf bounds change?

//                        if (Util.equals(et.freq(), it.freq(), Param.truth.TRUTH_EPSILON)) {
//                            float ete = et.conf(), ite = it.conf();
//                            if (ete >= ite && ee.containsRaw((LongInterval)ii)) {
//                                m = ee;
//                            } else if (ite >= ete && ii.containsRaw((LongInterval)ee)) {
//                                m = ii;
//                            }
//                        }

                }
            }
            return m;
        }

        public boolean mergeCanStretch() {
            return true;
        }


    }

    private static class TaskInsertion extends RInsertion<TaskRegion> {

        @Nullable TaskRegion mergedWith = null;

        public TaskInsertion(TaskRegion t) {
            super(t, RTreeBeliefModel.the);
        }

        @Override
        public void mergeIdentity() {
            mergedWith = x;
        }

        @Nullable
        @Override
        public TaskRegion merge(TaskRegion y) {
            TaskRegion m = super.merge(y);
            if (m!=null)
                mergedWith = m;

            return m;
        }
    }

//    /** TODO */
//    public static class EternalizingRTreeBeliefTable extends RTreeBeliefTable {
//
//        final TruthAccumulator ete = new TruthAccumulator();
//        private final boolean beliefOrGoal;
//
//        public EternalizingRTreeBeliefTable(boolean beliefOrGoal) {
//            this.beliefOrGoal = beliefOrGoal;
//
//        }
//
//        @Override
//        public void match(Answer m) {
//            super.match(m);
//            if (m.template!=null) {
//                Truth t = ete.peekAverage();
//                if (t != null) {
//
//
//                    Task tt = new NALTask(m.template, beliefOrGoal ? BELIEF : GOAL, t,
//                            m.nar.time(), m.time.start, m.time.end,
//                            m.nar.evidence() //TODO hold rolling evidence buffer
//                    ).pri(m.nar);
//
////                System.out.println(tt);
//                    m.tryAccept(tt);
//                }
//            } else {
//                if (Param.DEBUG)
//                    throw new WTF("null template"); //HACK
//            }
//        }
//
//        @Override
//        protected void remember(Remember r, Task input) {
//            super.remember(r, input);
//
//            {
//
//                @Nullable TaskRegion bounds = bounds();
//                if (bounds==null)
//                    return;
//
//                int cap = capacity();
//                float e = TruthIntegration.evi(input) * (((float) input.range()) / bounds.range() );
//                float ee = TruthFunctions.eternalize(e);
//                float ce = w2cSafe(ee);
//                if (ce > Param.TRUTH_EPSILON) {
//                    ete.addAt(input.freq(), ce);
//                }
//            }
//        }
//    }


//    private final static class ExpandingScan extends CachedTopN<TaskRegion> implements Predicate<TaskRegion> {
//
//        private final Predicate<Task> filter;
//        private final int minResults, attempts;
//        int attemptsRemain;
//
//
//        ExpandingScan(int minResults, int maxResults, FloatFunction<TaskRegion> strongestTask, int maxTries) {
//            this(minResults, maxResults, strongestTask, maxTries, null);
//        }
//
//
//        ExpandingScan(int minResults, int maxResults, FloatFunction<TaskRegion> strongestTask, int maxTries, Predicate<Task> filter) {
//            super(maxResults, strongestTask);
//            this.minResults = minResults;
//            this.attempts = maxTries;
//            this.filter = filter;
//        }
//
//        @Override
//        public boolean accept(TaskRegion taskRegion) {
//
//            return (!(taskRegion instanceof Task)) || super.accept(taskRegion);
//        }
//
//        @Override
//        public boolean test(TaskRegion x) {
//            accept(x);
//            return --attemptsRemain > 0;
//        }
//
//        @Override
//        public boolean valid(TaskRegion x) {
//            return ((!(x instanceof Task)) || (filter == null || filter.test((Task) x)));
//        }
//
//        boolean continueScan(TimeRange t) {
//            return size() < minResults && attemptsRemain > 0;
//        }
//
//        /**
//         * TODO add a Random argument so it can decide randomly whether to scan the left or right zone first.
//         * order matters because the quality limit may terminate it.
//         * however maybe the quality can be specified in terms that are compared
//         * only after the pair has been scanned making the order irrelevant.
//         */
//        ExpandingScan scan(RTreeBeliefTable table, long _start, long _end) {
//
//            /* whether eternal is the time bounds */
//            boolean eternal = _start == ETERNAL;
//
//
//            this.attemptsRemain = attempts;
//
//            int s = table.size();
//            if (s == 0)
//                return this;
//
//            /* if eternal is being calculated, include up to the maximum number of truthpolated terms.
//                otherwise limit by the Leaf capacity */
//            if ((!eternal && s <= COMPLETE_SCAN_SIZE_THRESHOLD) || (eternal && s <= Answer.TASK_LIMIT)) {
//                table.forEach /*forEachOptimistic*/(this::accept);
//                //TODO this might be faster to add directly then sort the results after
//                //eliminating need for the Cache map
//                return this;
//            }
//
//            TaskRegion bounds = (TaskRegion) (table.root().bounds());
//
//            long boundsStart = bounds.start();
//            long boundsEnd = bounds.end();
//            if (boundsEnd == XTERNAL || boundsEnd < boundsStart) {
//                throw WTF();
//            }
//
//            int ss = s / COMPLETE_SCAN_SIZE_THRESHOLD;
//
//            long scanStart, scanEnd;
//            int confDivisions, timeDivisions;
//            if (!eternal) {
//
//                scanStart = Math.min(boundsEnd, Math.max(boundsStart, _start));
//                scanEnd = Math.max(boundsStart, Math.min(boundsEnd, _end));
//
//
//                timeDivisions = Math.max(1, Math.min(SCAN_TIME_OCTAVES_MAX, ss));
//                confDivisions = Math.max(1, Math.min(SCAN_CONF_OCTAVES_MAX,
//                        ss / Util.sqr(1 + timeDivisions)));
//            } else {
//                scanStart = boundsStart;
//                scanEnd = boundsEnd;
//
//                confDivisions = Math.max(1, Math.min(SCAN_TIME_OCTAVES_MAX /* yes TIME here, ie. the axes are switched */,
//                        Math.max(1, ss - minResults)));
//                timeDivisions = 1;
//            }
//
//            long expand = Math.max(1, (
//                    Math.round(((double) (boundsEnd - boundsStart)) / (1 << (timeDivisions))))
//            );
//
//
//            long mid = (scanStart + scanEnd) / 2;
//            long leftStart = scanStart, leftMid = mid, rightMid = mid, rightEnd = scanEnd;
//            boolean leftComplete = false, rightComplete = false;
//
//
//            TimeRange ll = confDivisions > 1 ? new TimeConfRange() : new TimeRange();
//            TimeRange rr = confDivisions > 1 ? new TimeConfRange() : new TimeRange();
//
//            float maxConf = bounds.confMax();
//            float minConf = bounds.confMin();
//
//            int FATAL_LIMIT = s * 2;
//            int count = 0;
//            boolean done = false;
//            do {
//
//                float cMax, cDelta, cMin;
//                if (confDivisions == 1) {
//                    cMax = 1;
//                    cMin = 0;
//                    cDelta = 0;
//                } else {
//                    cMax = maxConf;
//                    cDelta =
//                            Math.max((maxConf - minConf) / Math.min(s, confDivisions), Param.TRUTH_EPSILON);
//                    cMin = maxConf - cDelta;
//                }
//
//                for (int cLayer = 0;
//                     cLayer < confDivisions && !(done = !continueScan(ll.setAt(leftStart, rightEnd)));
//                     cLayer++, cMax -= cDelta, cMin -= cDelta) {
//
//
//                    TimeRange lll;
//                    if (!leftComplete) {
//                        if (confDivisions > 1)
//                            ((TimeConfRange) ll).setAt(leftStart, leftMid, cMin, cMax);
//                        else
//                            ll.setAt(leftStart, leftMid);
//
//                        lll = ll;
//                    } else {
//                        lll = null;
//                    }
//
//                    TimeRange rrr;
//                    if (!rightComplete && !(leftStart == rightMid && leftMid == rightEnd)) {
//                        if (confDivisions > 1)
//                            ((TimeConfRange) rr).setAt(rightMid, rightEnd, cMin, cMax);
//                        else
//                            rr.setAt(rightMid, rightEnd);
//                        rrr = rr;
//                    } else {
//                        rrr = null;
//                    }
//
//                    if (lll != null || rrr != null) {
//                        table.read /*readOptimistic*/((Space<TaskRegion> tree) -> {
//                            if (lll != null)
//                                tree.whileEachIntersecting(lll, this);
//                            if (rrr != null)
//                                tree.whileEachIntersecting(rrr, this);
//                        });
//                    }
//
//                    if (count++ == FATAL_LIMIT) {
//                        throw new RuntimeException("livelock in rtree scan");
//                    }
//                }
//
//                if (done)
//                    break;
//
//
//                long ls0 = leftStart;
//                leftComplete |= (ls0 == (leftStart = Math.max(boundsStart, leftStart - expand - 1)));
//
//                if (leftComplete && rightComplete) break;
//
//                long rs0 = rightEnd;
//                rightComplete |= (rs0 == (rightEnd = Math.min(boundsEnd, rightEnd + expand + 1)));
//
//                if (leftComplete && rightComplete) break;
//
//                leftMid = ls0 - 1;
//                rightMid = rs0 + 1;
//                expand *= 2;
//            } while (true);
//
//            return this;
//        }
//    }


}































