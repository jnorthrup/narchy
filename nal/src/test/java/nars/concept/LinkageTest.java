package nars.concept;


import nars.$;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.term.Termed;
import nars.term.Variable;
import nars.test.NALTest;
import nars.test.TestNAR;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static nars.Op.*;
import static org.junit.jupiter.api.Assertions.*;


@Disabled class LinkageTest extends NALTest {

    private final int runCycles = 170;


//    private void ProperlyLinkedTest(String premise1, String premise2) throws Exception {
//
//        test.requireConditions = false;
//        TestNAR tester = test;
//        tester.believe(premise1);
//        tester.believe(premise2);
//
//        tester.run(runCycles);
//
//        Concept ret = tester.nar.conceptualize(premise1);
//        assertTrue(isPassed2(premise2, ret), ret + " termlinks contains " + premise2);
//
//        Concept ret2 = tester.nar.conceptualize(premise2);
//        assertTrue(isPassed2(premise1, ret2), ret2 + " termlinks contains " + premise1);
//
//    }

//    private boolean isPassed2(String premise1Str, @Nullable Concept ret2) {
//        Term premise1 = null;
//        try {
//            premise1 = $.$(premise1Str).concept();
//        } catch (Narsese.NarseseException e) {
//            return false;
//        }
//        if (ret2 != null) {
//            for (PriReference<Term> entry : ret2.termlinks()) {
//                Term w = entry.get().target();
//                if (w.equals(premise1)) {
//                    return true;
//                }
//            }
//        }
//        return false;
//    }

    private void assertLinked(String spremise1, String spremise2) throws Exception {
        assertLinked(spremise1, BELIEF, spremise2);
    }


    private void assertLinked(String spremise1, byte punc, String spremise2) throws Exception {


        test.requireConditions = false;
        NAR nar = test.nar;


        Termed premise1 = $.$(spremise1);
        assertEquals($.$(spremise1), premise1, "reparsed");
        assertNotNull(premise1);
        assertEquals($.$(spremise1), premise1);

        Termed premise2 = $.$(spremise2);
        assertEquals($.$(spremise2), premise2, "reparsed");
        assertNotNull(premise2);
        assertEquals($.$(spremise2), premise2);

        String t1 = getTask(punc, premise1);
        String t2 = getTask(punc, premise2);

        nar.input(t1, t2).run(runCycles);


        @Nullable Concept p1 = nar.concept(premise1);


        Concept p2 = nar.concept(premise2);
        assertNotNull(p2);


        //AdjGraph<Term, Float> g = TermGraph.termlink(nar);


        boolean p12 = linksIndirectly(p1, p2, nar);
        assertTrue(p12, why(nar, premise1, premise2));

        boolean p21 = linksIndirectly(p2, p1, nar);
        assertTrue(p21, why(nar, premise2, premise1));


//        int numNodes = g.nodeCount();
//        assertTrue(numNodes > 0);
//        assertTrue(g.edgeCount()>0, g.toString());


    }

    private static Supplier<String> why(NAR nar, Termed premise1, Termed premise2) {
        return () -> premise1 + " no link to " + premise2 + "\n" +
                (((nar.concept(premise1) != null) ? nar.concept(premise1).printToString() : (premise1 + " unconceptualized"))) + "\n" +
                (((nar.concept(premise2) != null) ? nar.concept(premise2).printToString() : (premise2 + " unconceptualized"))) + "\n";
    }

    private static String getTask(byte punc, Termed premise1) {
        if (punc == QUESTION) {
            return premise1.toString() + (char) (QUESTION);
        } else {
            return premise1.toString() + (char) (punc) + " %1.0;0.9%";
        }
    }

    private static boolean linksIndirectly(Concept src, Concept target, NAR nar) {


        final boolean[] result = {false};
        src.linker().targets().forEach(w -> {
            if (target.equals(w)) {
                result[0] = true;
                return;
            }
            if (!w.op().conceptualizable)
                return;

            Concept ww = nar.concept(w);

            if (ww != null) {
                if (target.equals(ww)) {
                    return;
                }


                ww.linker().targets().forEach(e -> {
                    if (target.equals(e)) {
                        result[0] = true;
                        return;
                    }
                    if (!e.op().conceptualizable)
                        return;

                    Concept ee = nar.concept(e);
                    if (ee != null && target.equals(ee)) {
                        result[0] = true;
                        return;
                    }


                });
            }

        });
        return result[0];
    }


    void ProperlyLinkedIndirectlyLayer2Test(String premise1, String premise2) throws Exception {

        TestNAR tester = test;
        tester.believe(premise1);
        tester.believe(premise2);
        tester.nar.run(1);

        boolean passed = links(premise1, premise2, tester);
        boolean passed2 = links(premise2, premise1, tester);
        assertTrue(passed);
        assertTrue(passed2);


        tester.believe("(a-->b)");
        tester.mustBelieve(1, "(a-->b)", 0.9f);
    }

    private boolean links(String premise1, String premise2, TestNAR tester) throws Narsese.NarseseException {
        Concept ret = tester.nar.conceptualize(premise1);
        final boolean[] passed = {false};
        if (ret != null) {


            ret.linker().targets().forEach(et1 -> {
                if (et1.toString().equals(premise2)) {
                    passed[0] = true;
                    return;
                }

                if (!(et1 instanceof Variable)) {
                    Concept Wc = tester.nar.concept(et1);
                    if (Wc != null) {
                        Wc.linker().targets().forEach(et2 -> {
                            if (et2.toString().equals(premise2)) {
                                passed[0] = true;
                                return;
                            }
                            Concept Wc2 = tester.nar.concept(et2);
                            if (Wc2 != null) {
                                Wc2.linker().targets().forEach(et3 -> {
                                    if (et3.toString().equals(premise2)) {
                                        passed[0] = true;
                                        return;
                                    }
                                });
                            }
                        });
                    }
                }
                /*if (w.toString().equals(premise2)) {
                    passed = true;
                }*/
            });
        }
        return passed[0];
    }


    @Test
    void Linkage_NAL5_abduction() throws Exception {
        assertLinked("((robin-->bird)==>(robin-->animal))", "(robin-->animal)");
    }


    @Test
    void Linkage_NAL5_detachment() throws Exception {
        assertLinked("((robin-->bird)==>(robin-->animal))", "(robin-->bird)");
    }

    @Test
    void Linkage_NAL6_variable_elimination2() throws Exception {
        assertLinked("<<$1-->bird>==><$1-->animal>>", "(tiger-->animal)");
    }


    @Test
    void Part_Indirect_Linkage_NAL6_multiple_variable_elimination4() throws Exception {
        assertLinked("<#1 --> lock>", "<{lock1} --> lock>");
    }


    @Test
    void Indirect_Linkage_NAL6_multiple_variable_elimination() throws Exception {
        assertLinked("<(&&, <$1 --> lock>, <$2 --> key>) ==> open($2, $1)>",
                "<{lock1} --> lock>");
    }


    @Test
    void Indirect_Linkage_NAL6_variable_elimination_deduction() throws Exception {
        assertLinked(
                "<lock1 --> lock>",
                "<(&&, open($2, #1), <#1 --> lock>) ==> <$2 --> key>>");
    }

    @Test
    void Indirect_Linkage_NAL6_variable_unification7() throws Exception {
        assertLinked(
                "<(&&, <$1 --> flyer>, <($1, worms) --> food>) ==> <$1 --> bird>>",
                "<<$1 --> flyer> ==> <$1 --> [withWings]>>");
    }

    @Test
    void Indirect_Linkage_NAL6_variable_unification6() throws Exception {
        assertLinked(
                "<(&&, <$1 --> flyer>, <$1 --> [chirping]>, <($1, worms) --> food>) ==> <$1 --> bird>>",
                "<(&&, <$1 --> [chirping]>, <$1 --> [withWings]>) ==> <$1 --> bird>>");
    }

    @Test
    void Indirect_Linkage_NAL6_second_level_variable_unification() throws Exception {

        assertLinked(
                "(lock:#1 && (key:$2 ==> (open-->($2, #1))))",
                "key:{key1}");
    }


    @Test
    void Indirect_Linkage_Basic() throws Exception {
        assertLinked("(a-->b)", "(b-->c)");
    }

    @Test
    void Indirect_Linkage_Layer2_Basic() throws Exception {
        assertLinked("<a --> <b --> (k-->x)>>", "(k-->x)");
    }

    @Test
    void Indirect_Linkage_Layer2_Basic_WithVar() throws Exception {
        assertLinked("<#1 --> <b --> (k-->x)>>", "(k-->x)");
    }

    @Test
    void Indirect_Linkage_Inh_WithSect() throws Exception {
        assertLinked("(a-->b)", "(a --> (b & c))");
    }

    @Test
    void Indirect_Linkage_Inh_WithSectVar() throws Exception {
        assertLinked("(#1-->b)", "<a --> (b & c)>");
    }


    @Test
    @Disabled /* requires inheritance to have termlink templates to level 2, but this doesnt seem critical otherwise */ void Indirect_Linkage_Layer2_Basic_WithVar2() throws Exception {
        assertLinked("<a --> <b --> <#1 --> x>>>", BELIEF, "(k-->x)");
    }

    @Test
    @Disabled /* requires inheritance to have termlink templates to level 2, but this doesnt seem critical otherwise */ void Indirect_Linkage_Layer2_Basic_WithVar2_Goal() throws Exception {
        assertLinked("<a --> <b --> <#1 --> x>>>", GOAL, "(k-->x)");
    }

    @Test
    @Disabled /* requires inheritance to have termlink templates to level 2, but this doesnt seem critical otherwise */ void Indirect_Linkage_Layer2_Basic_WithVar2_Question() throws Exception {
        assertLinked("<a --> <b --> <#1 --> x>>>", QUESTION, "(k-->x)");
    }

    private void testConceptFormed(String s) throws Exception {

        test.requireConditions = false;
        TestNAR tester = test;
        tester.believe(s, 1.0f, 0.9f);
        tester.nar.run(10);
        Concept ret = tester.nar.conceptualize(s);

        assertNotNull(ret, "Failed to create a concept for " + s);
    }

    @Test
    void Basic_Concept_Formation_Test() throws Exception {
        testConceptFormed("(a-->b)");
    }

    @Test
    void Advanced_Concept_Formation_Test() throws Exception {
        testConceptFormed("<#1 --> b>");
    }

    @Test
    void Advanced_Concept_Formation_Test2() throws Exception {
        testConceptFormed("<<$1 --> a> ==> <$1 --> b>>");
    }

    @Test
    void Advanced_Concept_Formation_Test2_2() throws Exception {
        testConceptFormed("<<$1 --> bird> ==> <$1 --> animal>>");
    }

    @Test
    void Advanced_Concept_Formation_Test3() throws Exception {
        testConceptFormed("(&&,<#1 --> lock>,<<$2 --> key> ==> open($2, #1)>)");
    }


    @Test
    void Variable_Normalization_1() throws Exception {

        NAR tester = NARS.tmp();
        test.requireConditions = false;

        String nonsense = "<(&&,<#1 --> M>,<#2 --> M>) ==> <#1 --> nonsense>>";
        tester.believe(nonsense);
        tester.run(1);
        Concept c = tester.conceptualize(nonsense);
        assertNotNull(c);
    }

}
