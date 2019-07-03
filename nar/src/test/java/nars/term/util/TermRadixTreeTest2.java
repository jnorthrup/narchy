package nars.term.util;

import nars.NARS;
import nars.Narsese;
import nars.memory.RadixTreeMemory;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static nars.$.$;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TermRadixTreeTest2 {

    @Test
    void testCompoundInsertion() throws Narsese.NarseseException {

        RadixTreeMemory index;
        new NARS().index(
                index = new RadixTreeMemory(1000)
        ).get();


        int preSize = index.size();

        String[] terms = {
                "x", "y", "z",
                "(x)",
                "(x,y)", "(x,z)",
                "(x --> z)", "(x <-> z)",
                "(x&&z)"
        };
        HashSet<Term> input = new HashSet();
        for (String s : terms) {
            Term x = $(s).concept();
            input.add(x);

            @Nullable Termed y = index.get(x, true);

//            System.out.println(index.concepts.prettyPrint());

            assertEquals(x.concept(), y.term(),
                    ()->y + " is " + y.getClass() + " and should have target equal to " + x.concept());
        }

        assertEquals(terms.length + preSize, index.size());

        //check again
        for (Term x : input)
            assertEquals(x.concept(), index.get(x,false).term());


        System.out.println(index.concepts.prettyPrint());
        index.print(System.out);








    }
}
