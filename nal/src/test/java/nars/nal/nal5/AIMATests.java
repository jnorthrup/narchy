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

import static nars.Op.BELIEF;
import static nars.Op.QUESTION;
import static nars.time.Tense.ETERNAL;
import static nars.truth.TruthFunctions.c2wSafe;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AIMATests {


    final NAR n = NARS.tmp(6);


    @ParameterizedTest
    @ValueSource(doubles = { 0.01, 0.02, 0.05, 0.1, 0.2, 0.25, 0.5 })
    public void testAIMAExample(double truthRes) throws Narsese.NarseseException {

        n.freqResolution.set((float)truthRes);

        n.believe("(P ==> Q)",
                "((L && M) ==> P)",
                "((B && L) ==> M)",
                "((A && P) ==> L)",
                "((A && B) ==> L)",
                "A",
                "B");

        assertBelief(true, "Q", 4000);

    }

    @Test
    public void testWeaponsDomain() throws Narsese.NarseseException {

        n.freqResolution.set(0.1f);
        n.confResolution.set(0.02f);
//        n.confMin.set(0.02f);
        n.priDefault(QUESTION, 0.9f);
        n.priDefault(BELIEF, 0.7f);
        n.termVolumeMax.set(40);
        //n.conceptActivation.set(0.5f);

        //new QuerySpider(n);
        //new PrologCore(n);
        //n.run(1);

//        n.log();
        n.believe(
            //"((&&, American($x),Weapon(#y),Sells($x,#y,#z),Hostile(#z)) ==> Criminal($x))",
            "((&&,Weapon(#y),Sells($x,#y,#z),Hostile(#z)) ==> Criminal($x))",
            "Owns(Nono, M1)",
            "Missile(M1)",
            "((Missile($x) && Owns(Nono,$x)) ==> Sells(West,$x,Nono))",
            "(Missile($x) ==> Weapon($x))",
            "(Enemy($x,America) ==> Hostile($x))",
            "American(West)",
            "Enemy(Nono,America)"
        );

//        n.run(20);
//        n.clear();
        //n.log();



//        Set<Task> questions = new LinkedHashSet();
//        n.onTask(x -> {
//           if (x.isQuestion() && !x.isInput()) {
//               questions.add(x);
//           }
//        });


//        n.input("Criminal(?x)?");
//        n.input("Criminal(?x)?");
//                n.input("Criminal(?x)?");

        n.run(3500);
        //n.clear();
        n.question($.$(
                //"Criminal(?x)"
                "Criminal:?x"

        ), ETERNAL, (q,a)->{
            System.out.println(a);
        });
        n.run(1);



        n.run(5500);
        n.concept($.$("Criminal")).print();
        n.concept($.$("Criminal:?1")).print();
//        if (!questions.isEmpty()) {
//            System.out.println("Questions Generated:");
//            questions.forEach(System.out::println);
//        }

        Task y = n.belief($.$("Criminal(West)"));
        assertNotNull(y);

    }


    void assertBelief(boolean expcted, String x, int time) {

        final int metricPeriod = 150;

        PairedStatsAccumulator timeVsConf = new PairedStatsAccumulator();

        float symConf = 0;
        Task y = null;
        List<Float> evis = new FasterList();
        for (int i = 0; i < time; i += metricPeriod) {
            n.run(metricPeriod);

            y = n.belief($.the(x));
            if (y != null) {
                symConf = y.conf();
            }

            evis.add(c2wSafe(symConf, 1));
            timeVsConf.add(n.time(), symConf);
        }


        assertNotNull(y);
        assertTrue(y.isPositive() == expcted && y.polarity() > 0.5f);

        for (char c : "ABLMPQ".toCharArray()) {
            Term t = $.the(String.valueOf(c));
            Task cc = n.belief($.the(c));
            System.out.println(cc);
        }
        System.out.println(timeVsConf.yStats());
        System.out.println(
                SparkLine.renderFloats(evis, 8)
        );
    }

//    @Test(expected = IllegalArgumentException.class)
//    public void testKBWithNonDefiniteClauses() {
//        KnowledgeBase kb = new KnowledgeBase();
//        "P => Q");
//        "L & M => P");
//        "B & L => M");
//        "~A & P => L"); // Not a definite clause
//        "A & B => L");
//        "A");
//        "B");
//        PropositionSymbol q = (PropositionSymbol) parser.parse("Q");
//
//        Assert.assertEquals(true, plfce.plfcEntails(kb, q));
//    }
}
