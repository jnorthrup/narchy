package nars.table.temporal;

import jcog.WTF;
import jcog.data.list.FasterList;
import jcog.sort.FloatRank;
import jcog.sort.Top;
import jcog.tree.rtree.*;
import jcog.tree.rtree.split.LinearSplitLeaf;
import nars.NAR;
import nars.Task;
import nars.control.op.Remember;
import nars.task.Revision;
import nars.task.TaskProxy;
import nars.task.signal.SignalTask;
import nars.task.util.Answer;
import nars.task.util.TaskRegion;
import nars.task.util.TimeRange;
import nars.time.Tense;
import nars.truth.polation.Projection;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.lang.Float.NEGATIVE_INFINITY;

public class RTreeBeliefTable extends ConcurrentRTree<TaskRegion> implements TemporalBeliefTable {

    private static final float PRESENT_AND_FUTURE_BOOST_BELIEF =
            1f;
            //1.5f;
    private static final float PRESENT_AND_FUTURE_BOOST_GOAL =
            1f;
            //2f;

    private static final int MAX_TASKS_PER_LEAF = 4;

    private static final Split SPLIT =
//            new AxialSplitLeaf() {
//                /* TODO tune */
//            };
              new LinearSplitLeaf() {
                /* TODO tune */
              };


    private static final int CURSOR_CAPACITY = 32;


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
            for (int i = 0, dataLength = l.size; i < dataLength; i++) {
                Task x = (Task) data[i];
                if (x.isDeleted()) {

                    boolean removed = tree.remove(x);

                    assert (removed);
                    return false;
                }

                weakest.accept(x);

//                if (closest != null)
//                    closest.accept(x);
            }

            if (l.size >= 2)
                mergeableLeaf.accept(l);

        } else {

            Branch b = (Branch) next;
            Node[] bd = b.data;
            for (int i = 0, dataLength = b.size; i < dataLength; i++) {
                Node bb = bd[i];
                if (!findEvictable(tree, bb, /*closest, */weakest, mergeableLeaf))
                    return false;
            }
        }

        return true;
    }

    /**
     * TODO use the same heuristics as task strength
     */
    private static FloatRank<TaskRegion> regionWeakness(long now, float futureFactor) {


        float pastDiscount = 1f - (futureFactor - 1f);

        return (TaskRegion r, float min) -> {

//            double y;
//            double timeDist =
//                    Math.log(1 + r.maxTimeTo(now)); //ensure that the number is small enough for when it gets returned as float
//            y = timeDist;
//            if (y < min) return Float.NaN;
            float y = 1;

            //r.midTimeTo(when);
            //r.maxTimeTo(when); //pessimistic, prevents wide-spanning taskregions from having an advantage over nearer narrower ones

            float conf = //(((float) r.coord(2, false)) + ((float) r.coord(2, true))) / 2;
                    ((float) r.coord(2, false));

            y = y * (1 - conf);
            //-Param.evi(c2wSafe(conf),  timeDist, perceptDur);
            if (y < min)
                return Float.NaN;

            if (pastDiscount!=1 && r.end() < now)
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

    //    private static Predicate<TaskRegion> scanWhile(Predicate<? super Task> each) {
//        return t -> {
//            Task tt = ((Task) t);
//            return tt.isDeleted() || each.test(tt);
//        };
//    }


    @Override
    @Deprecated
    public void update(SignalTask task, Runnable change) {
        write(treeRW -> {

            boolean removed = treeRW.remove(task);
            /*if (!removed)
                return;*/

            change.run();

            if (!task.isDeleted()) {
                boolean added = treeRW.add(task);
            }
        });
    }

    @Override
    public boolean isEmpty() {
        return super.isEmpty();
    }


    @Override
    public final void match(Answer a) {

        HyperIterator.iterate(this,
                RTreeBeliefTable.CURSOR_CAPACITY,
                a::temporalDistanceFn,
                n->a.tryAccept((Task)n)
        );
    }

    @Override
    public void setTaskCapacity(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public void add(Remember r, NAR n) {

        if (r.input.isEternal())
            return;

        if (capacity == 0)
            return;

        /** buffer removal handling until outside of the locked section */


        Task input;
        if (r.input instanceof TaskProxy) {
            //dont store TaskProxy's
            input = ((TaskProxy) r.input).the();
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


        /** inserted but not necessarily kept */
        write(treeRW -> {
            if (treeRW.add(input)) {
                ensureCapacity(treeRW, input.isBelief() /* else Goal*/, r, n);
            }
        });

        Task existing = RTreeBeliefModel.merged.get();
        if (existing != null && existing.equals(input)) {
            r.merge(existing, n);
            RTreeBeliefModel.merged.remove();
        } else {
            if (!input.isDeleted()) {
                r.remember(input);
                onRemember(input, n);
            } else {
                r.forget(input);
                onReject(input, n);
            }
        }


    }

    protected void onReject(Task input, NAR n) {
        /* optional: implement in subclasses */
    }

    protected void onRemember(Task input, NAR n) {
        /* optional: implement in subclasses */

    }

    private boolean ensureCapacity(Space<TaskRegion> treeRW, boolean beliefOrGoal, Remember remember, NAR
            nar) {

        FloatFunction<Task> taskStrength = null;
        FloatRank<TaskRegion> leafRegionWeakness = null;
        int e = 0, cap;
        long now = nar.time();
        while (treeRW.size() > (cap = capacity)) {
            if (taskStrength == null) {
                taskStrength = taskStrength(beliefOrGoal, now, Math.max(1,Tense.occToDT(tableDur())));
                leafRegionWeakness = regionWeakness(now, beliefOrGoal ? PRESENT_AND_FUTURE_BOOST_BELIEF : PRESENT_AND_FUTURE_BOOST_GOAL);
            }
            if (!compress(treeRW,  /** only limit by inputRegion on first iter */
                    taskStrength, leafRegionWeakness,
                    remember, nar))
                return false;
            e++;
            assert (e < cap);
        }

        return true;
    }

    /** decides the value of keeping a task, used in compression decision */
    protected FloatRank<Task> taskStrength(boolean beliefOrGoal, long now, int dur) {
        //return FloatRank.the(t->t.evi(now, dur));

        return Answer.beliefStrength(now-dur, now+dur);

//        return Answer.taskStrengthWithFutureBoost(now, now - dur,
//                beliefOrGoal ? PRESENT_AND_FUTURE_BOOST_BELIEF : PRESENT_AND_FUTURE_BOOST_GOAL,
//                dur
//        );
    }

    /**
     * returns true if at least one net task has been removed from the table.
     */
    /*@NotNull*/
    private static boolean compress(Space<TaskRegion> tree, FloatFunction<Task> taskStrength, FloatRank<TaskRegion> leafRegionWeakness, Remember
            remember, NAR nar) {


        FloatRank<Leaf<TaskRegion>> leafWeakness =
                (L, min) -> leafRegionWeakness.rank((TaskRegion) L.bounds(), min);

        Top<Leaf<TaskRegion>> mergeableLeaf = new Top<>(leafWeakness);

        FloatRank<Task> weakestTask = (t, min) -> -taskStrength.floatValueOf(t);


        Top<Task> weakest = new Top<>(weakestTask);

//        Top<TaskRegion> closest = input != null ? new Top<>(Answer.mergeability(input)) : null;


        if (!findEvictable(tree, tree.root(), /*closest, */weakest, mergeableLeaf))
            return true;


        //assert (tree.size() >= cap);


        return mergeOrDelete(tree, weakest, mergeableLeaf, taskStrength, remember, nar);


    }

    private static boolean mergeOrDelete(Space<TaskRegion> treeRW,
                                         Top<Task> weakest,
                                         Top<Leaf<TaskRegion>> mergeableLeaf,
                                         FloatFunction<Task> taskStrength,
                                         Remember r,
                                         NAR nar) {

        Task W = weakest.the;

        float valueEvictWeakest = -taskStrength.floatValueOf(W);

        float valueMergeLeaf = NEGATIVE_INFINITY;
        Pair<Task, Projection> AB;
        if (!mergeableLeaf.isEmpty()) {
            Leaf<TaskRegion> ab = mergeableLeaf.get();
            AB = Revision.merge(nar, true,
                    ab.size == ab.data.length ? ab.data : Arrays.copyOf(ab.data, ab.size)); //HACK type adaptation
            if (AB!=null) {
                valueMergeLeaf = (float) (
                        +taskStrength.floatValueOf(AB.getOne())
                        -AB.getTwo().sumOfFloat((Projection.TaskComponent tv)->
                                taskStrength.floatValueOf(tv.task()))
                );
            }
        } else {
            AB = null;
        }

        if (valueMergeLeaf > valueEvictWeakest) {
            //merge leaf
            Task m = AB.getOne();
            Projection merge = AB.getTwo();
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
            }
        }

        throw new UnsupportedOperationException();
    }


    /**
     * this is the range as a radius then further subdivided
     * to represent half inside the super-duration, half outside the super-duration
     */
    @Override public long tableDur() {
        TaskRegion root = bounds();
        return root == null ? 0 : 1 + root.range()/2/2;
    }


    @Override
    public Stream<? extends Task> streamTasks() {
        return stream().map(TaskRegion::task);
    }

    @Override
    public Task[] toArray() {
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
            //t.forEach(r -> ((Task)r).delete());
            t.clear();
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
        public final HyperRegion bounds(TaskRegion taskRegion) {
            return taskRegion;
        }

        @Override
        public Leaf<TaskRegion> newLeaf(int capacity) {
            return new Leaf<>(new TaskRegion[capacity]);
        }

        /**
         * HACK store merge notifications
         */
        @Deprecated
        final static ThreadLocal<Task> merged = new ThreadLocal();

        @Override
        protected void onMerge(TaskRegion existing, TaskRegion incoming) {
            merged.set((Task) existing);
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































