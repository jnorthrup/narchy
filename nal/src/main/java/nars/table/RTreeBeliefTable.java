package nars.table;

import jcog.list.FasterList;
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
import nars.task.signal.SignalTask;
import nars.task.util.TaskRegion;
import nars.task.util.TimeRange;
import nars.term.Term;
import nars.truth.PreciseTruth;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static nars.table.TemporalBeliefTable.temporalTaskPriority;
import static nars.time.Tense.ETERNAL;
import static nars.truth.TruthFunctions.c2wSafe;
import static nars.truth.TruthFunctions.w2cSafe;

public class RTreeBeliefTable extends ConcurrentRTree<TaskRegion> implements TemporalBeliefTable {

    /**
     * max fraction of the fully capacity table to compute in a single truthpolation
     */
    static final float SCAN_QUALITY =
            1f;
    //0.5f;

    /**
     * max allowed truths to be truthpolated in one test
     */
    static final int TRUTHPOLATION_LIMIT = 3;

    public static final float PRESENT_AND_FUTURE_BOOST = 2f;

    static final int SCAN_DIVISIONS = 4;

    public static final int MIN_TASKS_PER_LEAF = 3;
    public static final int MAX_TASKS_PER_LEAF = 4;
    public static final Split<TaskRegion> SPLIT =
            Spatialization.DefaultSplits.AXIAL.get(); //Spatialization.DefaultSplits.LINEAR; //<- probably doesnt work here


    private int capacity;


    @Override
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

    public RTreeBeliefTable() {
        super(new RTree<>(RTreeBeliefModel.the));
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
                TaskRegion r = ((TaskRegion) root().bounds());
                if (r == null)
                    break x;
                //return ete != null ? ete.truth() : null;

                start = r.start();
                end = r.end();
            }

            FloatFunction<Task> ts = taskStrength(start, end, dur);
            FloatFunction<TaskRegion> strongestTask =
                    new CachedFloatFunction<>(t -> +ts.floatValueOf((Task) t));


            int maxTruths = TRUTHPOLATION_LIMIT;

            int maxTries = (int) Math.max(1, Math.ceil(capacity * SCAN_QUALITY));
            maxTries = Math.min(s * 2 /* in case the same task is encountered twice HACK*/,
                    maxTries);

            //scan
            ScanFilter tt = new ScanFilter(new TaskRegion[maxTruths], strongestTask, maxTries);

            scan(tt, start - dur, end + dur);

            if (!tt.isEmpty()) {
                return Param.truth(ete, start, end, dur, tt);
//                PreciseTruth pt = Param.truth(null, start, end, dur, tt);
//                if (pt!=null && (ete == null || (pt.evi() >= ete.evi())))
//                    return pt;
            }
        }

        return ete != null ? ete.truth() : null;

    }

    @Override
    public boolean isEmpty() {
        return super.isEmpty();
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
    public Task match(long start, long end, @Nullable Term template, NAR nar, Predicate<Task> filter) {

        int s = size();
        if (s == 0) //quick exit
            return null;

        if (start == ETERNAL) start = end = nar.time();
        assert (end >= start);

        int dur = nar.dur();
        FloatFunction<Task> ts =
                (template != null && template.isTemporal()) ?
                        taskStrength(template, start, end, dur) :
                        taskStrength(start, end, dur);

        FloatFunction<TaskRegion> strongestTask =
                new CachedFloatFunction<>(t -> +ts.floatValueOf((Task) t));

        int maxTries = (int) Math.max(1, Math.ceil(capacity * SCAN_QUALITY));
        ScanFilter tt = new ScanFilter(new TaskRegion[2], strongestTask, maxTries, filter);
        scan(tt, start, end);

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
                Task c = Revision.merge(a, b, start, c2wSafe(nar.confMin.floatValue()) /* TODO */, nar);
                if (c != null) {

                    if (c == a) //c.equals(a))
                        return a;
                    if (c == b) //c.equals(b))
                        return b;

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
    private void scan(ScanFilter update, long _start, long _end) {


        read /*readOptimistic*/((Space<TaskRegion> tree) -> {

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
            TimeRange r = new TimeRange(); //recycled
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

                if (/*attempts[0] >= maxTries || */!update.continueScan(r.set(leftStart, rightEnd)))
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
    public boolean add(Task x, TaskConcept c, NAR n) {

        if (capacity() == 0)
            return false;

        float incoming = x.priElseZero();

        List<Task> added = new FasterList<>(2);
        write(treeRW -> {
            if (treeRW.add(x)) {
                if (!x.isDeleted()) {
                    added.add(x);
                    ensureCapacity(treeRW, x, added::add, n);
                }
            }
        });


        for (int i = 0, addedSize = added.size(); i < addedSize; i++) {
            Task y = added.get(i);
            //full activation
            float pri = y.pri();
            if (pri == pri) {
                Tasklinks.linkTask(y, pri, c, n);
            }
        }

        if (x.isDeleted()) {
            Task xisting = x.meta("merge");
            if (xisting != null) {
                Tasklinks.linkTask(xisting, incoming, c, n); //use incoming priority but the existing task instance
            }
            return false;
        } else {
            return true;
        }

    }

    boolean ensureCapacity(Space<TaskRegion> treeRW, @Nullable Task inputRegion, Consumer<Task> added, NAR nar) {
        int cap = this.capacity;
        int size = treeRW.size();
        if (size <= cap)
            return true;

        //int dur = 1 + (int) (tableDur());

        long now = nar.time();
        int perceptDur = nar.dur();
        FloatFunction<Task> taskStrength =
                new CachedFloatFunction<>(
                        //taskStrength(now-dur/2, now+dur/2, dur);
                        taskStrengthWithFutureBoost(now, PRESENT_AND_FUTURE_BOOST, now, perceptDur)
                );

        int e = 0;
        while (treeRW.size() > cap) {
            if (!compress(treeRW, e == 0 ? inputRegion : null /** only limit by inputRegion first */, taskStrength, added, cap,
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
    private boolean compress(Space<TaskRegion> tree, @Nullable Task inputRegion, FloatFunction<Task> taskStrength, Consumer<Task> added, int cap, long now, long tableDur, int perceptDur, NAR nar) {

        FloatFunction<TaskRegion> weakestTask = (t -> -taskStrength.floatValueOf((Task) t));

        float inputStrength = inputRegion != null ? taskStrength.floatValueOf(inputRegion) : Float.POSITIVE_INFINITY;

        FloatFunction<TaskRegion> leafRegionWeakness =
                /*new CachedFloatFunction*/(regionWeakness(now, tableDur, perceptDur));
        FloatFunction<Leaf<TaskRegion>> leafWeakness =
                L -> leafRegionWeakness.floatValueOf((TaskRegion) L.bounds());

        Top2<Leaf<TaskRegion>> mergeVictim = new Top2(leafWeakness);

        //0.
        //int startSize = tree.size();
        //if (startSize <= cap) return true; //compressed thanks to another thread


        //1.
        findEvictable(tree, tree.root(), mergeVictim);
        if (tree.size() <= cap)
            return true; //done, due to a removal of deleted items while finding eviction candiates

        //2.

        if (mergeVictim.size() > 0) {
            if (mergeOrDelete(tree, mergeVictim, taskStrength, inputStrength, weakestTask, added, nar)) {
                return tree.size() <= cap;
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


    private boolean mergeOrDelete(Space<TaskRegion> treeRW, Top2<Leaf<TaskRegion>> l, FloatFunction<Task> taskStrength, float inputStrength, FloatFunction<TaskRegion> weakestTasks, Consumer<Task> added, NAR nar) {
        TaskRegion a, b;

        Leaf<TaskRegion> la = l.a;
        short sa = la.size;
        if (sa > 2) {
            Top2<TaskRegion> w = new Top2<>(weakestTasks);
            la.forEach(w::add);
            a = w.a;
            b = w.b;
        } else if (sa == 2) {
            a = la.get(0);
            b = la.get(1);
        } else {
            a = la.get(0);
            Leaf<TaskRegion> lb = l.b;
            if (lb != null) {
                int sb = lb.size();
                if (sb > 1) {
                    Top<TaskRegion> w = new Top<>(weakestTasks);
                    lb.forEach(w::accept);
                    b = w.the;
                } else if (sb == 1) {
                    b = lb.get(0);
                } else {
                    b = null;  //??
                }
            } else {
                b = null;
            }
        }

        assert (a != null);
        Task at = a.task();
        float aPri = at.pri();

        treeRW.remove(at);


        if (b != null) {
            Task bt = b.task();

            if (bt.isDeleted()) {
                treeRW.remove(bt);
                return true;
            } else {
                at.meta("@", bt);
            }

            if (aPri != aPri) //already deleted
                return true;

            Task c = Revision.merge(at, bt, nar.time(), c2wSafe(nar.confMin.floatValue()), nar);
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

                    ((NALTask) at).delete(c); //forward
                    ((NALTask) bt).delete(c); //forward

                    if (treeRW.add(c))
                        added.accept(c);

                    return true;
                } else {
                    //merge result is not strong enough
                }

            }
        }

        //TODO do this outside of the locked section
        if (Param.ETERNALIZE_EVICTED_TEMPORAL_TASKS)
            eternalize(at, added, nar);

        return true;
    }

    protected void eternalize(Task x, Consumer<Task> added, NAR nar) {
        if (!(x instanceof SignalTask)) {
//        if (x.op().temporal) { //==IMPL /*x.op().statement */ /*&& !x.term().isTemporal()*/) {
//            //experimental eternalize
            float xc = x.conf();
            float c = w2cSafe(x.eviEternalized((1/xc) * size() /* eternalize inversely proportional to the size of this table, emulating the future evidence that can be considered */));
            if (c >= nar.confMin.floatValue()) {
                Task eternalized = Task.clone(x, x.term(), new PreciseTruth(x.freq(), c),
                        x.punc(), x.creation(), ETERNAL, ETERNAL
                );
                if (eternalized != null) {
                    eternalized.priMult(c/xc);
                    if (Param.DEBUG)
                        eternalized.log("Eternalized Temporal");

                    nar.runLater(() -> nar.input(eternalized));
                    //added.accept(eternalized);
                    ((NALTask) x).delete(eternalized);
                    return;
                }
            }
//
        }

        x.delete();
    }


    static boolean findEvictable(Space<TaskRegion> tree, Node<TaskRegion, ?> next, Top2<Leaf<TaskRegion>> mergeVictims) {
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
            Node<TaskRegion, ?>[] ww = b.data;
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

    FloatFunction<Task> taskStrength(long start, long end, int dur) {
        return (Task x) -> temporalTaskPriority(x, start, end, dur);
    }

    public double tableDur() {
        HyperRegion root = root().bounds();
        if (root == null)
            return 0;
        else
            return root.rangeIfFinite(0, 1);
    }

    FloatFunction<Task> taskStrengthWithFutureBoost(long now, float presentAndFutureBoost, long when, int perceptDur) {
        //int tableDur = 1 + (int) (tableDur());
        return (Task x) -> {
            if (x.isDeleted())
                return Float.NEGATIVE_INFINITY;

            //boost for present and future
            return (!x.isBefore(now - perceptDur) ? presentAndFutureBoost : 1f) * temporalTaskPriority(x, when, when, perceptDur);
        };
    }

    FloatFunction<Task> taskStrength(@Nullable Term template, long start, long end, int dur) {
        if (template == null || !template.isTemporal() || template.equals(template.root())) { //TODO this result can be cached for the entire table once knowing what term it stores
            return taskStrength(start, end, dur);
        } else {
            //int tableDur = 1 + (int) (tableDur());
            return (Task x) ->
                    temporalTaskPriority(x, start, end, dur) / (1f + Revision.dtDiff(template, x.term()));
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
    public Stream<Task> streamTasks() {
        return stream().map(TaskRegion::task);
    }

    static Predicate<TaskRegion> scanWhile(Predicate<? super Task> each) {
        return (t) -> {
            Task tt = t.task();
            return tt == null || each.test(tt);
        };
    }

    @Override
    public void whileEach(Predicate<? super Task> each) {
        whileEachIntersecting(root().bounds(), scanWhile(each));
    }

    @Override
    public void whileEach(long minT, long maxT, Predicate<? super Task> each) {
        whileEachIntersecting(new TimeRange(minT, maxT), scanWhile(each));
    }

    @Override
    public void forEachTask(Consumer<? super Task> each) {
        forEach(t -> {
//            Task tt = t.task();
//            if (tt != null)
            each.accept((Task) t);
        });
    }

    @Override
    public boolean removeTask(Task x) {
        return remove(x);
    }


    public void print(PrintStream out) {
        forEachTask(t -> out.println(t.toString(true)));
        stats().print(out);
    }


    private static final class RTreeBeliefModel extends Spatialization<TaskRegion> {

        final static double EPSILON = 0.0001f;

        public static final Spatialization<TaskRegion> the = new RTreeBeliefModel();


        private RTreeBeliefModel() {
            super((t -> t), RTreeBeliefTable.SPLIT, RTreeBeliefTable.MIN_TASKS_PER_LEAF, RTreeBeliefTable.MAX_TASKS_PER_LEAF);
        }

        @Override
        public double epsilon() {
            return EPSILON;
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
        private final Predicate<Task> filter;
        int attemptsRemain;


        public ScanFilter(TaskRegion[] taskRegions, FloatFunction<TaskRegion> strongestTask, int maxTries) {
            this(taskRegions, strongestTask, maxTries, null);
        }

        public ScanFilter(TaskRegion[] taskRegions, FloatFunction<TaskRegion> strongestTask, int maxTries, Predicate<Task> filter) {
            super(taskRegions, strongestTask);
            this.attemptsRemain = maxTries;
            this.filter = filter;
        }


        @Override
        public boolean test(TaskRegion x) {

            boolean duplicate = false;

            int s = size;
            TaskRegion[] l = list;
            for (int i = 0; i < s; i++) {
                if (l[i] == x) {
                    duplicate = true;
                    break;
                }
            }

            if (!duplicate) {
                if (filter == null || (!(x instanceof Task)) || filter.test((Task) x))
                    add(x);
            }
            return --attemptsRemain > 0;
        }

        public boolean continueScan(TimeRange t) {
            return isEmpty();
        }
    }

//    /** adds a node filter for repeated scans that may encounter the same nodes that do not repeat processing */
//    private static class UniqueTimeRange extends TimeRange {
//        SimpleIntSet tried = null;
//
//        public UniqueTimeRange() {
//        }
//
//
//        @Override
//        public boolean intersects(HyperRegion x) {
//            if (x instanceof Leaf && tried(x))
//                return false;
//            else
//                return super.intersects(x);
//        }
//
//        @Override
//        public boolean contains(HyperRegion x) {
//            if (x instanceof Leaf && tried(x))
//                return false;
//            else
//                return super.contains(x);
//        }
//
//        boolean tried(HyperRegion x) {
//            if (x instanceof TasksRegion) {
//
//                //filter leafs and branches already processed
//                if (tried == null)
//                    tried = new SimpleIntSet(4);
//
//                if (!tried.add( ((TasksRegion)x).serial))
//                    return true;
//            }
//            return false;
//        }
//
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
