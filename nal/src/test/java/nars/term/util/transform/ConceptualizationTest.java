package nars.term.util.transform;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.concept.TaskConcept;
import nars.concept.util.DefaultConceptBuilder;
import nars.term.Term;
import nars.term.Termed;
import org.junit.jupiter.api.Test;

import static nars.$.$;
import static nars.$.$$;
import static nars.term.util.TermTest.assertEq;
import static nars.time.Tense.XTERNAL;
import static org.junit.jupiter.api.Assertions.*;

/** TODO move more conceptualization/root tests from TemporalTermTest to here */
class ConceptualizationTest {

    private static final NAR n = NARS.shell();

    @Test
    void testFlattenAndDeduplicateAndUnnegateConj_Conceptualization() {
        Term t = $$("((||+- ,((--,(right-->fz)) &&+- (--,(right-->fz))),(fz-->race)) &&+- (fz-->race))");
        assertEq("( &&+- ,(--,(fz-->race)),(--,(right-->fz)),(fz-->race))", t.temporalize(Conceptualization.FlattenAndDeduplicateAndUnnegateConj));
    }

    @Test
    void testConceptualization() throws Narsese.NarseseException {

        Term t = $("(x==>y)");
        Term x = t.root();
        assertEquals(XTERNAL, x.dt());
        assertEquals("(x ==>+- y)", x.toString());

        n.input("(x ==>+0 y).", "(x ==>+1 y).").run(2);

        TaskConcept xImplY = (TaskConcept) n.conceptualize(t);
        assertNotNull(xImplY);

        assertEquals("(x ==>+- y)", xImplY.toString());

        assertEquals(3, xImplY.beliefs().size());

        int indexSize = n.concepts.size();
        n.concepts.print(System.out);

        n.input("(x ==>+1 y). :|:");
        n.run();


        assertEquals(4, xImplY.beliefs().size());

        n.concepts.print(System.out);
        assertEquals(indexSize, n.concepts.size());

        //n.conceptualize("(x==>y)").print();
    }

    @Test
    void testEmbeddedChangedRoot() throws Narsese.NarseseException {
        assertEquals("(a ==>+- (b &&+- c))",
                $("(a ==> (b &&+1 c))").root().toString());
    }


    @Test
    void testEmbeddedChangedRootVariations() throws Narsese.NarseseException {
        {

            Term x = $("(a ==> (b &&+1 (c && d)))");
            assertEquals("(a==>(b &&+1 (c&|d)))", x.toString());
            assertEquals("(a ==>+- ( &&+- ,b,c,d))", x.root().toString());
        }
        {
            Term x = $("(a ==> (b &&+1 (c &| d)))");
            assertEquals("(a ==>+- ( &&+- ,b,c,d))", x.root().toString());
        }

        Term x = $("(a ==> (b &&+1 (c &&+1 d)))");
        assertEquals("(a ==>+- ( &&+- ,b,c,d))", x.root().toString());
    }

    @Test
    void testStableNormalizationAndConceptualizationComplex() {
        String s = "(((--,checkScore(#1))&|#1) &&+14540 ((--,((#1)-->$2)) ==>+17140 (isRow(#1,(0,false),true)&|((#1)-->$2))))";
        Term t = $$(s);
        assertEquals(s, t.toString());
        assertEquals(s, t.normalize().toString());
        assertEquals(s, t.normalize().normalize().toString());
        String c = "( &&+- ,((--,((#1)-->$2)) ==>+- (isRow(#1,(0,false),true) &&+- ((#1)-->$2))),(--,checkScore(#1)),#1)";
        assertEquals(c, t.concept().toString());
        assertEquals(c, t.concept().concept().toString());
        Termed x = new DefaultConceptBuilder((z) -> {
        }).apply(t);
        assertTrue(x instanceof TaskConcept);
    }

    @Test
    void testAtemporalization() throws Narsese.NarseseException {
        assertEquals("(x ==>+- y)", n.conceptualize($("(x ==>+10 y)")).toString());
    }

}