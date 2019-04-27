package nars.nal.multistep;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class PtrManTests {
    
    @Disabled
    @Test
    void testOps1() throws Narsese.NarseseException {

        NAR n = NARS.tmp();
        n.input("opProximity2(OPmov(0,5),OPsub(0,5)).",
                "opProximity2(OPmov(1,5),OPsub(1,4)).",
                "opProximity2(OPmov(1,5),OPsub(1,7)).",

                "( opProximity3((($op1, $op2)-->$1), (($op3, $op4)-->$2), ((#op5, #op6)-->#3) ) =|> opProximity2( (($op1, $op2)-->$1), (($op3, $op4)-->$2) ).",
                "( opProximity3(#1, #op1, #op2, $2, $op3, $op4, $3, $op5, $op6) =|> opProximity2($2, $op3, $op4, $3, $op5, $op6) ).",

                "opProximity3(OPmov, {0}, {5}, OPsub, {0}, {5}, OPsub, {1}, {7}).",
                "opProximity3(OPsub, {1}, {7}, OPmul, {0}, {5}, OPmul, {0}, {5}).",
                "opProximity3(OPsub, {1}, {7}, OPdiv, {0}, {7}, OPmul, {0}, {5}).",

                "(OPcmp <-> OPadd). %0.55;0.9%",
                "(OPsub <-> OPadd). %0.7;0.9%",
                "(OPadd <-> OPmuladd). %0.6;0.9%",
                "(OPmov <-> OPxchg). %0.8;0.9%",

                "$1.0 ((OPxchg, {0}, {5}, OPsub, {0}, {5}) <-> (OPmov, {0}, {5}, OPadd, {0}, {5}))?",
                "$1.0 opProximity2(OPadd, {0}, {5}, OPsub, {1}, {7})?");

        
        n.run(1000);
    } 
        
}
