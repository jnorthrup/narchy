package nars.term;

import nars.$;
import nars.Op;
import nars.truth.Truth;
import nars.truth.func.NALTruth;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.Op.*;
import static nars.term.TermReductionsTest.assertReduction;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Bool and Tautology tests */
class BoolTest {


    @Test
    void testNegationTautologies() {
        assertEquals(True, True.unneg());
        assertEquals(False, True.neg());
        assertEquals(True, False.unneg());
        assertEquals(True, False.neg());
        assertEquals(Null, Null.neg());
        assertEquals(Null, Null.unneg());
    }

    @Test
    void testStatementTautologies()  {
        for (Op o : new Op[]{INH, SIM, IMPL}) {
            assertEquals(True, o.the(True, True));
            assertEquals(True, o.the(False, False));
            assertEquals(Null, o.the(Null, Null));
        }

        assertEquals("(x-->†)", INH.the(x, True).toString()); 
        assertEquals(Null, INH.the(True, x));
        assertEquals("((--,x)-->†)", INH.the(x.neg(), True).toString());
    }





























    @Test
    void testImplicationTautologies() {
        assertEquals("x", IMPL.the(True, x).toString());
        assertEquals(Null, IMPL.the(False, x));
        assertEquals(Null, IMPL.the(Null, x));
        assertEquals(Null, IMPL.the(x, True));
        assertEquals(Null, IMPL.the(x, False));


        assertEquals(Null, IMPL.the(x, Null));
    }

    @Test
    void testConjTautologies() {
        assertEquals("x", CONJ.the(True, x).toString());
        assertEquals(False, CONJ.the(False, x));
        assertEquals(False, CONJ.the(False, True));
        assertEquals(True, CONJ.the(True, True));
        assertEquals(False, CONJ.the(False, False));
        assertEquals(Null, CONJ.the(Null, x));
        assertEquals(Null, CONJ.the(Null, Null));
    }


    @Test
    void testDiffTautologies() {

        @Nullable Truth selfDiff = NALTruth.Difference.apply($.t(1, 0.9f), $.t(1f, 0.9f), null, 0);
        assertEquals($.t(0, 0.81f), selfDiff);

        @Nullable Truth negDiff = NALTruth.Difference.apply($.t(0, 0.9f), $.t(1f, 0.9f), null, 0);
        assertEquals($.t(0, 0.81f), negDiff);

        @Nullable Truth posDiff = NALTruth.Difference.apply($.t(1, 0.9f), $.t(0f, 0.9f), null, 0);
        assertEquals($.t(1, 0.81f), posDiff);

        


        for (Op o : new Op[] { DIFFe, DIFFi } ) {

            String diff = o.str;

            
            assertReduction(False, "(x" + diff + "x)");
            assertReduction(
                    
                    True,
                    "(x" + diff + "(--,x))");  
            assertReduction(
                    
                    False,
                    "((--,x)" + diff + "x)");  

            
            assertReduction(Null, "((x" + diff + "x)-->y)");
            assertReduction(Null, "(--(x" + diff + "x)-->y)");



            
            assertReduction("(y-->Ⅎ)",  "(y --> (x" + diff + "x))");
            assertReduction("(y-->†)",  "(y --> --(x" + diff + "x))");





            assertEquals(False, o.the(x,x));
            assertEquals(True, o.the(x,x.neg()));
            assertEquals(False, o.the(x.neg(),x));

            assertEquals(Null, o.the(x,False));
            assertEquals(Null, o.the(x,True));


            assertEquals(False, o.the(True,True));
            assertEquals(False, o.the(False,False));
            assertEquals(Null, o.the(Null,Null));

            assertEquals(True, o.the(True,False));
            assertEquals(False, o.the(False,True));


        }
    }

    @Test
    void testDiffOfIntersectionsWithCommonSubterms() {
        
        
        
        
        
        

        assertReduction("(c-->((a-b)&x))", $$("(c --> ((a & x)-(b & x)))"));
        assertReduction("(((a~b)|x)-->c)", $$("(((a | x)~(b | x)) --> c)"));

        
        assertEquals(Null, $$("((&,x,a)-(&,x,a,b))"));
        assertEquals(Null, $$("((&,x,a,b)-(&,x,a))"));
        assertEquals(Null, $$("((&,x,a)-(&,x,a,b))"));
    }

    @Test
    void testDiffOfUnionsWithCommonSubterms() {

        
            
            

        assertReduction("(c-->((a-b)|(--,x)))", $$("(c --> ((a | x)-(b | x)))"));
        assertReduction("(((a~b)&(--,x))-->c)", $$("(((a & x)~(b & x)) --> c)"));
    }


    @Disabled
    @Test
    void testIntersectionOfDiffsWithCommonSubterms() {

        

        
        
        
        
        

        
        
        
    }

    @Test
    void testIntersectionTautologies() {
        for (Op o : new Op[] { SECTe, SECTi } ) {

            String sect = o.str;

            
            assertEquals(x, o.the(x, x));
            assertReduction("((--,x)" + sect + "x)", o.the(x, x.neg())); 

            assertEquals(x, o.the(x, True));
            assertEquals(Null /* False ?  */, o.the(x, False));
            assertEquals(Null, o.the(x, Null));
        }
    }

    @Test
    void testSetTautologies() {
        
    }

    private static final Term x = $$("x");
    static final Term y = $$("y");

}
