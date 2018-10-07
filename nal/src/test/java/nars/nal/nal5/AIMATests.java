package nars.nal.nal5;

import com.google.common.math.PairedStatsAccumulator;
import jcog.data.list.FasterList;
import jcog.io.SparkLine;
import nars.*;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static nars.time.Tense.ETERNAL;
import static nars.truth.TruthFunctions.c2wSafe;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AIMATests {

    final NAR n = NARS.tmp(6);

    @ParameterizedTest
    @ValueSource(doubles = {0.01, 0.05, 0.1, 0.25, 0.5})
    void testAIMAExample(double truthRes) throws Narsese.NarseseException {

        n.termVolumeMax.set(10);
        n.freqResolution.set((float) truthRes);
        n.confMin.set(0.05f);
        n.confResolution.set(0.05f);

        n.believe(
                "(P ==> Q)",
                "((L && M) ==> P)",
                "((B && L) ==> M)",
                "((A && P) ==> L)",
                "((A && B) ==> L)",
                "A",
                "B");

        assertBelief(n, true, "Q", 9500);

    }

    @Test
    void testWeaponsDomain() throws Narsese.NarseseException {

        n.freqResolution.set(0.05f);
        n.confResolution.set(0.05f);
//        n.confMin.set(0.1f);

        //n.activateConceptRate.set(0.5f);
//        n.beliefPriDefault.set(0.3f);
//        n.beliefPriDefault.set(0.25f);
        n.questionPriDefault.set(0.5f);

        n.termVolumeMax.set(20);
       // n.log();


        n.believe(

                "((&&,Weapon(#y),Sells($x,#y,#z),Hostile(#z)) ==> Criminal($x))",
                "Owns(Nono, M1)",
                "Missile(M1)",
                "((Missile($x) && Owns(Nono,$x)) ==> Sells(West,$x,Nono))",
                "(Missile($x) ==> Weapon($x))",
                "(Enemy($x,America) ==> Hostile($x))",
                "American(West)",
                "Enemy(Nono,America)"
        );


        @Nullable Task Q = n.question($.$(
                "Criminal:?x"
                //"Criminal(?x)"

        ), ETERNAL, (q, a) -> {
            System.out.println(a);
        });

        //n.run(1);
        //n.concept("((&&,Weapon(#y),Sells($x,#y,#z),Hostile(#z)) ==> Criminal($x))").print();
        //n.concept("Criminal").print();

        n.run(12000);
//        n.synch();

        n.concept(Q).print();

        Task y = n.belief($.$("Criminal(West)"));
        assertNotNull(y);

    }

    private static void assertBelief(NAR n, boolean expcted, String x, int time) {

        final int metricPeriod = time / 4;

        PairedStatsAccumulator timeVsConf = new PairedStatsAccumulator();


        List<Float> evis = new FasterList();
        for (int i = 0; i < time; i += metricPeriod) {
            n.run(metricPeriod);

            float symConf = 0;

            Task y = n.belief($.the(x), i);
            if (y == null)
                continue;

            symConf = y.conf();
            assertTrue(y.isPositive() == expcted && y.polarity() > 0.5f);

            evis.add(c2wSafe(symConf, 1));
            timeVsConf.add(i, symConf);
        }


        assertTrue(!evis.isEmpty());


        for (char c : "ABLMPQ".toCharArray()) {
            Term t = $.the(String.valueOf(c));
            Task cc = n.belief(t);
            System.out.println(cc);
        }
        System.out.println(timeVsConf.yStats());
        System.out.println(
                SparkLine.renderFloats(evis, 8)
        );
    }


}
