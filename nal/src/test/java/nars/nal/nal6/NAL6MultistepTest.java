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
    @Test
    void testBurglarEarthquake1() throws Narsese.NarseseException {















        NAR n = NARS.tmp();

        
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
            
            n.run(100);
            burglary = n.conceptualize("burglary");
            earthquake = n.conceptualize("earthquake");            
            
        }

        n.stats(System.out);

        
        Truth burgTruth = n.beliefTruth(burglary, Tense.ETERNAL);
        assertEquals(0.65f, burgTruth.freq(), 0.1f /* approximate */);
        Truth eqTruth = n.beliefTruth(earthquake, Tense.ETERNAL);
        assertEquals(0.31f, eqTruth.freq(), 0.2f /* approximate */);
    }


    /** https://dtai.cs.kuleuven.be/problog/tutorial/basic/02_bayes.html */
    @Test
    void testBurglarEarthquake2() throws Narsese.NarseseException {











        NAR n = new NARS().get();

        
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
            
            
            n.run(100);
            burglary = (TaskConcept) n.conceptualize("burglary");
            earthquake = (TaskConcept)n.conceptualize("earthquake");            
            System.out.println("burglary=" + n.beliefTruth(burglary,0) + "\tearthquake=" + earthquake.beliefs().truth(Tense.ETERNAL, n));
        }

        
        assertEquals(0.99f, n.beliefTruth(burglary, 0).freq(), 0.33f /* approximate */);
        assertEquals(0.23f, n.beliefTruth(earthquake, 0).freq(), 0.1f /* approximate */);




    }





























































}
