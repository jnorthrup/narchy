package nars.nal.nal8;

import nars.$;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.time.Tense;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static nars.$.$;
import static nars.Op.GOAL;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by me on 12/26/15.
 */
class QuestTest {


    @Test
    void testQuestAfterGoal1() throws Narsese.NarseseException {
        testQuest(true, 0, 512);
    }
    @Test
    void testQuestAfterGoal2() throws Narsese.NarseseException {
        testQuest(true, 1, 256);
    }
    @Test
    void testQuestAfterGoal3() throws Narsese.NarseseException {
        testQuest(true, 2, 256);
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

        final NAR nar = NARS.tmp(1);
        

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
        nar.onTask(a -> {
            if (a.punc()==GOAL && a.toString().contains("(b-->a)!"))
                valid.set(true);
        });

        nar.quest($("a:?b@"));
    }

    private void goal(NAR nar) throws Narsese.NarseseException {
        nar.want($.$("a:b"), Tense.Eternal, 1.0f, 0.9f);
    }


}
