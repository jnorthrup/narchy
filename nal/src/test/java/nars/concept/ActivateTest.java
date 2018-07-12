package nars.concept;

import jcog.pri.PLink;
import nars.*;
import nars.derive.Derivers;
import nars.derive.deriver.MatrixDeriver;
import nars.link.Activate;
import nars.link.ActivatedLinks;
import nars.term.Term;
import nars.term.atom.Atom;
import org.eclipse.collections.api.tuple.primitive.ObjectIntPair;
import org.eclipse.collections.impl.bag.mutable.HashBag;
import org.junit.jupiter.api.Test;

import static nars.$.$;
import static org.junit.jupiter.api.Assertions.*;

class ActivateTest {

    private final NAR n = new NARS().shell();

    @Test
    void testConceptFireLinkSelection() throws Narsese.NarseseException {
        int count = 8;

        n.input("$0.01 a:b."); 
        n.run(1);

        System.out.println("inputs:\n");

        Concept c = n.conceptualize("a:b");
        for (int n = 0; n < count; n++) {
            PLink<Term> inserted = new PLink<>($("x" + n + ":a"), ((1 + n) / ((float) count)));
            System.out.println(inserted);
            c.termlinks().put(inserted);
        }


        System.out.println();

        HashBag<String> termlinkHits = new HashBag();
        HashBag<String> taskHits = new HashBag();
        
        Activate cf = new Activate(c, 1f);

        Term A = $.the("a");

        MatrixDeriver dummy = new MatrixDeriver(Derivers.parse(n,
        "(A --> B), (A --> C), neqRCom(B,C)      |- (C --> B), (Belief:Abduction, Goal:DesireWeak)"));

        for (int i = 0; i < 100; i++) {
            final int[] remain = {9};
            ActivatedLinks linkActivations = new ActivatedLinks();
            dummy.premiseMatrix(cf, (task, term) -> {
                Task ptask = task;
                Term pterm = term.get();
                System.out.println("tasklink=" + ptask + " termlink=" + pterm);
                if (pterm instanceof Atom || !A.equals(pterm.sub(0)))
                    return true;
                String tls = pterm.toString();


                termlinkHits.addOccurrences(/*tasklink.get() + " " +*/ tls, 1);
                taskHits.addOccurrences(/*tasklink.get() + " " +*/ (ptask + " " + pterm), 1);
                return --remain[0] > 0;
            }, 1, 3, linkActivations, n.random(), n);

            //TODO analyze linkActivations
            n.input(linkActivations);
        }


        System.out.println("termlinks pri (after):\n");
        c.termlinks().print();

        System.out.println("\ntermlink hits:\n");
        termlinkHits.topOccurrences(termlinkHits.size()).forEach(System.out::println);











        ObjectIntPair<String> top = termlinkHits.topOccurrences(1).get(0);
        ObjectIntPair<String> bottom = termlinkHits.bottomOccurrences(1).get(0);
        String min = bottom.getOne();
        assertTrue("(a-->x0)".equals(min) || "(a-->x1)".equals(min)); 
        assertEquals("(a-->x" + (count - 1) + ")", top.getOne());

    }

    @Test
    void testDerivedBudgets() throws Narsese.NarseseException {



        
        


        n.input("$0.1$ <a --> b>.");
        n.input("$0.1$ <b --> a>.");
        n.run(15);


        n.conceptsActive().forEach(System.out::println);
    }

    @Test
    void testConceptualizeNonTaskable_IndepVarUnbalanced() throws Narsese.NarseseException {
        assertNotNull(NARS.tmp(1).conceptualize($("(x --> $1)")));
    }


}






























































































































































































































































































































































































































