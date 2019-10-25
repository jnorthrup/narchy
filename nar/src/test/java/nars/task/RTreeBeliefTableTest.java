package nars.task;

import jcog.data.list.FasterList;
import jcog.math.MultiStatistics;
import jcog.pri.ScalarValue;
import jcog.signal.meter.event.CSVOutput;
import jcog.sort.FloatRank;
import jcog.tree.rtree.HyperIterator;
import jcog.tree.rtree.RTree;
import nars.*;
import nars.concept.TaskConcept;
import nars.control.op.Remember;
import nars.table.BeliefTable;
import nars.table.BeliefTables;
import nars.table.eternal.EternalTable;
import nars.table.temporal.RTreeBeliefTable;
import nars.table.temporal.TemporalBeliefTable;
import nars.task.util.Answer;
import nars.task.util.TaskRegion;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atomic;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.function.primitive.LongToFloatFunction;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.stream.Collectors.toList;
import static jcog.Texts.*;
import static nars.Op.BELIEF;
import static nars.task.TaskTest.task;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RTreeBeliefTableTest {

    private static final LongToFloatFunction stepFunction = new LongToFloatFunction() {
        @Override
        public float valueOf(long t) {
            return (Math.sin(t) / 2f + 0.5f) >= 0.5 ? 1f : 0f;
        }
    };

    private static Task add(BeliefTable r, Termed x, float freq, float conf, long start, long end, NAR n) {
        return add(r, x, freq, conf, start, end, n.evidence()[0], n);
    }

    private static Task add(BeliefTable r, Termed x, float freq, float conf, long start, long end, long evi, NAR n) {
        Task a = task(x.term(), BELIEF, freq, conf).time(start, start, end).evidence(evi).apply(n);
        a.pri(0.5f);
        r.remember(Remember.the(a, n.what()));
        return a;
    }
    static final Term x = Atomic.atom("x");
    private static void testAccuracy(int dur, int period, int end, int cap, LongToFloatFunction func) {

        NAR n = NARS.shell();

        n.time.dur(dur);


        TaskConcept c = (TaskConcept) n.conceptualize(x);
        @NotNull BeliefTables cb = (BeliefTables) (true ? c.beliefs() : c.goals());

        cb.tableFirst(EternalTable.class).setTaskCapacity(0);
        cb.tableFirst(TemporalBeliefTable.class).setTaskCapacity(cap);


        System.out.println("points:");
        long time;
        long start = n.time();
        while ((time = n.time()) < end) {
            float f = func.valueOf(time);
            System.out.print(time + "=" + f + '\t');
            n.input(task(x, BELIEF, f, 0.9f).time(time).withPri(0.5f).apply(n));
            n.run(period);
            c.beliefs().print();
            System.out.println();


        }
        System.out.println();
        System.out.println();


        MultiStatistics<Task> m = new MultiStatistics<Task>()
                .classify("input", Task::isInput)
                .classify("derived", new Predicate<Task>() {
                    @Override
                    public boolean test(Task t) {
                        return t instanceof DerivedTask;
                    }
                })

                .value("pri", ScalarValue::pri)
                .value2D("truth", new Function<Task, float[]>() {
                    @Override
                    public float[] apply(Task t) {
                        return new float[]{t.freq(), t.conf()};
                    }
                })
                .value("freqErr", new FloatFunction<Task>() {
                    @Override
                    public float floatValueOf(Task t) {
                        return Math.abs(((t.freq() - 0.5f) * 2f) - func.valueOf(t.mid()));
                    }
                })
                .add(c.beliefs().taskStream().collect(toList()));

        System.out.println();
        m.print();
        System.out.println();

        c.beliefs().print();


        CSVOutput csv = new CSVOutput(System.out, "time", "actual", "approx");

        double errSum = 0;
        for (long i = start; i < end; i++) {
            float actual = func.valueOf(i);

            Truth actualTruth = n.beliefTruth(x, i);
            float approx, err;
            if (actualTruth != null) {
                approx = actualTruth.freq();
                err = Math.abs(approx - actual);
            } else {
                approx = Float.NaN;
                err = 1f;
            }

            errSum += err;

            csv.out(i, actual, approx);

        }
        double avgErr = errSum / (end - start + 1);
        System.out.println();
        System.out.println(INSTANCE.n4(avgErr) + " avg freq err per point");
        assertTrue(avgErr < 0.4f);
    }

    static final Term ab = nars.$.INSTANCE.$$("a:b");
    @Test
    void testBasicOperations() {
        NAR n = NARS.shell();
        TaskConcept X = (TaskConcept) n.conceptualize(ab);
        RTreeBeliefTable r = new RTreeBeliefTable();
        r.setTaskCapacity(4);

        assertEquals(0, r.size());

        Term x = X.term();
        float freq = 1f;
        float conf = 0.9f;
//        int creationTime = 1;
        int start = 1, end = 1;

        Task a = add(r, x, freq, conf, start, end, n);
        assertEquals(1, r.size());


        r.remember(Remember.the(a, n.what()));
        r.print(System.out);
        assertEquals(1, r.size());

        Task b = add(r, x, 0f, 0.5f, 1, 1, n);
        assertEquals(2, r.size());

        Task c = add(r, x, 0.1f, 0.9f, 2, 2, n);
        assertEquals(3, r.size());

        Task d = add(r, x, 0.1f, 0.9f, 3, 4, n);
        assertEquals(4, r.size());

        System.out.println("at capacity");
        r.print(System.out);


        Task e = add(r, x, 0.3f, 0.9f, 3, 4, n);

        System.out.println("\nat capacity?");
        r.print(System.out);
        r.forEachTask(System.out::println);

//        assertEquals(4, r.size());

        System.out.println("after capacity compress inserting " + e.toString(true));
        r.print(System.out);
    }

    @Test
    void testProjection() throws Narsese.NarseseException {
        NAR nar = NARS.shell();
        Term ab = nars.$.INSTANCE.$("a:b");
        TaskConcept AB = (TaskConcept) nar.conceptualize(ab);
        RTreeBeliefTable r = new RTreeBeliefTable();
        r.setTaskCapacity(4);

        add(r, AB, 1f, 0.9f, 0, 1, nar);
        add(r, AB, 0f, 0.9f, 2, 3, nar);


        assertEquals("%1.0;.90%", r.truth(0, 0, ab, null, nar).toString());
        assertEquals("%1.0;.90%", r.truth(1, 1, ab, null, nar).toString());
        assertEquals("%1.0;.90%", r.truth(0, 1, ab, null, nar).toString());

        assertEquals("%0.0;.90%", r.truth(2, 3, ab, null, nar).toString());
        assertEquals("%0.0;.90%", r.truth(3, 3, ab, null, nar).toString());

        assertEquals("%.50;.90%", r.truth(1, 2, ab, null, nar).toString());

        assertEquals("%.33;.87%", r.truth(4, 4, ab, null, nar).toString());
        assertEquals("%.35;.85%", r.truth(4, 5, ab, null, nar).toString());
        assertEquals("%.38;.83%", r.truth(5, 5, ab, null, nar).toString());
        assertEquals("%.40;.79%", r.truth(6, 6, ab, null, nar).toString());
        assertEquals("%.39;.79%", r.truth(5, 8, ab, null, nar).toString());
        assertEquals("%.44;.67%", r.truth(10, 10, ab, null, nar).toString());

    }

    @Test
    void testAccuracyFlat() {

        testAccuracy(1, 1, 20, 8, new LongToFloatFunction() {
            @Override
            public float valueOf(long t) {
                return 0.5f;
            }
        });
    }

    @Test
    void testAccuracySineDur1() {

        testAccuracy(1, 1, 20, 8, new LongToFloatFunction() {
            @Override
            public float valueOf(long t) {
                return (float) (Math.sin(t / 5f) / 2f + 0.5f);
            }
        });
    }

    @Test
    void testAccuracySineDur1Ext() {
        testAccuracy(1, 1, 50, 8, new LongToFloatFunction() {
            @Override
            public float valueOf(long t) {
                return (float) (Math.sin(t / 1f) / 2f + 0.5f);
            }
        });
    }

    @Test
    void testAccuracySineDur() {
        testAccuracy(2, 2, 50, 8, new LongToFloatFunction() {
            @Override
            public float valueOf(long t) {
                return (float) (Math.sin(t / 5f) / 2f + 0.5f);
            }
        });
    }

    @Test
    void testAccuracySawtoothWave() {

        testAccuracy(1, 3, 15, 5, stepFunction);
    }

    @Test
    void testAccuracySquareWave() {
        testAccuracy(1, 1, 7, 5, stepFunction);
    }

    @Test
    void testHyperIterator() {
        NAR n = NARS.shell();

        n.time.dur(1);

        Term term = $.INSTANCE.p("x");

        TaskConcept c = (TaskConcept) n.conceptualize(term);
        BeliefTables cb = (BeliefTables) (true ? c.beliefs() : c.goals());

        int cap = 16;

        RTreeBeliefTable table = cb.tableFirst(RTreeBeliefTable.class);
        table.setTaskCapacity(cap);


        int horizon = 50;
        int maxRange = 8;

        //populate table randomly
        for (int i = 0; i < cap; i++) {
            long start = n.random().nextInt(horizon);
            long end = start + n.random().nextInt(maxRange);
            add(table, term, 1f, 0.9f, start, end, n);
        }
        table.print();


        List<TaskRegion> shouldBeAscendingTimes = seek(table, -1, -1);
        print("shouldBeAscendingTimes", shouldBeAscendingTimes);
        List<TaskRegion> shouldBeDescendingTimes = seek(table, horizon + 1, horizon + 1);
        print("shouldBeDescendingTimes", shouldBeDescendingTimes);
        List<TaskRegion> shouldOscillateOutwardFromMidpoint = seek(table, horizon / 2, horizon / 2);
        print("shouldOscillateOutwardFromMidpoint", shouldOscillateOutwardFromMidpoint);
    }

    @Deprecated
    static List<TaskRegion> seek(RTreeBeliefTable table, long s, long e) {
        int c = table.capacity();
        List<TaskRegion> seq = new FasterList(c);
        double dur = table.tableDur((s+e)/2);
        table.read(new Consumer<RTree<TaskRegion>>() {
            @Override
            public void accept(RTree<TaskRegion> t) {
                HyperIterator<TaskRegion> h = new HyperIterator(t.model,
                        new TaskRegion[Math.min(c, 32)],
                        FloatRank.the(Answer.beliefStrength(s, e)));
                while (h.hasNext()) {
                    seq.add(h.next());
                }
            }
        });
        return seq;
    }

    static void print(String msg, List<TaskRegion> seq) {
        System.out.println(msg);
        int i = 0;
        for (TaskRegion t : seq)
            System.out.println("\t" + (i++) + ": " + t);
        System.out.println();
    }

    @Test
    void testSplitOrdering() {
        NAR n = NARS.shell();

        n.time.dur(1);

        Term term = $.INSTANCE.p("x");

        TaskConcept c = (TaskConcept) n.conceptualize(term);
        BeliefTables cb = (BeliefTables) (true ? c.beliefs() : c.goals());

        int cap = 64;

        RTreeBeliefTable table = cb.tableFirst(RTreeBeliefTable.class);
        table.setTaskCapacity(cap);


        int horizon = 50;
        int maxRange = 8;

        //populate table randomly
        for (int i = 0; i < cap; i++) {
            long start = n.random().nextInt(horizon);
            long end = start + n.random().nextInt(maxRange);
            add(table, term, n.random().nextFloat(), n.random().nextFloat() * 0.8f + 0.1f, start, end, n);
        }
        table.print();

        table.read(new Consumer<RTree<TaskRegion>>() {
            @Override
            public void accept(RTree<TaskRegion> t) {
                t.root().streamNodesRecursively().forEach(System.out::println);
            }
        });
    }

    @Test
    void testSubsumeContainedEvent() {
        testSubsumeContainedEvent(true);
    }

    @Test
    void testSubsumeContainedByEvent() {
        testSubsumeContainedEvent(false);
    }

    static void testSubsumeContainedEvent(boolean forward) {
        NAR n = NARS.shell();
        TaskConcept AB = (TaskConcept) n.conceptualize(ab);
        RTreeBeliefTable r = new RTreeBeliefTable();
        r.setTaskCapacity(4);

        long as = 0;
        long ae = 3;
        long bs = 1;
        long be = 2;
        long sameStamp = 1;
        Task x = add(r, AB, 1f, 0.9f, forward ?  as : bs, forward ? ae : be, sameStamp, n);
        Task y = add(r, AB, 1f, 0.9f, forward ?  bs : as, forward ? be : ae, sameStamp, n);

        assertEquals(1, r.size());
        assertEquals(4, r.iterator().next().range());
        assertEquals(4, r.bounds().range(0));
    }
}