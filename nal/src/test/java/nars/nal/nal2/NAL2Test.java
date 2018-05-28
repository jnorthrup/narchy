package nars.nal.nal2;


import nars.$;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.test.TestNAR;
import nars.util.NALTest;
import org.junit.jupiter.api.Test;

import static nars.Op.DIFFe;
import static org.junit.jupiter.api.Assertions.assertEquals;

//@RunWith(Parameterized.class)
public class NAL2Test extends NALTest {

    static final int cycles = 450;


    @Override
    protected NAR nar() {

        NAR n = NARS.tmp(2);
        n.termVolumeMax.set(14);
        return n;
    }


    /* Handled by parser, this copula is just syntactic sugar
    @Test
    public void instanceToInheritance() throws InvalidInputException {
        test()
        .believe("<Tweety -{- bird>")//Tweety is a bird.");
        .mustBelieve(cycles,"<{Tweety} --> bird>",1.0f,0.9f)//Tweety is a bird.");
        .run();
    }*/

    /* Handled by parser, this copula is just syntactic sugar
    @Test
    public void propertyToInheritance() throws InvalidInputException {
        test().believe("<raven -]- black>")//Ravens are black.");
        .mustBelieve(cycles,"<raven --> [black]>",1.0f,0.9f)//Ravens are black.");
        .run();
    }*/

    /* Handled by parser, this copula is just syntactic sugar
    @Test
    public void instancePropertyToInheritance() throws InvalidInputException {
        test().believe("<Tweety {-] yellow>") //Tweety is yellow.");
        .mustBelieve(cycles,"<{Tweety} --> [yellow]>",1.0f,0.9f)//Tweety is yellow.");
        .run();
    }
*/

    @Test
    public void setDefinition() {

        TestNAR tester = test;

        tester.believe("<{Tweety} --> {Birdie}>");//Tweety is Birdie.");
        tester.mustBelieve(cycles, "<{Tweety} <-> {Birdie}>", 1.0f, 0.9f);//Birdie is similar to Tweety.");


    }

    @Test
    public void setDefinition2() {

        TestNAR tester = test;
        tester.believe("<[smart] --> [bright]>");//Smart thing is a type of bright thing.");
        tester.mustBelieve(cycles, "<[bright] <-> [smart]>", 1.0f, 0.9f);//Bright thing is similar to smart thing.");

    }

    @Test
    public void setDefinition3() {

        TestNAR tester = test;
        tester.believe("<{Birdie} <-> {Tweety}>");//Birdie is similar to Tweety.");
        tester.mustBelieve(cycles, "<Birdie <-> Tweety>", 1.0f, 0.9f);//Birdie is similar to Tweety.");
        tester.mustBelieve(cycles, "<{Tweety} --> {Birdie}>", 1.0f, 0.9f);//Tweety is Birdie.");

    }

    @Test
    public void setDefinition4() {

        TestNAR tester = test;
        tester.believe("<[bright] <-> [smart]>");//Bright thing is similar to smart thing.");
        tester.mustBelieve(cycles, "<bright <-> smart>", 1.0f, 0.9f);//Bright is similar to smart.");
        tester.mustBelieve(cycles, "<[bright] --> [smart]>", 1.0f, 0.9f);//Bright thing is a type of smart thing.");

    }

    @Test
    public void structureTransformation() throws nars.Narsese.NarseseException {

        TestNAR tester = test;
        tester.believe("<Birdie <-> Tweety>", 0.9f, 0.9f);//Birdie is similar to Tweety.");
        tester.ask("<{Birdie} <-> {Tweety}>");//Is Birdie similar to Tweety?");
        tester.mustBelieve(cycles, "<{Birdie} <-> {Tweety}>", 0.9f, 0.9f);//Birdie is similar to Tweety.");

    }

    @Test
    public void structureTransformation2() throws nars.Narsese.NarseseException {

        TestNAR tester = test;
        tester.believe("<bright <-> smart>", 0.9f, 0.9f);//Bright is similar to smart.");
        tester.ask("<[bright] --> [smart]>");//Is bright thing a type of smart thing?");
        tester.mustBelieve(cycles, "<[bright] --> [smart]>", 0.9f, 0.9f);//Bright thing is a type of smart thing.");

    }

    @Test
    public void structureTransformation3() throws nars.Narsese.NarseseException {
        /*
        <bright <-> smart>. %0.9;0.9%
        <{bright} --> {smart}>?
         */

        TestNAR tester = test;
        tester.believe("<bright <-> smart>", 0.9f, 0.9f);//Bright is similar to smart.");
        tester.ask("<{bright} --> {smart}>");//Is bright thing a type of smart thing?");
        tester.mustBelieve(cycles, "<{bright} --> {smart}>", 0.9f, 0.9f);//Bright thing is a type of smart thing.");

    }

    @Test
    public void backwardInference() throws nars.Narsese.NarseseException {

        TestNAR tester = test;


        //<bird --> swimmer>. <{?x} --> swimmer>?
        tester.believe("<bird --> swimmer>");//Bird is a type of swimmer. ");
        tester.ask("<{?x} --> swimmer>");//What is a swimmer?");
        tester.mustOutput(cycles, "<{?1} --> bird>?");//What is a bird?");

    }

    @Test
    public void analogyPos() {
        //		((<%1 --> %2>, <%2 <-> %3>, not_equal(%3, %1)), (<%1 --> %3>, (<Analogy --> Truth>, <Strong --> Desire>, <AllowBackward --> Derive>)))
        //((<%1 --> %2>, <%2 <-> %3>, not_equal(%3, %1)),
        //      (<%1 --> %3>,
        //((<p1 --> p2>, <p2 <-> p3>, not_equal(p3, p1)),
        //      (<p1 --> p3>,
        //        TestNAR tester = test();

        TestNAR tester = test;
        tester.believe("<p1 --> p2>");
        tester.believe("<p2 <-> p3>");
        tester.mustBelieve(cycles, "<p1 --> p3>",
                1.0f, 0.81f);
        //tester.debug();
    }

    @Test
    public void analogyNeg() {

        test
        .believe("--(p1 --> p2)")
        .believe("(p2 <-> p3)")
        .mustBelieve(cycles, "(p1 --> p3)",
                0f, 0.81f);
        //tester.debug();
    }

    @Test
    public void testUnion() {

        test
                .believe("a:{x}.")
                .believe("a:{y}.")
                .mustBelieve(cycles, "a:{x,y}", 1.0f, 0.81f);

    }
    @Test
    public void testSetDecomposePositive() {
        test
                .believe("<{x,y}-->c>")
                .mustBelieve(cycles,"({x}-->c)", 1f, 0.81f)
                .mustBelieve(cycles,"({y}-->c)", 1f, 0.81f)
        ;
    }

    @Test
    public void testSetDecomposeNegativeExt() {
        //tests that a termlink (which is always positive) can match a subterm which is negative to decompose the set
        test
                .believe("<{--x,y}-->c>")
                .mustBelieve(cycles,"({--x}-->c)", 1f, 0.81f)
                .mustBelieve(cycles,"({y}-->c)", 1f, 0.81f)
        ;
    }
    @Test
    public void testSetDecomposeNegativeInt() {
        //tests that a termlink (which is always positive) can match a subterm which is negative to decompose the set
        test
                .believe("<c-->[--x,y]>")
                .mustBelieve(cycles,"(c-->[--x])", 1f, 0.81f)
                .mustBelieve(cycles,"(c-->[y])", 1f, 0.81f)
        ;
    }

    @Test
    public void testIntersectDiffUnionOfCommonSubterms() {
        test
                .believe("<{x,y}-->c>")
                .believe("<{x,z}-->c>")
                .mustBelieve(cycles, "<{x,y,z}-->c>", 1f, 0.81f) //union
                .mustBelieve(cycles, "<{x}-->c>", 1f, 0.81f) //intersect
                .mustBelieve(cycles, "<{y}-->c>", 1f, 0.81f) //difference
                .mustBelieve(cycles, "<{z}-->c>", 1f, 0.81f) //difference
        //.mustBelieve(cycles, "<{y}-->c>", 0f, 0.81f) //difference
        //these are probably ok:
        //.mustNotOutput(cycles,"<{x}-->c>", BELIEF, 0, 0, 0.5f, 1, ETERNAL) //contradiction of input above conf=0.5
        //.mustNotOutput(cycles,"<{x,y}-->c>", BELIEF, 0, 0, 0.5f, 1, ETERNAL) //contradiction of input above conf=0.5
        //.mustNotOutput(cycles,"<{x,z}-->c>", BELIEF, 0, 0, 0.5f, 1, ETERNAL) //contradiction of input above conf=0.5
        ;

    }


    @Test
    public void set_operations() {

        test
                .believe("<planetX --> {Mars,Pluto,Venus}>", 0.9f, 0.9f) //.en("PlanetX is Mars, Pluto, or Venus.");
                .believe("<planetX --> {Pluto,Saturn}>", 0.7f, 0.9f) //.en("PlanetX is probably Pluto or Saturn.");
                .mustBelieve(cycles, "<planetX --> {Mars,Pluto,Saturn,Venus}>", 0.97f, 0.81f) //.en("PlanetX is Mars, Pluto, Saturn, or Venus.");
                .mustBelieve(cycles, "<planetX --> {Pluto}>", 0.63f, 0.81f); //.en("PlanetX is probably Pluto.");

    }

    @Test
    public void set_operationsSetExt_union() {

        TestNAR tester = test;
        tester.believe("<planetX --> {Mars,Pluto,Venus}>", 0.9f, 0.9f); //.en("PlanetX is Mars, Pluto, or Venus.");
        tester.believe("<planetX --> {Pluto,Saturn}>", 0.1f, 0.9f); //.en("PlanetX is probably neither Pluto nor Saturn.");
        tester.mustBelieve(cycles, "<planetX --> {Mars,Pluto,Saturn,Venus}>", 0.91f, 0.81f); //.en("PlanetX is Mars, Pluto, Saturn, or Venus.");
    }

    @Test
    public void set_operationsSetExt_unionNeg() {

        TestNAR tester = test;
        tester.believe("<planetX --> {Earth}>", 0.1f, 0.9f); //.en("PlanetX is Mars, Pluto, or Venus.");
        tester.believe("<planetX --> {Mars}>", 0.1f, 0.9f); //.en("PlanetX is probably neither Pluto nor Saturn.");
        tester.mustBelieve(cycles, "<planetX --> {Earth,Mars}>", 0.19f, 0.81f); //.en("PlanetX is Mars, Pluto, Saturn, or Venus.");
    }


    @Test
    public void set_operationsSetInt_union_2_3_4() {

        TestNAR tester = test;
        tester.believe("<planetX --> [marsy,earthly,venusy]>", 1.0f, 0.9f); //.en("PlanetX is Mars, Pluto, or Venus.");
        tester.believe("<planetX --> [earthly,saturny]>", 0.1f, 0.9f); //.en("PlanetX is probably neither Pluto nor Saturn.");
        tester.mustBelieve(cycles, "<planetX --> [marsy,earthly,saturny,venusy]>", 0.1f, 0.81f); //.en("PlanetX is Mars, Pluto, Saturn, or Venus.");
    }

    @Test
    public void set_operationsSetInt_union1_1_2_3() {

        TestNAR tester = test;
        tester.believe("<planetX --> [marsy,venusy]>", 1.0f, 0.9f); //.en("PlanetX is Mars, Pluto, or Venus.");
        tester.believe("<planetX --> [earthly]>", 0.1f, 0.9f); //.en("PlanetX is probably neither Pluto nor Saturn.");
        tester.mustBelieve(cycles, "<planetX --> [marsy,earthly,venusy]>", 0.1f, 0.81f); //.en("PlanetX is Mars, Pluto, Saturn, or Venus.");

    }

    @Test
    public void set_operations2_difference() throws Narsese.NarseseException {
        assertEquals("{Mars,Venus}", DIFFe.the($.$("{Mars,Pluto,Venus}"), $.$("{Pluto,Saturn}")).toString());

        TestNAR tester = test;
        tester.believe("(planetX --> {Mars,Pluto,Venus})", 0.9f, 0.9f); //.en("PlanetX is Mars, Pluto, or Venus.");
        tester.believe("(planetX --> {Pluto,Saturn})", 0.1f, 0.9f); //.en("PlanetX is probably neither Pluto nor Saturn.");
        tester.mustBelieve(cycles, "(planetX --> {Mars,Venus})", 0.9f, 0.73f /*0.81f ,0.81f*/); //.en("PlanetX is either Mars or Venus.");

    }


    @Test
    public void set_operations3_difference() {

        TestNAR tester = test;
        tester.believe("<planetX --> [marsy,earthly,venusy]>", 1.0f, 0.9f); //.en("PlanetX is Mars, Pluto, or Venus.");
        tester.believe("<planetX --> [earthly,saturny]>", 0.1f, 0.9f); //.en("PlanetX is probably neither Pluto nor Saturn.");
        tester.mustBelieve(cycles, "<planetX --> [marsy,earthly,saturny,venusy]>", 0.1f ,0.81f); //.en("PlanetX is Mars, Pluto, Saturn, or Venus.");
        tester.mustBelieve(cycles, "<planetX --> [marsy,venusy]>", 1 /*0.90f*/ ,0.81f); //.en("PlanetX is either Mars or Venus.");
    }

    @Test
    public void set_operations4() {

        TestNAR tester = test;
        tester.believe("<[marsy,earthly,venusy] --> planetX>", 1.0f, 0.9f); //.en("PlanetX is Mars, Pluto, or Venus.");
        tester.believe("<[earthly,saturny] --> planetX>", 0.1f, 0.9f); //.en("PlanetX is probably neither Pluto nor Saturn.");
        tester.mustBelieve(cycles, "<[marsy,earthly,saturny,venusy] --> planetX>", 1.0f, 0.81f); //.en("PlanetX is Mars, Pluto, Saturn, or Venus.");
        tester.mustBelieve(cycles, "<[marsy,venusy] --> planetX>", 0.90f, 0.81f); //.en("PlanetX is either Mars or Venus.");

    }

    @Test
    public void set_operations5Half() {

        TestNAR tester = test;
        tester.believe("<{Mars,Pluto,Venus} --> planetX>", 1.0f, 0.9f); //.en("PlanetX is Mars, Pluto, or Venus.");
        tester.mustBelieve(cycles, "<{Mars,Venus} --> planetX>", 1.0f, 0.81f); //.en("PlanetX is either Mars or Venus.");
    }

    @Test
    public void set_operations5() {

        TestNAR tester = test;
        tester.believe("<{Mars,Pluto,Venus} --> planetX>", 1.0f, 0.9f); //.en("PlanetX is Mars, Pluto, or Venus.");
        tester.believe("<{Pluto,Saturn} --> planetX>", 0.1f, 0.9f); //.en("PlanetX is probably neither Pluto nor Saturn.");
        tester.mustBelieve(cycles, "<{Mars,Pluto,Saturn,Venus} --> planetX>", 0.1f, 0.81f); //.en("PlanetX is Mars, Pluto, Saturn, or Venus.");
        tester.mustBelieve(cycles, "<{Mars,Venus} --> planetX>", 0.9f, 0.81f); //.en("PlanetX is either Mars or Venus.");
    }

}

