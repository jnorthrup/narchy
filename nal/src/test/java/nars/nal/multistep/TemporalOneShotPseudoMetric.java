package nars.nal.multistep;

import jcog.random.XoRoShiRo128PlusRandom;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.Task;
import nars.task.ActiveQuestionTask;
import nars.term.Term;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;

import static java.lang.String.format;
import static nars.$.$$;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * temporal metric to test and quantify the capability of a NARS implementation to retain a temporal relationship it had learned a long time ago with events.
 * https://github.com/opennars/opennars/blob/01c0a6242acc9a1a6fe12533c4a55ec3481fbfd1/src/test/java/org/opennars/metrics/TemporalOneShotPseudoMetric.java
 */
@Disabled
public class TemporalOneShotPseudoMetric implements BiConsumer<ActiveQuestionTask, Task> {
    private Random rng = new XoRoShiRo128PlusRandom(1);

    public int numberOfTermNames = 500;


    private List<String> termNames = new ArrayList<>();
    {
        // generate set of random target names
        for (int i=0;i<numberOfTermNames;i++) {
            termNames.add(createRandomString(4, rng));
        }
    }


    private boolean success = false;

    @Test
    void test1() throws Narsese.NarseseException {
        NAR nar = NARS.tmp();

        assertTrue(new TemporalOneShotPseudoMetric().spam(5, nar));

    }

    private boolean spam(int randomEvents, NAR nar) throws Narsese.NarseseException {

        success = false;

        //nar.log();

        nar.termVolumeMax.set(10);

        float lowPri = 0.01f, hiPri = 0.9f;
        nar.beliefPriDefault.set(hiPri);
        //nar.questionPriDefault.set(hiPri);

        // one shot learned knowledge
        long lightningAt = nar.time();
        nar.input("seen:flash. |");
        nar.run(1);
        nar.input("B:b. |");
        nar.run(1);
        nar.input("observed:spam. |");
        nar.run(1);
        nar.input("heard:thunder. |");
        nar.run(1);

        // feed the NAR with random events

        for (int i=0;i<randomEvents;i++) {
            final String a = randomTerm(), b = randomTerm();
            if (!a.equals(b)) {
                nar.input("$" + lowPri + " " +
                        //format("<%s-->%s>", a, b)
                        format("%s", a)
                        + ". |"
                );
                nar.run(1);
            }
        }


        // check if NAR still knows the one shot knowledge
        Term q = $$(
                //"(((( seen:flash &&+- ?1) &&+- heard:thunder) &&+- ?2) ==> ?3)" //volume 11
                "((( seen:flash &&+- ?1) &&+- heard:thunder) &&+- ?2)" //volume 9
        );
        nar.question(q, lightningAt, this);

        /// give NAR enough time to reason
        nar.run(15000);

        return success;

    }

    public String randomTerm() {
        return termNames.get(rng.nextInt(termNames.size()));
    }

    private static String createRandomString(final int length, Random rng) {
        String res = "";

        for (int i=0;i<length;i++)
            res += (char)('a' + rng.nextInt(26));

        return res;
    }

    @Override
    public void accept(ActiveQuestionTask activeQuestionTask, Task task) {
        System.out.println(task);
        success = true;
    }
}
