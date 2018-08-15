package nars.op;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.term.Evaluation;
import nars.term.Term;
import nars.test.TestNAR;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static nars.$.$$;
import static nars.time.Tense.ETERNAL;
import static org.junit.jupiter.api.Assertions.*;


class FunctorTest {

    @Test
    void testImmediateTransformOfInput() throws Narsese.NarseseException {
        NAR n = NARS.tmp();



        final boolean[] got = {false};
        n.onTask(t -> {
            String s = t.toString();
            assertFalse(s.contains("union"));
            if (s.contains("[a,b]"))
                got[0] = true;
        });
        n.input("union([a],[b]).");

        n.run(1);

        assertTrue(got[0]);
        assertTrue(n.beliefTruth($$("[a,b]"), ETERNAL)!=null);
    }

    @Test
    void testAdd1() throws Narsese.NarseseException {
        NAR d = NARS.tmp();

        d.input("add(1,2,#x)!");
        d.run(16);
        d.input("add(4,5,#x)!");
        d.run(16);
    }

    @Test
    void testAdd1Temporal() throws Narsese.NarseseException {
        NAR d = NARS.tmp();

        d.input("add(1,2,#x)! :|:");
        d.run(16);
        d.input("add(4,5,#x)! :|:");
        d.run(16);
    }

    /** tests correct TRUE fall-through behavior, also backward question triggered execution */
    @Test
    void testFunctor1() throws Narsese.NarseseException {


        TestNAR t = new TestNAR(NARS.tmp());
        t.nar.freqResolution.set(0.25f);

        t.log();

        t.believe("((complexity($1)<->3)==>c3($1))");
        t.ask("c3(x:y)");



        t.mustBelieve(1024, "c3(x:y)", 1f, 0.81f);





        t.test();

    }

    @Test
    void testFunctor2() throws Narsese.NarseseException {
        

        int TIME = 512;
        TestNAR t = new TestNAR(NARS.tmp());

        
        
        t.believe("(equal(complexity($1),complexity($2)) ==> c({$1,$2}))");
        t.ask("c({x, y})");
        t.ask("c({x, (x)})");
        t.mustBelieve(TIME, "equal(complexity((x)),complexity(x))", 0f, 0.90f);
        t.mustBelieve(TIME, "c({x,y})", 1f, 0.81f);
        t.mustBelieve(TIME, "c({x,(x)})", 0f, 0.81f);
        t.test();
    }

    @Disabled
    @Test
    void testExecutionResultIsCondition() throws Narsese.NarseseException {
        NAR d = NARS.tmp();
        d.input("(add($x,1,$y) ==> ($y <-> inc($x))).");
        d.input("((inc(2) <-> $x) ==> its($x)).");
        d.run(64);
    }

    @Test
    void testAnon1() {
        NAR d = NARS.shell();
        Set<Term> result = Evaluation.solveAll($$("anon((a,b),#x)"), d);
        assertEquals("[anon((a,b),(_1,_2))]", result.toString());
    }

    @Test
    void testAnon2() throws Narsese.NarseseException {
        NAR d = NARS.shell();
        d.log();
        d.input("anon((a,b),#x)?");
        d.run(3);
    }






















}
