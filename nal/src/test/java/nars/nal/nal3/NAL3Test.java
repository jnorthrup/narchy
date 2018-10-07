package nars.nal.nal3;


import nars.NAR;
import nars.NARS;
import nars.test.NALTest;
import nars.test.TestNAR;
import org.junit.jupiter.api.Test;

import static nars.Op.BELIEF;
import static nars.time.Tense.ETERNAL;

public class NAL3Test extends NALTest {

    static final int cycles = 100;
    
    @Override protected NAR nar() {
        NAR n= NARS.tmp(3);
        n.termVolumeMax.set(7);
        return n;
    }

    @Test
    void compound_composition_two_premises() {

        TestNAR tester = test;
        tester.believe("(swan --> swimmer)", 0.9f, 0.9f); 
        tester.believe("(swan --> bird)", 0.8f, 0.9f); 
        tester.mustBelieve(cycles, "(swan --> (bird | swimmer))", 0.98f, 0.81f); 
        tester.mustBelieve(cycles, "(swan --> (bird & swimmer))", 0.72f, 0.81f); 

    }

    @Test
    void compound_composition_two_premises2() {

        TestNAR tester = test;
        tester.believe("<sport --> competition>", 0.9f, 0.9f); 
        tester.believe("<chess --> competition>", 0.8f, 0.9f); 
        tester.mustBelieve(cycles, "<(|,chess,sport) --> competition>", 0.72f, 0.81f); 
        tester.mustBelieve(cycles, "<(&,chess,sport) --> competition>", 0.98f, 0.81f); 

    }

    @Test
    void compound_decomposition_two_premises() {

        TestNAR tester = test;
        tester.believe("<robin --> (|,bird,swimmer)>", 1.0f, 0.9f); 
        tester.believe("<robin --> swimmer>", 0.0f, 0.9f); 
        tester.mustBelieve(cycles, "<robin --> bird>", 1.0f, 0.81f); 

    }

    @Test
    void compound_decomposition_two_premises2() {

        TestNAR tester = test;

        tester.believe("<robin --> swimmer>", 0.0f, 0.9f); 
        tester.believe("<robin --> (-,mammal,swimmer)>", 0.0f, 0.9f); 
        tester.mustBelieve(cycles, "<robin --> mammal>", 0.0f, 0.81f); 

    }




    @Test
    void intersectionComposition(){
        test
                .believe("(swan --> bird)")
                .believe("(swimmer--> bird)")
                .mustBelieve(cycles, "((swan&swimmer) --> bird)", 1f, 0.81f);
    }

    @Test
    void intersectionCompositionWrappedInProd(){
        test
                .believe("((swan) --> bird)")
                .believe("((swimmer)--> bird)")
                .mustBelieve(cycles, "(((swan)&(swimmer)) --> bird)", 1f, 0.81f);
    }



    @Test
    void compound_decomposition_one_premise() {

        TestNAR tester = test;

        tester.believe("(robin --> (bird - swimmer))", 0.9f, 0.9f); 
        tester.mustBelieve(cycles, "<robin --> bird>", 0.90f, 0.73f); 

    }

    @Test
    void compound_decomposition_one_premise3() {

        TestNAR tester = test;
        tester.believe("<(boy ~ girl) --> [strong]>", 0.9f, 0.9f); 
        tester.mustBelieve(cycles, "<boy --> [strong]>", 0.90f, 0.73f); 
    }

    @Test
    void compound_decomposition_one_premise2() {

        TestNAR tester = test;
        tester.believe("<(boy | girl) --> youth>", 0.9f, 0.9f); 
        tester.mustBelieve(cycles*2, "<boy --> youth>", 0.90f, 0.73f); 

    }


    @Test
    void testDifference() {

        TestNAR tester = test;
        tester.believe("<swan --> bird>", 0.9f, 0.9f); 
        tester.believe("<dinosaur --> bird>", 0.7f, 0.9f); 
        tester.mustBelieve(cycles, "bird:(swan ~ dinosaur)", 0.27f, 0.81f); 
        tester.mustBelieve(cycles, "bird:(dinosaur ~ swan)", 0.07f, 0.81f); 
    }

    @Test
    void testArity1_Decomposition_IntersectExt() {
        

        test
                .believe("(a-->b)")
                .believe("(a-->(b&c))", 0f, 0.9f)
                .mustBelieve(cycles, "(a-->c)", 0f, 0.81f, ETERNAL);

    }

    @Test
    void testArity1_Decomposition_IntersectExt2() {
        test
                .believe("(b-->a)", 0.25f, 0.9f)
                .believe("((b&c)-->a)", 0.25f, 0.9f)
                .mustBelieve(cycles, "(c-->a)", 0.19f, 0.15f, ETERNAL);
    }

    @Test
    void testArity1_Decomposition_IntersectInt() {
        

        test
                .believe("(a-->b)", 0.25f, 0.9f)
                .believe("(a-->(b|c))", 0.25f, 0.9f)
                .mustBelieve(cycles, "(a-->c)", 0.19f, 0.15f, ETERNAL);
    }

    @Test
    void testDisjoint2() {
        
        
        test
                .believe("--(x-->(RealNumber&ComplexNumber))")
                .believe("(x-->RealNumber)")
                .mustBelieve(cycles, "(x-->ComplexNumber)", 0f, 0.81f);
        ;

    }
    @Test
    void testDisjoint2Learned() {


        test
                .believe("--(x-->ComplexNumber)")
                .believe("(x-->RealNumber)")
                .mustBelieve(cycles, "(x-->(RealNumber&ComplexNumber))", 0f, 0.81f);
        ;

    }
    @Test
    void testDisjoint3() {

        test
                .believe("--(x-->(&,RealNumber,ComplexNumber,Letter))")
                .believe("(x-->RealNumber)")
                .mustBelieve(cycles, "(x-->(ComplexNumber&Letter))", 0f, 0.81f)
                .mustNotOutput(cycles, "(x-->((&,RealNumber,ComplexNumber,Letter)|RealNumber))", BELIEF, ETERNAL)
        ;

    }
    @Test
    void testDisjointWithVar() {
        

        test
            .believe("--(#1-->(RealNumber&ComplexNumber))")
            .believe("(x-->RealNumber)")
            .mustBelieve(cycles, "(x-->ComplexNumber)", 0f, 0.81f)
        ;

    }

}

