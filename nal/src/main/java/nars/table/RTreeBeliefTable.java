package nars.table;

import jcog.Util;
import jcog.WTF;
import jcog.decide.MutableRoulette;
import jcog.list.FasterList;
import jcog.pri.Deleteable;
import jcog.sort.CachedTopN;
import jcog.sort.Top;
import jcog.sort.Top2;
import jcog.tree.rtree.*;
import jcog.tree.rtree.split.AxialSplitLeaf;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.concept.TaskConcept;
import nars.task.NALTask;
import nars.task.Revision;
import nars.task.signal.SignalTask;
import nars.task.util.TaskRegion;
import nars.task.util.TimeConfRange;
import nars.task.util.TimeRange;
import nars.term.Term;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.truth.polation.TruthPolation;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.set.primitive.ImmutableLongSet;
import org.eclipse.collections.api.set.primitive.LongSet;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static nars.table.TemporalBeliefTable.value;
import static nars.time.Tense.ETERNAL;
import static nars.time.Tense.XTERNAL;
import static nars.truth.TruthFunctions.c2wSafe;
import static nars.truth.TruthFunctions.w2cSafe;

public abstract class RTreeBeliefTable extends ConcurrentRTree<TaskRegion> implements TemporalBeliefTable {

    /**
     * max fraction of the fully capacity table to compute in a single truthpolation
     */
    private static final float SCAN_QUALITY =
            1f;

    /**
     * max allowed truths to be truthpolated in one test
     * must be less than or equal to Stamp.CAPACITY otherwise stamp overflow
     */
    private static final int TRUTHPOLATION_LIMIT = Param.STAMP_CAPACITY / 2;

    /**
     * max tasks which can be merged (if they have equal occurrence and term) in a match's generated Task
     */
    private static final int SIMPLE_EVENT_MATCH_LIMIT = TRUTHPOLATION_LIMIT;

    private static final int COMPLEX_EVENT_MATCH_LIMIT =
            SIMPLE_EVENT_MATCH_LIMIT;
    //Math.max(1, SIMPLE_EVENT_MATCH_LIMIT / 2);

    private static final int SAMPLE_MATCH_LIMIT = TRUTHPOLATION_LIMIT;

    private static final float PRESENT_AND_FUTURE_BOOST =
            1f;
    //1.5f;
    //2f;
    //4f;
    //8f;
    //10f;


    private static final int SCAN_CONF_OCTAVES_MAX = 1;
    private static final int SCAN_TIME_OCTAVES_MAX = 3;

    private static final int MIN_TASKS_PER_LEAF = 2;
    private static final int MAX_TASKS_PER_LEAF = 4;
    private static final Split<TaskRegion> SPLIT =
            new AxialSplitLeaf<>();
    //Spatialization.DefaultSplits.LINEAR; //<- probably doesnt work here

    /**
     * if the size is less than equal to this value, the entire table is scanned in one sweep (no time or conf sub-sweeps)
     */
    private static final int COMPLETE_SCAN_SIZE_THRESHOLD = MAX_TASKS_PER_LEAF;


    private static final int RejectInput = 0, EvictWeakest = 1, MergeInputClosest = 2, MergeLeaf = 3;

    protected int capacity;


    private RTreeBeliefTable() {
        super(new RTree<>(RTreeBeliefModel.the));
    }

    @Deprecated
    private static FloatFunction<TaskRegion> task(FloatFunction<Task> ts) {
        //return new CachedFloatFunction<>(size(), t -> +ts.floatValueOf((Task) t));
        return t -> ts.floatValueOf((Task) t);
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

    /**
     * immediately returns false if space removed at least one as a result of the scan, ie. by removing
     * an encountered deleted task.
     */
    private static boolean findEvictable(Space<TaskRegion> tree, Node<TaskRegion, ?> next, @Nullable Top<TaskRegion> closest, Top<TaskRegion> weakest, Consumer<Leaf<TaskRegion>> weakLeaf) {
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

                weakest.accept(x);

                if (closest != null)
                    closest.accept(x);
            }

            if (l.size >= 2)
                weakLeaf.accept(l);

        } else { //if (next instanceof Branch)

            Branch b = (Branch) next;

            for (Node ww : b.data) {
                if (ww == null)
                    break; //end of list
                else if (!findEvictable(tree, ww, closest, weakest, weakLeaf))
                    return false;
            }
        }

        return true;
    }

    /**
     * TODO use the same heuristics as task strength
     */
    private static FloatFunction<TaskRegion> regionWeakness(long when, long perceptDur) {

        return (TaskRegion r) -> {

            long regionTimeDist =
                    //r.furthestTimeTo(when);
                    //when - r.nearestPointInternal(when);
                    r.midDistanceTo(when);

            float timeDist = (regionTimeDist) / ((float) perceptDur);


            float evi =
                    c2wSafe((float) r.coord(true, 2)); //max
//            float dt =
//                    (float) r.range(1);
            //(float)r.coord(false, 2); //min
            //(float) (r.coord(true, 2) + r.coord(false, 2)) / 2f; //avg

            //float antiConf = 1f - conf;
            float antivalue = 1f / (1f + evi);

            if (PRESENT_AND_FUTURE_BOOST != 1 && r.end() >= when - perceptDur)
                antivalue /= PRESENT_AND_FUTURE_BOOST;

            //float span = (float)(1 + r.range(0)/dur); //span becomes less important the further away, more fair to short near-term tasks

            return (float) ((antivalue) * (1 + timeDist));
        };
    }

    static FloatFunction<Task> taskStrength(long start, long end, int dur) {
        if (start == ETERNAL) {
            return RTreeBeliefTable::valueInEternity;
        } else {
            return x -> value(x, start, end, dur);
        }
    }

    private static float valueInEternity(Task x) {
        return x.eviEternalized() * x.range();
    }

    private static Predicate<TaskRegion> scanWhile(Predicate<? super Task> each) {
        return t -> {
            Task tt = ((Task) t);
            return tt.isDeleted() || each.test(tt);
        };
    }

    public static RTreeBeliefTable build(Term concept) {
        if (!concept.hasAny(Op.Temporal)) {
            return new RTreeBeliefTable.Simple();
        } else {
            return new RTreeBeliefTable.Complex();
        }
    }

    abstract protected FloatFunction<Task> taskStrength(@Nullable Term template, long start, long end, int dur);

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

    @Override
    public boolean isEmpty() {
        return super.isEmpty();
    }

    @Override
    public Truth truth(long start, long end, EternalTable eternal, Term template, int dur) {




        assert (end >= start);

        int s = size();
        if (s > 0) {


            int maxTruths = TRUTHPOLATION_LIMIT;

            int maxTries = (int) Math.max(1, Math.ceil(capacity * SCAN_QUALITY));
            maxTries = Math.min(s * 2 /* in case the same task is encountered twice HACK*/,
                    maxTries);

            ExpandingScan temporalTasks = new ExpandingScan(maxTruths, maxTruths,
                    task(taskStrength(template, start, end, dur)),
                    maxTries)
                    .scan(this, start, end);

            if (!temporalTasks.isEmpty()) {

                TruthPolation t = Param.truth(start, end, dur).add(temporalTasks);

                LongSet temporalStamp = t.filterCyclic();
                if (eternal != null && !eternal.isEmpty()) {
                    Task ee = eternal.select(ete -> !Stamp.overlapsAny(temporalStamp, ete.stamp()));
                    if (ee != null) {
                        t.add(ee);
                    }
                }

                return t.truth();
            }
        }

        return eternal != null ? eternal.strongestTruth() : null;
    }

    @Override
    public final Task match(long start, long end, @Nullable Term template, EternalTable eternals, NAR nar, Predicate<Task> filter) {
        int s = size();
        if (s > 0) {
            int dur = nar.dur();
            assert (end >= start);

            Task t = match(start, end, template, nar, filter, dur);
            if (t != null) {
                if (eternals != null) {
                    ImmutableLongSet tStamp = Stamp.toSet(t);
                    Task e = eternals.select(x ->
                            (filter == null || filter.test(x)) &&
                                    !Stamp.overlapsAny(tStamp, x.stamp()
                                    ));
                    if (e != null) {
                        return Revision.mergeTasks(nar, t, e);
                    } else {
                        return t;
                    }
                }
            }
        }

        return eternals != null ? eternals.select(filter) : null;
    }

    abstract protected Task match(long start, long end, @Nullable Term template, NAR nar, Predicate<Task> filter, int dur);

    @Override
    public void match(TaskMatch m, NAR nar, Consumer<Task> target) {

        if (isEmpty())
            return;

        ExpandingScan tt = new ExpandingScan(SAMPLE_MATCH_LIMIT, SAMPLE_MATCH_LIMIT,
                task(m.value()),
                (int) Math.max(1, Math.ceil(capacity * SCAN_QUALITY)), //maxTries
                null)
                .scan(this, m.start(), m.end());

        int tts = tt.size();
        if (tts > 0) {
            if (tts == 1) {
                target.accept((Task) tt.get(0)); //simple case
            } else {

                final int[] limit = {m.limit()};
                float[] ww = Util.map(tt::pri, new float[tts]);
                MutableRoulette.run(ww, m.random(), t -> 0, y -> {
                    target.accept((Task) tt.get(y));
                    return --limit[0] > 0;
                });
            }
        }

    }

    //    /**
//     * measures only temporal proximity to the given range
//     */
//    private FloatFunction<Task> taskRelevance(long start, long end) {
//        double dur = 1 + tableDur();
//        return (Task x) -> (float) (dur / (1 + x.minDistanceTo(start, end))); // * .range() ?
//    }

    @Override
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public boolean add(Task x, TaskConcept c, NAR n) {

        if (capacity() == 0)
            return false;

        write(treeRW -> {
            if (treeRW.add(x)) {
                if (!x.isDeleted()) {
                    ensureCapacity(treeRW, x, n);
                }
            }
        });


//        for (int i = 0, addedSize = revisions.size(); i < addedSize; i++) {
//            Task revision = revisions.get(i).task();
//            if (revision != null) {
//                //completely activate a temporal task being stored in this table
//                float pri = revision.pri();
//                if (pri == pri) {
//                    Tasklinks.linkTask(revision, pri, c, n);
//                }
//            } else {
//                //eternal task input already done by calling the .task() method. it willl have returned null
//            }
//        }

        return !x.isDeleted();

    }


    private boolean ensureCapacity(Space<TaskRegion> treeRW, @Nullable Task inputRegion, NAR nar) {
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
                taskStrengthWithFutureBoost(now, PRESENT_AND_FUTURE_BOOST, now,
                        perceptDur,
                        perceptDur
                        //(long) Math.ceil(tableDur())
                );

        //);

        int e = 0;
        while (treeRW.size() > cap) {
            if (!compress(treeRW, e == 0 ? inputRegion : null /** only limit by inputRegion first */,
                    taskStrength, cap,
                    now,
                    //now + nar.dur() /* compress ahead for next duration */,
                    perceptDur, nar))
                return false;
            e++;
        }

        assert (treeRW.size() <= cap);
        return true;
    }

    /**
     * returns true if at least one net task has been removed from the table.
     */
    /*@NotNull*/
    private boolean compress(Space<TaskRegion> tree, @Nullable Task input, FloatFunction<Task> taskStrength, int cap, long now, int perceptDur, NAR nar) {


        float inputStrength = input != null ? taskStrength.floatValueOf(input) : Float.POSITIVE_INFINITY;

        FloatFunction<TaskRegion> leafRegionWeakness =
                /*new CachedFloatFunction*/(regionWeakness(now, perceptDur));
        FloatFunction<Leaf<TaskRegion>> leafWeakness =
                L -> leafRegionWeakness.floatValueOf((TaskRegion) L.bounds());
        Top<Leaf<TaskRegion>> weakLeaf = new Top<>(leafWeakness);

        FloatFunction<TaskRegion> weakestTask = (t ->
                -taskStrength.floatValueOf((Task) t));

        Top<TaskRegion> closest = input != null ? new Top<>(
                TemporalBeliefTable.mergabilityWith(input, perceptDur)
        ) : null;
        Top<TaskRegion> weakest = new Top<>(
                weakestTask
        );

        //evaluate the table
        if (!findEvictable(tree, tree.root(), closest, weakest, weakLeaf)) {
            //removed at least one, due to encountering deleted items while searching eviction candidates
            return true;
        }

        //otherwise, still necessary to remove or evict:
        assert (tree.size() >= cap);

        //decide what to do and do it
        return mergeOrDelete(tree, input, closest, weakest, weakLeaf, taskStrength, inputStrength, weakestTask, nar);


    }


    private boolean mergeOrDelete(Space<TaskRegion> treeRW,
                                  @Nullable Task I /* input */,
                                  @Nullable Top<TaskRegion> closest, Top<TaskRegion> weakest,
                                  Top<Leaf<TaskRegion>> weakLeaf,
                                  FloatFunction<Task> taskStrength, float inputStrength,
                                  FloatFunction<TaskRegion> weakness,
                                  NAR nar) {

        //TODO compare the value of revising with input task against its cost of removing the input,
        // and also the cost of removing tasks from the weakest leaf below


        Task A, B, W, AB, IC, C;

        if (I != null && closest != null && closest.the != null) {
            C = (Task) closest.the;
            IC = Revision.mergeTasks(nar, I, C);
            if (IC != null && (IC.equals(I) || IC.equals(C)))
                IC = null; //ignore
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

//
//            Leaf<TaskRegion> la = weakLeaf.a;
//            short sa = la.size;
//            if (sa > 2) {
//                Top2<TaskRegion> w = new Top2<>(weakness);
//                la.forEach(w::add);
//                a = w.a;
//                b = w.b;
//            } else if (sa == 2) {
//                a = la.get(0);
//                b = la.get(1);
//            } else {
//                a = la.get(0);
//                Leaf<TaskRegion> lb = weakLeaf.b;
//                if (lb != null) {
//                    int sb = lb.size();
//                    if (sb > 1) {
//                        Top<TaskRegion> w = new Top<>(weakness);
//                        lb.forEach(w);
//                        b = w.the;
//                    } else if (sb == 1) {
//                        b = lb.get(0);
//                    } else {
//                        b = null;  //??
//                    }
//                } else {
//                    b = null;
//                }
//            }
            A = (Task) a;
            B = (Task) b;
        } else {
            A = null;
            B = null;
        }


        W = (weakest != null && weakest.the != null) ? (Task) weakest.the : A;
        if (W == null)
            return false;  //??

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
            value[MergeLeaf] = Float.NEGATIVE_INFINITY; //impossible
        } else {
            AB = Revision.mergeTasks(nar, A, B);
            if (AB == null || (AB.equals(A) || AB.equals(B))) {
                value[MergeLeaf] = Float.NEGATIVE_INFINITY; //impossible
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

        if (value[best] == Float.NEGATIVE_INFINITY) {
            return false; //no options
        }

        switch (best) {

            case EvictWeakest: {
                delete(treeRW, W, nar);
                return true;
            }

            case RejectInput: {
                I.delete();
                return false;
            }

            case MergeInputClosest: {

                if (treeRW.add(IC)) {
                    delete(treeRW, C, nar);
                    //I.delete(); //allow inputter to think it was successful
                    return true;
                } else {
                    return false;
                }
            }

            case MergeLeaf: {


                if (treeRW.add(AB)) {
                    delete(treeRW, A, nar);
                    delete(treeRW, B, nar);

                    return true;
                } else {
                    if (I != null)
                        I.delete();
                    return false; //?? not sure why this might happen but in case it does, reject the input
                }
            }

            default:
                throw new UnsupportedOperationException();
        }


    }

    private void delete(Space<TaskRegion> treeRW, Task x, NAR nar) {
        boolean removed = treeRW.remove(x);
        assert (removed);
        if (Param.ETERNALIZE_FORGOTTEN_TEMPORALS)
            eternalize(x, nar);
        //x.delete();
    }

    private void eternalize(Task x, NAR nar) {
        if ((x instanceof SignalTask)) {
            //ignore for now
            return;
        }

        float xPri = x.priElseZero();
//        if (xPri != xPri)
//            return; //deleted already somehow

        float xc = x.conf();
        float e = x.evi();
        // (1 / xc) * size() /* eternalize inversely proportional to the size of this table, emulating the future evidence that can be considered */);
        float c = w2cSafe(e);

        if (c >= nar.confMin.floatValue()) {

            //added.accept(() -> {
            //        if (x.op().temporal) { //==IMPL /*x.op().statement */ /*&& !x.term().isTemporal()*/) {
            //            //experimental eternalize
            Task eternalized = Task.eternalized(x, 1f / size());

            if (eternalized != null) {

                eternalized.pri(xPri * c / xc);

                if (Param.DEBUG)
                    eternalized.log("Eternalized Temporal");

                nar.input(eternalized);

                if (!(eternalized.isDeleted()))
                    x.delete(/*fwd: eternalized*/);
            }

            //    return null;
            //});
        }

    }

    private double tableDur() {
        HyperRegion root = root().bounds();
        if (root == null)
            return 1;
        else
            return 1 + root.rangeIfFinite(0, 1);
    }

    static private FloatFunction<Task> taskStrengthWithFutureBoost(long now, float presentAndFutureBoost, long when, int perceptDur, long tableDur) {
        return (Task x) -> {
            //boost for present and future
            return (!x.isAfter(now) ? presentAndFutureBoost : 1f) *
                    value(x, when, when, tableDur);
        };
    }

    @Override
    public int capacity() {
        return capacity;
    }

    @Override
    public Stream<Task> streamTasks() {
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
    public void whileEach(Predicate<? super Task> each) {
        whileEachIntersecting(root().bounds(), scanWhile(each));
    }

    @Override
    public void whileEach(long minT, long maxT, Predicate<? super Task> each) {
        whileEachIntersecting(new TimeRange(minT, maxT), scanWhile(each));
    }

    @Override
    public void forEachTask(Consumer<? super Task> each) {
        forEach(t -> each.accept((Task) t));
    }

    @Override
    public boolean removeTask(Task x) {
        return remove(x);
    }

    public void print(PrintStream out) {
        forEachTask(t -> out.println(t.toString(true)));
        stats().print(out);
    }

    private static class Simple extends RTreeBeliefTable {

        @Override
        protected FloatFunction<Task> taskStrength(@Nullable Term templateIgnored, long start, long end, int dur) {
            return taskStrength(start, end, dur);
        }

        @Override
        protected Task match(long start, long end, @Nullable Term template, NAR nar, Predicate<Task> filter, int dur) {

            ExpandingScan tt = new ExpandingScan(SIMPLE_EVENT_MATCH_LIMIT, SIMPLE_EVENT_MATCH_LIMIT,
                    task(taskStrength(start, end, dur)),
                    (int) Math.max(1, Math.ceil(capacity * SCAN_QUALITY)), //maxTries
                    filter)
                    .scan(this, start, end);


            int n = tt.size();
            return n > 0 ? Revision.mergeTasks(nar, start, end, tt.array(TaskRegion[]::new)) : null;
        }
    }

    private static class Complex extends RTreeBeliefTable {

        @Override
        protected FloatFunction<Task> taskStrength(@Nullable Term template, long start, long end, int dur) {
            FloatFunction<Task> f = taskStrength(start, end, dur);
            if (template == null) {
                //should this be allowed, or should we assume the root form of the term?
                return f;
            } else {
                return x -> f.floatValueOf(x) / TemporalBeliefTable.costDtDiff(template, x, dur);
            }
        }

        @Override
        protected Task match(long start, long end, @Nullable Term template, NAR nar, Predicate<Task> filter, int dur) {

            ExpandingScan tt = new ExpandingScan(COMPLEX_EVENT_MATCH_LIMIT, COMPLEX_EVENT_MATCH_LIMIT,
                    task(taskStrength(template, start, end, dur)),
                    (int) Math.max(1, Math.ceil(capacity * SCAN_QUALITY)), //maxTries
                    filter)
                    .scan(this, start, end);


            int n = tt.size();
            if (n == 0)
                return null;

            TaskRegion[] ttt = tt.array(TaskRegion[]::new);

//            if (n > 1) {
//                //find the most consistent term of the set of tasks and remove any that dont match it
//                //terms are classified by their dt:
//                //
//                int pos = 0, neg = 0;
//
//                for (TaskRegion x : ttt) {
//                    if (x == null)
//                        break;//end of list
//                    int dt = (((Task) x).term()).dt();
//                    if (dt != 0 && dt != DTERNAL) {
//                        if (dt < -dur / 2) neg++;
//                        else if (dt > +dur / 2) pos++;
//                    }
//                }
//                if (pos > 0 && neg > 0) {
//                    boolean polarity =
//                            (pos == neg) ?
//                                    ((template.dt() != DTERNAL && template.dt() != 0 && template.dt() != XTERNAL)) ? (template.dt() > 0) : nar.random().nextBoolean() //if equal, choose on polarity at random
//                                    :
//                                    (pos > neg);
//
//                    FasterList<TaskRegion> xx = new FasterList<>(0, new TaskRegion[n - (polarity ? neg : pos)]);
//                    for (TaskRegion x : ttt) {
//                        if (x == null)
//                            break;//end of list
//                        int dt = (((Task) x).term()).dt();
//                        if (dt == 0 || dt == DTERNAL || (polarity && dt > +dur / 2) || (!polarity && dt < -dur / 2))
//                            xx.addWithoutResizeCheck(x);
//                    }
//                    ttt = xx.array();
//                    n = ttt.length;
//                }
//            }


            return Revision.mergeTasks(nar, start, end, ttt);
        }


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

        //        @Override
//        public Node<TaskRegion, TaskRegion> newLeaf() {
//            return new BeliefLeaf(max);
//        }

        @Override
        protected void merge(TaskRegion existing, TaskRegion incoming) {
            Task i = incoming.task();
            Task e = existing.task();
            if (e instanceof NALTask) //HACK
                ((NALTask) e).causeMerge(i);
            //i.delete();
            //i.meta("merge", e); //signals success to activate what was input
        }

    }

    private final static class ExpandingScan extends CachedTopN<TaskRegion> implements Predicate<TaskRegion> {

        private final Predicate<Task> filter;
        private final int minResults, attempts;
        int attemptsRemain;


        ExpandingScan(int minResults, int maxResults, FloatFunction<TaskRegion> strongestTask, int maxTries) {
            this(minResults, maxResults, strongestTask, maxTries, null);
        }


        ExpandingScan(int minResults, int maxResults, FloatFunction<TaskRegion> strongestTask, int maxTries, Predicate<Task> filter) {
            super(maxResults, strongestTask);
            this.minResults = minResults;
            this.attempts = maxTries;
            this.filter = filter;
        }

        @Override
        public boolean add(TaskRegion taskRegion) {
            //skip adding/ranking task region (branches)
            return (!(taskRegion instanceof Task)) || super.add(taskRegion);
        }

        @Override
        public boolean test(TaskRegion x) {
            add(x);
            return --attemptsRemain > 0;
        }

        @Override public boolean valid(TaskRegion x) {
            return ((!(x instanceof Task)) || (filter == null || filter.test((Task) x)));
        }

        boolean continueScan(TimeRange t) {
            return top.size() < minResults && attemptsRemain > 0;
        }

        /**
         * TODO add a Random argument so it can decide randomly whether to scan the left or right zone first.
         * order matters because the quality limit may terminate it.
         * however maybe the quality can be specified in terms that are compared
         * only after the pair has been scanned making the order irrelevant.
         */
        ExpandingScan scan(RTreeBeliefTable table, long _start, long _end) {

            /* whether eternal is the time bounds */
            boolean eternal = _start == ETERNAL;


            this.attemptsRemain = attempts; //reset attempts count

            int s = table.size();
            if (s == 0)
                return this;

            /* if eternal is being calculated, include up to the maximum number of truthpolated terms.
                otherwise limit by the Leaf capacity */
            if ((!eternal && s <= COMPLETE_SCAN_SIZE_THRESHOLD) || (eternal && s <= TRUTHPOLATION_LIMIT)) {
                table.forEachOptimistic(this::add);
                return this;
            }

            TaskRegion bounds = (TaskRegion) (table.root().bounds());

            long boundsStart = bounds.start();
            long boundsEnd = bounds.end();
            if (boundsEnd == XTERNAL || boundsEnd < boundsStart) {
                throw new WTF();
            }

            int ss = s / COMPLETE_SCAN_SIZE_THRESHOLD;

            long scanStart, scanEnd;
            int confDivisions, timeDivisions;
            if (!eternal) {

                scanStart = Math.min(boundsEnd, Math.max(boundsStart, _start));
                scanEnd = Math.max(boundsStart, Math.min(boundsEnd, _end));

                //TODO use different CONF divisions strategy for eternal to select highest confidence tasks irrespective of their time


                timeDivisions = Math.max(1, Math.min(SCAN_TIME_OCTAVES_MAX, ss));
                confDivisions = Math.max(1, Math.min(SCAN_CONF_OCTAVES_MAX,
                        ss / Util.sqr(1 + timeDivisions)));
            } else {
                scanStart = boundsStart;
                scanEnd = boundsEnd;

                confDivisions = Math.max(1, Math.min(SCAN_TIME_OCTAVES_MAX /* yes TIME here, ie. the axes are switched */,
                        Math.max(1, ss - minResults)));
                timeDivisions = 1;
            }

            long expand = Math.max(1, (
                    Math.round(((double) (boundsEnd - boundsStart)) / (1 << (timeDivisions))))
            );

            //TODO use a polynomial or exponential scan expansion, to start narrow and grow wider faster


            long mid = (scanStart + scanEnd) / 2;
            long leftStart = scanStart, leftMid = mid, rightMid = mid, rightEnd = scanEnd;
            boolean leftComplete = false, rightComplete = false;
            //TODO float complete and use this as the metric for limiting with scan quality parameter

            TimeRange ll = confDivisions > 1 ? new TimeConfRange() : new TimeRange();
            TimeRange rr = confDivisions > 1 ? new TimeConfRange() : new TimeRange();

            float maxConf = bounds.confMax();
            float minConf = bounds.confMin();

            int FATAL_LIMIT = s * 2;
            int count = 0;
            boolean done = false;
            do {

                float cMax, cDelta, cMin;
                if (confDivisions == 1) {
                    cMax = 1;
                    cMin = 0;
                    cDelta = 0; //unused
                } else {
                    cMax = maxConf;
                    cDelta =
                            Math.max((maxConf - minConf) / Math.min(s, confDivisions), Param.TRUTH_EPSILON);
                    cMin = maxConf - cDelta;
                }

                for (int cLayer = 0;
                     cLayer < confDivisions && !(done = !continueScan(ll.set(leftStart, rightEnd)));
                     cLayer++, cMax -= cDelta, cMin -= cDelta) {


                    TimeRange lll;
                    if (!leftComplete) {
                        if (confDivisions > 1)
                            ((TimeConfRange) ll).set(leftStart, leftMid, cMin, cMax);
                        else
                            ll.set(leftStart, leftMid);

                        lll = ll;
                    } else {
                        lll = null;
                    }

                    TimeRange rrr;
                    if (!rightComplete && !(leftStart == rightMid && leftMid == rightEnd)) {
                        if (confDivisions > 1)
                            ((TimeConfRange) rr).set(rightMid, rightEnd, cMin, cMax);
                        else
                            rr.set(rightMid, rightEnd);
                        rrr = rr;
                    } else {
                        rrr = null;
                    }

                    if (lll!=null || rrr!=null) {
                        table.readOptimistic((Space<TaskRegion> tree) -> {
                            if (lll!=null)
                                tree.whileEachIntersecting(lll, this);
                            if (rrr!=null)
                                tree.whileEachIntersecting(rrr, this);
                        });
                    }

                    if (count++ == FATAL_LIMIT) {
                        throw new RuntimeException("livelock in rtree scan");
                    }
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
