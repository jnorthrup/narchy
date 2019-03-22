package nars.term.index;

import jcog.data.byt.ArrayBytes;
import jcog.tree.radix.MyRadixTree;
import nars.$;
import nars.Narsese;
import nars.index.concept.TreeMemory;
import nars.term.Term;
import nars.term.util.map.TermRadixTree;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static nars.$.$;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by me on 10/21/16.
 */
class TreeMemoryTest {

    @Test
    void testTermIndex() {
        TermRadixTree<Term> t = new TermRadixTree<>();
        byte[] a = t.key($.func("x", $.the("y"))).arrayCompactDirect();
        System.out.println(Arrays.toString(a));
        t.put($.func("x", $.the("y")), $.the(1));
        t.put($.func("x", $.the("z")), $.the(2));
        Term y = t.get(
                new ArrayBytes(a)
        );
        assertEquals($.the(1), y);

//        Term yy = t.get(new ConcatBytes(
//                    new ArrayBytes((byte)2, (byte)2, (byte)8, (byte)1),
//                    new ArrayBytes((byte)0, (byte)0, (byte)1, (byte)121,
//                                   (byte)0, (byte)0, (byte)1, (byte)120
//                    )
//                ));
//        assertEquals($.the(1), yy);


        System.out.println(t.prettyPrint());

    }

    @Test
    void testVolumeSubTrees() throws Narsese.NarseseException {
        TreeMemory t = new TreeMemory( 128);
        t.set($("a"));
        t.set($("(a)"));
        t.set($("(a-->b)"));
        t.set($("(a-->(b,c,d))"));
        t.set($("(a-->(b,c,d,e,f,g))"));
        t.set($("(a-->(b,c,d,e,f,g,h,i,j,k))"));
        t.concepts.prettyPrint(System.out);
        t.print(System.out);
        assertEquals(6, t.size());
        System.out.println(t.concepts.root);

        
        List<MyRadixTree.Node> oe = t.concepts.root.getOutgoingEdges();
        assertEquals(6, oe.size());

        assertTrue(oe.get(0).toString().length() < oe.get(1).toString().length());
        assertTrue(oe.get(0).toString().length() < oe.get(oe.size()-1).toString().length());
    }
}