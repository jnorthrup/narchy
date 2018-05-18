package nars.nal.nal6;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.time.Tense;
import nars.truth.Truth;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by me on 10/29/16.
 */
public class NAL6MultistepTest {


    /** https://dtai.cs.kuleuven.be/problog/tutorial/basic/02_bayes.html */
    @Test public void testBurglarEarthquake1() throws Narsese.NarseseException {
//        0.7::burglary.
//        0.2::earthquake.
//        0.9::p_alarm1.
//        0.8::p_alarm2.
//        0.1::p_alarm3.
//
//        alarm :- burglary, earthquake, p_alarm1.
//                alarm :- burglary, \+earthquake, p_alarm2.
//                alarm :- \+burglary, earthquake, p_alarm3.
//
//                evidence(alarm,true).
//
//                queryburglary.
//                queryearthquake.

        NAR n = NARS.tmp();

        //d.log();
        n.input(
                "burglary. %0.7;0.9%",
                "earthquake. %0.2;0.9%",
                "p_alarm1. %0.9;0.9%",
                "p_alarm2. %0.8;0.9%",
                "p_alarm3. %0.1;0.9%",
                "((&&, burglary, earthquake, p_alarm1) ==> alarm). %1.0;0.95%",
                "((&&, burglary, --earthquake, p_alarm2) ==> alarm). %1.0;0.95%",
                "((&&, --burglary, earthquake, p_alarm3) ==> alarm). %1.0;0.95%",
                "alarm.",
                "burglary?",
                "earthquake?"
        );


        Concept burglary = null, earthquake = null;
        for (int i = 0; i < 5; i++) {
            //long now = d.time();
            n.run(100);
            burglary = n.conceptualize("burglary");
            earthquake = n.conceptualize("earthquake");            // burglary.print();  earthquake.print();
            //System.out.println("burglary=" + burglary.belief(Tense.ETERNAL,0, nar) + "\tearthquake=" + earthquake.belief(Tense.ETERNAL,0, nar));
        }

        n.stats(System.out);

        //result from Probcog:  earthquake=23%, burglary=99%
        Truth burgTruth = n.beliefTruth(burglary, Tense.ETERNAL);
        assertEquals(0.65f, burgTruth.freq(), 0.1f /* approximate */);
        Truth eqTruth = n.beliefTruth(earthquake, Tense.ETERNAL);
        assertEquals(0.31f, eqTruth.freq(), 0.2f /* approximate */);
    }


    /** https://dtai.cs.kuleuven.be/problog/tutorial/basic/02_bayes.html */
    @Test public void testBurglarEarthquake2() throws Narsese.NarseseException {
//        0.7::burglary.
//        0.2::earthquake.
//
//        0.9::alarm :- burglary, earthquake.
//        0.8::alarm :- burglary, \+earthquake.
//        0.1::alarm :- \+burglary, earthquake.
//
//                evidence(alarm,true).
//                queryburglary.
//                queryearthquake.

        NAR n = new NARS().get();

        //d.log();
        n.input(
                "burglary.   %0.7;0.9%",
                "earthquake. %0.2;0.9%",
                "((&&, burglary, earthquake) ==> alarm).      %0.9;0.9%",
                "((&&, burglary, (--,earthquake)) ==> alarm). %0.8;0.9%",
                "((&&, (--,burglary), earthquake) ==> alarm). %0.1;0.9%",
                "alarm.",
                "burglary?",
                "earthquake?"
        );

        TaskConcept burglary = null, earthquake = null;
        for (int i = 0; i < 5; i++) {
            // burglary.print();  earthquake.print();
            //long now = d.time();
            n.run(100);
            burglary = (TaskConcept) n.conceptualize("burglary");
            earthquake = (TaskConcept)n.conceptualize("earthquake");            // burglary.print();  earthquake.print();
            System.out.println("burglary=" + n.beliefTruth(burglary,0) + "\tearthquake=" + earthquake.beliefs().truth(Tense.ETERNAL, n));
        }

        //result from Probcog:  earthquake=23%, burglary=99%
        assertEquals(0.99f, n.beliefTruth(burglary, 0).freq(), 0.33f /* approximate */);
        assertEquals(0.23f, n.beliefTruth(earthquake, 0).freq(), 0.1f /* approximate */);




    }


//    /** https://dtai.cs.kuleuven.be/problog/tutorial/basic/02_bayes.html */
//    @Test public void testGraphProbabality() {
////        0.6::edge(1,2).
////        0.1::edge(1,3).
////        0.4::edge(2,5).
////        0.3::edge(2,6).
////        0.3::edge(3,4).
////        0.8::edge(4,5).
////        0.2::edge(5,6).
////        path(X,Y) :- edge(X,Y).
////                path(X,Y) :- edge(X,Z),
////                Y \== Z,
////                path(Z,Y).
////
////        query(path(1,5)).
////        query(path(1,6)).
//
//
//        Default d = new Default();
//        //d.logSummaryGT(System.out, 0.1f);
//        d.log();
//        d.input(
//                //"$0.5$ vertex:{x1,x2,x3,x4,x5,x6}.",
//                "$0.5$ edge(x1,x5)?",
//                "$0.5$ edge(x1,x6)?",
//                "$0.5$ edge(x1,x2). %0.6;0.9%",
//                "$0.5$ edge(x1,x3). %0.1;0.9%",
//                "$0.5$ edge(x2,x5). %0.4;0.9%",
//                "$0.5$ edge(x2,x6). %0.3;0.9%",
//                "$0.5$ edge(x3,x4). %0.3;0.9%",
//                "$0.5$ edge(x4,x5). %0.8;0.9%",
//                "$0.5$ edge(x5,x6). %0.2;0.9%",
//                //"(edge($x,$y) ==> path($x,$y)).",
//                //"((&&, edge($x,#z), path(#z,$y)) ==> path($x,$y)).",
//                "$0.5$ ((&&, edge($x,?z), edge(?z,$y)) <=> edge($x,$y))."
//                //"$0.5$ (edge($x,$y) <=> ($x <-> $y))."
//        );
//
//        Concept path15 = d.concept("edge(x1,x5)");
//        Concept path16 = d.concept("edge(x1,x6)");
//
//        for (int i = 0; i < 1; i++) {
//            path15.print();
//            path16.print();
//            //long now = d.time();
//            System.out.println(path15 + "=" + path15.belief(Tense.ETERNAL,0) + "\t" + path16 + "=" + path16.belief(Tense.ETERNAL,0));
//            d.run(100);
//        }
//
//
//        assertEquals(0.25, path15.belief(Tense.ETERNAL,0).freq(), 0.1f /* approximate */);
//        assertEquals(0.21f, path16.belief(Tense.ETERNAL,0).freq(), 0.1f /* approximate */);
//
//
//
//
//    };


}
