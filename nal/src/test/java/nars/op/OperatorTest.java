package nars.op;

import nars.*;
import nars.concept.Concept;
import nars.term.Term;
import nars.test.TestNAR;
import nars.time.Tense;
import nars.util.AtomicExec;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static nars.Op.COMMAND;
import static nars.concept.Operator.argsArray;
import static org.junit.jupiter.api.Assertions.*;


public class OperatorTest {


    @Test
    public void testEcho() throws Narsese.NarseseException {
        NAR n = NARS.tmp();
        AtomicBoolean invoked = new AtomicBoolean();
        n.on("c", (args) -> {
            assertEquals("(x)", args.toString());
            invoked.set(true);
            return null;
        });
        Task t = Narsese.the().task("c(x);", n);
        assertNotNull(t);
        assertEquals(COMMAND, t.punc());
        assertTrue(t.isCommand());
        assertEquals("c(x);", t.toString());

        n.input(t);
        n.run(1);

        assertTrue(invoked.get());
    }

    /** tests Builtin.System and evaluating a term input as a command */
    @Test public void testThe() throws Narsese.NarseseException {
        NAR n = NARS.tmp();


        @Nullable Concept statusFunctor = n.concept($.the("the"));
        statusFunctor.print();

        StringBuilder b = new StringBuilder();
        n.log(b);
        n.input("the(sys)");

        String s = b.toString();
        assertTrue(s.contains("→("), ()->s);
    }

    @Test
    public void testAtomicExec() throws Narsese.NarseseException {
        NAR n = NARS.tmp();
        final int[] count = {0};
        n.log();
        n.onOp("x", new AtomicExec((x, nar) -> {
            System.err.println("INVOKE " + x);
            count[0]++;
            n.believe(x); //quench feedback
        }, 0.66f));
        n.run(1);
        n.input("x(1)! :|:");
        n.run(4);
        assertEquals(1, count[0]);

        n.run(10);
        n.input("x(3)! :|:");
        n.run(10);
        assertEquals(2, count[0]);
    }

    @Test
    public void testChoose() throws Narsese.NarseseException {
        NAR n = NARS.tmp();
        n.time.dur(10);
        n.onOp("x", new AtomicExec((x, nar) -> {
            Term[] args = argsArray(x);
            if (args.length > 0) {
                Term r;
                if ($.the(1).equals(args[0])) {
                    System.err.println("YES");
                    r = $.the("good");
                } else if ($.the(0).equals(args[0])) {
                    r = $.the("good").neg();
                } else {
                    return;
                }

                n.believe($.impl(x.term(), r), Tense.Present);
            }
        }, 0.51f));
        n.log();
        n.input("x(1)! :|:");
        n.run(4);
        n.input("x(0)! :|:");
        n.run(4);
        n.goal("good");
        n.run(1000);
    }

    @Test
    public void testGoal2() throws Narsese.NarseseException {
        NAR n = NARS.tmp();
        n.onOp("x", new AtomicExec((t, nar) -> {
            Term x = t.term();
            Term[] args = argsArray(t);
            Term y = $.func("args", args);
            Term xy = $.impl(x, y);
            n.believe(xy, Tense.Present);
        }, 1));
        n.log();
        n.run(1);
        n.input("x(1)! :|:");
        n.run(1);
        n.run(10);
        n.input("x(3)! :|:");
        n.run(10);
    }

    @Test
    public void testSliceAssertEtc() throws Narsese.NarseseException {
        //https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/slice
        //_.slice(array, [start=0], [end=array.length])

        Param.DEBUG = true;

        NAR n = NARS.tmp();
        n.log();
        n.input("(slice((a,b,c),2)).");
        n.input("assertEquals(c, slice((a,b,c),add(1,1)));");
        n.input("assertEquals((a,b), slice((a,b,c),(0,2)));");

        //TODO add invalid slice conditions

        n.input("(quote(x)).");
        n.input("log(quote(x));");
        n.input("assertEquals(c, c);");
        n.input("assertEquals(x, quote(x));");
        n.input("assertEquals(c, slice((a,b,c),2));");
        n.input("assertEquals(quote(slice((a,b,c),#x)), slice((a,b,c),#x));");
        n.run(5);
    }

    @Test
    public void testCommandDefault() throws Narsese.NarseseException {
        final NAR t = NARS.shell();
        Task a = t.input("(a, b, c);").get(0);
        assertNotNull(a);
        assertTrue(a.isCommand());
        assertEquals($.$("(a, b, c)"), a.term());
    }
////    @Test public void testOperatorEquality() {
////        assertNotNull( $.oper("echo") );
////        assertEquals( $.oper("echo"), $.oper("echo"));
////        assertNotEquals( $.oper("echo"), $.the("echo")); //echo vs. ^echo
////        //assertNotEquals( $.oper("echo"), $.the("^echo")); //'^echo' vs echo .. this should be disallowed
////    }
//
//    @Test public void testMustExecuteSuccess() {
//
//
//        NAR n = new Default(100, 1, 1, 1);
//        TestNAR t = new TestNAR(n);
//        t.mustExecute(0, 1, "operator");
//
//        n.input("operator()!");
//
//
//    }
//
//    @Test public void testMustExecuteFailure() {
//
//        try {
//            NAR n = new Default(100, 1, 1, 1);
//            TestNAR t = new TestNAR(n);
//            t.mustExecute(0, 1, "operator");
//
//            n.input("xoperator()!");
//
//            assertTrue(false);
//        }
//        catch (AssertionError e) {
//            //failure should occurr
//            assertTrue(true);
//        }
//    }
//
//

    @Disabled
    @Test
    public void testRecursiveEvaluation2() {
        //assertTrue( t.nar.concept($.the("count")) instanceof Functor);

        testIO("count({ count({a,b}), 2})!",
                "(count({count({a,b},SELF),2},$1,SELF) ==> ($1 <-> 1)). :|: %1.00;0.90%"
        );
    }

    static void testIO(String input, String output) {

        TestNAR t = new TestNAR(NARS.tmp());
        t.log();
        t.mustOutput(16, output);
        t.input(input);

        t.test(4);

    }
////
////    @Test public void testOutputInVariablePosition() {
////        testIO("count({a,b}, #x)!",
////                "<2 --> (/,^count,{a,b},_,SELF)>. :|: %1.00;0.99%");
////    }
//
//    @Test public void testOperationIsInheritance() {
//        Compound o = $.func($.the("x"), $.p("y"));
//        assertEquals(Op.INH, o.op());
//    }

////    @Test public void testTermReactionRegistration() {
////
////        AtomicBoolean executed = new AtomicBoolean(false);
////
////        NAR n = new Default();
////        n.onExecTerm("exe", (Term[] event) -> {
////            //System.out.println("executed: " + Arrays.toString(args));
////            executed.set(true);
////            return null;
////        });
////
////        n.input("exe(a,b,c,#x)!");
////
////        n.run(1);
////
////        assertTrue(executed.get());
////
////    }
//
////    @Test public void testSynchOperator() {
////
////
////        AtomicBoolean executed = new AtomicBoolean(false);
////
////        NAR n = new Default();
////        n.onExec("exe", (exec) -> {
////            executed.set(true);
////        });
////
////        //n.trace();
////        n.input("exe(a,b,c)!");
////
////        n.run(1);
////
////        assertTrue(executed.get());
////
////        assertNotNull("should have conceptualized or linked to ^exe",
////                n.concept("^exe"));
////
////        assertNull("^exe should not conceptualize nor link to atom exe", n.index.get("exe") );
////
////
////    }
//
////    @Disabled @Test public void testCompoundOperator() {
////
////        AtomicBoolean executed = new AtomicBoolean(false);
////
////        NAR n = new Default();
////
////        n.onExec((Term)n.term("<a --> b>"), (exec) -> {
////            executed.set(true);
////        });
////
////        n.input("<a --> b>(a,b,c)!");
////
////        n.frame(1);
////
////        assertTrue(executed.get());
////
////    }
//
////    @Test public void testPatternExecution() {
////        AtomicInteger count = new AtomicInteger();
////
////        PatternOperation f = new PatternOperation("(%A,%B)") {
////            @Override
////            public List<Task> run(Task operationTask, Subst map1) {
////                //System.out.println(this.pattern + " " + operationTask + "\n\t" + map1);
////                count.getAndIncrement();
////                return null;
////            }
////        };
////        Terminal t = new Terminal(16);
////
////        Task matching = t.task("(x,y)!");
////        f.apply(matching);
////
////        assertEquals(1, count.get());
////
////        Task nonMatching = t.task("(x,y,z)!");
////        f.apply(nonMatching);
////
////        //should only be triggered once, by the matching term
////        assertEquals(1, count.get());
////    }
//
////    @Test public void testPatternAnswerer() {
////        AtomicInteger count = new AtomicInteger();
////
////        PatternAnswer f = new PatternAnswer("add(%a,%b,#x)") {
////            @Override
////            public List<Task> run(Task t, @NotNull Subst s) {
////
////                return null;
////            }
////        };
////        Terminal t = new Terminal(16);
////        t.ask($("add(%a,%b,#x)"), ETERNAL, a-> {
////            //TODO
//////            System.out.println(t + " " + s);
//////            assertEquals($("x"), s.term($("%a")));
//////            assertEquals($("y"), s.term($("%b")));
////            count.getAndIncrement();
////            return true;
////        });
////
////        Task matching = t.task("add(x,y,#x)?");
////        f.apply(matching);
////
////        assertEquals(1, count.get()); //should only be triggered once, by the matching term
////
////        Task nonMatching = t.task("add(x)?");
////        f.apply(nonMatching);
////
////        assertEquals(1, count.get()); //should only be triggered once, by the matching term
////
////        Task nonMatching3 = t.task("add(x,y,z)?");
////        f.apply(nonMatching3);
////
////        assertEquals(1, count.get()); //should only be triggered once, by the matching term
////
////        Task nonMatching2 = t.task("add(x,y,$x)?");
////        f.apply(nonMatching2);
////
////        assertEquals(1, count.get()); //should only be triggered once, by the matching term
////    }
//
////    @Disabled
////    @Test public void testPatternAnswererInNAR() {
////        NAR n = new Default(100,1,1,1);
////
////        PatternAnswer addition = new PatternAnswer("add(%a,%b,#x)") {
////            @Nullable
////            final Term A = $("%a"), B = $("%b");
////            @Override public List<Task> run(@NotNull Task question, @NotNull Subst s) {
////                int a = i(s.term(A).toString());
////                int b = i(s.term(B).toString());
////
////                return Lists.newArrayList(
////                        $.task("add(" + a + ',' + b + ',' + Integer.toString(a+b) + ')',
////                                '.', 1.0f, 0.99f)
////                            .eternal()
////                            .parent(question)
////                            .budget(question)
////                            .because("Addition")
////                );
////            }
////        };
////        n.onQuestion(addition);
////        //n.log();
////
////        TestNAR t = new TestNAR(n);
////        n.input("add(1,2,#x)?");
////        n.input("add(1,1,#x)?");
////        t.mustBelieve(8, "add(1, 1, 2)", 1.0f, 0.99f);
////        t.mustBelieve(8, "add(1, 2, 3)", 1.0f, 0.99f);
////        t.test();
////
////        assertEquals(1, n.concept("add(1, 1, 2)").beliefs().size());
////        assertEquals(1, n.concept("add(1, 1, #x)").questions().size());
////        n.concept("add(1, 1, 2)").print(System.out);
////    }
//
//
//
//
////TODO: allow this in a special eval operator
//
////    //almost finished;  just needs condition to match the occurence time that it outputs. otherwise its ok
////
////    @Test
////    public void testRecursiveEvaluation1() {
////        testIO("add( count({a,b}), 2)!",
////                "<(^add,(^count,{a,b},SELF),2,$1,SELF) =/> <$1 <-> 4>>. :|: %1.00;0.90%"
////        );
////    }
////


//    @Test public void testEval1() {
//        assertEquals("(1)",
//                t.eval(
//                        //"(add 1 2)"
//                        "(list 1)"
//                ).toString());
//    }

//    @Test public void testStaticMethodInvoke() {
//        assertEquals(System.getProperty("java.vm.version"),
//                t.eval("(System/getProperty \"java.vm.version\")").toString() );
//    }
//    @Test public void testClojuredCompound() {
//        assertEquals("[\"==>\" ([\"-->\" (a b)] (println x))]", t.eval("(quote <<a-->b> ==> (println x)>)").toString());
//    }

//    @Test public void testClojure2() {
//            //t.eval("(\"org.junit.Assert/assertTrue\" false)");
//        NAR n = new Default();
//        n.log();
//        n.input("a:b!");
//        //n.input("<(println (System/getProperty \"java.vm.version\"))==>a:b>.");
//        n.input("<(println 1)==>a:b>.");
//        //n.input("<echo(y)==>a:b>.");
//        //n.input("echo(z)!");
//        n.run(16);
//
//    }

}
