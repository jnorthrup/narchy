package nars.op.prolog;

import alice.tuprolog.InvalidTheoryException;
import alice.tuprolog.MalformedGoalException;
import alice.tuprolog.Theory;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static nars.time.Tense.ETERNAL;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by me on 3/3/16.
 */
public class PrologCoreTest {

    @Disabled
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

        assertTrue(p.isTrue("'-->'('_b','_a')."));
        assertFalse(p.isTrue("'-->'('_a','_b')."));
        assertTrue(p.isTrue("'--'('-->'('_d','_c'))."));
        assertFalse(p.isTrue("'-->'('_d','_c')."));

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

        //assertTrue(p.isTrue("','('_a','_b','_c')."));
        assertTrue(p.isTrue("'_a','_b','_c'."));
        
    }

    @Test
    public void testConjunction3b() throws Exception {
        NAR n = NARS.tmp();


        PrologCore p = new PrologCore(n);
        n.believe("x:a");
        assertTrue(p.isTrue("'-->'('_a','_x')."));
        assertFalse(p.isTrue("'-->'('_a','_y')."));
        n.believe("y:b");
        n.believe("z:c", false);
        n.run(1);

        assertTrue(p.isTrue("'-->'('_a','_x'), '-->'('_b','_y')."));
        assertTrue(p.isTrue("'-->'('_a','_x'), '-->'('_b','_y'), '--'('-->'('_c','_z'))."));
        assertFalse(p.isTrue("'-->'('_a','_x'), '-->'('_b','_y'), '-->'('_c','_z')."));
        

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
