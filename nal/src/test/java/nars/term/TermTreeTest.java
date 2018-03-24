package nars.term;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.index.term.TermKey;
import nars.index.term.TermTree;
import nars.index.term.TreeConceptIndex;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.function.Function;

import static nars.$.$;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by me on 10/14/16.
 */
public class TermTreeTest {

    @Test
    public void testAtomInsertion() throws Narsese.NarseseException {

        TermTree tree = new TermTree();

        Function<Term, Term> cb = (t)->t;

        tree.computeIfAbsent(TermKey.termByVolume($("concept")), cb);
        tree.computeIfAbsent(TermKey.termByVolume($("term")), cb);
        tree.computeIfAbsent(TermKey.termByVolume($("termutator")), cb);
        //tree.print(System.out);

        assertNotNull(tree.get(TermKey.termByVolume($("term"))));
        assertNull(tree.get(TermKey.termByVolume($("xerm"))));
        assertNull(tree.get(TermKey.termByVolume($("te")))); //partial

        assertNotNull(tree.computeIfAbsent(TermKey.termByVolume($("term")), cb));
        assertEquals(3, tree.size());

        assertNotNull(tree.computeIfAbsent(TermKey.termByVolume($("termunator")), cb));

        tree.prettyPrint(System.out);

        assertEquals(4, tree.size());


//        String stringWithUnicode = "unicode\u00easomething";
//        assertNull( tree.resolveOrAdd(stringWithUnicode)); //unicode not supported yet

    }


    @Test
    public void testCompoundInsertion() throws Narsese.NarseseException {

        TreeConceptIndex index;
        NAR nar = new NARS().index(
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