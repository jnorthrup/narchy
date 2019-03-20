package nars.nal.nal2;


import nars.$;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.test.NALTest;
import nars.test.TestNAR;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class NAL2Test extends NALTest {

    private static final int cycles = 1000;


    @Override
    protected NAR nar() {

        NAR n = NARS.tmp(2);
        n.confMin.set(0.3f);
        n.termVolumeMax.set(8);
        return n;
    }


    /* Handled by parser, this copula is just syntactic sugar
    @Test
    public void instanceToInheritance() throws InvalidInputException {
        test()
        .believe("<Tweety -{- bird>")
        .mustBelieve(cycles,"<{Tweety} --> bird>",1.0f,0.9f)
        .run();
    }*/

    /* Handled by parser, this copula is just syntactic sugar
    @Test
    public void propertyToInheritance() throws InvalidInputException {
        test().believe("<raven -]- black>")
        .mustBelieve(cycles,"<raven --> [black]>",1.0f,0.9f)
        .run();
    }*/

    /* Handled by parser, this copula is just syntactic sugar
    @Test
    public void instancePropertyToInheritance() throws InvalidInputException {
        test().believe("<Tweety {-] yellow>") 
        .mustBelieve(cycles,"<{Tweety} --> [yellow]>",1.0f,0.9f)
        .run();
    }
*/

    @Disabled
    @Test
    void setDefinition() {

        TestNAR tester = test;

        tester.believe("<{Tweety} --> {Birdie}>");
        tester.mustBelieve(cycles, "<{Tweety} <-> {Birdie}>", 1.0f, 0.9f);


    }

    @Disabled
    @Test
    void setDefinition2() {

        TestNAR tester = test;
        tester.believe("<[smart] --> [bright]>");
        tester.mustBelieve(cycles, "<[bright] <-> [smart]>", 1.0f, 0.9f);

    }

    @Test
    void setDefinition3() {

        TestNAR tester = test;
        tester.believe("<{Birdie} <-> {Tweety}>");
        tester.mustBelieve(cycles, "<Birdie <-> Tweety>", 1.0f, 0.9f);
        tester.mustBelieve(cycles, "<{Tweety} --> {Birdie}>", 1.0f, 0.9f);

    }

    @Test
    void setDefinition4() {

        TestNAR tester = test;
        tester.believe("<[bright] <-> [smart]>");
        tester.mustBelieve(cycles, "<bright <-> smart>", 1.0f, 0.9f);
        tester.mustBelieve(cycles, "<[bright] --> [smart]>", 1.0f, 0.9f);

    }

    @Test
    void structureTransformation() {

        TestNAR tester = test;
        tester.believe("<Birdie <-> Tweety>", 0.9f, 0.9f);
        tester.ask("<{Birdie} <-> {Tweety}>");
        tester.mustBelieve(cycles, "<{Birdie} <-> {Tweety}>", 0.9f, 0.73f /*0.9f*/);

    }

    @Test
    void structureTransformation2() {

        TestNAR tester = test;
        tester.believe("<bright <-> smart>", 0.9f, 0.9f);
        tester.ask("<[bright] --> [smart]>");
        tester.mustBelieve(cycles, "<[bright] --> [smart]>", 0.9f, 0.73f /*0.9f*/);

    }

    @Test
    void structureTransformation3() {
        /*
        <bright <-> smart>. %0.9;0.9%
        <{bright} --> {smart}>?
         */

        TestNAR tester = test;
        tester.believe("<bright <-> smart>", 0.9f, 0.9f);
        tester.ask("<{bright} --> {smart}>");
        tester.mustBelieve(cycles, "<{bright} --> {smart}>", 0.9f, 0.73f /*0.9f*/);

    }

    @Test
    void backwardInference() {

        TestNAR tester = test;


        tester.believe("<bird --> swimmer>");
        tester.ask("<{?x} --> swimmer>");
        tester.mustOutput(cycles, "<{?1} --> bird>?");

    }

    @Test
    void analogyPos() {


        TestNAR tester = test;
        tester.believe("<p1 --> p2>");
        tester.believe("<p2 <-> p3>");
        tester.mustBelieve(cycles, "<p1 --> p3>",
                1.0f, 0.81f);

    }

    @Test
    void analogyNeg() {

        test
                .believe("--(p1 --> p2)")
                .believe("(p2 <-> p3)")
                .mustBelieve(cycles, "(p1 --> p3)",
                        0f, 0.81f);

    }

    @Test
    void testUnion() {

        test
                .believe("a:{x}.")
                .believe("a:{y}.")
                .mustBelieve(cycles, "a:{x,y}", 1.0f, 0.81f);

    }

    @Test
    void testSetDecomposePositive() {
        test
                .believe("({x,y}-->c)")
                .mustBelieve(cycles, "({x}-->c)", 1f, 0.81f)
                .mustBelieve(cycles, "({y}-->c)", 1f, 0.81f)
        ;
    }

    @Test
    void testInstToNonSet() {
        test
                .believe("({x}-->c)")
                .mustBelieve(cycles, "(x-->c)", 1f, 0.81f)
        ;
    }
    @Test
    void testPropToNonSet() {
        test
                .believe("(c --> [x])")
                .mustBelieve(cycles, "(c-->x)", 1f, 0.81f)
        ;
    }

    @Test
    void testSetDecomposeNegativeExt() {

        test
                .believe("<{--x,y}-->c>")
                .mustBelieve(cycles, "({--x}-->c)", 1f, 0.81f)
                .mustBelieve(cycles, "({y}-->c)", 1f, 0.81f)
        ;
    }

    @Test
    void testSetDecomposeNegativeInt() {

        test
                .believe("<c-->[--x,y]>")
                .mustBelieve(cycles, "(c-->[--x])", 1f, 0.81f)
                .mustBelieve(cycles, "(c-->[y])", 1f, 0.81f)
        ;
    }

    @Test
    void testIntersectDiffUnionOfCommonSubtermsPre() {
        test
                .believe("<{x,y}-->c>")
                .mustBelieve(cycles, "<{x}-->c>", 1f, 0.81f)
                .mustBelieve(cycles, "<{y}-->c>", 1f, 0.81f)
        ;
    }
    @Test
    void testIntersectDiffUnionOfCommonSubterms() {
        test
                .believe("<{x,y}-->c>")
                .believe("<{x,z}-->c>")
                .mustBelieve(cycles, "<{x,y,z}-->c>", 1f, 0.81f)
                .mustBelieve(cycles, "<{x}-->c>", 1f, 0.81f)
                .mustBelieve(cycles, "<{y}-->c>", 1f, 0.81f)
                .mustBelieve(cycles, "<{z}-->c>", 1f, 0.81f)
        ;
    }


    @Test
    void set_operations() {

        test
                .termVolMax(7)
                .believe("<planetX --> {Mars,Pluto,Venus}>", 0.9f, 0.9f)
                .believe("<planetX --> {Pluto,Saturn}>", 0.7f, 0.9f)
                .mustBelieve(cycles, "<planetX --> {Mars,Pluto,Saturn,Venus}>", 0.97f, 0.81f)
                .mustBelieve(cycles, "<planetX --> {Pluto}>",
                        0.8f, 0.73f);
        //0.63f, 0.81f);

    }

    @Test
    void set_operationsSetExt_union() {

        TestNAR tester = test;
        tester.believe("<planetX --> {Mars,Pluto,Venus}>", 0.9f, 0.9f);
        tester.believe("<planetX --> {Pluto,Saturn}>", 0.1f, 0.9f);
        tester.mustBelieve(cycles, "<planetX --> {Mars,Pluto,Saturn,Venus}>", 0.91f, 0.81f);
    }

    @Test
    void set_operationsSetExt_unionNeg() {

        TestNAR tester = test;
        tester.believe("<planetX --> {Earth}>", 0.1f, 0.9f);
        tester.believe("<planetX --> {Mars}>", 0.1f, 0.9f);
        tester.mustBelieve(cycles, "<planetX --> {Earth,Mars}>", 0.19f, 0.81f);
    }


    @Test
    void set_operationsSetInt_union_2_3_4() {

        TestNAR tester = test;
        tester.believe("<planetX --> [marsy,earthly,venusy]>", 1.0f, 0.9f);
        tester.believe("<planetX --> [earthly,saturny]>", 0.1f, 0.9f);
        tester.mustBelieve(cycles, "<planetX --> [marsy,earthly,saturny,venusy]>", 0.1f, 0.81f);
    }

    @Test
    void set_operationsSetInt_union1_1_2_3() {

        TestNAR tester = test;
        tester.nar.termVolumeMax.set(6);
        tester.believe("<planetX --> [marsy,venusy]>", 1.0f, 0.9f);
        tester.believe("<planetX --> [earthly]>", 0.1f, 0.9f);
        tester.mustBelieve(cycles, "<planetX --> [marsy,earthly,venusy]>", 0.1f, 0.81f);

    }

    @Test
    void set_operations2_difference() throws Narsese.NarseseException {
        assertEquals("{Mars,Venus}", $.diff($.$("{Mars,Pluto,Venus}"), $.$("{Pluto,Saturn}")).toString());

        TestNAR tester = test;
        tester.believe("(planetX --> {Mars,Pluto,Venus})", 0.9f, 0.9f);
        tester.believe("(planetX --> {Pluto,Saturn})", 0.1f, 0.9f);
        tester.mustBelieve(cycles, "(planetX --> {Mars,Venus})", 0.81f ,0.81f);

    }


    @Test
    void set_operations3_difference() {

        TestNAR tester = test;
        tester.believe("<planetX --> [marsy,earthly,venusy]>", 1.0f, 0.9f);
        tester.believe("<planetX --> [earthly,saturny]>", 0.1f, 0.9f);
        tester.mustBelieve(cycles, "<planetX --> [marsy,earthly,saturny,venusy]>", 0.1f, 0.81f);
        tester.mustBelieve(cycles, "<planetX --> [marsy,venusy]>", 0.90f, 0.81f);
    }

    @Test
    void set_operations4() {

        TestNAR tester = test;
        tester.termVolMax(7);
        tester.believe("([marsy,earthly,venusy] --> planetX)", 1.0f, 0.9f);
        tester.believe("([earthly,saturny] --> planetX)", 0.1f, 0.9f);
        tester.mustBelieve(cycles, "<[marsy,earthly,saturny,venusy] --> planetX>", 1.0f, 0.81f);
        tester.mustBelieve(cycles, "<[marsy,venusy] --> planetX>", 0.90f, 0.81f);

    }

    @Test
    void set_operations5Half() {

        TestNAR tester = test;
        tester.believe("<{Mars,Pluto,Venus} --> planetX>", 1.0f, 0.9f);
        tester.mustBelieve(cycles, "<{Mars,Venus} --> planetX>", 1.0f, 0.81f);
    }

    @Test
    void set_operations5() {

        TestNAR tester = test;
        tester.believe("<{Mars,Pluto,Venus} --> planetX>", 1.0f, 0.9f);
        tester.believe("<{Pluto,Saturn} --> planetX>", 0.1f, 0.9f);
        tester.mustBelieve(cycles*2, "<{Mars,Pluto,Saturn,Venus} --> planetX>", 0.1f, 0.81f);
        tester.mustBelieve(cycles*2, "<{Mars,Venus} --> planetX>", 0.9f, 0.81f);
    }

}

