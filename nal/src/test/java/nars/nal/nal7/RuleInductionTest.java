package nars.nal.nal7;

import com.google.common.math.PairedStatsAccumulator;
import jcog.Texts;
import nars.NAR;
import nars.NARS;
import nars.Task;
import nars.derive.Derivation;
import nars.derive.deriver.MatrixDeriver;
import nars.derive.premise.PremiseDeriverRuleSet;
import nars.op.stm.STMLinkage;
import nars.term.Term;
import nars.truth.Truth;
import nars.util.time.Tense;
import org.HdrHistogram.Histogram;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.util.time.Tense.DTERNAL;
import static nars.util.time.Tense.ETERNAL;
import static org.junit.jupiter.api.Assertions.*;

/**
 * tests the time constraints in which a repeatedly inducted
 * conj/impl belief can or can't "snowball" into significant confidence
 */
public class RuleInductionTest {
    @Test
    public void test1() {
        int dur = 1;
        int loops = 10;
        int period = 5;
        int dutyPeriod = 1;

        NAR n = NARS.shell();
        n.termVolumeMax.set(
            //3
            8
        );

        MatrixDeriver d = new MatrixDeriver(new PremiseDeriverRuleSet(n,
            //CONJ induction
        "B, A, notImpl(A),notImpl(B)                  |- (polarize(B,task) &&+- polarize(A,belief)), (Belief:IntersectionDepolarized, Time:TaskRelative)"
            //CONJ decompose

            ////IMPL induction
            //,"B, A, notImpl(A),notImpl(B) |- (A ==>+- B), (Belief:InductionUnprojected)"
        ), n) {

            @Override
            protected boolean derivable(Derivation d) {
                if (super.derivable(d)) {
                    //..
                    System.out.println(
                        "task: " + d._task + " @ " + d.taskAt + "\t" +
                        "belief: " + d._belief + " @ " + d.beliefAt);

                    return true;
                }
                return false;
            }
        };
        new STMLinkage(n, 1, false);

        d.conceptsPerIteration.set(2);

        n.time.dur(dur);

        n.log();

        Term aConjB = $$("(a &&+" + dutyPeriod + " b)");
        Term aConjB_root = aConjB.concept();
        Term aImpB = $$("(a ==>+" + dutyPeriod + " b)");

        PairedStatsAccumulator aConjB_exp = new PairedStatsAccumulator();
        Histogram aConjB_start = new Histogram(4);
        Histogram aConjB_dt = new Histogram(4);

        n.onTask(t -> {
           if (!t.isInput() && t.term().root().equals(aConjB_root)) {
               long start = t.start();
               int dt = Math.abs(t.dt()); //histogram wont record negatives
               aConjB_start.recordValue(start);
               aConjB_dt.recordValue(dt);

               assertEquals(start, t.end());
               assertNotEquals(ETERNAL, start);
               assertNotEquals(DTERNAL, dt);
           }
        });

        PairedStatsAccumulator aImpB_exp = new PairedStatsAccumulator();
        for (int i = 0; i < loops; i++) {
//            n.clear(); //distraction clear

            n.believe("a", Tense.Present, 1, 0.9f);
            if (i > 0) {
                //TODO test that the newest tasklink inserted into concept 'a' is the, or nearly the strongest
                //n.concept("a").tasklinks().print();
            }
            n.run(dutyPeriod);
            n.believe("b", Tense.Present, 1, 0.9f);
            n.run(period-dutyPeriod); //delay

            long now = n.time();

            System.out.println("\n" + now);
            aConjB_exp.add(now, observe(n, aConjB, now));
            //aImpB_exp.add(now, observe(n, aImpB, now));
        }

        //print(aImpB_exp);

        {
            System.out.println("<" + aConjB + " @ " + n.time() + ">");
            System.out.println("expectation vs. time: \t" + aConjB_exp.yStats());
            System.out.println("\tslope=" + aConjB_exp.leastSquaresFit().slope());

            System.out.println("start time:");
            Texts.histogramPrint(aConjB_start, System.out);

            System.out.println("dt:");
            Texts.histogramPrint(aConjB_dt, System.out);

            System.out.println("</>\n");
        }

        double aConjB_pearsonCorrelationCoeff = aConjB_exp.pearsonsCorrelationCoefficient();
        assertTrue(aConjB_pearsonCorrelationCoeff > 0.4f,
                ()->aConjB + " confidence increases smoothly: correlation quality=" + aConjB_pearsonCorrelationCoeff); //http://mathworld.wolfram.com/CorrelationCoefficient.html
        assertTrue(aConjB_exp.leastSquaresFit().slope() > 0, ()->aConjB + " confidence increases");

        //TODO measure the distribution of occurrence times and intervals of all inducted CONJ beliefs
        //they should be uniformly distributed; any anisotropic bias indicates a problem
        //somewhere between premise formation or temporalization

    }


    static float observe(NAR n, Term x, long now) {
        Task nb = n.belief(x, now);
        Truth xTruth = nb!=null ? nb.truth(now, n.dur()) : null;

        System.out.println(x + "\t" + xTruth);
        n.conceptualize(x).beliefs().print();
        System.out.println();

        //        if (!(exp >= lastAConjB_exp)) {
//            //for debug
//            Task tt = n.belief(x, now);
//
//        }
        return xTruth!=null ? xTruth.expectation() : 0;
    }
}
//    @Test
//    public void testImagePatternMatching()  {
//
//        Deriver.derivers(test.nar).forEach(d->d.conceptsPerIteration.set(200));
//        test
//                //.log()
//                .inputAt(0, "x(0,0,0,0). :|:")
//                .inputAt(2, "x(0,1,0,1). :|:")
//                .inputAt(4, "x(0,0,0,0). :|:")
//                .inputAt(6, "x(0,1,0,1). :|:")
//                .inputAt(8, "$1.0 (?1-->x)? :|:")
//                .inputAt(10, "$1.0 (?1-->x)? :|:")
//                .mustBelieve(8, "x(0,0,0,0)", 1f, 0.5f)
//                .mustBelieve(10, "x(0,1,0,1)", 1f, 0.5f)
//        ;
//
//    }
