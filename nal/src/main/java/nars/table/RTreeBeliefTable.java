package nars.table;

import com.google.common.collect.Iterators;
import jcog.data.SimpleIntSet;
import jcog.math.CachedFloatFunction;
import jcog.sort.Top;
import jcog.sort.Top2;
import jcog.sort.TopN;
import jcog.tree.rtree.*;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.TaskConcept;
import nars.link.Tasklinks;
import nars.task.NALTask;
import nars.task.Revision;
import nars.task.Tasked;
import nars.task.signal.SignalTask;
import nars.task.util.TaskRegion;
import nars.task.util.TimeRange;
import nars.term.Term;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectBooleanHashMap;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static nars.table.TemporalBeliefTable.temporalTaskPriority;
import static nars.time.Tense.ETERNAL;
import static nars.truth.TruthFunctions.c2wSafe;

public class RTreeBeliefTable implements TemporalBeliefTable {

    /**
     * max fraction of the fully capacity table to compute in a single truthpolation
     */
    static final float SCAN_QUALITY =
            1f;
    //0.5f;

    /**
     * max allowed truths to be truthpolated in one test
     */
    static final int TRUTHPOLATION_LIMIT = 8;

    public static final float PRESENT_AND_FUTURE_BOOST = 2f;

    static final int SCAN_DIVISIONS = 5;

    public static final int MIN_TASKS_PER_LEAF = 2;
    public static final int MAX_TASKS_PER_LEAF = 4;
    public static final Spatialization.DefaultSplits SPLIT =
            Spatialization.DefaultSplits.AXIAL; //Spatialization.DefaultSplits.LINEAR; //<- probably doesnt work here


    private int capacity;

    final Space<TaskRegion> tree;

    @Override
    public void update(SignalTask task, Runnable change) {
        ((ConcurrentRTree<TaskRegion>) tree).write(treeRW -> {

            boolean removed = treeRW.remove(task);
            if (!removed) {
                return;
            }

            change.run();

            if (!task.isDeleted()) {
                boolean added = treeRW.add(task);
            }
        });
    }

    public RTreeBeliefTable() {
        tree = new ConcurrentRTree<>(new RTree<>(RTreeBeliefModel.the));
    }

//    private static final class TopDeleteVictims extends TopN<TaskRegion> {
//
//        private final float inputStrength;
//
//        public TopDeleteVictims(int count, FloatFunction<TaskRegion> weakestTask, float inputStrength) {
//            super(new TaskRegion[count], weakestTask);
//            this.inputStrength = inputStrength;
//        }
//
//        @Override
//        public int add(TaskRegion element, float elementRank, FloatFunction<TaskRegion> cmp) {
//            if (elementRank > inputStrength)
//                return -1;
//            return super.add(element, elementRank, cmp);
//        }
//    }


    @Override
    public Truth truth(long start, long end, EternalTable eternal, int dur) {

        assert (end >= start);

        final Task ete = eternal != null ? eternal.strongest() : null;

        int s = size();
        x:
        if (s > 0) {

            if (start == ETERNAL) {
                TaskRegion r = ((TaskRegion) tree.root().bounds());
                if (r == null)
                    break x;
                //return ete != null ? ete.truth() : null;

                start = r.start();
                end = r.end();
            }

            FloatFunction<Task> ts = taskStrength(start, end);
            FloatFunction<TaskRegion> strongestTask =
                    new CachedFloatFunction<>(t -> +ts.floatValueOf((Task) t));


            int maxTruths = TRUTHPOLATION_LIMIT;

            int maxTries = (int) Math.max(1, Math.ceil(capacity * SCAN_QUALITY));
            maxTries = Math.min(s * 2 /* in case the same task is encountered twice HACK*/,
                    maxTries);

            //scan
            ScanFilter tt = new ScanFilter(new TaskRegion[maxTruths], strongestTask, maxTries);

            scan(tt, start - dur, end + dur, RTreeBeliefTable.ONLY_NEED_ONE_AFTER_THAT_SCANNED_RANGE_THANKS);

            if (!tt.isEmpty()) {
                return Param.truth(ete, start, end, dur, tt);
            }
        }

        return ete != null ? ete.truth() : null;

    }

//    /**
//     * timerange spanned by entries in this table
//     */
//    public float timeRange() {
//        if (tree.isEmpty())
//            return 0f;
//        return (float) tree.root().region().range(0);
//    }

    @Override
    public Task match(long start, long end, @Nullable Term template, NAR nar) {

        int s = size();
        if (s == 0) //quick exit
            return null;

        if (start == ETERNAL) start = end = nar.time();
        assert (end >= start);

        FloatFunction<Task> ts =
                (template != null && template.isTemporal()) ?
                        taskStrength(template, start, end) :
                        taskStrength(start, end);

        FloatFunction<TaskRegion> strongestTask =
                new CachedFloatFunction<>(t -> +ts.floatValueOf((Task) t));

        int maxTries = (int) Math.max(1, Math.ceil(capacity * SCAN_QUALITY));
        ScanFilter tt = new ScanFilter(new TaskRegion[2], strongestTask, maxTries);
        scan(tt, start, end, ONLY_NEED_ONE_AFTER_THAT_SCANNED_RANGE_THANKS);

        switch (tt.size()) {

            case 0:
                return null;

            case 1:
                return tt.first().task();

            default:
                Task a = tt.first().task();
                Task b = tt.last().task();

                if (template != null) {
                    //choose if either one (but not both or neither) matches template's time
                    boolean at = (a.term().equals(template));
                    boolean bt = (b.term().equals(template));
                    if (at && !bt)
                        return a;
                    else if (bt && !at)
                        return b;
                }

                //otherwise interpolate
                Task c = Revision.merge(a, b, start, nar);
                if (c != null) {

                    if (c == a) //c.equals(a))
                        return a;
                    if (c == b) //c.equals(b))
                        return b;

                    int dur = nar.dur();
                    if (c.evi(start, end, dur) > a.evi(start, end, dur))
                        return c;
                }

                return a;


        }
    }

    /**
     * TODO add a Random argument so it can decide randomly whether to scan the left or right zone first.
     * order matters because the quality limit may terminate it.
     * however maybe the quality can be specified in terms that are compared
     * only after the pair has been scanned making the order irrelevant.
     */
    private void scan(ScanFilter update, long _start, long _end, BiPredicate<ScanFilter, TimeRange> continueScanning) {


        ((ConcurrentRTree<TaskRegion>) tree).readOptimistic((Space<TaskRegion> tree) -> {

            update.clear(); //in case of optimisticRead, if tried twice

            int s = tree.size();
            if (s == 0)
                return;
            if (s == 1) {
                tree.forEach(update::add);
                return;
            }


            TaskRegion bounds = (TaskRegion) (tree.root().bounds());

            long boundsStart = bounds.start();
            long boundsEnd = bounds.end();

            long start = Math.min(boundsEnd, Math.max(boundsStart, _start));
            long end = Math.max(boundsStart, Math.min(boundsEnd, _end));

            float maxTimeRange = boundsEnd - boundsStart;
            long expand = Math.max(1,
                    (
                            //Math.min(
                            //(end-start)/2,
                            Math.round(maxTimeRange / (1 << (1 + SCAN_DIVISIONS)))));


            //TODO use a polynomial or exponential scan expansion, to start narrow and grow wider faster


            long mid = (start + end) / 2;
            long leftStart = start, leftMid = mid, rightMid = mid, rightEnd = end;
            boolean leftComplete = false, rightComplete = false;
            //TODO float complete and use this as the metric for limiting with scan quality parameter
            TimeRange r = //recycled
                    new TimeRange();
            //new TimeRangeUniqueNodes(); <- not safe yet because task's taskregion is re-used for branch/leaf bounds and this could cause a false positive in the bloom filter test
            do {

                //random scan order
//                if (leftComplete || rightComplete || rng.nextBoolean()) {
                if (!leftComplete)
                    tree.whileEachIntersecting(r.set(leftStart, leftMid), update);
                if (!rightComplete && !(leftStart == rightMid && leftMid == rightEnd))
                    tree.whileEachIntersecting(r.set(rightMid, rightEnd), update);
//                } else {
//                    if (!rightComplete)
//                        tree.whileEachIntersecting(r.set(rightMid, rightEnd), update);
//                    if (!leftComplete && !(leftStart == rightMid && leftMid == rightEnd))
//                        tree.whileEachIntersecting(r.set(leftStart, leftMid), update);
//                }

                if (/*attempts[0] >= maxTries || */!continueScanning.test(update, r.set(leftStart, rightEnd)))
                    break;

                leftMid = leftStart - 1;
                long ls0 = leftStart;
                leftStart = Math.max(boundsStart, leftStart - expand - 1);
                if (ls0 == leftStart) { //no change
                    leftComplete = true;
                }

                rightMid = rightEnd + 1;
                long rs0 = rightEnd;
                rightEnd = Math.min(boundsEnd, rightEnd + expand + 1);
                if (rs0 == rightEnd) {
                    rightComplete = true;
                }

                if (leftComplete && rightComplete)
                    break;

                expand *= 2;
            } while (true);

        });
    }

    @Override
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public void add(Task x, TaskConcept c, NAR n) {

        assert (capacity > 0);

        ObjectBooleanHashMap<Task> changes = new ObjectBooleanHashMap<>(1);
        ((ConcurrentRTree<TaskRegion>) tree).write(treeRW -> {

//            ensureCapacity(treeRW, null, changes, n);
//
//            if (!changes.getIfAbsent(x, true))
//                return; //this can theoretically happen if a duplicate is inserted that the pre-compress just extracted. this catches it

            if (treeRW.add(x)) {
                changes.put(x, true);
                ensureCapacity(treeRW, x, changes, n);
            }
        });


        changes.forEachKeyValue((task, addOrRemove) -> {
            if (addOrRemove) {
                //full activation
                float pri = x.pri();
                if (pri == pri) {
                    Tasklinks.linkTask(x, pri, c, n);
                }
            }
        });

        if (x.isDeleted()) {
            Task xisting = x.meta("merge");
            if (xisting != null) {
                float incoming = x.priElseZero();
                Tasklinks.linkTask(xisting, incoming, c, n); //use incoming priority but the existing task instance
            }
        }
    }

    boolean ensureCapacity(Space<TaskRegion> treeRW, @Nullable Task inputRegion, ObjectBooleanHashMap<Task> changes, NAR nar) {
        int cap = this.capacity;
        int size = treeRW.size();
        if (size <= cap)
            return true;

        //int dur = 1 + (int) (tableDur());

        long now = nar.time();
        int perceptDur = nar.dur();
        FloatFunction<Task> taskStrength =
                new CachedFloatFunction(
                        //taskStrength(now-dur/2, now+dur/2, dur);
                        taskStrengthWithFutureBoost(now, PRESENT_AND_FUTURE_BOOST, now, perceptDur)
                );

        int e = 0;
        while (treeRW.size() > cap) {
            if (!compress(treeRW, e == 0 ? inputRegion : null /** only limit by inputRegion first */, taskStrength, changes, cap,
                    now, (long) (1 + tableDur()), perceptDur, nar))
                return false;
            e++;
        }

        assert (treeRW.size() <= cap);
        return true;
    }

    /**
     * results in at least 1 less task being present in the table
     * assumes called with writeLock
     * returns false if the input was rejected as too weak
     */
    /*@NotNull*/
    private boolean compress(Space<TaskRegion> tree, @Nullable Task inputRegion, FloatFunction<Task> taskStrength, ObjectBooleanHashMap<Task> changes, int cap, long now, long tableDur, int perceptDur, NAR nar) {

        FloatFunction<TaskRegion> weakestTask = (t -> -taskStrength.floatValueOf((Task) t));

        float inputStrength = inputRegion != null ? taskStrength.floatValueOf(inputRegion) : Float.POSITIVE_INFINITY;

        FloatFunction<TaskRegion> leafRegionWeakness =
                /*new CachedFloatFunction*/(regionWeakness(now, tableDur, perceptDur));
        FloatFunction<Leaf<TaskRegion>> leafWeakness =
                L -> leafRegionWeakness.floatValueOf((TaskRegion) L.bounds());

        Top<Leaf<TaskRegion>> mergeVictim = new Top(leafWeakness);

        //0.
        //int startSize = tree.size();
        //if (startSize <= cap) return true; //compressed thanks to another thread


        //1.
        findEvictable(tree, tree.root(), mergeVictim);
        if (tree.size() <= cap)
            return true; //done, due to a removal of deleted items while finding eviction candiates

        //2.
        @Nullable Leaf<TaskRegion> toMerge = mergeVictim.the;
        if (toMerge != null) {
            if (mergeOrDelete(tree, toMerge, taskStrength, inputStrength, weakestTask, changes, nar)) {
                if (tree.size() <= cap) return true;
            }
        }

        //3.
        /*
                    Object[] ld = l.data;

            // remove any deleted tasks while scanning for victims
            for (int i = 0; i < size; i++) {
                TaskRegion t = (TaskRegion) ld[i];
//                if (t.task().isDeleted()) {
//                    //TODO this may disrupt the iteration being conducted, it may need to be deferred until after
//                    //boolean deleted = tree.remove(t); //already has write lock so just use non-async methods
//
//                } else {
                deleteVictims.accept(t);
//                }
            }
         */
//        for (TaskRegion d : deleteVictim.list) {
//            if (d != null) {
//                if (tree.remove(d)) {
//                    //TODO forward to a neighbor?
//                    Task dt = d.task();
//                    dt.delete();
//                    changes.put(dt, false);
//                    if (tree.size() <= cap)
//                        return true;
//                }
//            } else {
//                break;
//            }
//        }

        return false; //?? could be a problem if it reaches here
    }


    private static boolean mergeOrDelete(Space<TaskRegion> treeRW, Leaf<TaskRegion> l, FloatFunction<Task> taskStrength, float inputStrength, FloatFunction<TaskRegion> weakestTasks, ObjectBooleanHashMap<Task> changes, NAR nar) {
        short s = l.size;
        assert (s > 0);

        TaskRegion a, b;
        if (s > 2) {
            Top2<TaskRegion> w = new Top2<>(weakestTasks);
            l.forEach(w::add);
            a = w.a;
            b = w.b;
        } else {
            a = l.get(0);
            b = l.get(1);
        }

        assert (a != null);
        Task at = a.task();
        boolean aAlreadyDeleted = at.isDeleted();
        if (!aAlreadyDeleted)
            at.delete();
        treeRW.remove(at);
        changes.put(at, false);


        if (b != null) {
            Task bt = b.task();

            if (bt.isDeleted()) {
                treeRW.remove(bt);
                changes.put(bt, false);
                return true;
            } else {
                at.meta("@", bt);
            }

            if (aAlreadyDeleted)
                return true;

            Task c = Revision.merge(at, bt, nar.time(), nar);
            if (c != null && !c.equals(a) && !c.equals(b)) {

                boolean allowMerge;

                if (inputStrength != inputStrength) {
                    allowMerge = true;
                } else {
                    float strengthRemoved = taskStrength.floatValueOf(at) + taskStrength.floatValueOf(bt);
                    float strengthAdded = taskStrength.floatValueOf(c) + inputStrength;
                    allowMerge = strengthAdded >= strengthRemoved;
                }

                if (allowMerge) {

                    treeRW.remove(bt);
                    changes.put(bt, false);

                    ((NALTask) at).delete(c); //forward
                    ((NALTask) bt).delete(c); //forward

                    changes.put(c, true); //but dont add it now, because it may be for another concept
                    treeRW.add(c);
                    return true;
                } else {
                    //merge result is not strong enough
                }

            }
        }

//        if (aAlreadyDeleted)
//            return true;

//        //merge impossible, delete a
//        if (b != null)
//            ((NALTask) at).delete(b.task()); //forward
//        else
//            ((NALTask) at).delete();

        return true;
    }


    static boolean findEvictable(Space<TaskRegion> tree, Node<TaskRegion, ?> next, Top<Leaf<TaskRegion>> mergeVictims) {
        if (next instanceof Leaf) {

            Leaf l = (Leaf) next;
            for (Object _x : l.data) {
                if (_x == null)
                    break;
                TaskRegion x = (TaskRegion) _x;
                if (((Task) x).isDeleted()) {
                    //found a deleted task in the leaf, we need look no further
                    boolean removed = tree.remove(x);
//                    if (!removed) {
//                        tree.remove(x);
//                    }
                    assert (removed);
                    return false;
                }
            }

            mergeVictims.accept(l);

        } else { //if (next instanceof Branch)

            Branch b = (Branch) next;
            int size = b.size();
            Node<TaskRegion, ?>[] ww = b.child;
            for (int i = 0; i < size; i++) {
                if (!findEvictable(tree, ww[i], mergeVictims))
                    return false; //done
            }
        }

        return true;
    }


    /**
     * TODO use the same heuristics as task strength
     */
    private static FloatFunction<TaskRegion> regionWeakness(long when, long tableDur, long perceptDur) {

        return (TaskRegion r) -> {

            long regionTime =
                    //r.furthestTimeTo(when);
                    r.myNearestTimeTo(when);

            float timeDist = (Math.abs(when - regionTime)) / ((float) perceptDur);

            if (r.start() >= when - perceptDur)
                timeDist /= PRESENT_AND_FUTURE_BOOST; //shrink the apparent time if it's present and future

            float evi =
                    c2wSafe((float) r.coord(true, 2)); //max
//            float dt =
//                    (float) r.range(1);
            //(float)r.coord(false, 2); //min
            //(float) (r.coord(true, 2) + r.coord(false, 2)) / 2f; //avg

            //float antiConf = 1f - conf;
            float antivalue = 1f / (1f + evi);
            //float span = (float)(1 + r.range(0)/dur); //span becomes less important the further away, more fair to short near-term tasks

            return (float) ((antivalue) * (1 + timeDist));
        };
    }

    FloatFunction<Task> taskStrength(long start, long end) {
        int tableDur = 1 + (int) (tableDur()); //TODO HACK should be 'long' the belief table could span a long time
        return (Task x) -> temporalTaskPriority(x, start, end, tableDur);
    }

    public double tableDur() {
        HyperRegion root = tree.root().bounds();
        if (root == null)
            return 0;
        else
            return root.rangeIfFinite(0, 1);
    }

    FloatFunction<Task> taskStrengthWithFutureBoost(long now, float presentAndFutureBoost, long when, int perceptDur) {
        int tableDur = 1 + (int) (tableDur());
        return (Task x) -> {
            if (x.isDeleted())
                return Float.NEGATIVE_INFINITY;

            //boost for present and future
            return (!x.isBefore(now - perceptDur) ? presentAndFutureBoost : 1f) * temporalTaskPriority(x, when, when, tableDur);
        };
    }

    FloatFunction<Task> taskStrength(@Nullable Term template, long start, long end) {
        if (template == null || !template.isTemporal() || template.equals(template.root())) { //TODO this result can be cached for the entire table once knowing what term it stores
            return taskStrength(start, end);
        } else {
            int tableDur = 1 + (int) (tableDur());
            return (Task x) ->
                    temporalTaskPriority(x, start, end, tableDur) / (1f + Revision.dtDiff(template, x.term()));
        }
    }


//    protected Task find(/*@NotNull*/ TaskRegion t) {
//        final Task[] found = {null};
//        tree.intersecting(t, (x) -> {
//            if (x.equals(t)) {
//                Task xt = x.task();
//                if (xt != null) {
//                    found[0] = xt;
//                    return false; //finished
//                }
//            }
//            return true;
//        });
//        return found[0];
//    }


    @Override
    public int capacity() {
        return capacity;
    }

    @Override
    public int size() {
        return tree.size();
    }

    @Override
    public Iterator<Task> iterator() {
        return Iterators.transform(tree.iterator(), Tasked::task);
    }

    @Override
    public Stream<Task> stream() {
        return tree.stream().map(TaskRegion::task);
    }

    static Predicate<TaskRegion> scanWhile(Predicate<? super Task> each) {
        return (t) -> {
            Task tt = t.task();
            return tt == null || each.test(tt);
        };
    }

    @Override
    public void whileEach(Predicate<? super Task> each) {
        tree.whileEachIntersecting(tree.root().bounds(), scanWhile(each));
    }

    @Override
    public void whileEach(long minT, long maxT, Predicate<? super Task> each) {
        tree.whileEachIntersecting(new TimeRange(minT, maxT), scanWhile(each));
    }

    @Override
    public void forEachTask(Consumer<? super Task> each) {
        tree.forEach(t -> {
//            Task tt = t.task();
//            if (tt != null)
            each.accept((Task) t);
        });
    }

    @Override
    public boolean removeTask(Task x) {
        return tree.remove(x);
    }


    @Override
    public void clear() {
        tree.clear();
    }

    public void print(PrintStream out) {
        forEachTask(t -> out.println(t.toString(true)));
        tree.stats().print(out);
    }

    private static final BiPredicate<ScanFilter, TimeRange> ONLY_NEED_ONE_AFTER_THAT_SCANNED_RANGE_THANKS = (u, r) -> {
        return u.isEmpty(); //quit after even only one, now that an entire range has been scanned
    };

    private static final class RTreeBeliefModel extends Spatialization<TaskRegion> {


        public static final Spatialization<TaskRegion> the = new RTreeBeliefModel();


        public RTreeBeliefModel() {
            super((t -> t), RTreeBeliefTable.SPLIT, RTreeBeliefTable.MIN_TASKS_PER_LEAF, RTreeBeliefTable.MAX_TASKS_PER_LEAF);
        }

        @Override
        public final HyperRegion bounds(TaskRegion taskRegion) {
            return taskRegion;
        }

        //        @Override
//        public Node<TaskRegion, TaskRegion> newLeaf() {
//            return new BeliefLeaf(max);
//        }

        @Override
        protected void merge(TaskRegion existing, TaskRegion incoming) {
            Task i = incoming.task();
            Task e = existing.task();
            ((NALTask) e).causeMerge(i);
            i.delete();
            i.meta("merge", e);
        }

    }

    private final static class ScanFilter extends TopN<TaskRegion> implements Predicate<TaskRegion> {
        int attemptsRemain;

        final SimpleIntSet tried = new SimpleIntSet(16);

        public ScanFilter(TaskRegion[] taskRegions, FloatFunction<TaskRegion> strongestTask, int maxTries) {
            super(taskRegions, strongestTask);
            this.attemptsRemain = maxTries;
        }


        @Override
        public boolean test(TaskRegion x) {
            //identity test, relatively fast
            main: if (!tried.add(x.hashCode())) {
                int s = size;
                TaskRegion[] l = list;
                for (int i = 0; i < s; i++) {
                    if (l[i] == x) {
                        break main; //duplicate
                    }
                }
            } else {
                add(x);
            }
            return --attemptsRemain > 0;
        }
    }

//    private static class TimeRangeUniqueNodes extends TimeRange {
//        //Set<HyperRegion> visited = new HashSet();
//        final LongBitsetBloomFilter bpp = new LongBitsetBloomFilter(64, 0.01f);
//
//        public TimeRangeUniqueNodes() {
//
//        }
//
//        @Override
//        public boolean intersects(HyperRegion x) {
//            //if (x instanceof Task) {
//                int h = x.hashCode();
//                byte[] hh = intToByteArrayLE(h);
//                if (bpp.test(hh))
//                    return false;
//
//                if (super.intersects(x)) {
//                    if (x instanceof Task || contains(x)) //only record visited if intersecting a task or containing an entire node
//                        bpp.add(hh);
//                    return true;
//                }
//                return false;
//            /*} else
//                return super.intersects(x);*/
//        }
//    }

//    private static class BeliefLeaf extends Leaf<TaskRegion> {
//        public BeliefLeaf(int max) {
//            super(new TaskRegion[max]);
//        }
//
//
////        @Override
////        public boolean contains(TaskRegion t, Spatialization<TaskRegion> model) {
////            if (region == null)
////                return false;
//////            if (!region.contains(t))
//////                return false;
////
////            Task incomingTask = t.task();
////            TaskRegion[] data = this.data;
////            final int s = size;
////            for (int i = 0; i < s; i++) {
////                TaskRegion d = data[i];
////                if (d == t) {
////                    return true;
////                }
////                if (d.contains(t)) {
////                    if (d.equals(t)) {
////                        model.merge(d, t);
////                        return true;
////                    } else {
////                        NALTask existingTask = (NALTask) d.task();
////                        if (existingTask.term().equals(incomingTask.term())) {
////                            if (Stamp.equalsIgnoreCyclic(existingTask.stamp(), incomingTask.stamp())) {
////                                existingTask.causeMerge(incomingTask);
////                                existingTask.priMax(incomingTask.priElseZero());
////                                return true;
////                            }
////                        }
////                    }
////                }
////            }
////            return false;
////
////        }
//    }

}
