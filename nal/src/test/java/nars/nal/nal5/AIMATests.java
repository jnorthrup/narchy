package nars.nal.nal5;

import com.google.common.math.PairedStatsAccumulator;
import jcog.io.SparkLine;
import jcog.list.FasterList;
import nars.*;
import nars.term.Term;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static nars.time.Tense.ETERNAL;
import static nars.truth.TruthFunctions.c2wSafe;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AIMATests {


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

    @ParameterizedTest
    @ValueSource(doubles = {0.01, 0.02, 0.05, 0.1, 0.2, 0.25, 0.5})
    void testAIMAExample(double truthRes) throws Narsese.NarseseException {
        final NAR n = NARS.tmp(6);

        n.termVolumeMax.set(18);
        n.freqResolution.set((float) truthRes);

        n.believe("(P ==> Q)",
                "((L && M) ==> P)",
                "((B && L) ==> M)",
                "((A && P) ==> L)",
                "((A && B) ==> L)",
                "A",
                "B");

        assertBelief(n, true, "Q", 1750);

    }

    @Test
    void testWeaponsDomain() throws Narsese.NarseseException {
        final NAR n = NARS.tmp(6);

        n.freqResolution.set(0.1f);
        n.confMin.set(0.02f);

        n.questionPriDefault.set(0.8f);

        n.termVolumeMax.set(24);
//        n.log();


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


        n.question($.$(
                "Criminal:?x"
                //"Criminal(?x)"

        ), ETERNAL, (q, a) -> {
            System.out.println(a);
        });


        n.run(3500);
        n.synch();


        Task y = n.belief($.$("Criminal(West)"));
        if (y == null) {

            n.belief($.$("Criminal(West)"));
        }
        assertNotNull(y);

    }


}
