package nars.term.util;

import jcog.data.byt.AbstractBytes;
import nars.Narsese;
import nars.term.atom.Atomic;
import nars.term.util.map.TermRadixTree;
import org.junit.jupiter.api.Test;

import static nars.$.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by me on 10/14/16.
 */
class TermRadixTreeTest {

    @Test
    void testAtomInsertion() throws Narsese.NarseseException {

        TermRadixTree tree = new TermRadixTree();

        AbstractBytes s4 = TermRadixTree.termByVolume(INSTANCE.$("concept"));
        tree.putIfAbsent(s4, (Atomic.the(s4.toString())));
        AbstractBytes s3 = TermRadixTree.termByVolume(INSTANCE.$("target"));
        tree.putIfAbsent(s3, (Atomic.the(s3.toString())));
        AbstractBytes s2 = TermRadixTree.termByVolume(INSTANCE.$("termutator"));
        tree.putIfAbsent(s2, (Atomic.the(s2.toString())));


        assertNotNull(tree.get(TermRadixTree.termByVolume(INSTANCE.$("target"))));
        assertNull(tree.get(TermRadixTree.termByVolume(INSTANCE.$("xerm"))));
        assertNull(tree.get(TermRadixTree.termByVolume(INSTANCE.$("te"))));

        AbstractBytes s1 = TermRadixTree.termByVolume(INSTANCE.$("target"));
        assertNotNull(tree.putIfAbsent(s1, (Atomic.the(s1.toString()))));
        assertEquals(3, tree.size());

        AbstractBytes s = TermRadixTree.termByVolume(INSTANCE.$("termunator"));
        assertNotNull(tree.putIfAbsent(s, (Atomic.the(s.toString()))));

        tree.prettyPrint(System.out);

        assertEquals(4, tree.size());





    }



}