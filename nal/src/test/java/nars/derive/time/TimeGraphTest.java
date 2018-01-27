package nars.derive.time;

import com.google.common.collect.Sets;
import nars.$;
import nars.Narsese;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static nars.time.Tense.ETERNAL;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TimeGraphTest {

    /**
     * example time graphs
     */
    final TimeGraph A; {
        A = newTimeGraph(1);
        A.know($.$safe("((one &&+1 two) ==>+1 (three &&+1 four))"), ETERNAL);
        A.know($.$safe("one"), 1);
        A.know($.$safe("two"), 20);

    }
    final TimeGraph B; {
        //               .believe("(y ==>+3 x)")
        //                .believe("(y ==>+2 z)")
        //                .mustBelieve(cycles, "(z ==>+1 x)", 1.00f, 0.45f)
        //                .mustNotOutput(cycles, "(z ==>-1 x)", BELIEF, Tense.ETERNAL)
        //                .mustBelieve(cycles, "(x ==>-1 z)", 1.00f, 0.45f)
        //                .mustNotOutput(cycles, "(x ==>+1 z)", BELIEF, Tense.ETERNAL)
        B = newTimeGraph(1);
        B.know($.$safe("(y ==>+3 x)"), ETERNAL);
        B.know($.$safe("(y ==>+2 z)"), ETERNAL);
    }

//    @BeforeEach
//    void init() {
//    }

    @Test
    public void testAtomEvent() {
        A.print();
        assertSolved("one", A, "one@1", "one@19");
    }

    @Test
    public void testSimpleConjWithOneKnownAbsoluteSubEvent1() {

        assertSolved("(one &&+1 two)", A,
                "(one &&+1 two)@1", "(one &&+1 two)@19");

    }

    @Test
    public void testSimpleConjWithOneKnownAbsoluteSubEvent2() {
        assertSolved("(one &&+- two)", A,
                "(one &&+1 two)","(one &&+1 two)@1", "(one &&+19 two)@1", "(one &&+1 two)@19");
    }

    @Test
    public void testSimpleConjOfTermsAcrossImpl1() {

        assertSolved("(two &&+1 three)", A,
                "(two &&+1 three)@2", "(two &&+1 three)@20");
    }
    @Test
    public void testSimpleConjOfTermsAcrossImpl2() {

        assertSolved("(two &&+- three)", A,

                "(two &&+1 three)", "(two &&+1 three)@2", "(two &&+1 three)@20");

    }

    @Test
    public void testSimpleImplWithOneKnownAbsoluteSubEvent() {
        A.print();
        assertSolved("(one ==>+- three)", A,
                "(one ==>+2 three)", "(one ==>+20 three)", "(one ==>-16 three)");
    }

    @Test public void testImplChain1() {
        B.print();
        assertSolved("(z ==>+- x)", B, "(z ==>+1 x)");
    }

    @Test public void testImplChain2() {
        assertSolved("(x ==>+- z)", B, "(x ==>-1 z)");
    }



    @Test public void testConjChain1() throws Narsese.NarseseException {
        /** c is the same event, @6 */
        TimeGraph cc1 = newTimeGraph(1);
        cc1.know($.$("(a &&+5 b)"), 1);
        cc1.know($.$("(b &&+5 c)"), 6);
        cc1.print();
        assertSolved("(a &&+- c)", cc1,
                "(a &&+10 c)@1");
    }

    @Test public void testExact() throws Narsese.NarseseException {

        TimeGraph cc1 = newTimeGraph(1);
        cc1.know($.$("(a &&+5 b)"), 1);
        cc1.print();
        assertSolved("(a &&+- b)", cc1,
                "(a &&+5 b)@1");
    }

    @Test public void testLinkedTemporalConj() throws Narsese.NarseseException {

        TimeGraph cc1 = newTimeGraph(1);
        cc1.know($.$("(a &&+5 b)"), 1);
        cc1.know($.$("(b &&+5 c)"), 6);
        cc1.print();
        assertSolved("((b &&+5 c) &&+- (a &&+5 b))", cc1,
                "((a &&+5 b) &&+5 c)", "((a &&+5 b) &&+5 c)@1");
    }

    @Test
    public void testImplWithConjPredicate1() {
        assertSolved("(one ==>+- (two &&+1 three))", A,
                "(one ==>+1 (two &&+1 three))");
        A.print();
    }


    @Test public void testDecomposeImplConj() throws Narsese.NarseseException {
        /*
          $.02 ((b &&+10 d)==>a). 6 %1.0;.38% {320: 1;2;3;;} (((%1==>%2),(%1==>%2),is(%1,"&&")),((dropAnyEvent(%1) ==>+- %2),((StructuralDeduction-->Belief))))
            $.06 (((b &&+5 c) &&+5 d) ==>-15 a). 6 %1.0;.42% {94: 1;2;3} ((%1,%2,time(raw),belief(positive),task("."),notImpl(%2),notImpl(%1)),((%2 ==>+- %1),((Induction-->Belief))))
        */
        TimeGraph C = newTimeGraph(1);
        C.know($.$("(((b &&+5 c) &&+5 d) ==>-15 a)"), 6);
        assertSolved("((b &&+10 d) ==>+- a)", C, "((b &&+10 d) ==>-15 a)");
    }

    @Test
    public void testImplWithConjPredicate2() {
        assertSolved("(one ==>+- (two &&+- three))", A, new String[]{
                //using one@1

                "((one &&+1 two) ==>+1 three)", "(one ==>+- (two &&+1 three))" //@ETE ?
                //using two@20
                //"(one ==>+1 (two &&+1 three))@19"
        });
    }
    @Test public void testNobrainerNegation() throws Narsese.NarseseException {

        TimeGraph C = newTimeGraph(1);
        C.know($.$("x"), 1);
        C.know($.$("y"), 2);
        C.print();
        System.out.println();
        assertSolved("(--x ==>+- y)", C, "((--,x) ==>+1 y)");
        C.print();
    }
    @Test
    public void testConjSimpleOccurrences() throws Narsese.NarseseException {
        TimeGraph C = newTimeGraph(1);
        C.know($.$("(x &&+5 y)"), 1);
        C.know($.$("(y &&+5 z)"), 6);
        C.know($.$("(w &&+5 x)"), -4);
        C.print();
        System.out.println();
        assertSolved("x", C, "x@1");
        assertSolved("y", C, "y@6");
        assertSolved("z", C, "z@11");
        assertSolved("w", C, "w@-4");
        C.print();
    }
    @Test
    public void testConjTrickyOccurrences() throws Narsese.NarseseException {
        TimeGraph C = newTimeGraph(1);
        C.know($.$("(x &&+5 y)"), 1);
        C.know($.$("(y &&+5 z)"), 3);
        assertSolved("x", C, "x@1", "x@-2");
        assertSolved("y", C, "y@6", "y@3");
        assertSolved("z", C, "z@11", "z@8");
    }

    @Test
    public void testImplCrossDternalInternalConj() throws Narsese.NarseseException {
        TimeGraph C = newTimeGraph(1);

        C.know($.$("((a&&x) ==>+1 b)"), 1);
        //C.print(); System.out.println();
        assertSolved("(a ==>+- b)", C, "(a ==>+1 b)");
        //C.print(); System.out.println();
    }
    @Test
    public void testImplCrossParallelInternalConj() throws Narsese.NarseseException {
        TimeGraph C = newTimeGraph(1);
        C.know($.$("((a&|x) ==>+1 b)"), 1);
        assertSolved("(a ==>+- b)", C, "(a ==>+1 b)");
    }

    final List<Runnable> afterEach = $.newArrayList();

    @AfterEach
    void test() {
        afterEach.forEach(Runnable::run);
    }

    void assertSolved(String inputTerm, TimeGraph t, String... solutions) {

        int nodesBefore = A.nodes().size();
        String nodes = A.nodes().toString();
        long edgesBefore = A.edges().count();

        {
            ExpectSolutions ee = new ExpectSolutions(solutions);

            t.solve($.$safe(inputTerm), false, ee);
        }

        int nodesAfter = A.nodes().size();
        long edgesAfter = A.edges().count();
        //assertEquals(edgesBefore, edgesAfter, "# of edges changed as a result of solving");
//        assertEquals(nodesBefore, nodesAfter, ()->"# of nodes changed as a result of solving:\n\t" + nodes + "\n\t" + A.nodes());

    }

    private class ExpectSolutions extends TreeSet<String> implements Predicate<TimeGraph.Event> {

        final Supplier<String> errorMsg;
        private final String[] solutions;

        public ExpectSolutions(String... solutions) {
            this.solutions = solutions;
            errorMsg = () ->
                    "expect: " + Arrays.toString(solutions) + "\n   got: " + toString();

            afterEach.add(() -> {
                assertEquals(Sets.newTreeSet(List.of(solutions)), this, errorMsg);
                System.out.print(errorMsg.get());
            });
        }

        @Override
        public boolean test(TimeGraph.Event termEvent) {
            add(termEvent.toString());
            return true;
        }

    }

    static TimeGraph newTimeGraph(long seed) {
//        XoRoShiRo128PlusRandom rng = new XoRoShiRo128PlusRandom(seed);
        return new TimeGraph() {
//            @Override
//            protected Random random() {
//                return rng;
//            }
        };
    }


}

//    public static void main(String[] args) throws Narsese.NarseseException {
//
//        TimeGraph t = new TimeGraph();
////        t.know($.$("(a ==>+1 b)"), ETERNAL);
////        t.know($.$("(b ==>+1 (c &&+1 d))"), 0);
////        t.know($.$("(a &&+1 b)"), 4);
//
//        t.know($.$("((one &&+1 two) ==>+1 (three &&+1 four))"), ETERNAL);
//        t.know($.$("one"), 1);
//        t.know($.$("two"), 20);
//
//        t.print();
//
//        System.out.println();
//
//        for (String s : List.of(
//                "one",
//                "(one &&+1 two)", "(one &&+- two)",
//                "(two &&+1 three)", "(two &&+- three)",
//                "(one ==>+- three)",
//                "(one ==>+- (two &&+1 three))",
//                "(one ==>+- (two &&+- three))"
//        )) {
//            Term x = $.$(s);
//            System.out.println("SOLVE: " + x);
//            t.solve(x, (y) -> {
//                System.out.println("\t" + y);
//                return true;
//            });
//        }
//
//
//    }