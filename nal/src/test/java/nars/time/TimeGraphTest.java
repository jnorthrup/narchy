package nars.time;

import com.google.common.collect.Sets;
import jcog.random.XoRoShiRo128PlusRandom;
import nars.$;
import nars.Narsese;
import nars.Op;
import nars.term.Term;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static nars.$.*;
import static nars.$.*;
import static nars.Op.CONJ;
import static nars.time.Tense.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeGraphTest {

    /**
     * example time graphs
     */
    private final TimeGraph A;

    {
        A = newTimeGraph(1);
        A.know(INSTANCE.$$("((one &&+1 two) ==>+1 (three &&+1 four))"), ETERNAL);
        A.know(INSTANCE.$$("one"), 1);
        A.know(INSTANCE.$$("two"), 20);

    }

    private final TimeGraph B;

    {


        B = newTimeGraph(1);
        B.know(INSTANCE.$$("(y ==>+3 x)"), ETERNAL);
        B.know(INSTANCE.$$("(y ==>+2 z)"), ETERNAL);
    }


    @Test
    void testAtomEvent() {

        assertSolved("one", A, "one@1", "one@19");
    }

    @Test
    void testSimpleConjWithOneKnownAbsoluteSubEvent1() {

        assertSolved("(one &&+1 two)", A,
                "(one &&+1 two)@1", "(one &&+1 two)@19");

    }

    @Test
    void testSimpleConjWithOneKnownAbsoluteSubEvent2() {
        assertSolved("(one &&+- two)", A,
                "(one &&+1 two)@1", "(one &&+19 two)@1", "(one &&+1 two)@19", "(two &&+17 one)@2", "(one &&+1 two)@21");

    }

    @Test
    void testSimpleConjOfTermsAcrossImpl1() {

        assertSolved("(two &&+1 three)", A,
                "(two &&+1 three)@2", "(two &&+1 three)@20");
    }

    @Test
    void testSimpleConjOfTermsAcrossImpl2() {

        assertSolved("(two &&+- three)", A,

                "(two &&+1 three)@2", "(three &&+17 two)@3", "(two &&+1 three)@2", "(two &&+1 three)@2", "(two &&+1 three)@20", "(two &&+19 three)@2");

    }

    @Test
    void testSimpleImplWithOneKnownAbsoluteSubEvent() {

        assertSolved("(one ==>+- three)", A,
                "(one ==>+2 three)", "(one ==>+20 three)", "(one ==>-16 three)");
    }
    @Test
    void testSimpleImplWithOneKnownAbsoluteSubEvent2() {

        TimeGraph cc1 = newTimeGraph(1);
        cc1.know(INSTANCE.$$("(x ==>+1 y)"), 1);
        cc1.know(INSTANCE.$$("x"), 1);
        assertSolved("y", cc1, "y@2");
    }
    @Test
    void testSimpleSelfImplWithOneKnownAbsoluteSubEvent2() {

        for (String rel : new String[] { "==>+1", "==>-1" }) {
            TimeGraph cc1 = newTimeGraph(1);
            cc1.know(INSTANCE.$$("(x " + rel + " x)"), ETERNAL);
            cc1.know(INSTANCE.$$("x"), 1);

            assertSolved("x", cc1, "x@1", "x@2", "x@0" /* .. */);
        }
    }
    @Test
    void implInvert1() {
        TimeGraph cc1 = newTimeGraph(1);
        cc1.know(INSTANCE.$$("(--x ==>+1 x)"));
        cc1.know(INSTANCE.$$("x"), 1);
        assertSolved("--x", cc1, "(--,x)@0");
    }
    @Test
    void implInvert2() {
        TimeGraph cc1 = newTimeGraph(1);
        cc1.know(INSTANCE.$$("(--x ==>+1 x)"));
        cc1.know(INSTANCE.$$("--x"), 1);
        assertSolved("x", cc1, "x@2");
    }


    @Test
    void testSimpleImplWithOneKnownAbsoluteSubEventNegOpposite() {

        TimeGraph cc1 = newTimeGraph(1);
//        cc1.autoNeg.add($$("x"));
        cc1.know(INSTANCE.$$("(x ==>+1 y)"), 1);
        cc1.know(INSTANCE.$$("x"), 1);
        assertSolved("y", cc1, "y@2");
    }

    @Test
    void testImplChain1() {

        assertSolved("(z ==>+- x)", B, "(z ==>+1 x)");
    }

    @Test
    void testImplChain2() {
        assertSolved("(x ==>+- z)", B, "(x ==>-1 z)");
    }


    @Test
    void testConjChain1() throws Narsese.NarseseException {
        /** c is the same event, @6 */
        TimeGraph cc1 = newTimeGraph(1);
        cc1.know(INSTANCE.$("(a &&+5 b)"), 1);
        cc1.know(INSTANCE.$("(b &&+5 c)"), 6);

        assertSolved("(a &&+- c)", cc1,
                "(a &&+10 c)@1");
    }

    @Test
    void testImplSame() throws Narsese.NarseseException {
        TimeGraph c = newTimeGraph(1);
        c.know(INSTANCE.$("(x ==>+1 x)"));
        c.know(INSTANCE.$("x"), 0);
        assertSolved("x", c, "x@-1", "x@0", "x@1");
    }

    @Test
    void testConjNegComponent() throws Narsese.NarseseException {

        TimeGraph g = newTimeGraph(1);
        g.autoneg = true;
        g.know(INSTANCE.$("(--x || y)"));
        g.know(INSTANCE.$("x"), 0);
        assertSolved("(--x || y)", g, "(--x || y)@0");
        assertSolved("y", g, "y@0");
    }

    @Test
    void testExact() throws Narsese.NarseseException {

        TimeGraph cc1 = newTimeGraph(1);
        cc1.know(INSTANCE.$("(a &&+5 b)"), 1);

        assertSolved("(a &&+- b)", cc1,
                "(a &&+5 b)@1");
    }

    @Test
    void testLinkedTemporalConj() throws Narsese.NarseseException {

        TimeGraph cc1 = newTimeGraph(1);
        cc1.know(INSTANCE.$("(a &&+5 b)"), 1);
        cc1.know(INSTANCE.$("(b &&+5 c)"), 6);

        assertSolved("((b &&+5 c) &&+- (a &&+5 b))", cc1,
                "((a &&+5 b) &&+5 c)@1");
    }

    @Test
    void testImplWithConjPredicate1() {
        ExpectSolutions t = assertSolved(
                "(one ==>+- (two &&+1 three))", A,
                "(one ==>+1 (two &&+1 three))", "(one ==>+19 (two &&+1 three))", "(one ==>-17 (two &&+1 three))");
    }

    @Test
    void testImplWithConjPredicate1a() {
        ExpectSolutions t = assertSolved(
                "(one ==>+- two)", A,
                "(one ==>+1 two)", "(one ==>+19 two)", "(one ==>-17 two)");
    }

    @Test
    void testDecomposeImplConj() throws Narsese.NarseseException {
        /*
          $.02 ((b &&+10 d)==>a). 6 %1.0;.38% {320: 1;2;3;;} (((%1==>%2),(%1==>%2),is(%1,"&&")),((dropAnyEvent(%1) ==>+- %2),((StructuralDeduction-->Belief))))
            $.06 (((b &&+5 c) &&+5 d) ==>-15 a). 6 %1.0;.42% {94: 1;2;3} ((%1,%2,time(raw),belief(positive),task("."),notImpl(%2),notImpl(%1)),((%2 ==>+- %1),((Induction-->Belief))))
        */
        TimeGraph C = newTimeGraph(1);
        C.know(INSTANCE.$("(((b &&+5 c) &&+5 d) ==>-15 a)"), 6);
        assertSolved("((b &&+5 c) ==>+- a)", C, "((b &&+5 c) ==>-10 a)");
        assertSolved("((b &&+10 d) ==>+- a)", C, "((b &&+10 d) ==>-15 a)");
    }

    @Test
    void testImplWithConjPredicate2pre() {
        assertSolved("(two &&+- three)", A,
        "(three &&+17 two)@3", "(two &&+1 three)@2", "(two &&+1 three)@20", "(two &&+19 three)@2"
        );
    }

    @Test
    void testImplWithConjPredicate2() {
        //results may be random
        assertSolved("(one ==>+- (two &&+- three))", A,

                "(one ==>+- (three &&+17 two))", "(one ==>+- (two &&+1 three))", "(one ==>+- (two &&+19 three))", "(one ==>+1 (two &&+19 three))", "(one ==>+19 (two &&+1 three))", "(one ==>+19 (two &&+19 three))", "(one ==>+38 (three &&+17 two))"
                //"(one ==>+19 (two &&+19 three))"
                //"(one ==>+2 (three &&+17 two))", "(one ==>+20 (three &&+17 two))", "(one ==>-16 (three &&+17 two))", "(one ==>-17 (two &&+1 three))", "(one ==>-17 (two &&+19 three))"
        );
    }

    @Test
    void testConj3() throws Narsese.NarseseException {
        TimeGraph C = newTimeGraph(1);
        C.know(INSTANCE.$("a"), 1);
        C.know(INSTANCE.$("b"), 2);
        C.know(INSTANCE.$("c"), 3);
        assertSolved("(&&+-, a,b,c)", C, "((a &&+1 b) &&+1 c)@1");
    }
    @Test
    void testConj3_parallel_seq() throws Narsese.NarseseException {
        TimeGraph C = newTimeGraph(1);
        C.know(INSTANCE.$("(a&&b)"), 1);
        C.know(INSTANCE.$("c"), 2);
        assertSolved("(&&+-, a,b,c)", C, "((a&&b) &&+1 c)@1");
    }
    @Test
    void testConj3_multiple_solutions() throws Narsese.NarseseException {
        TimeGraph C = newTimeGraph(1);
        C.know(INSTANCE.$("a"), 1);
        C.know(INSTANCE.$("a"), 0);
        C.know(INSTANCE.$("b"), 2);
        C.know(INSTANCE.$("c"), 3);
        assertSolved("(&&+-, a,b,c)", C, "((a &&+1 b) &&+1 c)@1", "((a &&+2 b) &&+1 c)@0");
    }

    @Test
    void testConj3_repeat() throws Narsese.NarseseException {
        TimeGraph C = newTimeGraph(1, true);
        C.know(INSTANCE.$("(a ==>+1 a)"));
        C.know(INSTANCE.$("a"), 0);
        C.know(INSTANCE.$("b"), 2);
        C.know(INSTANCE.$("c"), 3);
        assertSolved("(&&+-, (a &&+- a),b,c)", C,
                "(((a &&+1 a) &&+1 b) &&+1 c)@0", "((a &&+2 b) &&+1 c)@0"
        );
    }
    @Test
    void repeatCollapse() throws Narsese.NarseseException {
        TimeGraph C = newTimeGraph(1);
        C.know(INSTANCE.$("a"), 0);
        C.know(INSTANCE.$("b"), 2);
        assertSolved("(a &&+- a)", C, "a@0");
        assertSolved("((a &&+- a) ==>+- b)", C, "(a ==>+2 b)");
    }
    @Test
    void repeatCollapse2() throws Narsese.NarseseException {
        TimeGraph C = newTimeGraph(1);
        C.know(INSTANCE.$("a"), 0);
        C.know(INSTANCE.$("a"), 2);
        assertSolved("(a ==>+- a)", C, "(a ==>+2 a)", "(a ==>-2 a)"); //1 solution
        assertSolvedIncludes("(a &&+- a)", C, "a@0", "a@2", "(a &&+2 a)@0" ); //2+ solutions
    }

    @Test
    void testConj3_partial() throws Narsese.NarseseException {
        TimeGraph C = newTimeGraph(1);
        C.know(INSTANCE.$("a"), 1);
        C.know(INSTANCE.$("b"), 2);
        assertSolved("(&&+-, a,b,c)", C, "((a &&+1 b) &&+- c)@1");
    }

    @Test
    void testImplWithTwoConjPredicates() throws Narsese.NarseseException {


        TimeGraph C = newTimeGraph(1);
        C.know(INSTANCE.$("(a &&+5 b)"), 1);
        C.know(INSTANCE.$("(b &&+5 c)"), 3);
        assertSolved("((a &&+5 b) ==>+- (b &&+5 c))", C,
                "((a &&+5 b) ==>+5 c)", "((a &&+5 b) ==>-3 (b &&+5 c))", "((a &&+5 b) ==>+3 (b &&+5 c))");
    }

    @Test
    void testImplWithConjSubjDecomposeProperly() throws Narsese.NarseseException {


        TimeGraph C = newTimeGraph(1);
        C.know(INSTANCE.$("b"), 6);
        C.know(INSTANCE.$("((a &&+1 a2)=|>b)"), 1);

        System.out.println();
        assertSolved("(a &&+1 a2)", C,
                "(a &&+1 a2)@5");

    }

    @Test void NoBrainerNegation_impl() {

        TimeGraph C = newTimeGraph(1);
        C.know(INSTANCE.$$("x"), 1);
        C.know(INSTANCE.$$("y"), 2);
        C.autoneg = true;

        assertSolved("(--x ==>+- y)", C, "((--,x) ==>+1 y)");
    }
    @Test void NoBrainerNegation_conj() {

        TimeGraph C = newTimeGraph(1);
        C.know(INSTANCE.$$("x"), 1);
        C.know(INSTANCE.$$("y"), 2);

        assertSolved("(--x &&+- y)", C, "((--,x) &&+1 y)");
    }

    @Test
    void testComponentInTwoConjunctionSequences() {

        TimeGraph C = newTimeGraph(1);
        C.know(INSTANCE.$$("(_1 &&+1 _2)"), ETERNAL);
        C.know(INSTANCE.$$("((_4 &&+1 _1) &&+1 (_2 &&+1 _3))"), ETERNAL);

        System.out.println();
        assertSolved("_4", C,
                "_4@ETE");

    }
    @Test
    void testConjSimpleOccurrences() throws Narsese.NarseseException {
        TimeGraph C = newTimeGraph(1);
        C.know(INSTANCE.$("(x &&+5 y)"), 1);
        C.know(INSTANCE.$("(y &&+5 z)"), 6);
        C.know(INSTANCE.$("(w &&+5 x)"), -4);

        System.out.println();
        assertSolved("x", C, "x@1");
        assertSolved("y", C, "y@6");
        assertSolved("z", C, "z@11");
        assertSolved("w", C, "w@-4");

    }

    @Test
    void testConjTrickyOccurrences() throws Narsese.NarseseException {
        TimeGraph C = newTimeGraph(1);
        C.know(INSTANCE.$("(x &&+5 y)"), 1);
        C.know(INSTANCE.$("(y &&+5 z)"), 3);
        assertSolved("x", C, "x@1", "x@-2");
        assertSolved("y", C, "y@6", "y@3");
        assertSolved("z", C, "z@11", "z@8");
    }

    @Test
    void testImplCross_Subj_DternalConj() throws Narsese.NarseseException {
        for (String inner : new String[]{" ==>+1 ", " ==>-1 ", "==>" }) {
            TimeGraph C = newTimeGraph(1);
            C.know(INSTANCE.$("((a&&x)" + inner + "b)"), 1);
            assertSolved("(a ==>+- b)", C, "(a" + inner + "b)");
        }
    }
    @Test
    void testImplCross_Pred_DternalConj() throws Narsese.NarseseException {

        for (String inner : new String[]{" ==>+1 ", " ==>-1 ", "==>" }) {
            TimeGraph C = newTimeGraph(1);
            String i = "(b" + inner + "(a&&x))";
            Term ii = INSTANCE.$(i);
            assertTrue(ii.op().eventable, new Supplier<String>() {
                @Override
                public String get() {
                    return i;
                }
            });
            C.know(ii, 1);
            C.print();
            assertSolved("(b ==>+- a)", C, "(b" + inner + "a)");
        }
    }


    @Test
    void testDepVarEvents() throws Narsese.NarseseException {
        TimeGraph C = newTimeGraph(1);

        C.know(INSTANCE.$("#1"), 1);
        C.know(INSTANCE.$("x"), 3);

        assertSolved("(#1 ==>+- x)", C, "(#1 ==>+2 x)");

    }

    @Test
    void testImplConjComposeSubjNeg() throws Narsese.NarseseException {
        TimeGraph C = newTimeGraph(1);

        int NA = 1; //Not/Applicable, should also work for ETE
        C.know(INSTANCE.$("((--,y) ==>+3 z)"), NA);
        C.know(INSTANCE.$("((--,x) ==>+5 z)"), NA);
        C.print();

        assertSolved("((--x &&+- --y) ==>+- z)", C,
                "(((--,x) &&+2 (--,y)) ==>+3 z)");
    }

    @Test
    void testImplConjComposePred() throws Narsese.NarseseException {
        TimeGraph C = newTimeGraph(1);

        int NA = 1; //Not/Applicable
        C.know(INSTANCE.$("(x ==>+1 y)"), NA);
        C.know(INSTANCE.$("(x ==>+2 z)"), NA);
        assertSolved("(x ==>+- (y &&+- z))", C, "(x ==>+1 (y &&+1 z))");

    }



    @Test
    void testImplCrossParallelInternalConj() throws Narsese.NarseseException {
        TimeGraph C = newTimeGraph(1);
        C.know(INSTANCE.$("((a&|x) ==>+1 b)"), 1);
        assertSolved("(a ==>+- b)", C, "(a ==>+1 b)");
    }


    private final List<Runnable> afterEach = $.INSTANCE.newArrayList();

    @AfterEach
    void test() {
        for (Runnable runnable : afterEach) {
            runnable.run();
        }
    }

    private ExpectSolutions assertSolved(String inputTerm, TimeGraph t, boolean equalsOrContains, String... solutions) {
        return new ExpectSolutions(t, equalsOrContains, solutions).solve(inputTerm);
    }
    private ExpectSolutions assertSolved(String inputTerm, TimeGraph t, String... solutions) {
        return assertSolved(inputTerm, t, true, solutions);
    }
    private ExpectSolutions assertSolvedIncludes(String inputTerm, TimeGraph t, String... solutions) {
        return assertSolved(inputTerm, t, false, solutions);
    }

    @Test
    void testConjPartiallyEternal() {
        TimeGraph C = newTimeGraph(1);
        C.know(INSTANCE.$$("x"), ETERNAL);
        C.know(INSTANCE.$$("y"), 0);
        assertSolved("(x &&+- y)", C, "(x&&y)@0");
    }

    @Test
    void testImplicationPartiallyEternal() {
        TimeGraph C = newTimeGraph(1);
        C.know(INSTANCE.$$("x"), ETERNAL);
        C.know(INSTANCE.$$("y"), 0);
        assertSolved("(x ==>+- y)", C, "(x==>y)");
    }
    @Test
    void testImplicationPartiallyEternalReverse() {
        TimeGraph C = newTimeGraph(1);
        C.know(INSTANCE.$$("x"), ETERNAL);
        C.know(INSTANCE.$$("y"), 0);
        assertSolved("(y ==>+- x)", C, "(y==>x)");
    }
    @Test
    void testImplicationPartiallyEternal_Conj() {
        TimeGraph C = newTimeGraph(1);
        C.know(INSTANCE.$$("x"), 0);
        C.know(INSTANCE.$$("(y&&z)"), 1);
        assertSolved("(x ==>+- z)", C, "(x ==>+1 z)");
    }
    @Test
    void testImplicationPartiallyEternal_Conj_b() {
        TimeGraph C = newTimeGraph(1);
        C.know(INSTANCE.$$("x"), 0);
        C.know(INSTANCE.$$("(y&&z)"), 1);
        assertSolved("(y &&+- z)", C, "(y&&z)@1");
    }

    @Test void TemporalInRelation1() {
        TimeGraph C = newTimeGraph(1);

        C.know(INSTANCE.$$("a"), 1); //C.autoNeg.add($$("a"));
        C.know(INSTANCE.$$("b"), 2); //C.autoNeg.add($$("b"));

        assertSolved("(x-->(a &&+- b))", C, "(x-->(a &&+1 b))");
        //assertSolved("(x--> --(a &&+- b))", C, "(x-->(--,(a &&+1 b)))");
        assertSolved("(x-->(a &&+- --b))", C, "(x-->(a &&+1 (--,b)))");
        //assertSolved("(x--> --(a &&+- --b))", C, "(x-->(--,(a &&+1 (--,b))))");
        assertSolved("(x, (a ==>+- b))", C, "(x,(a ==>+1 b))");
    }

    @Test void TemporalInRelation2() {
        TimeGraph C = newTimeGraph(1);
        C.know(INSTANCE.$$("(a &&+1 b)")/*, ETERNAL*/);

        assertSolved("(x-->(a &&+- b))", C, "(x-->(a &&+1 b))");
        assertSolved("(x, (a ==>+- b))", C, "(x,(a ==>+1 b))");
    }

    @Test void TemporalInRelation2_neg() {
        TimeGraph C = newTimeGraph(1);
        C.know(INSTANCE.$$("(a &&+1 b)")/*, ETERNAL*/);

        assertSolved("(x-->(a &&+- --b))", C, "(x-->(a &&+1 (--,b)))");
        //assertSolved("(x--> --(a &&+- b))", C, "(x-->(--,(a &&+1 b)))");
        //assertSolved("(x--> --(a &&+- --b))", C, "(x-->(--,(a &&+1 (--,b))))");

    }
    private class ExpectSolutions extends ConcurrentSkipListSet<String> implements Predicate<TimeGraph.Event> {

        final Supplier<String> errorMsg;
        final String[] solutions;
        private final TimeGraph time;
        volatile int uniqueSolutions = 0;
        volatile int repeatSolutions = 0;
        Set seen = new ConcurrentSkipListSet() {

            @Override
            public boolean add(Object o) {

                if (super.add(o.toString())) {
                    uniqueSolutions++;
                    return true;
                }
                repeatSolutions++;
                return false;
            }
        };

        ExpectSolutions(TimeGraph time, boolean equalsOrContains, String... solutions) {
            this.time = time;
            this.solutions = solutions;
            errorMsg = new Supplier<String>() {
                @Override
                public String get() {
                    return "expect: " + Arrays.toString(solutions) + "\n   got: " + ExpectSolutions.this;
                }
            };

            TreeSet<String> solutionSet = Sets.newTreeSet(List.of(solutions));

            afterEach.add(new Runnable() {
                @Override
                public void run() {
                    if (equalsOrContains)
                        assertEquals(solutionSet, ExpectSolutions.this, errorMsg);
                    else
                        assertTrue(ExpectSolutions.this.containsAll(solutionSet), errorMsg);
                }
            });
        }

        @Override
        public boolean test(TimeGraph.Event y) {
            add(y.toString());
            return true;
        }

        final ExpectSolutions solve(String x) {
            solve(INSTANCE.$$(x));
            return this;
        }

        void solve(Term x) {
            time.solve(x, this);
        }


        protected void validate() {

            Term[] events = time.byTerm.keySet().toArray(Op.EmptyTermArray);

            IntHashSet[][] dt = new IntHashSet[events.length][events.length];
            for (int xx = 0, eventsLength = events.length; xx < eventsLength; xx++) {
                Term x = events[xx];
                for (int yy = 0, eventsLength1 = events.length; yy < eventsLength1; yy++) {
                    if (xx == yy) continue;
                    Term y = events[yy];

                    IntHashSet d = dt[xx][yy] = new IntHashSet(2);
                    Term between = CONJ.the(x, XTERNAL, y);
                    time.solve(between, new Predicate<TimeGraph.Event>() {
                        @Override
                        public boolean test(TimeGraph.Event each) {
                            if (each.id.equalsRoot(between)) {
                                int xydt = each.id.dt();
                                if (xydt != DTERNAL && xydt != XTERNAL) {
                                    d.add(xydt);
                                }
                            }
                            return true;
                        }
                    });
                }
            }

            System.out.println("\n");
            System.out.println(Arrays.toString(events));
            for (IntHashSet[] r : dt) {
                System.out.println(Arrays.toString(r));
            }


            for (int xx = 0, eventsLength = events.length; xx < eventsLength; xx++) {
                for (int yy = xx + 1, eventsLength1 = events.length; yy < eventsLength1; yy++) {
                    assertEquals(dt[xx][yy], dt[yy][xx]);
                }
            }

        }

        void print() {
            time.print();
            System.out.println(uniqueSolutions + " unique solutions / " + repeatSolutions + " repeat solutions");
        }
    }

    private static TimeGraph newTimeGraph(long seed) {
        return newTimeGraph(seed, false);
    }

    private static TimeGraph newTimeGraph(long seed, boolean trace) {
        return new TimeGraph() {
            XoRoShiRo128PlusRandom rng = new XoRoShiRo128PlusRandom(seed);

            @Override
            protected Random random() {
                return rng;
            }

            @Override
            public boolean solve(Term x, Predicate<Event> target) {
                return super.solve(x, new Predicate<Event>() {
                    @Override
                    public boolean test(Event y) {
                        if (trace) {
                            System.err.println("SOLUTION: " + y);
                            Thread.dumpStack();
                            System.out.println();
                        }
                        return target.test(y);
                    }
                });
            }
        };
    }


}

































