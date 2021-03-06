package nars.nal.nal7;

import com.google.common.math.PairedStatsAccumulator;
import nars.NAR;
import nars.NARS;
import nars.Task;
import nars.derive.Deriver;
import nars.derive.rule.PremiseRuleSet;
import nars.op.stm.STMLinker;
import nars.term.Term;
import nars.time.Tense;
import nars.truth.Truth;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static nars.$.*;
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

        NAR n = NARS.shell();
        n.termVolMax.set(8);

        PremiseRuleSet r = new PremiseRuleSet(n,

            "B, A, --is(A,\"==>\"),--is(B,\"==>\"), neq(A,B) |- (polarizeTask(B) &&+- polarizeBelief(A)), (Belief:IntersectionDD, Time:Sequence)"


        ).add(new STMLinker(1));
//            .wrap(a -> {
//            return new PremiseActionProxy(a) {
//
//            }
//        })


        Deriver d = new Deriver(r);

        d.program.print();


        int dur = 2;
        n.time.dur(dur);

        n.log();

        int period = dur * 4;
        int dutyPeriod = period / 2;
        Term aConjB = INSTANCE.$$("(a &&+" + dutyPeriod + " --a)");
        Term aConjB_root = aConjB.concept();
        Term aImpB = INSTANCE.$$("(a ==>+" + dutyPeriod + " --a)");

        PairedStatsAccumulator aConjB_exp = new PairedStatsAccumulator();

//        Histogram aConjB_dt = new Histogram(4);

        n.onTask(new Consumer<Task>() {
            @Override
            public void accept(Task t) {
                if (!t.isInput() && t.term().root().equals(aConjB_root)) {
                    long start = t.start();
                    int dt = Math.abs(t.dt());

//                aConjB_dt.recordValue(dt); //cant accept -dt

                    assertEquals(start, t.end());
                    assertNotEquals(ETERNAL, start);
                    assertNotEquals(DTERNAL, dt);
                }
            }
        });

        PairedStatsAccumulator aImpB_exp = new PairedStatsAccumulator();
        int loops = 10;
        for (int i = 0; i < loops; i++) {


            n.believe("a", Tense.Present, 1, 0.9f);
//            n.believe("b", Tense.Present, 0, 0.9f);
            n.run(dutyPeriod);
//            n.believe("b", Tense.Present, 1, 0.9f);
            n.believe("a", Tense.Present, 0, 0.9f);
            n.run(period - dutyPeriod);

            long now = n.time();

            System.out.println("\n" + now);
            aConjB_exp.add(now, observe(n, aConjB, now));

        }


        {
            System.out.println("<" + aConjB + " @ " + n.time() + '>');
            System.out.println("expectation vs. time: \t" + aConjB_exp.yStats());
            System.out.println("\tslope=" + aConjB_exp.leastSquaresFit().slope());


//            System.out.println("dt:");
//            Texts.histogramPrint(aConjB_dt, System.out);

            System.out.println("</>\n");
        }

        double aConjB_pearsonCorrelationCoeff = aConjB_exp.pearsonsCorrelationCoefficient();
        assertTrue(aConjB_pearsonCorrelationCoeff > 0.4f,
                new Supplier<String>() {
                    @Override
                    public String get() {
                        return aConjB + " confidence increases smoothly: correlation quality=" + aConjB_pearsonCorrelationCoeff;
                    }
                });
        assertTrue(aConjB_exp.leastSquaresFit().slope() > 0, new Supplier<String>() {
            @Override
            public String get() {
                return aConjB + " confidence increases";
            }
        });


    }


    private static float observe(NAR n, Term x, long now) {
        Task nb = n.belief(x, now);
        Truth xTruth = nb != null ? nb.truth(now, now, n.dur()) : null;

        System.out.println(x + "\t" + xTruth);
        n.conceptualize(x).beliefs().print();
        System.out.println();


        return xTruth != null ? xTruth.expectation() : 0;
    }
}

















