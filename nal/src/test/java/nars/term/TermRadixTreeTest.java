package nars.term;

import jcog.data.byt.AbstractBytes;
import nars.NARS;
import nars.Narsese;
import nars.index.concept.TreeConceptIndex;
import nars.term.atom.Atomic;
import nars.util.term.TermBytes;
import nars.util.term.TermRadixTree;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static nars.$.$;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by me on 10/14/16.
 */
public class TermRadixTreeTest {

    @Test
    public void testAtomInsertion() throws Narsese.NarseseException {

        TermRadixTree tree = new TermRadixTree();

        AbstractBytes s4 = TermBytes.termByVolume($("concept"));
        tree.putIfAbsent(s4, (Atomic.the(s4.toString())));
        AbstractBytes s3 = TermBytes.termByVolume($("term"));
        tree.putIfAbsent(s3, (Atomic.the(s3.toString())));
        AbstractBytes s2 = TermBytes.termByVolume($("termutator"));
        tree.putIfAbsent(s2, (Atomic.the(s2.toString())));
        //tree.print(System.out);

        assertNotNull(tree.get(TermBytes.termByVolume($("term"))));
        assertNull(tree.get(TermBytes.termByVolume($("xerm"))));
        assertNull(tree.get(TermBytes.termByVolume($("te")))); //partial

        AbstractBytes s1 = TermBytes.termByVolume($("term"));
        assertNotNull(tree.putIfAbsent(s1, (Atomic.the(s1.toString()))));
        assertEquals(3, tree.size());

        AbstractBytes s = TermBytes.termByVolume($("termunator"));
        assertNotNull(tree.putIfAbsent(s, (Atomic.the(s.toString()))));

        tree.prettyPrint(System.out);

        assertEquals(4, tree.size());


//        String stringWithUnicode = "unicode\u00easomething";
//        assertNull( tree.resolveOrAdd(stringWithUnicode)); //unicode not supported yet

    }


    @Test
    public void testCompoundInsertion() throws Narsese.NarseseException {

        TreeConceptIndex index;
        new NARS().index(
            index = new TreeConceptIndex(1000)
        ).get();


        int preSize = index.size();

        String[] terms = {
                "x",
                "(x)", "(xx)", "(xxx)",
                "(x,y)", "(x,z)",
                "(x --> z)", "(x <-> z)",
                "(x&&z)"
        };
        HashSet<Term> input = new HashSet();
        for (String s : terms) {
            @NotNull Term ts = $(s);
            input.add(ts);
            index.get(ts, true);
        }

        assertEquals(terms.length + preSize, index.size());

        for (Term x : input)
            assertNotNull(index.get(x,false));


        //Set<Termed> stored = StreamSupport.stream(index.concepts.spliterator(), false).collect(Collectors.toSet());

        //assertEquals(Sets.symmetricDifference(input, stored) + " = difference", input, stored);

        index.concepts.prettyPrint();
        index.print(System.out);

//        String stringWithUnicode = "unicode\u00easomething";
//        assertNull( tree.resolveOrAdd(stringWithUnicode)); //unicode not supported yet

    }

}