package nars.table;

import jcog.list.FasterList;
import jcog.math.LongInterval;
import jcog.pri.Deleteable;
import jcog.sort.CachedTopN;
import jcog.sort.Top;
import jcog.sort.Top2;
import jcog.tree.rtree.*;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.concept.TaskConcept;
import nars.link.Tasklinks;
import nars.task.NALTask;
import nars.task.Revision;
import nars.task.Tasked;
import nars.task.TruthPolation;
import nars.task.signal.SignalTask;
import nars.task.util.TaskRegion;
import nars.task.util.TimeConfRange;
import nars.task.util.TimeRange;
import nars.term.Term;
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

public abstract class RTreeBeliefTable extends ConcurrentRTree<TaskRegion> implements TemporalBeliefTable {

    /**
     * max fraction of the fully capacity table to compute in a single truthpolation
     */
    private static final float SCAN_QUALITY =
            1f;
            //0.5f;

    /** if the size is less than equal to this value, the entire table is scanned in one sweep (no time or conf sub-sweeps) */
    private static final int COMPLETE_SCAN_SIZE_THRESHOLD = 4;

    /**
     * max allowed truths to be truthpolated in one test
     */
    private static final int TRUTHPOLATION_LIMIT = 8;

    /** max tasks which can be merged (if they have equal occurrence and term) in a match's generated Task */
    private static final int SIMPLE_EVENT_MATCH_LIMIT = 6;
    private static final int COMPLEX_EVENT_MATCH_LIMIT = Math.max(1, SIMPLE_EVENT_MATCH_LIMIT/2);

    private static final float PRESENT_AND_FUTURE_BOOST =
            //1f;
            1.5f;


    private static final int SCAN_CONF_DIVISIONS = 3;
    private static final int SCAN_TIME_DIVISIONS = 4;

    private static final int MIN_TASKS_PER_LEAF = 3;
    private static final int MAX_TASKS_PER_LEAF = 4;
    private static final Split<TaskRegion> SPLIT =
            Spatialization.DefaultSplits.AXIAL.get(); //Spatialization.DefaultSplits.LINEAR; //<- probably doesnt work here


    protected int capacity;


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

    private RTreeBeliefTable() {
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
    public boolean isEmpty() {
        return super.isEmpty();
    }

    @Override
    public Truth truth(long start, long end, EternalTable eternal, int dur) {


        assert (end >= start);

        final Task ete = eternal != null ? eternal.strongest() : null;

        int s = size();
        x:
        if (s > 0) {

            if (start == ETERNAL) {
                LongInterval r = ((LongInterval) root().bounds());
                if (r == null)
                    break x;
                //return ete != null ? ete.truth() : null;

                start = r.start();
                end = r.end();
            }

            int maxTruths = TRUTHPOLATION_LIMIT;

            int maxTries = (int) Math.max(1, Math.ceil(capacity * SCAN_QUALITY));
            maxTries = Math.min(s * 2 /* in case the same task is encountered twice HACK*/,
                    maxTries);

            ScanFilter tt = new ScanFilter(maxTruths, maxTruths, task(
                    //taskRelevance(start, end)
                    taskStrength(start, end, dur)
            ), maxTries)
                    .scan(this, start - dur, end + dur, SCAN_CONF_DIVISIONS);

            if (!tt.isEmpty()) {
                return new TruthPolation(start, end, dur, tt).get(ete);
//                PreciseTruth pt = Param.truth(null, start, end, dur, tt);
//                if (pt!=null && (ete == null || (pt.evi() >= ete.evi())))
//                    return pt;
            }
        }

        return ete != null ? ete.truth() : null;

    }

    @Override
    public Task match(long start, long end, @Nullable Term template, NAR nar, Predicate<Task> filter) {
        int s = size();
        if (s == 0) return null; //quick exit

        int dur = nar.dur();
        if (start == ETERNAL) {
            long now = nar.time();
            start = now - dur/2;
            end = now + dur/2;
        }
        assert (end >= start);


        return match(start, end, template, nar, filter, dur);
    }

    abstract protected Task match(long start, long end, @Nullable Term template, NAR nar, Predicate<Task> filter, int dur);





    @Deprecated private static FloatFunction<TaskRegion> task(FloatFunction<Task> ts) {
        //return new CachedFloatFunction<>(size(), t -> +ts.floatValueOf((Task) t));
        return t -> ts.floatValueOf((Task)t);
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

        List<Tasked> added = new FasterList<>(2);
        write(treeRW -> {
            if (treeRW.add(x)) {
                if (!x.isDeleted()) {
                    added.add(x);
                    ensureCapacity(treeRW, x, added::add, n);
                }
            }
        });


        for (int i = 0, addedSize = added.size(); i < addedSize; i++) {
            Task y = added.get(i).task();
            if (y != null) {
                //completely activate a temporal task being stored in this table
                float pri = y.pri();
                if (pri == pri) {
                    Tasklinks.linkTask(y, pri, c, n);
                }
            } else {
                //eternal task input already done by calling the .task() method. it willl have returned null
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

    private boolean ensureCapacity(Space<TaskRegion> treeRW, @Nullable Task inputRegion, Consumer<Tasked> added, NAR nar) {
        int cap = this.capacity;
        int s = treeRW.size();
        if (s <= cap)
            return true;

        //int dur = 1 + (int) (tableDur());

        long now = nar.time();
        int perceptDur = nar.dur();
        FloatFunction<Task> taskStrength =
                //new CachedFloatFunction<>(
                        //s,
                        //taskStrength(now-dur/2, now+dur/2, dur);
                        taskStrengthWithFutureBoost(now, PRESENT_AND_FUTURE_BOOST, now, perceptDur);
                //);

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
    private boolean compress(Space<TaskRegion> tree, @Nullable Task inputRegion, FloatFunction<Task> taskStrength, Consumer<Tasked> added, int cap, long now, long tableDur, int perceptDur, NAR nar) {

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


    private boolean mergeOrDelete(Space<TaskRegion> treeRW, Top2<Leaf<TaskRegion>> l, FloatFunction<Task> taskStrength, float inputStrength, FloatFunction<TaskRegion> weakestTasks, Consumer<Tasked> added, NAR nar) {
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
                    lb.forEach(w);
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

            Task c =
                    (this instanceof Simple || (at.term().equals(bt.term()))) ?  //HACK
                        Revision.mergeTemporal(nar, at, bt) :
                        Revision.merge(at, bt, nar.time(), c2wSafe(nar.confMin.floatValue()), nar); //TODO remove this when the mergeTemporal fully supports CONJ and Temporal

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
                    c.delete();
                }

            }
        }

        //TODO do this outside of the locked section
        if (Param.ETERNALIZE_EVICTED_TEMPORAL_TASKS)
            eternalize(at, added, nar);

        at.delete();

        return true;
    }

    private void eternalize(Task x, Consumer<Tasked> added, NAR nar) {
        if ((x instanceof SignalTask)) {
            //ignore for now
            return;
        }

        float xPri = x.pri();
        if (xPri != xPri)
            return; //deleted already somehow

        float xc = x.conf();
        float c = w2cSafe(x.eviEternalized((1 / xc) * size() /* eternalize inversely proportional to the size of this table, emulating the future evidence that can be considered */));

        if (c >= nar.confMin.floatValue()) {

            added.accept(() -> {
                //        if (x.op().temporal) { //==IMPL /*x.op().statement */ /*&& !x.term().isTemporal()*/) {
                //            //experimental eternalize

                Task eternalized = Task.clone(x, x.term(), Truth.theDiscrete(x.freq(), c, nar),
                        x.punc(), x.creation(), ETERNAL, ETERNAL
                );

                if (eternalized != null) {

                    eternalized.pri(xPri * c / xc);

                    if (Param.DEBUG)
                        eternalized.log("Eternalized Temporal");

                    nar.input(eternalized);

                    if (!(eternalized.isDeleted()))
                        ((NALTask) x).delete(eternalized);
                }

                return null;
            });
        }

    }


    private static boolean findEvictable(Space<TaskRegion> tree, Node<TaskRegion, ?> next, Top2<Leaf<TaskRegion>> mergeVictims) {
        if (next instanceof Leaf) {

            Leaf l = (Leaf) next;
            for (Object _x : l.data) {
                if (_x == null)
                    break; //end of list

                TaskRegion x = (TaskRegion) _x;
                if (((Deleteable) x).isDeleted()) {
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

            for (Node ww : b.data) {
                if (ww == null)
                    break; //end of list
                else if (!findEvictable(tree, ww, mergeVictims))
                    return false;
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
                    r.nearestPointInternal(when);

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

//    /**
//     * measures only temporal proximity to the given range
//     */
//    private FloatFunction<Task> taskRelevance(long start, long end) {
//        double dur = 1 + tableDur();
//        return (Task x) -> (float) (dur / (1 + x.minDistanceTo(start, end))); // * .range() ?
//    }

    private static FloatFunction<Task> taskStrength(long start, long end, int dur) {
        return (Task x) -> temporalTaskPriority(x, start, end, dur);
    }

    private double tableDur() {
        HyperRegion root = root().bounds();
        if (root == null)
            return 0;
        else
            return root.rangeIfFinite(0, 1);
    }

    private static FloatFunction<Task> taskStrengthWithFutureBoost(long now, float presentAndFutureBoost, long when, int perceptDur) {
        //int tableDur = 1 + (int) (tableDur());
        return (Task x) -> {
            if (x.isDeleted())
                return Float.NEGATIVE_INFINITY;

            //boost for present and future
            return (!x.isBefore(now - perceptDur) ? presentAndFutureBoost : 1f) *
                    temporalTaskPriority(x, when, when, perceptDur);
        };
    }

    /**
     * simple version, ignores term content
     */
    private static FloatFunction<Task> taskStrength(@Nullable Term template, long start, long end, int dur) {
        return taskStrength(start, end, dur);
    }

//    /**
//     * dtDiff needs work
//     */
//    static FloatFunction<Task> taskStrengthComparingTemplates(@Nullable Term template, long start, long end, int dur) {
//        if (template == null || !template.isTemporal() || template.equals(template.root())) { //TODO this result can be cached for the entire table once knowing what term it stores
//            return taskStrength(start, end, dur);
//        } else {
//            //int tableDur = 1 + (int) (tableDur());
//            return (Task x) ->
//                    temporalTaskPriority(x, start, end, dur) / (1f + Revision.dtDiff(template, x.term()));
//        }
//    }


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

    private Predicate<TaskRegion> scanWhile(Predicate<? super Task> each) {
        //Set<TaskRegion> seen = new HashSet(size());
        return (t) -> {
            //if (seen.add(t)) {
                Task tt = t.task();
                if (!tt.isDeleted())
                    return each.test(tt);
//            }
//            else {
//                System.out.println("aha");
//            }
            return true;
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

    public static RTreeBeliefTable build(Term concept) {
        if (!concept.hasAny(Op.Temporal)) {
            return new RTreeBeliefTable.Simple();
        } else {
            return new RTreeBeliefTable.Complex();
        }
    }

    private static class Simple extends RTreeBeliefTable {

        @Override
        protected Task match(long start, long end, @Nullable Term template, NAR nar, Predicate<Task> filter, int dur) {

            FloatFunction<Task> taskStrength = taskStrength(start, end, dur);

            ScanFilter tt = new ScanFilter(SIMPLE_EVENT_MATCH_LIMIT, SIMPLE_EVENT_MATCH_LIMIT,
                    task(taskStrength),
                    (int) Math.max(1, Math.ceil(capacity * SCAN_QUALITY)), //maxTries
                    filter)
                    .scan(this, start, end, SCAN_CONF_DIVISIONS );

            return Revision.mergeTemporal(nar, tt.list, tt.size());
        }
    }

    private static class Complex extends RTreeBeliefTable {

        @Override
        public Truth truth(long start, long end, EternalTable eternal, int dur) {
            //disallows computing point truth values on temporal concepts (compound events can not be compared directly)
            return null;
        }

        @Override
        protected Task match(long start, long end, @Nullable Term template, NAR nar, Predicate<Task> filter, int dur) {

            ScanFilter tt = new ScanFilter(COMPLEX_EVENT_MATCH_LIMIT, COMPLEX_EVENT_MATCH_LIMIT,
                    task(taskStrength(template, start, end, dur)),
                    (int) Math.max(1, Math.ceil(capacity * SCAN_QUALITY)), //maxTries
                    filter)
                .scan(this, start, end, SCAN_CONF_DIVISIONS);

//            //merge up to the top 2
//            switch (tt.size()) {
//                case 0:
//                    return null;
//
//                case 1:
//                    return tt.first().task();
//
//                default:
//                    return merge2(tt.first(), tt.last(), start, end, dur, template, nar);
//            }

            return Revision.mergeTemporal(nar, tt.list, tt.size());
        }


    }


    private static final class RTreeBeliefModel extends Spatialization<TaskRegion> {

        final static double EPSILON = 0.0001f;

        static final Spatialization<TaskRegion> the = new RTreeBeliefModel();


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

    private final static class ScanFilter extends CachedTopN<TaskRegion> implements Predicate<TaskRegion> {

        private final Predicate<Task> filter;
        private final int minResults;
        int attemptsRemain;


        ScanFilter(int minResults, int maxResults, FloatFunction<TaskRegion> strongestTask, int maxTries) {
            this(minResults, maxResults, strongestTask, maxTries, null);
        }


        ScanFilter(int minResults, int maxResults, FloatFunction<TaskRegion> strongestTask, int maxTries, Predicate<Task> filter) {
            this(minResults, new TaskRegion[maxResults], strongestTask, maxTries, filter);
        }

        ScanFilter(int minResults, TaskRegion[] taskRegions, FloatFunction<TaskRegion> strongestTask, int maxTries, Predicate<Task> filter) {
            super(taskRegions, strongestTask);
            this.minResults = minResults;
            this.attemptsRemain = maxTries;
            this.filter = filter;
        }

        @Override
        public boolean test(TaskRegion x) {
            add(x);
            return attemptsRemain > 0;
        }

        @Override
        protected boolean addUnique(TaskRegion x) {
            --attemptsRemain;
            return  (filter == null || (!(x instanceof Task)) || validTask((Task)x))
                    &&
                    super.addUnique(x);
        }

        private boolean validTask(Task x) {
            return !x.isDeleted() && filter.test(x);
        }

        boolean continueScan(TimeRange t) {
            return size < minResults && attemptsRemain > 0;
        }

        /**
         * TODO add a Random argument so it can decide randomly whether to scan the left or right zone first.
         * order matters because the quality limit may terminate it.
         * however maybe the quality can be specified in terms that are compared
         * only after the pair has been scanned making the order irrelevant.
         */
        ScanFilter scan(RTreeBeliefTable table, long _start, long _end, int confDivisions) {


            table.readOptimistic((Space<TaskRegion> tree) -> {

                this.clear(); //in case of optimisticRead, if tried twice

                int s = tree.size();
                if (s <= COMPLETE_SCAN_SIZE_THRESHOLD) {
                    if (s > 0)
                        tree.forEach(this::add);
                    return;
                }

                TaskRegion bounds = (TaskRegion) (tree.root().bounds());

                long boundsStart = bounds.start();
                long boundsEnd = bounds.end();

                long start = Math.min(boundsEnd, Math.max(boundsStart, _start));
                long end = Math.max(boundsStart, Math.min(boundsEnd, _end));

                long expand = Math.max(1, (
                        Math.round(((double) (boundsEnd - boundsStart)) / (1 << (1+SCAN_TIME_DIVISIONS))))
                );

                //TODO use a polynomial or exponential scan expansion, to start narrow and grow wider faster


                long mid = (start + end) / 2;
                long leftStart = start, leftMid = mid, rightMid = mid, rightEnd = end;
                boolean leftComplete = false, rightComplete = false;
                //TODO float complete and use this as the metric for limiting with scan quality parameter

                TimeConfRange r = new TimeConfRange(); //recycled


                float maxConf = bounds.confMax();
                float minConf = bounds.confMin();

                boolean done = false;
                do {

                    float cMax = maxConf;
                    float cDelta =
                            Math.max((maxConf - minConf) / Math.min(s, confDivisions), Param.TRUTH_EPSILON);
                    float cMin = maxConf - cDelta;

                    for (int cLayer = 0;
                         cLayer < confDivisions && !(done = !continueScan(r.set(leftStart, rightEnd)));
                         cLayer++, cMax -= cDelta, cMin -= cDelta) {

                        if (!leftComplete)
                            tree.whileEachIntersecting(r.set(leftStart, leftMid, cMin, cMax), this);
                        if (!rightComplete && !(leftStart == rightMid && leftMid == rightEnd))
                            tree.whileEachIntersecting(r.set(rightMid, rightEnd, cMin, cMax), this);

                    }

                    if (done)
                        break;

                    //detect if either side can go no further
                    long ls0 = leftStart;
                    leftComplete |= (ls0 == (leftStart = Math.max(boundsStart, leftStart - expand - 1))); //no change

                    if (leftComplete && rightComplete) break;

                    long rs0 = rightEnd;
                    rightComplete |= (rs0 == (rightEnd = Math.min(boundsEnd, rightEnd + expand + 1)));

                    if (leftComplete && rightComplete) break;

                    leftMid = ls0 - 1;
                    rightMid = rs0 + 1;
                    expand *= 2;
                } while (true);

            });


            return this;
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
//    protected static Task merge2(Tasked a, Tasked b, long start, long end, int dur, Term template, NAR nar) {
//        return merge2(a.task(), b.task(), start, end, dur, template, nar);
//    }
//
//    protected static Task merge2(Task a, Task b, long start, long end, int dur, Term template, NAR nar) {
//        if (template != null) {
//            //choose if either one (but not both or neither) matches template's time
//            boolean at = (a.term().equals(template));
//            boolean bt = (b.term().equals(template));
//            if (at && !bt)
//                return a;
//            else if (bt && !at)
//                return b;
//        }
//
//        //otherwise interpolate
//        Task c = Revision.merge(a, b, start, c2wSafe(nar.confMin.floatValue()) /* TODO */, nar);
//        if (c != null) {
//
//            if (c == a) //c.equals(a))
//                return a;
//            if (c == b) //c.equals(b))
//                return b;
//
//            if (c.eviInteg() > a.eviInteg())
//                return c;
//        }
//
//        return a;
//    }
//
