package nars.nal.nal5;

import nars.NAR;
import nars.NARS;
import nars.term.Term;
import nars.test.NALTest;
import nars.test.TestNAR;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.Op.BELIEF;
import static nars.Op.QUESTION;
import static nars.time.Tense.ETERNAL;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class NAL5Test extends NALTest {

    private final int cycles = 100;

    @Override
    protected NAR nar() {
        NAR n = NARS.tmp(6);
        n.termVolumeMax.set(17);
        return n;
    }

    @BeforeEach void setup() {
        test.confTolerance(0.2f);
    } 

    @Test
    void revision() {

        test
                .mustBelieve(cycles, "<<robin --> [flying]> ==> <robin --> bird>>", 0.85f, 0.92f) 
                .believe("<<robin --> [flying]> ==> <robin --> bird>>") 
                .believe("<<robin --> [flying]> ==> <robin --> bird>>", 0.00f, 0.60f); 

    }

    @Test
    void deduction() {

        TestNAR tester = test;
        tester.believe("<<robin --> bird> ==> <robin --> animal>>"); 
        tester.believe("<<robin --> [flying]> ==> <robin --> bird>>"); 
        tester.mustBelieve(cycles, "<<robin --> [flying]> ==> <robin --> animal>>", 1.00f, 0.81f); 

    }
    @Test
    void deductionPosCommon() {

        test
                .believe("(x ==> z)")
                .believe("(z ==> y)")
                .mustBelieve(cycles, "(x ==> y)", 1.00f, 0.81f);

    }

    @Test
    void deductionNegCommon() {

        test
            .believe("(x ==> --z)")
            .believe("(--z ==> y)")
            .mustBelieve(cycles, "(x ==> y)", 1.00f, 0.81f);

    }

    @Test
    void exemplification() {

        TestNAR tester = test;
        tester.believe("<<robin --> [flying]> ==> <robin --> bird>>"); 
        tester.believe("<<robin --> bird> ==> <robin --> animal>>"); 
        tester.mustBelieve(cycles, "<<robin --> animal> ==> <robin --> [flying]>>.", 1.00f, 0.45f); 

    }




    @Test
    void induction() {
        /*
         <<robin --> bird> ==> <robin --> animal>>.
         <<robin --> [flying]> ==> <robin --> animal>>. %0.8%
         OUT: <<robin --> bird> ==> <robin --> [flying]>>. %1.00;0.39%
         OUT: <<robin --> [flying]> ==> <robin --> bird>>. %0.80;0.45%
         */
        TestNAR tester = test;
        tester.believe("<<robin --> bird> ==> <robin --> animal>>");
        tester.believe("<<robin --> [flying]> ==> <robin --> animal>>", 0.8f, 0.9f);
        tester.mustBelieve(cycles, "<<robin --> bird> ==> <robin --> [flying]>>", 1.00f, 0.39f);
        tester.mustBelieve(cycles, "<<robin --> [flying]> ==> <robin --> bird>>", 0.80f, 0.45f);

    }








    @Test
    void abduction() {

        /*
        <<robin --> bird> ==> <robin --> animal>>.         
        <<robin --> bird> ==> <robin --> [flying]>>. %0.80%  
        14
         OUT: <<robin --> [flying]> ==> <robin --> animal>>. %1.00;0.39% 
         OUT: <<robin --> animal> ==> <robin --> [flying]>>. %0.80;0.45% 
         */
        TestNAR tester = test;
        tester.believe("<<robin --> bird> ==> <robin --> animal>>"); 
        tester.believe("<<robin --> bird> ==> <robin --> [flying]>>", 0.8f, 0.9f); 
        tester.mustBelieve(cycles, "<<robin --> [flying]> ==> <robin --> animal>>", 1.00f, 0.39f); 
        tester.mustBelieve(cycles, "<<robin --> animal> ==> <robin --> [flying]>>", 0.80f, 0.45f); 


    }


    @Test
    void testImplBeliefPosPos() {
        

        test
                
                .believe("b")
                .believe("(b==>c)", 1, 0.9f)
                .mustBelieve(cycles, "c", 1.00f, 0.81f);
    }








    @Test
    void testImplBeliefPosNeg() {
        

        test
                .believe("b")
                .believe("(b ==> --c)", 1, 0.9f)
                .mustBelieve(cycles, "c", 0.00f, 0.81f);
    }

    @Test
    void detachment() {

        test
                .believe("<<robin --> bird> ==> <robin --> animal>>") 
                .believe("<robin --> bird>") 
                .mustBelieve(cycles, "<robin --> animal>", 1.00f, 0.81f); 

    }


    @Test
    void detachment2() {

        TestNAR tester = test;
        tester.believe("<<robin --> bird> ==> <robin --> animal>>", 0.70f, 0.90f); 
        tester.believe("<robin --> animal>"); 
        tester.mustBelieve(cycles, "<robin --> bird>",
                0.7f, 0.45f); 
        
        

    }


    








    @Test
    void anonymous_analogy1_depvar() {
        test
                .believe("(a:#1 && y)")
                .believe("a:x", 0.80f, 0.9f)
                .mustBelieve(cycles, "y", 0.80f, 0.42f);
    }

    @Test
    void anonymous_analogy1_pos2() {
        test
                
                .believe("(x && y)")
                .believe("x", 0.80f, 0.9f)
                .mustBelieve(cycles*10, "y",
                        0.80f, 0.58f);
                        
    }

    @Test
    void anonymous_analogy1_pos3() {
        test
                .believe("(&&, x, y, z)")
                .believe("x", 0.80f, 0.9f)
                .mustBelieve(cycles, "(y && z)", 0.80f,
                        0.58f);
                        
    }

    @Test
    void anonymous_analogy1_neg2() {
        test
                .believe("(&&, --x, y, z)")
                .believe("x", 0.20f, 0.9f)
                .mustBelieve(cycles, "(&&,y,z)", 0.80f,
                        
                        0.43f /*0.43f*/);
    }

    @Test
    void compound_composition_Pred() {

        TestNAR tester = test;
        tester.believe("<<robin --> bird> ==> <robin --> animal>>"); 
        tester.believe("<<robin --> bird> ==> <robin --> [flying]>>", 0.9f, 0.9f); 
        tester.mustBelieve(cycles, " <<robin --> bird> ==> (&&,<robin --> [flying]>,<robin --> animal>)>",
                0.90f, 0.81f);

    }
    @Test
    void compound_composition_PredPosNeg() {

        TestNAR tester = test;
        tester.believe("(a ==> b)");
        tester.believe("--(a==>c)");
        tester.mustBelieve(cycles, "(a ==> (b && --c))",
                1f, 0.81f);

    }


    @Test
    void compound_composition_Subj() {
        TestNAR tester = test;
        tester.believe("(bird:robin ==> animal:robin)"); 
        tester.believe("((robin-->[flying]) ==> animal:robin)", 0.9f, 0.81f);
        tester.mustBelieve(cycles, " ((bird:robin && (robin-->[flying])) ==> animal:robin)", 0.9f, 0.73f);
        tester.mustBelieve(cycles," ((bird:robin || (robin-->[flying])) ==> animal:robin)",1f,0.73f);
    }
    @Test
    void compound_composition_SubjNeg() {
        TestNAR tester = test;
        tester.believe("--(bird:robin ==> animal:nonRobin)"); 
        tester.believe("--((robin-->[flying]) ==> animal:nonRobin)", 0.9f, 0.81f);
        tester.mustBelieve(cycles, " ((bird:robin && (robin-->[flying])) ==> animal:nonRobin)", 0.1f, 0.73f);
        tester.mustBelieve(cycles," ((bird:robin || (robin-->[flying])) ==> animal:nonRobin)",0f,0.73f);
    }









    @Test
    void compound_decomposition_two_premises1() {

        TestNAR tester = test;
        tester.believe("--(bird:robin ==> (animal:robin && (robin-->[flying])))", 1.0f, 0.9f);
        tester.believe("(bird:robin ==> (robin-->[flying]))");
        tester.mustBelieve(cycles, "--(bird:robin ==> animal:robin)", 1.00f, 0.81f); 

    }

    @Test
    void compound_decomposition_subj_posneg() {
        test.believe("((b && --c)==>a)", 1.0f, 0.9f)
                .mustBelieve(cycles, "(b==>a)", 1.00f, 0.81f)
                .mustBelieve(cycles, "(--c==>a)", 1.00f, 0.81f);
    }

    @Test
    void compound_decomposition_pred_posneg() {

        test.believe("(a==>(b && --c))", 1.0f, 0.9f)
                .mustBelieve(cycles, "(a==>b)", 1.00f, 0.81f)
                .mustBelieve(cycles, "(a==>c)", 0.00f, 0.81f);
    }

    @Test
    void compound_decomposition_one_premise_pos() {

        TestNAR tester = test;

        tester.believe("(<robin --> [flying]> && <robin --> swimmer>)", 1.0f, 0.9f); 
        tester.mustBelieve(cycles, "<robin --> swimmer>", 1.00f, 0.81f); 
    }

    @Test
    void testConjStructuralDeduction() {
        test
                .believe("(&&, a, b)")
                .mustBelieve(cycles, "a", 1f, 0.81f)
                .mustBelieve(cycles, "b", 1f, 0.81f)
        ;
    }

    @Test @Disabled
    void testDisjStructuralDeduction() {
        test
                .believe("(||, a, b)")
                .mustBelieve(cycles, "a", 1f, 0.40f)
                .mustBelieve(cycles, "b", 1f, 0.40f)
        ;
    }

    @Test
    void testDisjStructuralDeductionQuestion() {
        test
                .input("a?")
                .believe("(||, a, b)")
                .mustOutput(cycles,"b", QUESTION)
        ;
    }

    @Test @Disabled
    void compound_decomposition_one_premise_neg() {
        

        TestNAR tester = test;
        tester.believe("(&&,<robin --> [flying]>,<robin --> swimmer>)", 0.0f, 0.9f); 
        tester.mustNotOutput(cycles, "<robin --> swimmer>", BELIEF, ETERNAL); 
    }


    @Test
    void compound_decomposition_two_premises3() {

        TestNAR tester = test;
        tester.believe("(||,<robin --> [flying]>,<robin --> swimmer>)"); 
        tester.believe("<robin --> swimmer>", 0.0f, 0.9f); 
        tester.mustBelieve(cycles, "<robin --> [flying]>", 1.00f, 0.81f); 

    }


    /**
     * not sure this one makes logical sense
     */
    @Disabled
    @Test
    void compound_composition_one_premises() throws nars.Narsese.NarseseException {

        TestNAR tester = test;
        tester.believe("<robin --> [flying]>"); 
        tester.ask("(||,<robin --> [flying]>,<robin --> swimmer>)"); 
        
        tester.mustBelieve(cycles, " (&&,(--,<robin --> swimmer>),(--,<robin --> [flying]>))", 0.00f, 0.81f); 
    }


    


    @Test
    void compound_decomposition_one_premises() {

        test

                .believe("(&&,<robin --> swimmer>,<robin --> [flying]>)", 0.9f, 0.9f) 
                .mustBelieve(cycles, "<robin --> swimmer>", 0.9f, 0.73f) 
                .mustBelieve(cycles, "<robin --> [flying]>", 0.9f, 0.73f); 

    }










    @Test
    void negation0() {

        test
                .mustBelieve(cycles, "<robin --> [flying]>", 0.10f, 0.90f) 
                .believe("(--,<robin --> [flying]>)", 0.9f, 0.9f); 


    }

    @Test
    void negation1() {

        test
                .mustBelieve(cycles, "<robin <-> parakeet>", 0.10f, 0.90f)
                .believe("(--,<robin <-> parakeet>)", 0.9f, 0.9f);


    }













    @Test
    void contraposition() {

        TestNAR tester = test;

        tester.believe("(--(robin --> bird) ==> (robin --> [flying]))", 0.1f, 0.9f); 

        
        

        tester.mustBelieve(cycles, " (--(robin --> [flying]) ==> (robin --> bird))",
                0f, 0.45f); 


    }

    @Test
    void contrapositionPos() {
        
        test
        .believe("(--B ==> A)", 0.9f, 0.9f)
        .mustBelieve(cycles, " (--A ==> B)", 0f, 0.08f);
    }

    @Test
    void contrapositionNeg() {
        test
        .believe("(--B ==> A)", 0.1f, 0.9f)
        .mustBelieve(cycles, " (--A ==> B)", 0f, 0.45f);
    }

    @Test
    void conditional_deduction() {

        TestNAR tester = test;
        tester.believe("<(&&,<robin --> [flying]>,<robin --> [withWings]>) ==> <robin --> bird>>"); 
        tester.believe("<robin --> [flying]>"); 
        tester.mustBelieve(cycles, " <<robin --> [withWings]> ==> <robin --> bird>>", 1.00f, 0.81f); 

    }

    @Test
    void conditional_deduction_neg() {

        TestNAR tester = test;
        tester.believe("<(&&,--<robin --> [swimming]>,<robin --> [withWings]>) ==> <robin --> bird>>"); 
        tester.believe("--<robin --> [swimming]>"); 
        tester.mustBelieve(cycles, " <<robin --> [withWings]> ==> <robin --> bird>>", 1.00f, 0.81f); 

    }


    @Test
    void conditional_deduction2() {


        
        test
        .believe("<(&&,<robin --> [chirping]>,<robin --> [flying]>,<robin --> [withWings]>) ==> <robin --> bird>>") 
        .believe("<robin --> [flying]>") 
        .mustBelieve(cycles, " <(&&,<robin --> [chirping]>,<robin --> [withWings]>) ==> <robin --> bird>>", 1.00f, 0.81f); 

    }


    @Test
    void conditional_deduction3() {

        TestNAR tester = test;
        tester.believe("<(&&,<robin --> bird>,<robin --> [living]>) ==> <robin --> animal>>"); 
        tester.believe("<<robin --> [flying]> ==> <robin --> bird>>"); 
        tester.mustBelieve(cycles, " <(&&,<robin --> [flying]>,<robin --> [living]>) ==> <robin --> animal>>", 1.00f, 0.81f); 

    }


    @Test
    void conditional_abduction_viaMultiConditionalSyllogism() {
        

        TestNAR tester = test;
        
        tester.believe("((robin-->[flying]) ==> bird:robin)");
        tester.believe("((swimmer:robin && (robin-->[flying])) ==> bird:robin)");
        tester.mustBelieve(cycles * 4, "swimmer:robin", 1.00f, 0.45f /*0.4f*/); 

    }

    @Test
    void conditional_abduction_viaMultiConditionalSyllogismEasier() {
        

        TestNAR tester = test;
        
        tester.believe("(flying:robin ==> bird:robin)"); 
        tester.believe("((swimmer:robin && flying:robin) ==> bird:robin)"); 
        tester.mustBelieve(cycles, "swimmer:robin", 1.00f, 0.45f /*0.4f*/); 

    }

    @Test
    void conditional_abduction2_viaMultiConditionalSyllogism() {
        

        test
                
                .believe("<(&&,<robin --> [withWings]>,<robin --> [chirping]>) ==> <robin --> bird>>") 
                .believe("<(&&,<robin --> [flying]>,<robin --> [withWings]>,<robin --> [chirping]>) ==> <robin --> bird>>") 
                .mustBelieve(cycles, "<robin --> [flying]>",
                        1.00f, 0.45f
                ) 
                .mustNotOutput(cycles, "<robin --> [flying]>", BELIEF, 0f, 0.5f, 0, 1, ETERNAL);
    }


    







    @Test
    void conditional_abduction3_semigeneric2() {

        TestNAR tester = test;
        tester.believe("<(&&,<ro --> [f]>,<ro --> [w]>) ==> <ro --> [l]>>", 0.9f, 0.9f);
        tester.believe("<(&&,<ro --> [f]>,<ro --> b>) ==> <ro --> [l]>>");
        tester.mustBelieve(cycles, "<<ro --> b> ==> <ro --> [w]>>", 1.00f, 0.42f);
        tester.mustBelieve(cycles, "<<ro --> [w]> ==> <ro --> b>>", 0.90f, 0.45f);
    }


    @Test
    void conditional_abduction3_semigeneric3() {

        TestNAR tester = test;
        tester.believe("<(&&,<R --> [f]>,<R --> [w]>) ==> <R --> [l]>>", 0.9f, 0.9f);
        tester.believe("<(&&,<R --> [f]>,<R --> b>) ==> <R --> [l]>>");
        tester.mustBelieve(cycles * 4, "<<R --> b> ==> <R --> [w]>>", 1f, 0.42f /*0.36f*/);
        tester.mustBelieve(cycles * 4, "<<R --> [w]> ==> <R --> b>>", 0.90f, 0.45f);
    }

    @Test
    void conditional_abduction3() {

        TestNAR tester = test;
        tester.believe("<(&&,<robin --> [flying]>,<robin --> [withWings]>) ==> <robin --> [living]>>", 0.9f, 0.9f); 
        tester.believe("<(&&,<robin --> [flying]>,<robin --> bird>) ==> <robin --> [living]>>"); 
        tester.mustBelieve(cycles, "<<robin --> bird> ==> <robin --> [withWings]>>",
                
                1.00f, 0.42f); 
        tester.mustBelieve(cycles, "<<robin --> [withWings]> ==> <robin --> bird>>",
                0.90f, 0.45f); 

    }
    @Test
    void conditional_abduction3_generic_simpler() {

        test
        .believe("((a && b) ==> d)", 0.9f, 0.9f)
        .believe("((a && c) ==> d)", 1f, 0.9f)
        .mustBelieve(cycles*2, "(c ==> b)", 1f, 0.42f)
        .mustBelieve(cycles*2, "(b ==> c)", 0.90f, 0.45f);
    }

    @Test
    void conditional_abduction3_generic() {

        TestNAR tester = test;
        tester.believe("<(&&,<r --> [f]>,<r --> [w]>) ==> <r --> [l]>>", 0.9f, 0.9f);
        tester.believe("<(&&,<r --> [f]>,<r --> b>) ==> <r --> [l]>>");
        tester.mustBelieve(cycles, "<<r --> b> ==> <r --> [w]>>", 1f, 0.42f);
        tester.mustBelieve(cycles, "<<r --> [w]> ==> <r --> b>>", 0.90f, 0.45f);
    }

    @Test
    void conditional_induction() {

        TestNAR tester = test;
        tester.believe("<(&&,<robin --> [chirping]>,<robin --> [flying]>) ==> <robin --> bird>>"); 
        tester.believe("<<robin --> [flying]> ==> <robin --> [withBeak]>>", 0.9f, 0.9f); 
        tester.mustBelieve(cycles, "<(&&,<robin --> [chirping]>,<robin --> [withBeak]>) ==> <robin --> bird>>",
                1.00f, 0.45f); 

    }

    @Test
    void conditional_induction0Simple() {
        TestNAR tester = test;
        tester.believe("((&&,x1,a) ==> c)");
        tester.believe("((&&,y1,a) ==> c)");
        tester.mustBelieve(cycles, "(x1 ==> y1)", 1.00f, 0.45f);
        tester.mustBelieve(cycles, "(y1 ==> x1)", 1.00f, 0.45f);
    }

    @Test
    void conditional_induction0SimpleDepVar() {
        TestNAR tester = test;
        tester.believe("((&&,x1,#1) ==> c)");
        tester.believe("((&&,y1,#1) ==> c)");
        tester.mustBelieve(cycles, "(x1 ==> y1)", 1.00f, 0.45f);
        tester.mustBelieve(cycles, "(y1 ==> x1)", 1.00f, 0.45f);
    }

    @Test
    void conditional_induction0SimpleDepVar2() {
        TestNAR tester = test;
        tester.believe("((&&,x1,#1) ==> (a && #1))");
        tester.believe("((&&,y1,#1) ==> (a && #1))");
        tester.mustBelieve(cycles, "(x1 ==> y1)", 1.00f, 0.45f);
        tester.mustBelieve(cycles, "(y1 ==> x1)", 1.00f, 0.45f);
    }

    @Test
    void conditional_induction0SimpleDepVar3() {
        TestNAR tester = test;
        tester.believe("((&&,x1,#1) ==> (a && #1))");
        tester.believe("((&&,#1,#2) ==> (a && #2))");
        tester.mustBelieve(cycles, "(x1 ==> #1)", 1.00f, 0.45f);
        tester.mustBelieve(cycles, "(#1 ==> x1)", 1.00f, 0.45f);
    }

    @Test
    void conditional_induction0SimpleIndepVar() {
        TestNAR tester = test;
        tester.believe("((&&,x1,$1) ==> (a,$1))");
        tester.believe("((&&,y1,$1) ==> (a,$1))");
        tester.mustBelieve(cycles, "(x1 ==> y1)", 1.00f, 0.45f);
        tester.mustBelieve(cycles, "(y1 ==> x1)", 1.00f, 0.45f);
    }

    @Test
    void conditional_induction0() {
        

        TestNAR tester = test;
        test.nar.termVolumeMax.set(12);
        tester.believe("((&&,x1,x2,a) ==> c)");
        tester.believe("((&&,y1,y2,a) ==> c)");
        tester.mustBelieve(cycles*2, "((x1&&x2) ==> (y1&&y2))", 1.00f, 0.45f);
        tester.mustBelieve(cycles*2, "((y1&&y2) ==> (x1&&x2))", 1.00f, 0.45f);
    }

    @Test
    void conditional_induction0NegBothSimple() {
        TestNAR tester = test;
        tester.believe("--((x&&a) ==> c)");
        tester.believe("--((x&&b) ==> c)");
        tester.mustBelieve(cycles, "(a ==> b)", 1.00f, 0.45f);
        tester.mustBelieve(cycles, "(b ==> a)", 1.00f, 0.45f);
    }

    @Test
    void conditional_induction0NegBoth() {
        Term both = $$("(((x1&&x2) ==> (y1&&y2))&&((y1&&y2) ==> (x1&&x2)))");
        assertEquals("(((x1&&x2)==>(y1&&y2))&&((y1&&y2)==>(x1&&x2)))",
                both.toString());

        TestNAR tester = test;
        tester.believe("--((&&,x1,x2,a) ==> c)");
        tester.believe("--((&&,y1,y2,a) ==> c)");
        tester.mustBelieve(cycles, "((x1&&x2) ==> (y1&&y2))", 1.00f, 0.45f);
        tester.mustBelieve(cycles, "((y1&&y2) ==> (x1&&x2))", 1.00f, 0.45f);
    }

    @Test
    void conditional_induction0NegInner() {
        TestNAR tester = test;
        tester.believe("((x&&a) ==> c)");
        tester.believe("(--(x&&b) ==> c)");
        tester.mustBelieve(cycles*3, "(a ==> --b)", 1.00f, 0.45f);
        tester.mustBelieve(cycles*3, "(--b ==> a)", 1.00f, 0.45f);
    }

    /* will be moved to NAL multistep test file!!
    
    
    @Test public void deriveFromConjunctionComponents() { 
        TestNAR tester = test();
        tester.believe("(&&,<a --> b>,<b-->a>)", Eternal, 1.0f, 0.9f);

        
        tester.mustBelieve(70, "<a --> b>", 1f, 0.81f);
        tester.mustBelieve(70, "<b --> a>", 1f, 0.81f);

        tester.mustBelieve(70, "<a <-> b>", 1.0f, 0.66f);
        tester.run();
    }*/














    @Test
    void testPosPosImplicationConc() {

        
        test
                
                .input("x. %1.0;0.90%")
                .input("(x ==> y).")
                .mustBelieve(cycles, "y", 1.0f, 0.81f)
                .mustNotOutput(cycles, "y", BELIEF, 0f, 0.5f, 0, 1, ETERNAL);

    }









    


    @Test
    void testImplNegPos() {

        test
                .input("--x.")
                .input("(--x ==> y).")
                .mustBelieve(cycles, "y", 1.0f, 0.81f)
                .mustNotOutput(cycles, "((--,#1)==>y)", BELIEF, 0f, 0.5f, 0, 1, ETERNAL) 
                .mustNotOutput(cycles, "y", BELIEF, 0f, 0.5f, 0, 1, ETERNAL)
        ;
    }


    @Test
    void testImplNegNeg() {

        test
                
                .input("--x.")
                .input("(--x ==> --y).")
                .mustBelieve(cycles, "y", 0.0f, 0.81f)
                .mustNotOutput(cycles, "y", BELIEF, 0.5f, 1f, 0.1f, 1, ETERNAL)
        ;
    }













    @Test
    void testAbductionNegPosImplicationPred() {

        test
                
                .input("y. %1.0;0.90%")
                .input("(--x ==> y).")
                .mustBelieve(cycles, "x", 0.0f, 0.45f)
                .mustNotOutput(cycles, "x", BELIEF, 0.5f, 1f, 0, 1, ETERNAL)
        ;
    }


    @Disabled
    @Test
    void testAbductionPosNegImplicationPred() {

        test
                .input("y. %1.0;0.90%")
                .input("--(x ==> y).")
                .mustBelieve(cycles, "x", 0.0f, 0.45f)
                .mustNotOutput(cycles, "x", BELIEF, 0.5f, 1f, 0, 1, ETERNAL)
        ;
    }

    @Disabled
    @Test
    void testAbductionNegNegImplicationPred() {

        /*
        via contraposition:
        $.32 x. %1.0;.30% {11: 1;2} ((%1,%2,time(raw),belief(positive),task("."),time(dtEvents),notImpl(%2)),((%2 ==>+- %1),((Induction-->Belief))))
            $.21 ((--,y)==>x). %0.0;.47% {1: 2;;} ((((--,%1)==>%2),%2),(((--,%2) ==>+- %1),((Contraposition-->Belief))))
              $.50 ((--,x)==>y). %0.0;.90% {0: 2}
            $.50 y. %1.0;.90% {0: 1}
         */
        test
                .input("y. %1.0;0.90%")
                .input("--(--x ==> y).")
                .mustBelieve(cycles, "x", 1.0f, 0.45f)
                .mustNotOutput(cycles, "x", BELIEF, 0.0f, 0.5f, 0, 1, ETERNAL)
        ;
    }

    @Test
    void testDeductionPosNegImplicationPred() {

        test
                .believe("y")
                .believe("(y ==> --x)")
                .mustBelieve(cycles, "x", 0.0f, 0.81f)
                .mustNotOutput(cycles, "x", BELIEF, 0.5f, 1f, 0, 1, ETERNAL)
        ;
    }










    








    @Test
    void testConversion0() {

        test
                .input("(x==>y)?")
                .input("(y==>x).")
                .mustBelieve(cycles, "(x==>y).", 1.0f, 0.47f)
        ;
    }

    @Test
    void testConversion() {

        test
                
                .input("(x==>y)?")
                .input("(y==>x).")
                .mustBelieve(cycles, "(x==>y).", 1.0f, 0.47f)
        ;
    }

    @Test
    void testConversionNeg() {

        test
                .input("(x ==> y)?")
                .input("(--y ==> x).")
                .mustBelieve(cycles, "(x==>y).", 0.0f, 0.47f)
        ;
    }










    @Test
    void testConversionNeg3() {
        test
                .input("(--x ==> y)?")
                .input("(y ==> --x).")
                .mustBelieve(cycles, "(--x ==> y).", 0f, 0.47f)
        ;
    }


}

