package nars.op;

import nars.*;
import nars.concept.Concept;
import nars.subterm.Subterms;
import nars.term.Functor;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.test.TestNAR;
import nars.time.Tense;
import nars.util.AtomicOperations;
import nars.util.Timed;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static nars.Op.COMMAND;
import static nars.term.Functor.f;
import static org.junit.jupiter.api.Assertions.*;


class OperatorTest {


    @Test
    void testEcho() throws Narsese.NarseseException {
        NAR n = NARS.tmp();
        AtomicBoolean invoked = new AtomicBoolean();
        n.add(f("c", new Function<Subterms, Term>() {
            @Override
            public Term apply(Subterms args) {
                assertEquals("(x)", args.toString());
                invoked.set(true);
                return null;
            }
        }));
        Task t = Narsese.task("c(x);", n);
        assertNotNull(t);
        assertEquals(COMMAND, t.punc());
        assertTrue(t.isCommand());
        assertEquals("c(x);", t.toString());

        n.input(t);
        n.run(1);

        assertTrue(invoked.get());
    }

    /** tests Builtin.System and evaluating a target input as a command */
    @Disabled @Test
    void testThe() throws Narsese.NarseseException {
        NAR n = NARS.tmp();


        @Nullable Concept statusFunctor = n.concept($.INSTANCE.the("the"));
        statusFunctor.print();

        StringBuilder b = new StringBuilder();
        n.log(b);
        n.input("log(the(sys))");

        String s = b.toString();
        assertTrue(s.contains("→("), new Supplier<String>() {
            @Override
            public String get() {
                return s;
            }
        });
    }

    @Test
    void testAtomicExec() throws Narsese.NarseseException {
        NAR n = NARS.tmp();
        int[] count = {0};

        n.setOp(Atomic.atom("x"), new AtomicOperations(new BiConsumer<Term, Timed>() {
            @Override
            public void accept(Term x, Timed nar) {
                System.err.println("INVOKE " + x);
                count[0]++;
                n.believe(x);
            }
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
    void testChoose() throws Narsese.NarseseException {
        NAR n = NARS.tmp();
        n.time.dur(10);
        n.setOp(Atomic.atom("x"), new AtomicOperations(new BiConsumer<Term, Timed>() {
            @Override
            public void accept(Term x, Timed nar) {
                Subterms args = Functor.args(x);
                if (args.subs() > 0) {
                    Term r;
                    if ($.INSTANCE.the(1).equals(args.sub(0))) {
                        System.err.println("YES");
                        r = $.INSTANCE.the("good");
                    } else if ($.INSTANCE.the(0).equals(args.sub(0))) {
                        r = $.INSTANCE.the("good").neg();
                    } else {
                        return;
                    }

                    n.believe($.INSTANCE.impl(x.term(), r), Tense.Present);
                }
            }
        }, 0.51f));

        n.input("x(1)! :|:");
        n.run(4);
        n.input("x(0)! :|:");
        n.run(4);
        n.want("good");
        n.run(1000);
    }

    @Test
    void testGoal2() throws Narsese.NarseseException {
        NAR n = NARS.tmp();
        n.setOp(Atomic.atom("x"), new AtomicOperations(new BiConsumer<Term, Timed>() {
            @Override
            public void accept(Term t, Timed nar) {
                Term x = t.term();
                Subterms args = Functor.args(t);
                Term y = $.INSTANCE.func("args", args);
                Term xy = $.INSTANCE.impl(x, y);
                n.believe(xy, Tense.Present);
            }
        }, 1));

        n.run(1);
        n.input("x(1)! :|:");
        n.run(1);
        n.run(10);
        n.input("x(3)! :|:");
        n.run(10);
    }

    @Test
    void testSliceAssertEtc() throws Narsese.NarseseException {
        
        



        NAR n = NARS.tmp();
        
        n.input("(slice((a,b,c),2)).");
        n.input("assertEquals(c, slice((a,b,c),add(1,1)));");
        n.input("assertEquals((a,b), slice((a,b,c),(0,2)));");

        

        n.input("(quote(x)).");
        n.input("log(quote(x));");
        n.input("assertEquals(c, c);");
        n.input("assertEquals(x, quote(x));");
        n.input("assertEquals(c, slice((a,b,c),2));");
        n.input("assertEquals(quote(slice((a,b,c),#x)), slice((a,b,c),#x));");
        n.run(5);
    }

    @Test
    void testCommandDefault() throws Narsese.NarseseException {
        NAR t = NARS.shell();
        Task a = t.input("(a, b, c);").get(0);
        assertNotNull(a);
        assertTrue(a.isCommand());
        assertEquals($.INSTANCE.$("(a, b, c)"), a.term());
    }






































    @Disabled
    @Test
    void testRecursiveEvaluation2() {
        

        testIO("count({ count({a,b}), 2})!",
                "(count({count({a,b},SELF),2},$1,SELF) ==> ($1 <-> 1)). :|: %1.00;0.90%"
        );
    }

    private static void testIO(String input, String output) {

        TestNAR t = new TestNAR(NARS.tmp());
        t.mustOutput(16, output);
        t.input(input);

        t.run((long) 4);

    }




























































































































































































































}
