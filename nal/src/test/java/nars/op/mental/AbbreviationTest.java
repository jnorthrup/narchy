package nars.op.mental;

import jcog.data.list.FasterList;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.concept.Concept;
import nars.term.Term;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AbbreviationTest {

    @Test
    void test1() throws Narsese.NarseseException {
        NAR n = NARS.tmp();
        n.log();
        List<Term> abbrSeq = new FasterList();
        new Abbreviation("z", 3, 5, n) {
            @Override
            protected void onAbbreviated(Term term) {
                super.onAbbreviated(term);
                abbrSeq.add(term);
            }
        };
        for (int i = 0; i < 5; i++) {
            n.believe("((x,y)-->a" + i + ")");
        }
        n.run(6);

        System.out.println("abbreviation sequence:\n\t" + abbrSeq);

        //1. abbrSeq should start with (x,y) due to how many repetitions of it that it saw
        assertEquals("(x,y)", abbrSeq.get(0).toString());
        //2. there should be no repeats in abbrSeq
        assertEquals(new HashSet<>(abbrSeq).size(), abbrSeq.size());

        Concept xy = n.concept("z1");
        assertNotNull(xy);

        //3. z1 should have priority equal to or greater than (x,y)

        //4. TODO z1 ( aka (x,y) ) should have at least N tasklinks, borrowed from (x,y) and rewritten
        //5. TODO z1's beliefs proxied
    }


}