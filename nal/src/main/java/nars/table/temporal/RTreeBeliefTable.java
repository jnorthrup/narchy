package nars.table.temporal;

import jcog.Util;
import jcog.data.list.FasterList;
import jcog.pri.Deleteable;
import jcog.sort.Top;
import jcog.sort.Top2;
import jcog.tree.rtree.*;
import jcog.tree.rtree.split.AxialSplitLeaf;
import nars.NAR;
import nars.Task;
import nars.control.proto.Remember;
import nars.task.Revision;
import nars.task.TaskProxy;
import nars.task.signal.SignalTask;
import nars.task.util.*;
import nars.truth.polation.TruthIntegration;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static jcog.WTF.WTF;

public class RTreeBeliefTable extends ConcurrentRTree<TaskRegion> implements TemporalBeliefTable {

    private static final float PRESENT_AND_FUTURE_BOOST_BELIEF = 2f;
    private static final float PRESENT_AND_FUTURE_BOOST_GOAL = 10f;


    private static final int MIN_TASKS_PER_LEAF = 2;
    private static final int MAX_TASKS_PER_LEAF = 4;
    private static final Split<TaskRegion> SPLIT = new AxialSplitLeaf<>();




    private static final int RejectInput = 0, EvictWeakest = 1, MergeInputClosest = 2, MergeLeaf = 3;

    protected int capacity;


    public RTreeBeliefTable() {
        super(new RTree<>(RTreeBeliefModel.the));
    }


    /**
     * immediately returns false if space removed at least one as a result of the scan, ie. by removing
     * an encountered deleted task.
     */
    private static boolean findEvictable(Space<TaskRegion> tree, Node<TaskRegion> next, @Nullable Top<TaskRegion> closest, Top<TaskRegion> weakest, Consumer<Leaf<TaskRegion>> weakLeaf) {
        if (next instanceof Leaf) {

            Leaf l = (Leaf) next;
            for (Object _x : l.data) {
                if (_x == null)
                    break;

                TaskRegion x = (TaskRegion) _x;
                if (((Deleteable) x).isDeleted()) {

                    boolean removed = tree.remove(x);

                    assert (removed);
                    return false;
                }

                weakest.accept(x);

                if (closest != null)
                    closest.accept(x);
            }

            if (l.size >= 2)
                weakLeaf.accept(l);

        } else {

            Branch b = (Branch) next;

            for (Node ww : b.data) {
                if (ww == null)
                    break;
                else if (!findEvictable(tree, ww, closest, weakest, weakLeaf))
                    return false;
            }
        }

        return true;
    }

    /**
     * TODO use the same heuristics as task strength
     */
    private static FloatFunction<TaskRegion> regionWeakness(long now, long perceptDur, float futureFactor) {

        return (TaskRegion r) -> {

            long timeDist =
                    r.maxTimeTo(now) / (1 + perceptDur);
            //r.midTimeTo(when);
            //r.maxTimeTo(when); //pessimistic, prevents wide-spanning taskregions from having an advantage over nearer narrower ones

            float conf = (((float) r.coord(false, 2)) + ((float) r.coord(true, 2))) / 2;

            float v = //-Param.evi(c2wSafe(conf),  timeDist, perceptDur);
                    (timeDist) * (1 - conf);

            if (r.endsAfter(now))
                v /= futureFactor;

            return v;

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

    static private FloatFunction<Task> taskStrengthWithFutureBoost(long now, float presentAndFutureBoost, long when, int perceptDur) {
        return taskStrengthWithFutureBoost(now, presentAndFutureBoost, when - perceptDur / 2, when + perceptDur / 2, perceptDur);
    }

    static private FloatFunction<Task> taskStrengthWithFutureBoost(long now, float presentAndFutureBoost, long start, long end, int perceptDur) {
        long futureStarts = now;
        // - perceptDur;
        return (Task x) -> (x.endsAfter(futureStarts) ? presentAndFutureBoost : 1f) *
                TruthIntegration.evi(x, start, end, perceptDur);
    }


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

//    @Override
//    public Truth truth(long start, long end, EternalTable eternal, Term template, int dur) {
//
//
//        assert (end >= start);
//
//        int s = size();
//        if (s > 0) {
//
//
//            int maxTruths = TRUTHPOLATION_LIMIT;
//
//            int maxTries = (int) Math.max(1, Math.ceil(capacity * SCAN_QUALITY));
//            maxTries = Math.min(s * 2 /* in case the same task is encountered twice HACK*/,
//                    maxTries);
//
//            ExpandingScan temporalTasks = new ExpandingScan(maxTruths, maxTruths,
//                    task(taskStrength(template, start, end, dur)),
//                    maxTries)
//                    .scan(this, start, end);
//
//            if (!temporalTasks.isEmpty()) {
//
//                TruthPolation t = Param.truth(start, end, dur);
//                temporalTasks.forEachItem(t::add);
//
//                if (eternal != null && !eternal.isEmpty()) {
//                    LongSet temporalStamp = t.filterCyclic();
//                    Task ee = eternal.select(ete -> !Stamp.overlapsAny(temporalStamp, ete.stamp()));
//                    if (ee != null) {
//                        t.add(ee);
//                    }
//                } else if (t.size() > 1) {
//                    t.filterCyclic();
//                }
//
//                return t.truth();
//            }
//        }
//
//        return eternal != null ? eternal.strongestTruth() : null;
//    }

//    @Override
//    public final Task match(long start, long end, @Nullable Term template, EternalTable eternals, NAR nar, Predicate<Task> filter) {
//        int s = size();
//        if (s > 0) {
//            int dur = nar.dur();
//            assert (end >= start);
//
//            Task t = match(start, end, template, nar, filter, dur);
//            if (t != null) {
//                if (eternals != null) {
//                    ImmutableLongSet tStamp = Stamp.toSet(t);
//                    Task e = eternals.select(x ->
//                            (filter == null || filter.test(x)) &&
//                                    !Stamp.overlapsAny(tStamp, x.stamp()));
//                    if (e != null) {
//                        return Revision.merge(nar, t, e);
//                    } else {
//                        return t;
//                    }
//                }
//            }
//        }
//
//        return eternals != null ? eternals.select(filter) : null;
//    }

    //abstract protected Task match(long start, long end, @Nullable Term template, NAR nar, Predicate<Task> filter, int dur);

    @Override
    public void match(Answer m) {

        int s = size();
        if (s == 0)
            return;

        TimeRangeFilter time = m.time;

        @Nullable TaskRegion bounds = this.bounds();

        boolean mustContain = !(time.intersectOrContain);

        if (!mustContain || time.intersects(bounds)) { //else nothing would match

            Predicate<TaskRegion> each = !mustContain ?
                            TaskRegion.asTask(m::tryAccept)
                            :
                            (n) -> {
                                if (time.contains(n)) {
                                    if (!m.tryAccept((Task) n)) {
                                        return false;
                                    }
                                }

                                return true;
                            };



            if (s == 1) {
                whileEach(each);
                return;
            }

            FloatFunction timeDist = TimeConfRange.distanceFunction( /* tableDur() */ time);

//            if (s <= MIN_TASKS_PER_LEAF*2 || time.start()==ETERNAL || time.range() <= Math.min(3, m.nar.dtDither())) {
                //single iterator
                read((tree)-> {
                    if (tree.root().size()==0)
                        return; //became null while waiting for lock

                    HyperIterator2<TaskRegion> ii = new HyperIterator2(tree, timeDist);

                    while (ii.hasNext() && each.test(ii.next())) {
                    }
                });
//            } else {
//
//                read((tree) -> {
//                    long mid = time.mid();
//
//
//                    NodeFilter<TaskRegion> f = new NodeFilter<>(
//                            (int) Math.round(Math.log(s)/Math.log((MIN_TASKS_PER_LEAF+MAX_TASKS_PER_LEAF)/2f) )
//                    );
//
//                    TimeRange left = new TimeRange(mustContain ? time.start() : Long.MIN_VALUE+1, mid);
//
//                    TimeRange right = new TimeRange(mid + 1,
//                            mustContain ? time.end() : Long.MAX_VALUE-1);
//
//                    Node<TaskRegion> root = tree.root();
//                    HyperIterator2<TaskRegion> L = new HyperIterator2(tree.model, root, left, timeDist);
//                    HyperIterator2<TaskRegion> R = new HyperIterator2(tree.model, root, right, timeDist);
//
//                    L.setNodeFilter(f);
//                    R.setNodeFilter(f);
//
//                    //must allow intersect for first stage match because the iterators may cut the item in half but it will still be a contained from the total bounds
//                    HyperIterator2[] ii;
//                    ii = new HyperIterator2[]{L, R};
//
//                    boolean swap = m.nar.random().nextBoolean();
//                    if (swap)
//                        ArrayUtils.swap(ii, 0, 1);
//
//                    int active;
//                    all:
//                    do {
//
//                        active = ii.length;
//                        for (int i = 0, iiLength = ii.length; i < iiLength; i++) {
//                            HyperIterator2<TaskRegion> h = ii[i];
//
//                            if (h == null) {
//                                active--;
//                                continue;
//                            }
//
//                            if (h.hasNext()) {
//                                TaskRegion next = h.next();
//                                if (!mustContain || time.contains(next))
//                                    if (!each.test(next))
//                                        break all; //done
//                            } else {
//                                ii[i] = null; //finished
//                                active--;
//                            }
//                        }
//
//                    } while (active > 0);
//
//                });
//
//            }
        }
//        ExpandingScan tt = new ExpandingScan(SAMPLE_MATCH_LIMIT, SAMPLE_MATCH_LIMIT,
//                m,//task(m.value()),
//                (int) Math.max(1, Math.ceil(capacity * SCAN_QUALITY)),
//                m::filter)
//                .scan(this, m.timeMin(), m.timeMax());
//
//        int tts = tt.size();
//        if (tts > 0) {
//            if (tts == 1) {
//                target.accept((Task) (tt.get(0).id));
//            } else {
//
//                final int[] limit = {m.limit()};
//                float[] ww = Util.map(tt::pri, new float[tts]);
//                MutableRoulette.run(ww, nar.random(), t -> 0, y -> {
//                    target.accept((Task) (tt.get(y).id));
//                    return --limit[0] > 0;
//                });
//            }
//        }


    }

//    /**
//     * TODO refactor as TimeRange method
//     */
//    private TimeRange expandLerpToTableBounds(TimeRange time, float lerpToTableExtents) {
//        if (time.start != ETERNAL) { //not already fully expanded?
//            long tableDur = tableDur();
//            long initialRange = time.end - time.start;
//            if (initialRange < tableDur) {
//
//
//                long halfExpand = Util.lerp(lerpToTableExtents, (tableDur - initialRange), tableDur) / 2;
//                if (halfExpand > 0) {
//                    return new TimeRange(time.start - halfExpand, time.end + halfExpand);
//                }
//            }
//
//        }
//        return time;
//    }
//
//    /**
//     * TODO refactor as TimeRange method
//     */
//    private TimeRange expandFactor(TimeRange time, double factor) {
//        if (time.start != ETERNAL) { //not already fully expanded?
//
//            long initialRange = time.end - time.start;
//            if (initialRange > 0) {
//
//
//                //long halfExpand = Util.lerp(lerpToTableExtents, (tableDur - initialRange), tableDur) / 2;
//                long halfExpand = Math.round(initialRange * factor) / 2;
//                if (halfExpand > 0) {
//                    return new TimeRange(time.start - halfExpand, time.end + halfExpand);
//                }
//            }
//
//        }
//        return time;
//    }

    @Override
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public void add(Remember r, NAR n) {

        if (r.input.isEternal())
            return;

        if (capacity == 0) {
            return;
        }

        /** buffer removal handling until outside of the locked section */

        Task input;
        if (r.input instanceof TaskProxy) {
            input = ((TaskProxy) r.input).the();
        } else {
            input = r.input;
        }


        /** inserted but not necessarily kept */
        write(treeRW -> {
            if (treeRW.add(input)) {
                ensureCapacity(treeRW, input, r, n);
            }
        });
        Task existing = RTreeBeliefModel.merged.get();
        if (existing != null && existing.equals(input)) {
            //assert(!input.isDeleted());
            RTreeBeliefModel.merged.remove();
            r.merge(existing);
            //assert(existing==input || r.forgotten.containsInstance(input));
        } else {
            if (!r.forgotten(input))
                r.remember(input);
        }


    }

    private boolean ensureCapacity(Space<TaskRegion> treeRW, @Nullable Task input, Remember remember, NAR nar) {

        FloatFunction<Task> taskStrength = null;
        FloatFunction<TaskRegion> leafRegionWeakness = null;
        int dur = 0, e = 0, cap;
        while (treeRW.size() > (cap = capacity)) {
            if (taskStrength == null) {
                long now = nar.time();
                dur = nar.dur();
                //  Math.max(1,Tense.occToDT(tableDur()/2));
                taskStrength = taskStrengthWithFutureBoost(now,
                        input.isBelief() ? PRESENT_AND_FUTURE_BOOST_BELIEF : PRESENT_AND_FUTURE_BOOST_GOAL,
                        now,
                        dur
                );
                leafRegionWeakness = regionWeakness(now, dur, input.isBeliefOrGoal() ? PRESENT_AND_FUTURE_BOOST_BELIEF : PRESENT_AND_FUTURE_BOOST_GOAL);
            }
            if (!compress(treeRW, e == 0 ? input : null /** only limit by inputRegion on first iter */,
                    taskStrength, leafRegionWeakness,
                    remember, nar))
                return false;
            e++;
            assert (e < cap);
        }

        return true;
    }

    /**
     * returns true if at least one net task has been removed from the table.
     */
    /*@NotNull*/
    private static boolean compress(Space<TaskRegion> tree, @Nullable Task
            input, FloatFunction<Task> taskStrength, FloatFunction<TaskRegion> leafRegionWeakness, Remember
                                            remember, NAR nar) {


        float inputStrength = input != null ? taskStrength.floatValueOf(input) : Float.POSITIVE_INFINITY;


        FloatFunction<Leaf<TaskRegion>> leafWeakness =
                L -> leafRegionWeakness.floatValueOf((TaskRegion) L.bounds());

        Top<Leaf<TaskRegion>> weakLeaf = new Top<>(leafWeakness);

        FloatFunction<TaskRegion> weakestTask = t ->
                -taskStrength.floatValueOf((Task) t);
//                (float) (-1 * Param.evi(taskStrength.floatValueOf((Task) t),
//                        //t.midTimeTo(now)
//                         t.maxTimeTo(now)
//                        , perceptDur));

        Top<TaskRegion> weakest = new Top<>(weakestTask);

        Top<TaskRegion> closest = input != null ? new Top<>(Answer.mergeability(input)) : null;


        if (!findEvictable(tree, tree.root(), closest, weakest, weakLeaf))
            return true;


        //assert (tree.size() >= cap);


        return mergeOrDelete(tree, input, closest, weakest, weakLeaf, taskStrength, inputStrength, weakestTask, remember, nar);


    }

    private static boolean mergeOrDelete(Space<TaskRegion> treeRW,
                                         @Nullable Task I /* input */,
                                         @Nullable Top<TaskRegion> closest,
                                         Top<TaskRegion> weakest,
                                         Top<Leaf<TaskRegion>> weakLeaf,
                                         FloatFunction<Task> taskStrength,
                                         float inputStrength,
                                         FloatFunction<TaskRegion> weakness,
                                         Remember r,
                                         NAR nar) {


        Task A, B, W, AB, IC, C;

        if (I != null && closest != null && closest.the != null) {
            C = (Task) closest.the;
            IC = Revision.merge(nar, I, C);
            if (IC != null && (IC.equals(I) || IC.equals(C)))
                IC = null;
        } else {
            IC = null;
            C = null;
        }


        if (!weakLeaf.isEmpty()) {
            Leaf<TaskRegion> la = weakLeaf.the;

            TaskRegion a, b;
            if (la.size > 2) {
                Top2<TaskRegion> w = new Top2<>(weakness);
                la.forEach(w::add);
                a = w.a;
                b = w.b;
            } else if (la.size == 2) {
                a = la.get(0);
                b = la.get(1);
            } else {
                throw new UnsupportedOperationException("should not have chosen leaf with size < 2");
            }


            A = (Task) a;
            B = (Task) b;
        } else {
            A = null;
            B = null;
        }


        W = (weakest != null && weakest.the != null) ? (Task) weakest.the : A;
        if (W == null)
            return false;

        float value[] = new float[4];
        value[RejectInput] =
                I != null ? -inputStrength : Float.NEGATIVE_INFINITY;
        value[EvictWeakest] =
                (I != null ? +inputStrength : 0) - taskStrength.floatValueOf(W);
        value[MergeInputClosest] =
                IC != null ? (
                        +taskStrength.floatValueOf(IC)
                                - taskStrength.floatValueOf(C)
                                - inputStrength)
                        : Float.NEGATIVE_INFINITY;

        if (B == null) {
            AB = null;
            value[MergeLeaf] = Float.NEGATIVE_INFINITY;
        } else {
            AB = Revision.merge(nar, A, B);
            if (AB == null || (AB.equals(A) || AB.equals(B))) {
                value[MergeLeaf] = Float.NEGATIVE_INFINITY;
            } else {
                value[MergeLeaf] =
                        (I != null ? +inputStrength : 0)
                                + taskStrength.floatValueOf(AB)
                                - taskStrength.floatValueOf(A)
                                - taskStrength.floatValueOf(B)
                ;
            }
        }


        int best = Util.maxIndex(value);

        if (value[best] == Float.NEGATIVE_INFINITY)
            return false;

        switch (best) {

            case EvictWeakest: {
                if (treeRW.remove(W)) {
                    r.forget(W);
                    return true;
                }
                throw WTF();
            }

            case RejectInput: {
                r.forget(I);
                return false;
            }

            case MergeInputClosest: {
                if (treeRW.remove(C)) {
                    if (treeRW.add(IC)) {
                        TemporalBeliefTable.fundMerge(IC, r, I, C);
                    } //else: already contained the merger

                    return true;
                }
                throw WTF();
            }

            case MergeLeaf: {


                if (treeRW.remove(A) && treeRW.remove(B)) {
                    if (treeRW.add(AB)) {
                        TemporalBeliefTable.fundMerge(AB, r, A, B);
                    } else {
                        //this may happen if the merge was acdtually a duplicate of what was in the table.
                        //this is fine. just forget the merge

                        AB.delete();

                        //TODO if I is more valuable than A, remove A and try to insert I

                    }
                    return true;
                }
                throw WTF();
            }

            default:
                throw new UnsupportedOperationException();
        }


    }


    @Override
    public long tableDur() {
        TaskRegion root = bounds();
        return root == null ? 0 : root.range();
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
        write((t)->{
            t.forEach(r -> ((Task)r).delete());
            t.clear();
        });
    }

    @Override
    public void whileEach(Predicate<? super Task> each) {
        whileEachIntersecting(root().bounds(), TaskRegion.asTask(each));
    }

    @Override
    public void whileEach(long minT, long maxT, Predicate<? super Task> each) {
        whileEachIntersecting(new TimeRange(minT, maxT), TaskRegion.asTask(each));
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
    public boolean removeTask(Task x) {
        if (x.isEternal())
            return false;

        x.delete();
        return remove(x);
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
                    RTreeBeliefTable.MIN_TASKS_PER_LEAF,
                    RTreeBeliefTable.MAX_TASKS_PER_LEAF);
        }

        @Override
        public final HyperRegion bounds(TaskRegion taskRegion) {
            return taskRegion;
        }


        /**
         * HACK store merge notifications
         */
        final static ThreadLocal<Task> merged = new ThreadLocal();

        @Override
        protected void merge(TaskRegion existing, TaskRegion incoming) {
            merged.set((Task) existing);
        }

    }

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
//                     cLayer < confDivisions && !(done = !continueScan(ll.set(leftStart, rightEnd)));
//                     cLayer++, cMax -= cDelta, cMin -= cDelta) {
//
//
//                    TimeRange lll;
//                    if (!leftComplete) {
//                        if (confDivisions > 1)
//                            ((TimeConfRange) ll).set(leftStart, leftMid, cMin, cMax);
//                        else
//                            ll.set(leftStart, leftMid);
//
//                        lll = ll;
//                    } else {
//                        lll = null;
//                    }
//
//                    TimeRange rrr;
//                    if (!rightComplete && !(leftStart == rightMid && leftMid == rightEnd)) {
//                        if (confDivisions > 1)
//                            ((TimeConfRange) rr).set(rightMid, rightEnd, cMin, cMax);
//                        else
//                            rr.set(rightMid, rightEnd);
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































