package nars.nal.nal8;

import jcog.data.list.FasterList;
import nars.*;
import nars.term.Term;
import nars.time.Tense;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

import static nars.$.$$;

/** test precision of sequence execution */
@Disabled
public class NAL8SequenceTest {

    @Test
    void test1() throws Narsese.NarseseException {

        String sequence = "(((f(a) &&+2 f(b)) &&+2 f(c)) &&+2 done)";
        String goal = "done";
        List<Term> log = new FasterList();

        NAR n = NARS.tmp();
        n.termVolumeMax.set(20);
        n.time.dur(4);
        n.log();

        n.onOp1("f", (x,nar)->{
            System.err.println(x);

            nar.want($.func("f", x).neg(), Tense.Present, 1f); //quench

            if (!log.isEmpty()) {
                Term prev = ((FasterList<Term>) log).getLast();
                if (prev.equals(x))
                    return; //same
                nar.believe($.func("f", prev).neg(), nar.time()); //TODO truthlet quench?
            }

            log.add(x);

            nar.believe($.func("f", x), nar.time());

            n.want($$(goal), Tense.Present, 1f); //reinforce
        });

        n.believe(sequence);
        //n.want($$(goal), Tense.Eternal /* Present */, 1f);
        n.want($$(goal), Tense.Present, 1f);
        n.run(1400);
    }
}
