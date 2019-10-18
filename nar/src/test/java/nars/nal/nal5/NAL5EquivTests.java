package nars.nal.nal5;

import nars.test.NALTest;
import nars.test.TestNAR;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.Op.BELIEF;
import static nars.time.Tense.ETERNAL;

/** original nal5 tests involving the equivalence operator */
@Disabled
class NAL5EquivTests extends NALTest {
    private static final int cycles = 50;


    @Test
    void comparisonEqui() {

        TestNAR tester = test;
        tester.believe("<<robin --> bird> ==> <robin --> animal>>"); 
        tester.believe("<<robin --> bird> ==> <robin --> [flying]>>", 0.8f, 0.9f); 
        tester.mustBelieve(cycles, "<<robin --> animal> <=> <robin --> [flying]>>", 0.80f, 0.45f); 
    }

    @Test
    void comparison2() {

        TestNAR tester = test;
        tester.believe("<<robin --> bird> ==> <robin --> animal>>", 0.7f, 0.9f); 
        tester.believe("<<robin --> [flying]> ==> <robin --> animal>>"); 
        tester.mustBelieve(cycles, "<<robin --> bird> <=> <robin --> [flying]>>", 0.70f, 0.45f); 
    }

    @Test
    void comparisonOppositeEqui() {

        TestNAR tester = test;
        tester.believe("<(x) ==> (z)>", 0.1f, 0.9f);
        tester.believe("<(y) ==> (z)>", 1.0f, 0.9f);
        tester.mustBelieve(cycles, "<(x) <=> (y)>", 0.10f, 0.45f);
    }
    @Test
    void comparisonImpl() {

        TestNAR tester = test;
        tester.believe("<x ==> y>", 1f, 0.9f); 
        tester.believe("<x ==> z>", 0.8f, 0.9f); 
        
        tester.mustBelieve(cycles, "<y ==> z>", 0.80f, 0.45f); 
        tester.mustBelieve(cycles, "<z ==> y>", 0.80f, 0.45f); 
    }

    @Test
    void comparisonOppositeImpl() {

        TestNAR t = test;
        t.believe("<x ==> z>", 0.1f, 0.9f);
        t.believe("<y ==> z>", 1.0f, 0.9f);
        
        t.mustBelieve(cycles, "<x ==> y>", 0.10f, 0.45f);
        t.mustBelieve(cycles, "<y ==> x>", 0.10f, 0.45f);
    }


    @Test
    void resemblance() {

        TestNAR tester = test;

        
        tester.believe("<<robin --> animal> ==> <robin --> bird>>"); 
        tester.believe("<<robin --> bird> ==> <robin --> animal>>"); 

        
        tester.believe("<<robin --> bird> ==> <robin --> [flying]>>", 0.9f, 0.9f); 
        tester.believe("<<robin --> [flying]> ==> <robin --> bird>>", 0.9f, 0.9f); 

        
        tester.mustBelieve(cycles, " <<robin --> animal> ==> <robin --> [flying]>>", 0.90f, 0.73f /*0.81f*/); 
        tester.mustBelieve(cycles, " <<robin --> [flying]> ==> <robin --> animal>>", 0.90f, 0.73f /*0.81f*/); 
    }


    @Test
    void testNegNegEquivPred() {

        test
                .input("(--,(y)).")
                .input("((--,(x)) <=> (--,(y))).")
                .mustBelieve(cycles, "(x)", 0.0f, 0.81f)
                .mustNotOutput(cycles, "(x)", BELIEF, 0.5f, 1f, 0, 1, ETERNAL)
        ;
    }

    @Test
    void testNegNegEquivPredInv() {

        test
                .input("(y).")
                .input("((--,(x)) <=> (--,(y))).")
                .mustBelieve(cycles, "(x)", 1.0f, 0.81f)
                .mustNotOutput(cycles, "(x)", BELIEF, 0f, 0.5f, 0, 1, ETERNAL)
        ;
    }

    @Test
    void analogy() {

        TestNAR tester = test;
        tester.believe("<<robin --> bird> ==> <robin --> animal>>"); 
        tester.believe("<<robin --> bird> <=> <robin --> [flying]>>", 0.80f, 0.9f); 
        tester.mustBelieve(cycles, "<<robin --> [flying]> ==> <robin --> animal>>", 0.80f, 0.65f); 

    }


    @Test
    void analogy2() {

        TestNAR tester = test;
        tester.believe("<robin --> bird>"); 
        tester.believe("<<robin --> bird> <=> <robin --> [flying]>>", 0.80f, 0.9f); 
        tester.mustBelieve(cycles, "<robin --> [flying]>", 0.80f,
                0.65f /*0.81f*/); 

    }

    @Test
    void conversions_between_Implication_and_Equivalence() {

        TestNAR tester = test;
        tester.believe("<<robin --> [flying]> ==> <robin --> bird>>", 0.9f, 0.9f); 
        tester.believe("<<robin --> bird> ==> <robin --> [flying]>>", 0.9f, 0.9f); 
        tester.mustBelieve(cycles, " <<robin --> bird> <=> <robin --> [flying]>>", 0.81f, 0.81f); 

    }
}
