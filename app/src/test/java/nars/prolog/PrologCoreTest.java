package nars.prolog;

import alice.tuprolog.InvalidTheoryException;
import alice.tuprolog.MalformedGoalException;
import alice.tuprolog.Theory;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.op.prolog.PrologCore;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static nars.time.Tense.ETERNAL;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by me on 3/3/16.
 */
public class PrologCoreTest {

    @Test public void testPrologShell() throws MalformedGoalException {
        NAR n = NARS.tmp();
        PrologCore p = new PrologCore(n);
        try {
            p.input(new Theory(PrologCoreTest.class.getClassLoader().getResource("shell.prolog").openStream()));
        } catch (InvalidTheoryException | IOException e) {
            e.printStackTrace();
        }

        p.solve("do(help).");
    }

    @Test
    public void testPrologCoreBeliefAssertion() throws Narsese.NarseseException {
        NAR n = NARS.tmp();
        PrologCore p = new PrologCore(n);
        n.input("a:b.");
        n.input("(--, c:d).");
        n.run(1);

        assertTrue(p.isTrue("'-->'(b,a)."));
        assertFalse(p.isTrue("'-->'(a,b)."));
        assertTrue(p.isTrue("'--'('-->'(d,c))."));
        assertFalse(p.isTrue("'-->'(d,c)."));

    }

    @Test
    public void testPrologCoreQuestionTruthAnswer() throws Narsese.NarseseException {
        NAR n = NARS.tmp();
        PrologCore p = new PrologCore(n);
        n.input("a:b.");
        n.input("a:c.");
        n.input("(--, c:d).");
        n.run(1);

        n.input("a:b?");
        
        n.run(1);

        n.input("c:d?");
        
        n.run(1);

        n.input("a:?x?");
        
        n.run(1);

    }

    @Test
    public void testPrologCoreDerivedTransitive() throws Narsese.NarseseException {
        NAR n = NARS.tmp();
        PrologCore p = new PrologCore(n);
        n.input("a:b.");
        n.input("b:c.");
        n.run(1);

        n.input("a:c?");
        
        n.run(1);

        n.input("a:d?");
        
        n.run(1);
    }

    @Test
    public void testConjunction3() throws Narsese.NarseseException {
        NAR n = NARS.tmp();
        PrologCore p = new PrologCore(n);
        n.input("(&&,a,b,c).");
        n.run(1);

        assertTrue(p.isTrue("'&&'(a,b,c)."));
        assertTrue(p.isTrue("a,b,c."));
        
    }

    @Test
    public void testConjunction3b() throws Exception {
        NAR n = NARS.tmp();


        PrologCore p = new PrologCore(n);
        n.believe("x:a");
        assertTrue(p.isTrue("'-->'(a,x)."));
        assertFalse(p.isTrue("'-->'(a,y)."));
        n.believe("y:b");
        n.believe("z:c", false);
        n.run(1);

        assertTrue(p.isTrue("'-->'(a,x), '-->'(b,y)."));
        assertTrue(p.isTrue("'-->'(a,x), '-->'(b,y), '--'('-->'(c,z))."));
        assertFalse(p.isTrue("'-->'(a,x), '-->'(b,y), '-->'(c,z)."));
        

    }

    @Test
    public void testPrologCoreDerivedTransitive2() throws Narsese.NarseseException {
        NAR n = NARS.tmp();
        PrologCore p = new PrologCore(n);
        n.input("a:b.");
        n.input("b:c.");
        n.input("c:d.");
        n.input("d:e.");
        n.input("e:f.");
        n.run(1);

        n.input("a:f?");
        
        n.run(1);

        n.input("a:?x?");
        
        n.run(1);

    }

    @Test
    public void testPrologCoreImplToRule() throws Narsese.NarseseException {
        NAR n = NARS.tmp(1);
        n.log();

        PrologCore p = new PrologCore(n);
        n.input("f(x,y).");
        n.input("(f($x,$y)==>g($y,$x)).");
        n.run(1);

        n.input("g(x,y)?");
        n.input("g(y,x)?");
        n.run(1);

        assertEquals(1f, n.beliefTruth("g(y,x)", ETERNAL).freq());
        assertNull( n.beliefTruth("g(x,y)", ETERNAL));

    }



















































































































}
