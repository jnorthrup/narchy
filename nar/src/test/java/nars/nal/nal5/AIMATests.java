package nars.nal.nal5;

import com.google.common.math.PairedStatsAccumulator;
import jcog.io.SparkLine;
import nars.*;
import nars.term.Term;
import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectIntHashMap;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static nars.$.$$;
import static nars.time.Tense.ETERNAL;
import static nars.truth.func.TruthFunctions.c2wSafe;
import static org.junit.jupiter.api.Assertions.*;

class AIMATests {


    @ParameterizedTest
    @ValueSource(doubles = {0.01, 0.05, 0.1, 0.25, 0.5})
    void testAIMAExample(double truthRes) throws Narsese.NarseseException {
        final NAR n = NARS.tmp(6);

        n.termVolMax.set(5);
        n.freqResolution.set((float) truthRes);
        n.confMin.set(0.05f);
//        n.attn.decay.set(0.1f);
        n.confResolution.set(0.1f);

//        n.questPriDefault.set(0.8f);
//        n.log();

        ObjectIntHashMap<Term> terms = new ObjectIntHashMap();
        n.onTask(t -> {
            terms.addToValue(t.term(), 1);
        });

        n.believe(
                "(P ==> Q)",
                "((L && M) ==> P)",
                "((B && L) ==> M)",
                "((A && P) ==> L)",
                "((A && B) ==> L)",
                "A",
                "B");

        n.question($$("Q"));

        try {
            assertBelief(n, true, "Q", (int) (NAL.test.TIME_MULTIPLIER * 1500));
        } finally {
            terms.keyValuesView().toSortedListBy(x -> -x.getTwo()).forEach(t -> {
                System.out.println(t);
            });
        }

    }

    @Test
    void testWeaponsDomain() throws Narsese.NarseseException {

        final NAR n = NARS.tmp();

//        n.freqResolution.set(0.25f);
//        n.confResolution.set(0.1f);
//        n.confMin.set(0.1f);

//        n.beliefPriDefault.set(0.5f);
//        n.questionPriDefault.set(0.5f);

        assertEquals(20, $$("((&&,Weapon(#y),Sells($x,#y,#z),Hostile(#z)) ==> Criminal($x))").volume());

        n.termVolMax.set(21);
        //n.log();

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

        n.run(5000);
//        n.synch();

//        Concept qc = n.concept(Q);
//        assertNotNull(qc)
//        qc.print();

        Task y = n.belief($.$("Criminal(West)"));
        assertNotNull(y);

    }

    private static void assertBelief(NAR n, boolean expcted, String x, int time) {

        final int metricPeriod = time / 4;

        PairedStatsAccumulator timeVsConf = new PairedStatsAccumulator();


        FloatArrayList evis = new FloatArrayList();
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
                SparkLine.renderFloats(evis)
        );
    }


}
