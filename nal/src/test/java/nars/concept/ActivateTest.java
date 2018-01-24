package nars.concept;

import jcog.pri.PLink;
import nars.*;
import nars.control.Activate;
import nars.control.BatchActivation;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atom;
import org.eclipse.collections.api.tuple.primitive.ObjectIntPair;
import org.eclipse.collections.impl.bag.mutable.HashBag;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.TreeSet;

import static nars.$.$;
import static org.junit.jupiter.api.Assertions.*;

public class ActivateTest {

    @Test
    public void testConceptFireLinkSelection() throws Narsese.NarseseException {
        int count = 8;

        NAR nar = new NARS().tmp();
        nar.input("$0.01 a:b."); //low priority so it doesnt affect links
        nar.run(1);

        System.out.println("inputs:\n");

        Concept c = nar.conceptualize("a:b");
        for (int n = 0; n < count; n++) {
            PLink<Term> inserted = new PLink<>($("x" + n + ":a"), ((1 + n) / ((float) count)));
            System.out.println(inserted);
            c.termlinks().put(inserted);
        }


        System.out.println();

        HashBag<String> termlinkHits = new HashBag();
        HashBag<String> taskHits = new HashBag();
        //HashBag<String> premiseHits = new HashBag();
        Activate cf = new Activate(c, 1f);

        Term A = $.the("a");

        BatchActivation ba = BatchActivation.get();
        for (int i = 0; i < 100; i++) {
            final int[] remain = {9};
            cf.premises(nar, ba, (task, term) -> {
                Task ptask = task.get();
                Term pterm = term.get();
                System.out.println("tasklink=" + ptask + " termlink=" + pterm);
                if (pterm instanceof Atom || !A.equals(pterm.sub(0)))
                    return true; //ignore
                String tls = pterm.toString();

                //premiseHits.addOccurrences(p.toString(), 1);
                termlinkHits.addOccurrences(/*tasklink.get() + " " +*/ tls, 1);
                taskHits.addOccurrences(/*tasklink.get() + " " +*/ (ptask + " " + pterm), 1);
                return --remain[0] > 0;
            }, 3);
            ba.commit(nar);
        }


        System.out.println("termlinks pri (after):\n");
        c.termlinks().print();

        System.out.println("\ntermlink hits:\n");
        termlinkHits.topOccurrences(termlinkHits.size()).forEach(System.out::println);
//        System.out.println("\ntask hits:\n");
//        taskHits.topOccurrences(taskHits.size()).forEach(System.out::println);

//        System.out.println("\npremise hits:\n");
//        premiseHits.topOccurrences(premiseHits.size()).forEach(System.out::println);

//        System.out.println();
//        c.print();

//        System.out.println();

        ObjectIntPair<String> top = termlinkHits.topOccurrences(1).get(0);
        ObjectIntPair<String> bottom = termlinkHits.bottomOccurrences(1).get(0);
        String min = bottom.getOne();
        assertTrue("(a-->x0)".equals(min) || "(a-->x1)".equals(min)); //allow either 0 or 1
        assertEquals("(a-->x" + (count - 1) + ")", top.getOne());

    }

    @Test
    public void testDerivedBudgets() throws Narsese.NarseseException {

        NAR n = new NARS().get();

        //TODO System.err.println("TextOutput.out impl in progress");
        //n.stdout();


        n.input("$0.1$ <a --> b>.");
        n.input("$0.1$ <b --> a>.");
        n.run(15);


        n.conceptsActive().forEach(System.out::println);
    }

    @Test
    public void testTemplates1() throws Narsese.NarseseException {

        //layer 1:
        testTemplates("open:door",
                "[door, open]");
    }

    @Test
    public void testTemplates2() throws Narsese.NarseseException {
        //layer 2:
        testTemplates("open(John,door)",
                //"[(John,door), John, door, open]"
                "[(John,door), open]"
                );
    }

    @Test
    public void testTemplates3() throws Narsese.NarseseException {
        //layer 3:
        testTemplates("(open(John,door) ==> #x)",
                //"[open(John,door), (John,door), John, door, open, #1]"
                "[open(John,door), (John,door), open, #1]"
        );
    }

    @Test
    public void testTemplates4() throws Narsese.NarseseException {
        //dont descend past layer 3:
        testTemplates("(open(John,portal:interdimensional) ==> #x)",
        //"[open(John,(interdimensional-->portal)), (John,(interdimensional-->portal)), (interdimensional-->portal), John, open, #1]"
                "[open(John,(interdimensional-->portal)), (John,(interdimensional-->portal)), open, #1]"

        );
    }

    @Test
    public void testTemplates4b() throws Narsese.NarseseException {
        testTemplates("(open(John,portal(a(d),b,c)) ==> #x)",
                //"[open(John,portal(a(d),b,c)), (John,portal(a(d),b,c)), portal(a(d),b,c), John, open, #1]"
                "[open(John,portal(a(d),b,c)), (John,portal(a(d),b,c)), open, #1]"
        );
    }

    @Test
    public void testFunction() throws Narsese.NarseseException {
        testTemplates("f(x)",
                //"[(x), f, x]"
                "[(x), f]"
        );
    }
    @Test
    public void testIntersection() throws Narsese.NarseseException {
        testTemplates("((0|1)-->2)",
                "[(0|1), 0, 1, 2]"
        );
    }

    @Test
    public void testTemplatesWithInt2() throws Narsese.NarseseException {
        testTemplates("num((0))",
                //"[((0)), (0), num]"
                "[((0)), num]"
        );
    }

    @Test
    public void testTemplatesWithInt1() throws Narsese.NarseseException {
        testTemplates("(0)",
                "[]");
    }

    @Test
    public void testTemplatesWithQueryVar() throws Narsese.NarseseException {
        testTemplates("(x --> ?1)",
                "[x, ?1]");
    }

    @Test
    public void testTemplatesWithDepVar() throws Narsese.NarseseException {
        testTemplates("(x --> #1)",
                "[x, #1]");
    }

    @Test
    public void testTemplateConj1() throws Narsese.NarseseException {
        testTemplates("(x && y)",
                "[x, y]");
    }
    @Test
    public void testTemplateConj1Neg() throws Narsese.NarseseException {
        testTemplates("(x &&+- --x)",
                "[x]");
    }

    @Test
    public void testTemplateConj2() throws Narsese.NarseseException {
        testTemplates("(&&,<#x --> lock>,(<$y --> key> ==> open($y,#x)))",
                "[((#1-->lock)&&($2-->key)), open($2,#1), (#1-->lock), ($2-->key), ($2,#1), lock, open, key, #1, $2]");

    }

    @Test
    public void testTemplateDiffRaw() throws Narsese.NarseseException {
        testTemplates("(x-y)",
                "[x, y]");
    }

    @Test
    public void testTemplateDiffRaw2() throws Narsese.NarseseException {
        testTemplates("((a,b)-y)",
                "[(a,b), y]");
    }

    @Test
    public void testTemplateProd() throws Narsese.NarseseException {
        testTemplates("(a,b)",
                "[]");
    }

    @Test
    public void testTemplateProdWithCompound() throws Narsese.NarseseException {
        testTemplates("(a,(b,c))",
                "[]");
    }

    @Test
    public void testTemplateSimProd() throws Narsese.NarseseException {
        testTemplates("(c<->a)",
                "[a, c]");
    }

    @Test
    public void testTemplateSimProdCompound() throws Narsese.NarseseException {
        testTemplates("((a,b)<->#1)",
                "[(a,b), #1]");
    }

    @Test
    public void testTemplatesAreEternal() throws Narsese.NarseseException {
        testTemplates("a:(x ==>+1 y)",
                "[(x==>y), a, x, y]");
    }

    static void testTemplates(String term, String expect) throws Narsese.NarseseException {
        NAR n = NARS.tmp(1);
        //n.believe(term + ".");
        Concept c = n.conceptualize($(term));
        Activate a = new Activate(c, 0.5f);
        Collection<Termed> t = new TreeSet(c.templates());
        assertEquals(expect, t.toString());
    }

    @Test public void testConceptualizeNonTaskable_IndepVarUnbalanced() throws Narsese.NarseseException {
        assertNotNull(NARS.tmp(1).conceptualize($("(x --> $1)")));
    }


}