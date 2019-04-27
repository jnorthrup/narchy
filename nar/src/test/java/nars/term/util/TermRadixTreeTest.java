package nars.term.util;

import jcog.data.byt.AbstractBytes;
import nars.NARS;
import nars.Narsese;
import nars.memory.RadixTreeMemory;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atomic;
import nars.term.util.map.TermRadixTree;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static nars.$.$;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by me on 10/14/16.
 */
class TermRadixTreeTest {

    @Test
    void testAtomInsertion() throws Narsese.NarseseException {

        TermRadixTree tree = new TermRadixTree();

        AbstractBytes s4 = TermRadixTree.termByVolume($("concept"));
        tree.putIfAbsent(s4, (Atomic.the(s4.toString())));
        AbstractBytes s3 = TermRadixTree.termByVolume($("target"));
        tree.putIfAbsent(s3, (Atomic.the(s3.toString())));
        AbstractBytes s2 = TermRadixTree.termByVolume($("termutator"));
        tree.putIfAbsent(s2, (Atomic.the(s2.toString())));


        assertNotNull(tree.get(TermRadixTree.termByVolume($("target"))));
        assertNull(tree.get(TermRadixTree.termByVolume($("xerm"))));
        assertNull(tree.get(TermRadixTree.termByVolume($("te"))));

        AbstractBytes s1 = TermRadixTree.termByVolume($("target"));
        assertNotNull(tree.putIfAbsent(s1, (Atomic.the(s1.toString()))));
        assertEquals(3, tree.size());

        AbstractBytes s = TermRadixTree.termByVolume($("termunator"));
        assertNotNull(tree.putIfAbsent(s, (Atomic.the(s.toString()))));

        tree.prettyPrint(System.out);

        assertEquals(4, tree.size());





    }


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