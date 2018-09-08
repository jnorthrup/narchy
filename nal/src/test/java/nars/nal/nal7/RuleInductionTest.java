package nars.nal.nal7;

import com.google.common.math.PairedStatsAccumulator;
import jcog.Texts;
import nars.NAR;
import nars.NARS;
import nars.Task;
import nars.derive.impl.MatrixDeriver;
import nars.derive.premise.PremiseDeriverRuleSet;
import nars.op.stm.STMLinkage;
import nars.term.Term;
import nars.time.Tense;
import nars.truth.Truth;
import org.HdrHistogram.Histogram;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.ETERNAL;
import static org.junit.jupiter.api.Assertions.*;

/**
 * tests the time constraints in which a repeatedly inducted
 * conj/impl belief can or can't "snowball" into significant confidence
 */
class RuleInductionTest {
    @Test
    void test1() {
        int dur = 3;
        int loops = 10;
        int period = 5;
        int dutyPeriod = 1;

        NAR n = NARS.shell();
        n.termVolumeMax.set(
            
            8
        );

        MatrixDeriver d = new MatrixDeriver(new PremiseDeriverRuleSet(n,
            
        "B, A, --is(A,\"==>\"),--is(B,\"==>\")                  |- (polarize(B,task) &&+- polarize(A,belief)), (Belief:IntersectionDepolarized, Time:TaskRelative)"
            

            
            
        ), n) {













        };
        new STMLinkage(n, 1);

        d.conceptsPerIteration.set(2);

        n.time.dur(dur);

        //n.log();

        Term aConjB = $$("(a &&+" + dutyPeriod + " b)");
        Term aConjB_root = aConjB.concept();
        Term aImpB = $$("(a ==>+" + dutyPeriod + " b)");

        PairedStatsAccumulator aConjB_exp = new PairedStatsAccumulator();

        Histogram aConjB_dt = new Histogram(4);

        n.onTask(t -> {
           if (!t.isInput() && t.term().root().equals(aConjB_root)) {
               long start = t.start();
               int dt = Math.abs(t.dt()); 

               aConjB_dt.recordValue(dt);

               assertEquals(start, t.end());
               assertNotEquals(ETERNAL, start);
               assertNotEquals(DTERNAL, dt);
           }
        });

        PairedStatsAccumulator aImpB_exp = new PairedStatsAccumulator();
        for (int i = 0; i < loops; i++) {


            n.believe("a", Tense.Present, 1, 0.9f);
            if (i > 0) {
                
                
            }
            n.run(dutyPeriod);
            n.believe("b", Tense.Present, 1, 0.9f);
            n.run(period-dutyPeriod); 

            long now = n.time();

            System.out.println("\n" + now);
            aConjB_exp.add(now, observe(n, aConjB, now));
            
        }

        

        {
            System.out.println("<" + aConjB + " @ " + n.time() + ">");
            System.out.println("expectation vs. time: \t" + aConjB_exp.yStats());
            System.out.println("\tslope=" + aConjB_exp.leastSquaresFit().slope());




            System.out.println("dt:");
            Texts.histogramPrint(aConjB_dt, System.out);

            System.out.println("</>\n");
        }

        double aConjB_pearsonCorrelationCoeff = aConjB_exp.pearsonsCorrelationCoefficient();
        assertTrue(aConjB_pearsonCorrelationCoeff > 0.4f,
                ()->aConjB + " confidence increases smoothly: correlation quality=" + aConjB_pearsonCorrelationCoeff); 
        assertTrue(aConjB_exp.leastSquaresFit().slope() > 0, ()->aConjB + " confidence increases");

        
        
        

    }


    private static float observe(NAR n, Term x, long now) {
        Task nb = n.belief(x, now);
        Truth xTruth = nb!=null ? nb.truth(now, n.dur()) : null;

        System.out.println(x + "\t" + xTruth);
        n.conceptualize(x).beliefs().print();
        System.out.println();

        




        return xTruth!=null ? xTruth.expectation() : 0;
    }
}

















