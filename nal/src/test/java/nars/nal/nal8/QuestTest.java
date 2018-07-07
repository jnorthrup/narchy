package nars.nal.nal8;

import nars.$;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.time.Tense;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static nars.$.$;
import static nars.Op.QUEST;
import static nars.time.Tense.ETERNAL;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by me on 12/26/15.
 */
class QuestTest {


    @Test
    void testQuestAfterGoal1() throws Narsese.NarseseException {
        testQuest(true, 0, 256);
    }
    @Test
    void testQuestAfterGoal2() throws Narsese.NarseseException {
        testQuest(true, 1, 256);
    }
    @Test
    void testQuestAfterGoal3() throws Narsese.NarseseException {
        testQuest(true, 4, 256);
    }

    @Test
    void testQuestBeforeGoal() throws Narsese.NarseseException {
        testQuest(false, 1, 32);
        testQuest(false, 4, 32);
    }
    @Test
    void testQuestBeforeGoal0() throws Narsese.NarseseException {
        testQuest(false, 0, 64);
    }


    private void testQuest(boolean goalFirst, int timeBetween, int timeAfter) throws Narsese.NarseseException {
        
        final NAR nar = NARS.tmpEternal();
        

        AtomicBoolean valid = new AtomicBoolean(false);

        if (goalFirst) {
            goal(nar);
            nar.run(timeBetween);
            quest(nar, valid);
        } else {
            quest(nar, valid);
            nar.run(timeBetween);
            goal(nar);
        }

        nar.run(timeAfter);

        assertTrue(valid.get());
    }

    private void quest(NAR nar, AtomicBoolean valid) throws Narsese.NarseseException {
        nar.question($("a:?b@"), ETERNAL, QUEST, (q, a) -> {
            
            
            if (a.toString().contains("(b-->a)!"))
                valid.set(true);
        });
    }

    private void goal(NAR nar) throws Narsese.NarseseException {
        nar.want($.$("a:b"), Tense.Eternal, 1.0f, 0.9f);
    }


}
