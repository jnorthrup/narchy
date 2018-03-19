package nars.nal.nal2;


import nars.$;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.test.TestNAR;
import nars.util.NALTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.Op.DIFFe;
import static org.junit.jupiter.api.Assertions.assertEquals;

//@RunWith(Parameterized.class)
public class NAL2Test extends NALTest {

    static final int cycles = 350;


    @Override
    protected NAR nar() {
        return NARS.tmp(2);
    }

    @Test
    public void revision() {

        TestNAR tester = test;
        tester.mustBelieve(cycles, "<robin <-> swan>", 0.87f, 0.91f);//;//Robin is probably similar to swan.");
        tester.believe("<robin <-> swan>");//;//Robin is similar to swan.");
        tester.believe("<robin <-> swan>", 0.1f, 0.6f);
    }

    @Test
    public void comparison() {

        TestNAR tester = test;
        tester.believe("<swan --> swimmer>", 0.9f, 0.9f);//Swan is a type of swimmer.");
        tester.believe("<swan --> bird>");//Swan is a type of bird.");
        tester.mustBelieve(cycles, "<bird <-> swimmer>", 0.9f, 0.45f);//I guess that bird is similar to swimmer.");

    }

    @Test
    public void comparison2() {

        TestNAR tester = test;
        tester.believe("<sport --> competition>"); //Sport is a type of competition.");
        tester.believe("<chess --> competition>", 0.9f, 0.9f);//Chess is a type of competition.");
        tester.mustBelieve(cycles, "<chess <-> sport>", 0.9f, 0.45f);//I guess chess is similar to sport.");

    }

//    @Test public void inductionNegation() {
//        //(A --> C), (B --> C), neq(A,B) |- (B --> A), (Belief:Induction, Desire:Weak, Derive:AllowBackward)
//        test().log()
//                .believe("<worm --> bird>", 0.1f, 0.9f)
//                .believe("<tweety --> bird>", 0.9f, 0.9f)
//                .mustBelieve(cycles, "<worm --> tweety>", 0.10f, 0.42f)
//                .mustBelieve(cycles, "<tweety --> worm>", 0.90f, 0.07f)
//                .mustBelieve(cycles, "<tweety <-> worm>", 0.10f, 0.42f)
//        ;
//    }

    @Test
    public void analogy() {

        TestNAR tester = test;
        tester.believe("<swan --> swimmer>");//Swan is a type of swimmer.");
        tester.believe("<gull <-> swan>");//Gull is similar to swan.");
        tester.mustBelieve(cycles, "<gull --> swimmer>", 1.0f, 0.81f);//I think gull is a type of swimmer.");

    }

    @Test
    public void analogy2() {

        TestNAR tester = test;
        tester.believe("<gull --> swimmer>");//Gull is a type of swimmer.");
        tester.believe("<gull <-> swan>");//Gull is similar to swan.");
        tester.mustBelieve(cycles, "<swan --> swimmer>", 1.0f, 0.81f);//I believe a swan is a type of swimmer.");
    }

    @Disabled
    @Test //TODO is this right
    public void analogyNeg() {

        test
                .believe("(bird --> swimmer)")
                .believe("--(rock <-> swimmer)")
                .mustBelieve(cycles, "<bird --> rock>", 0, 0.81f)
        ;
    }

    @Test
    public void resemblance() {

        TestNAR tester = test;
        //tester.log();
        tester.believe("<robin <-> swan>");//Robin is similar to swan.");
        tester.believe("<gull <-> swan>");//Gull is similar to swan.");
        tester.mustBelieve(cycles, "<gull <-> robin>", 1.0f, 0.81f);//Gull is similar to robin.");

    }

    @Test
    public void inheritanceToSimilarity() {

        TestNAR tester = test;
        tester.believe("<swan --> bird>");//Swan is a type of bird. ");
        tester.believe("<bird --> swan>", 0.1f, 0.9f);//Bird is not a type of swan.");
        tester.mustBelieve(cycles, "<bird <-> swan>", 0.1f, 0.81f);//Bird is different from swan.");

    }

    @Test
    public void inheritanceToSimilarity2() {

        TestNAR tester = test;
        tester.believe("<swan --> bird>");//Swan is a type of bird.");
        tester.believe("<bird <-> swan>", 0.1f, 0.9f);//Bird is different from swan.");
        tester.mustBelieve(cycles * 4, "<bird --> swan>", 0.1f, 0.73f);//Bird is probably not a type of swan.");
    }

    @Test
    public void inheritanceToSimilarity3() throws nars.Narsese.NarseseException {

        TestNAR tester = test;
        tester.believe("<swan --> bird>", 0.9f, 0.9f);//Swan is a type of bird.");
        tester.ask("<bird <-> swan>");//Is bird similar to swan?");
        tester.mustBelieve(cycles, "<bird <-> swan>", 0.9f, 0.45f);//I guess that bird is similar to swan.");

    }

    @Test
    public void inheritanceToSimilarity4() throws nars.Narsese.NarseseException {

        TestNAR tester = test;
        tester.believe("<bird <-> swan>", 0.9f, 0.9f);//a bird is similar to a swan.");
        tester.ask("<swan --> bird>");//Is swan a type of bird?");
        tester.mustBelieve(cycles, "<swan --> bird>", 0.9f, 0.73f);//A swan is a type of bird.");

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
    public void missingEdgeCase1() {
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
                1.0f, 1.0f, 0.81f, 1.0f);
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
                .mustBelieve(cycles*2,"({x}-->c)", 1f, 0.81f)
                .mustBelieve(cycles*2,"({y}-->c)", 1f, 0.81f)
        ;
    }

    @Test
    public void testSetDecomposeNegativeExt() {
        //tests that a termlink (which is always positive) can match a subterm which is negative to decompose the set
        test
                .believe("<{--x,y}-->c>")
                .mustBelieve(cycles*2,"({--x}-->c)", 1f, 0.81f)
                .mustBelieve(cycles*2,"({y}-->c)", 1f, 0.81f)
        ;
    }
    @Test
    public void testSetDecomposeNegativeInt() {
        //tests that a termlink (which is always positive) can match a subterm which is negative to decompose the set
        test
                .believe("<c-->[--x,y]>")
                .mustBelieve(cycles*2,"(c-->[--x])", 1f, 0.81f)
                .mustBelieve(cycles*2,"(c-->[y])", 1f, 0.81f)
        ;
    }

    @Test
    public void testIntersectDiffUnionOfCommonSubterms() {
        test
                .believe("<{x,y}-->c>")
                .believe("<{x,z}-->c>")
                .mustBelieve(cycles*2, "<{x,y,z}-->c>", 1f, 0.81f) //union
                .mustBelieve(cycles*2, "<{x}-->c>", 1f, 0.81f) //intersect
                .mustBelieve(cycles*2, "<{y}-->c>", 1f, 0.81f) //difference
                .mustBelieve(cycles*2, "<{z}-->c>", 1f, 0.81f) //difference
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
        tester.mustBelieve(cycles * 2, "(planetX --> {Mars,Venus})", 0.9f, 0.73f /*0.81f ,0.81f*/); //.en("PlanetX is either Mars or Venus.");

    }


    @Test
    public void set_operations3_difference() {

        TestNAR tester = test;
        tester.believe("<planetX --> [marsy,earthly,venusy]>", 1.0f, 0.9f); //.en("PlanetX is Mars, Pluto, or Venus.");
        tester.believe("<planetX --> [earthly,saturny]>", 0.1f, 0.9f); //.en("PlanetX is probably neither Pluto nor Saturn.");
        tester.mustBelieve(cycles*2, "<planetX --> [marsy,earthly,saturny,venusy]>", 0.1f ,0.81f); //.en("PlanetX is Mars, Pluto, Saturn, or Venus.");
        tester.mustBelieve(cycles*2, "<planetX --> [marsy,venusy]>", 0.90f ,0.81f); //.en("PlanetX is either Mars or Venus.");
    }

    @Test
    public void set_operations4() {

        TestNAR tester = test;
        tester.believe("<[marsy,earthly,venusy] --> planetX>", 1.0f, 0.9f); //.en("PlanetX is Mars, Pluto, or Venus.");
        tester.believe("<[earthly,saturny] --> planetX>", 0.1f, 0.9f); //.en("PlanetX is probably neither Pluto nor Saturn.");
        tester.mustBelieve(cycles * 2, "<[marsy,earthly,saturny,venusy] --> planetX>", 1.0f, 0.81f); //.en("PlanetX is Mars, Pluto, Saturn, or Venus.");
        tester.mustBelieve(cycles * 2, "<[marsy,venusy] --> planetX>", 0.90f, 0.81f); //.en("PlanetX is either Mars or Venus.");

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
        tester.mustBelieve(cycles * 3, "<{Mars,Pluto,Saturn,Venus} --> planetX>", 0.1f, 0.81f); //.en("PlanetX is Mars, Pluto, Saturn, or Venus.");
        tester.mustBelieve(cycles * 3, "<{Mars,Venus} --> planetX>", 0.9f, 0.81f); //.en("PlanetX is either Mars or Venus.");
    }

}

